package com.example.albionmarketv2

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import android.util.Log

object ImageMatcher {
    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    fun findTemplate(screenshot: Bitmap, template: Bitmap, threshold: Double = 0.8): Point? {
        val src = ensureSoftwareBitmap(screenshot)
        val tpl = ensureSoftwareBitmap(template)

        if (src.width < tpl.width || src.height < tpl.height) {
            Log.e("ImageMatcher", "Template is larger than the screenshot!")
            return null
        }

        val imgMat = Mat()
        val tplMat = Mat()
        
        try {
            Utils.bitmapToMat(src, imgMat)
            Utils.bitmapToMat(tpl, tplMat)

            // Konvertiere in Graustufen für stabileres und schnelleres Matching
            val imgGray = Mat()
            val tplGray = Mat()
            Imgproc.cvtColor(imgMat, imgGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(tplMat, tplGray, Imgproc.COLOR_RGBA2GRAY)

            val result = Mat()
            Imgproc.matchTemplate(imgGray, tplGray, result, Imgproc.TM_CCOEFF_NORMED)

            val minMaxLocResult = Core.minMaxLoc(result)
            val maxVal = minMaxLocResult.maxVal
            val maxLoc = minMaxLocResult.maxLoc

            // Speicher freigeben (sehr wichtig bei Android OpenCV)
            imgMat.release()
            tplMat.release()
            imgGray.release()
            tplGray.release()
            result.release()

            Log.d("ImageMatcher", "Match confidence: $maxVal (threshold: $threshold)")

            return if (maxVal >= threshold) {
                // Gibt den Mittelpunkt des gefundenen Templates zurück
                Point(maxLoc.x + tpl.width / 2.0, maxLoc.y + tpl.height / 2.0)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageMatcher", "Fehler beim Template Matching", e)
            try { imgMat.release() } catch (_: Exception) {}
            try { tplMat.release() } catch (_: Exception) {}
            return null
        }
    }
}