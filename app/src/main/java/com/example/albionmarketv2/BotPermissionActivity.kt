package com.example.albionmarketv2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class BotPermissionActivity : ComponentActivity() {

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenScannerService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
                putExtra("WORKFLOW_ID", "wf_1")
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "🚀 Auto-Bot im Hintergrund gestartet!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen Capture Berechtigung verweigert.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Fehler beim Starten der Bildschirmaufnahme", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}