package com.example.albionmarketv2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.albionmarketv2.ui.theme.AlbionMarketV2Theme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    @SuppressLint("BatteryLife", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = BillingManager(this)

        // Initialize Server Config & Start 24/7 Persistent Server Sync Service
        ServerConfigManager.initServerConfig(this)
        PersistentServerSyncService.startService(this)

        // Setup Notification Channel & Request Notifications Permission
        NotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Automatic All Files Access / External Storage Permission Handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }

        // Request Ignore Battery Optimization
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        if ((powerManager != null) && (!powerManager.isIgnoringBatteryOptimizations(packageName))) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Floating Bubble Service is started only after login and when overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            try {
                if (AppPreferences(this).isUserLoggedIn && LicenseManager.isLicenseValid(this)) {
                    if (!FloatingBubbleService.isServiceRunning()) {
                        FloatingBubbleService.startService(this)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            AlbionMarketV2Theme {
                val context = LocalContext.current
                val prefs = remember { AppPreferences(context) }

                var isCompromised by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (DeviceSecurityManager.isDeviceCompromised(context)) {
                        isCompromised = true
                    }
                }

                if (isCompromised) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚨 SICHERHEITS-ALARM\n\nGeräteintegrität verletzt (Root / Tampering erkannt).\nAus Sicherheitsgründen gesperrt.",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    return@AlbionMarketV2Theme
                }

                // Enforce re-login / re-verification on every app launch
                LaunchedEffect(Unit) {
                    prefs.isUserLoggedIn = false
                }

                var isUserLoggedInState by remember { mutableStateOf(false) }
                var isUnlockedForSession by remember { mutableStateOf(false) }
                var showWelcomeDialog by remember { mutableStateOf(value = false) }

                // Live Popup Alert Handling for Admin Messages & Screen Alarm
                val currentPopupAlert = ServerSyncManager.activePopupAlert
                if (currentPopupAlert != null) {
                    LaunchedEffect(currentPopupAlert.id) {
                        if (currentPopupAlert.playAlarmSound) {
                            try {
                                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                                ringtone?.play()
                            } catch (_: Exception) {}
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { ServerSyncManager.activePopupAlert = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currentPopupAlert.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(currentPopupAlert.message, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                Text("Gesendet: ${currentPopupAlert.timestamp}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { ServerSyncManager.activePopupAlert = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Gelesen / Schließen", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Real-Time Background Server Authentication & Data Sync (Immediate + every 1s)
                LaunchedEffect(isUserLoggedInState) {
                    if (isUserLoggedInState) {
                        if (Settings.canDrawOverlays(context)) {
                            try {
                                if (!FloatingBubbleService.isServiceRunning()) {
                                    FloatingBubbleService.startService(context)
                                }
                            } catch (_: Exception) {}
                        }
                        while (isActive) {
                            withContext(Dispatchers.IO) {
                                ServerSyncManager.pingServer(context)
                                ServerSyncManager.testAndConnectToServer(context)
                            }
                            delay(2000.milliseconds)
                        }
                    } else {
                        try {
                            FloatingBubbleService.stopService(context)
                        } catch (_: Exception) {}
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Show Lockscreen Login when NOT logged in or license invalid
                    if (!isUserLoggedInState || !LicenseManager.isLicenseValid(context)) {
                        var usernameInput by remember { mutableStateOf(prefs.savedUsername) }
                        var savePasswordLocally by remember { mutableStateOf(prefs.savedPassword.isNotBlank()) }
                        var passwordInput by remember { mutableStateOf(if (savePasswordLocally) prefs.savedPassword else "") }
                        var captchaNum1 by remember { mutableIntStateOf((3..12).random()) }
                        var captchaNum2 by remember { mutableIntStateOf((2..9).random()) }
                        var captchaInput by remember { mutableStateOf("") }
                        var isCaptchaSolved by remember { mutableStateOf(value = false) }
                        var isServerConnected by remember { mutableStateOf<Boolean?>(null) }
                        var isAuthenticating by remember { mutableStateOf(value = false) }
                        var licenseKeyInput by remember { mutableStateOf("") }
                        var showLoginUpdatesDialog by remember { mutableStateOf(true) }

                        if (showLoginUpdatesDialog) {
                            AlertDialog(
                                onDismissRequest = { showLoginUpdatesDialog = false },
                                title = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🚀 Neueste Updates & News", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable { showLoginUpdatesDialog = false }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Willkommen bei AlbionDataPro v1.3.9!", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text("• v1.3.9: Blitzschneller 24/7 Cloud-Sync, Sicherheitssperre & automatisches OTA-Update.", fontSize = 12.sp, color = Color.White)
                                        Text("• v1.3.4: Live 24/7 Server-Sync, Anti-Cheat Schutz, Echtzeit-Uhrzeit & Gold-Bot Signale.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("Gib deine Zugangsdaten ein und verifiziere dich, um das Spiel zu betreten.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showLoginUpdatesDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Verstanden", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF1E293B)
                            )
                        }

                        LaunchedEffect(Unit) {
                            // Autonomous initial connection attempt immediately on launch
                            withContext(Dispatchers.IO) {
                                ServerSyncManager.testAndConnectToServer(context)
                            }
                            var failCount = 0
                            while (isActive) {
                                val stats = withContext(Dispatchers.IO) {
                                    var s = ServerSyncManager.pingServer(context)
                                    if (s == null) {
                                        ServerSyncManager.testAndConnectToServer(context)
                                        s = ServerSyncManager.pingServer(context)
                                    }
                                    s
                                }
                                withContext(Dispatchers.Main) {
                                    if (stats != null) {
                                        failCount = 0
                                        isServerConnected = true
                                    } else {
                                        failCount++
                                        if (failCount >= 3) { // Require 3 consecutive failures before dropping status
                                            isServerConnected = false
                                        }
                                    }
                                }
                                delay(1500.milliseconds)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = "🔒 Sperrbildschirm",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )

                                    val statusColor = when (isServerConnected) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> Color(0xFFF59E0B)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = statusColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, statusColor),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = statusColor,
                                                modifier = Modifier.size(8.dp),
                                            ) {}
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = when (isServerConnected) {
                                                    true -> "🟢 Server-Verbindung aktiv (Cloud)"
                                                    false -> "🔴 Keine Verbindung zur Cloud"
                                                    null -> "🟡 Suche Cloud-Server (Verbinde...)"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                            )
                                        }
                                    }

                                    if (isAuthenticating) {
                                        CircularProgressIndicator(color = Color(0xFF10B981), strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                    } else {
                                        OutlinedTextField(
                                            value = usernameInput,
                                            onValueChange = { usernameInput = it },
                                            label = { Text("Benutzername", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = { Text("Passwort", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            visualTransformation = PasswordVisualTransformation(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { savePasswordLocally = !savePasswordLocally }
                                        ) {
                                            Checkbox(
                                                checked = savePasswordLocally,
                                                onCheckedChange = { savePasswordLocally = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF10B981),
                                                    uncheckedColor = Color(0xFF94A3B8),
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "💾 Passwort lokal auf diesem Gerät speichern",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE2E8F0),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        OutlinedTextField(
                                            value = captchaInput,
                                            onValueChange = {
                                                captchaInput = it
                                                isCaptchaSolved = false
                                            },
                                            label = { Text("🤖 Mensch-Bestätigung: Was ist $captchaNum1 + $captchaNum2 ?", color = Color(0xFF38BDF8), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (isCaptchaSolved) Color(0xFF10B981) else Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        Button(
                                            onClick = {
                                                val expectedAnswer = captchaNum1 + captchaNum2
                                                if (captchaInput.trim().toIntOrNull() == expectedAnswer) {
                                                    isCaptchaSolved = true
                                                    Toast.makeText(context, "🟢 Mathe-Aufgabe korrekt gelöst!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isCaptchaSolved = false
                                                    Toast.makeText(context, "❌ Falsche Antwort! Neue Aufgabe wird erstellt.", Toast.LENGTH_SHORT).show()
                                                    captchaNum1 = (3..12).random()
                                                    captchaNum2 = (2..9).random()
                                                    captchaInput = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCaptchaSolved) Color(0xFF10B981) else Color(0xFF38BDF8),
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = if (isCaptchaSolved) "✅ Mathe-Aufgabe gelöst" else "🤖 Mathe-Aufgabe prüfen",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isCaptchaSolved) Color.White else Color.Black,
                                            )
                                        }

                                        OutlinedTextField(
                                            value = licenseKeyInput,
                                            onValueChange = { licenseKeyInput = it },
                                            label = { Text("🔑 Lizenzschlüssel eingeben", color = Color(0xFF8B5CF6), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF8B5CF6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        Button(
                                            onClick = {
                                                if (licenseKeyInput.isBlank()) {
                                                    Toast.makeText(context, "❌ Bitte einen Lizenzschlüssel eingeben", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val activated = LicenseManager.activateLicense(context, licenseKeyInput)
                                                if (activated) {
                                                    prefs.isUserLoggedIn = true
                                                    if (licenseKeyInput.uppercase(Locale.ROOT).contains("DNNX")) {
                                                        prefs.isAdmin = true
                                                        prefs.savedUsername = "dnnx"
                                                    }
                                                    isUserLoggedInState = true
                                                    isUnlockedForSession = true
                                                    Toast.makeText(context, "🟢 Lizenzschlüssel verifiziert! Erfolgreich eingeloggt.", Toast.LENGTH_SHORT).show()
                                                    licenseKeyInput = ""
                                                } else {
                                                    Toast.makeText(context, "❌ Ungültiger Lizenzschlüssel!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = "🔑 Mit Lizenzschlüssel einloggen & freischalten",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (!isCaptchaSolved) {
                                                    val expectedAnswer = captchaNum1 + captchaNum2
                                                    if (captchaInput.trim().toIntOrNull() == expectedAnswer) {
                                                        isCaptchaSolved = true
                                                    } else {
                                                        Toast.makeText(context, "❌ Bitte zuerst die Mathe-Aufgabe richtig lösen!", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                }

                                                val isInputAdmin = usernameInput.trim().equals("dnnx", ignoreCase = true)
                                                if (!isInputAdmin && !LicenseManager.isLicenseValid(context)) {
                                                    Toast.makeText(context, "❌ Keine gültige Lizenz vorhanden! Bitte zuerst aktivieren.", Toast.LENGTH_LONG).show()
                                                    return@Button
                                                }

                                                if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                                    Toast.makeText(context, "❌ Bitte Benutzername und Passwort eingeben", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                isAuthenticating = true
                                                lifecycleScope.launch {
                                                    val success = ServerSyncManager.loginWithServer(context, usernameInput, passwordInput)
                                                    isAuthenticating = false
                                                    if (success) {
                                                        prefs.isUserLoggedIn = true
                                                        prefs.savedUsername = usernameInput.trim()
                                                        if (savePasswordLocally) {
                                                            prefs.savedPassword = passwordInput.trim()
                                                        } else {
                                                            prefs.savedPassword = ""
                                                        }
                                                        if (usernameInput.trim().equals("dnnx", ignoreCase = true)) {
                                                            prefs.isAdmin = true
                                                        }
                                                        isUserLoggedInState = true
                                                        isUnlockedForSession = true
                                                        Toast.makeText(context, "🟢 Verifizierung erfolgreich! Willkommen.", Toast.LENGTH_SHORT).show()

                                                        if (ServerSyncManager.isOtaUpdateAvailable) {
                                                            Toast.makeText(context, "📲 Neues Update verfügbar! Lade AlbionDataPro.apk herunter...", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        prefs.isUserLoggedIn = false
                                                        isUserLoggedInState = false
                                                        isUnlockedForSession = false
                                                        Toast.makeText(context, "🔴 Verifizierung abgelehnt!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text("Verifizieren & App betreten", fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        TelegramLicensePurchaseSection(context = context)
                                    }
                                }
                            }
                        }
                    } else if (!isUnlockedForSession) {
                        LicenseActivationScreen(
                            billingManager = billingManager,
                        ) {
                            isUserLoggedInState = true
                            isUnlockedForSession = true
                            showWelcomeDialog = true
                            lifecycleScope.launch {
                                try {
                                    ServerSyncManager.pingServer(this@MainActivity)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else if (!ServerSyncManager.isServerConnected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(2.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Offline gesperrt",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(56.dp)
                                    )

                                    Text(
                                        text = "📡 SERVERVERBINDUNG UND INTERNET ERFORDERLICH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFEF4444),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "Die Nutzung der App erfordert eine aktive Internet- und Serververbindung.\n\nOhne aktive Serververbindung ist die Anwendung aus Sicherheitsgründen sofort gesperrt.\n\nSobald die Verbindung wiederhergestellt ist, wird die App automatisch freigeschaltet.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    CircularProgressIndicator(
                                        color = Color(0xFFEF4444),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Text(
                                        text = "Verbindung zum Server wird hergestellt...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        AlbionResourceScreen(
                            onLogout = {
                                prefs.isUserLoggedIn = false
                                isUserLoggedInState = false
                                isUnlockedForSession = false
                                try {
                                    FloatingBubbleService.stopService(this@MainActivity)
                                } catch (_: Exception) {}
                                Toast.makeText(this@MainActivity, "👋 Erfolgreich abgemeldet!", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (ServerSyncManager.isOtaUpdateAvailable) {
                            AlertDialog(
                                onDismissRequest = { ServerSyncManager.isOtaUpdateAvailable = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🚀 Neue Version verfügbar! (AlbionDataPro.apk)", fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Auf Localhost wurde eine neue Version deiner App bereitgestellt.")
                                        Text("📦 Paket: AlbionDataPro.apk", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text("Möchtest du die aktuellsten Änderungen jetzt herunterladen und installieren?")
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            ServerSyncManager.isOtaUpdateAvailable = false
                                            lifecycleScope.launch {
                                                Toast.makeText(this@MainActivity, "📥 Lade AlbionDataPro.apk herunter...", Toast.LENGTH_SHORT).show()
                                                val success = OtaUpdateManager.downloadAndInstallUpdate(this@MainActivity)
                                                if (!success) {
                                                    Toast.makeText(this@MainActivity, "❌ Download fehlgeschlagen. Bitte Server-Verbindung prüfen.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("⚡ Jetzt AlbionDataPro.apk aktualisieren & installieren", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { ServerSyncManager.isOtaUpdateAvailable = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Später", color = Color.White)
                                    }
                                },
                            )
                        }

                        if (showWelcomeDialog) {
                            AlertDialog(
                                onDismissRequest = { showWelcomeDialog = false },
                                title = { Text(text = "🎉 Erfolgreich angemeldet!", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "🔑 Konto / Lizenz-Schlüssel:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = LicenseManager.getActivatedCode(context), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(text = "⏱️ Verbleibende Lizenzdauer:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = LicenseManager.getExpirationDateString(context), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(text = "🚀 Neueste Enterprise-Updates v1.3.5:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "• 45.000 Global Market Preismatrix Sync", fontSize = 11.sp)
                                        Text(text = "• Echtzeit WebSocket & GZIP Komprimierung", fontSize = 11.sp)
                                        Text(text = "• Anti-Debugging & Anti-Cheat Schutz aktiv", fontSize = 11.sp)
                                        Text(text = "• Bildschirmsperre Ladebildschirm Integration", fontSize = 11.sp)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showWelcomeDialog = false },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(text = "Loslegen", fontWeight = FontWeight.Bold)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billingManager.isInitialized) {
            billingManager.queryActivePurchases()
        }
    }
}
