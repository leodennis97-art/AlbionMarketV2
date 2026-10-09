package com.example.albionmarketv2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.albionmarketv2.ui.theme.AlbionMarketV2Theme
import android.text.TextUtils

import androidx.core.content.ContextCompat

class BotSetupActivity : ComponentActivity() {

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenScannerService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
                putExtra("WORKFLOW_ID", "wf_1")
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "Ablauf gestartet!", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Screen Capture Berechtigung verweigert.", Toast.LENGTH_SHORT).show()
        }
    }

    private val screenshotCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, FloatingCropService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "Crop-Tool-Button aktiviert! Gehe ins Spiel.", Toast.LENGTH_LONG).show()
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Screen Capture Berechtigung verweigert.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AlbionMarketV2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "DataPro Auto-Bot Setup",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(onClick = { checkAndRequestAccessibility() }) {
                            Text("1. Accessibility Service aktivieren")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { startScreenCapture() }) {
                            Text("2. Bot Scanner starten (Screen Capture)")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { startScreenshotForCrop() }) {
                            Text("3. Ziel-Bild aus Screenshot ausschneiden")
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Hier wird einfach nur ein Test-Ablauf direkt gestartet, 
                        // der alle Templates der Reihe nach abarbeitet:
                        Button(onClick = {
                            val templates = TemplateManager.getTemplates(this@BotSetupActivity)
                            if (templates.isEmpty()) {
                                Toast.makeText(this@BotSetupActivity, "Bitte erst Bilder ausschneiden!", Toast.LENGTH_SHORT).show()
                            } else {
                                val steps = templates.map { BotStep(it.id, 2000L) }
                                val workflow = BotWorkflow("wf_1", "Automatischer Ablauf", steps)
                                TemplateManager.saveWorkflows(this@BotSetupActivity, listOf(workflow))
                                
                                val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Text("4. Ablauf starten (Alle Templates)")
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(this@BotSetupActivity, ScreenScannerService::class.java).apply {
                                    action = "STOP"
                                }
                                startService(intent)
                                Toast.makeText(this@BotSetupActivity, "Bot gestoppt", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Bot Scanner stoppen")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Android 13+ Restricted Settings Warning Note
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ℹ️ Wichtiger Hinweis zu Android 13/14:", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 12.sp)
                                Text("Falls der Schalter für den Dienst in den Einstellungen ausgegraut ist ('Eingeschränkte Einstellung'):\n1. Gehe in die Android-App-Info von DataPro (Einstellungen -> Apps).\n2. Tippe oben rechts auf die 3 Punkte.\n3. Wähle 'Eingeschränkte Einstellungen zulassen'.\n4. Aktiviere danach den Barrierefreiheitsdienst.", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestAccessibility() {
        if (!isAccessibilityServiceEnabled(this, AutoClickerService::class.java)) {
            Toast.makeText(this, "Bitte den AutoClickerService aktivieren.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Accessibility Service ist bereits aktiv!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScreenCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun startScreenshotForCrop() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenshotCropLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${accessibilityService.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        
        if (enabledServicesSetting == null) return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}