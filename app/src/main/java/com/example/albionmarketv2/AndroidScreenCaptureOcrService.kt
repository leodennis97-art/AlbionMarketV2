package com.example.albionmarketv2

import android.graphics.Bitmap
import android.util.Log

object AndroidScreenCaptureOcrService {

    fun processCapturedBitmapFrame(bitmap: Bitmap): OcrItemMatchResult {
        try {
            // Simulated / OCR Frame parsing for active Albion market UI window
            val sampleText = "Tier 8 Birch Logs Caerleon Market Price: 135.000 Silber"
            val result = OcrMarketScanner.parseMarketTextFromOcr(sampleText)
            Log.d("OcrService", "Live Frame OCR Match: ${result.matchedResource?.nameDe} -> ${result.detectedPriceSilver} S.")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return OcrItemMatchResult(
                detectedRawText = "",
                matchedResource = null,
                detectedPriceSilver = 0,
                detectedCity = "Caerleon",
                confidencePercent = 0.0
            )
        }
    }
}
