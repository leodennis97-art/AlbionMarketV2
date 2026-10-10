package com.example.albionmarketv2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            handleSendImage(intent)
        } else {
            finish()
        }
    }

    private fun handleSendImage(intent: Intent) {
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }

        if (imageUri != null) {
            Toast.makeText(this, "DataPro: Bild wird analysiert...", Toast.LENGTH_SHORT).show()
            processImageForOCR(imageUri)
        } else {
            Toast.makeText(this, "DataPro: Konnte das Bild nicht laden.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun processImageForOCR(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank()) {
                        Toast.makeText(this, "Text gefunden: ${text.take(50)}...", Toast.LENGTH_LONG).show()
                        
                        // Send text to FloatingBubbleService
                        val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
                            action = "ACTION_OCR_RESULT"
                            putExtra("EXTRA_TEXT", text)
                        }
                        startService(serviceIntent)
                    } else {
                        Toast.makeText(this, "Kein Text auf dem Bild erkannt.", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Toast.makeText(this, "Fehler bei der Bilderkennung: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Fehler beim Laden des Bildes.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
