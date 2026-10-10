package com.example.albionmarketv2

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class BotPermissionActivity : ComponentActivity() {

    private var hasRequested = false

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if ((result.resultCode == RESULT_OK) && (result.data != null)) {
            val serviceIntent = Intent(this, ScreenScannerService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
                putExtra("WORKFLOW_ID", "wf_1")
            }
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
                Toast.makeText(this, "🚀 Auto-Bot im Hintergrund gestartet!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Fehler beim Starten des Bot-Dienstes: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "❌ Bildschirmaufnahme-Berechtigung verweigert oder vom System untersagt.", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (!hasRequested) {
            hasRequested = true
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    try {
                        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "⚠️ Fehler: Bildschirmaufnahme nicht erlaubt (${e.message})", Toast.LENGTH_LONG).show()
                        finish()
                    }
                },
                300,
            )
        }
    }
}
