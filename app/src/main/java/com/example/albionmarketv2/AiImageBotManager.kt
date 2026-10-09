package com.example.albionmarketv2

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import kotlinx.coroutines.*
import java.util.UUID
import kotlin.math.abs

data class BotTarget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val templateBitmap: Bitmap,
    val threshold: Double = 0.85,
    val enabled: Boolean = true
)

object TemplateMatcher {
    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    /**
     * Searches for templateBitmap inside sourceBitmap using normalized color distance / similarity.
     * Returns the Point (x, y) of the center of the best match, or null if below threshold.
     */
    fun findTemplate(source: Bitmap, template: Bitmap, threshold: Double = 0.85): Point? {
        try {
            val src = ensureSoftwareBitmap(source)
            val tpl = ensureSoftwareBitmap(template)

            val srcWidth = src.width
            val srcHeight = src.height
            val tplWidth = tpl.width
            val tplHeight = tpl.height

            if (tplWidth > srcWidth || tplHeight > srcHeight) return null

            var bestScore = -1.0
            var bestX = -1
            var bestY = -1

            val stepX = 4 // Step by 4 for fast scanning performance
            val stepY = 4

            for (y in 0..srcHeight - tplHeight step stepY) {
                for (x in 0..srcWidth - tplWidth step stepX) {
                    val score = calculateSimilarity(src, tpl, x, y, tplWidth, tplHeight)
                    if (score > bestScore) {
                        bestScore = score
                        bestX = x
                        bestY = y
                    }
                }
            }

            return if (bestScore >= threshold) {
                Point(bestX + tplWidth / 2, bestY + tplHeight / 2)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun calculateSimilarity(
        source: Bitmap,
        template: Bitmap,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): Double {
        var totalDiff = 0.0
        val maxDiffPerPixel = 255.0 * 3.0
        val sampleStep = 4 // sample every 4th pixel for speed

        var sampledPixels = 0

        for (ty in 0 until height step sampleStep) {
            for (tx in 0 until width step sampleStep) {
                val tplPixel = template.getPixel(tx, ty)
                val srcPixel = source.getPixel(startX + tx, startY + ty)

                val rDiff = abs(Color.red(tplPixel) - Color.red(srcPixel))
                val gDiff = abs(Color.green(tplPixel) - Color.green(srcPixel))
                val bDiff = abs(Color.blue(tplPixel) - Color.blue(srcPixel))

                totalDiff += (rDiff + gDiff + bDiff)
                sampledPixels++
            }
        }

        if (sampledPixels == 0) return 0.0
        val avgDiff = totalDiff / sampledPixels
        val similarity = 1.0 - (avgDiff / maxDiffPerPixel)
        return similarity
    }
}

class AiImageBotManager(
    private val onLog: (String) -> Unit,
    private val onTapAction: (Int, Int) -> Unit
) {
    private var botJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val targets = mutableListOf<BotTarget>()

    var isRunning: Boolean = false
        private set

    var checkIntervalMs: Long = 1000L

    fun addTarget(target: BotTarget) {
        targets.add(target)
        onLog("🤖 Ziel hinzugefügt: ${target.name}")
    }

    fun removeTarget(id: String) {
        targets.removeIf { it.id == id }
        onLog("🤖 Ziel entfernt: $id")
    }

    fun getTargets(): List<BotTarget> = targets.toList()

    fun startBot(screenshotProvider: () -> Bitmap?) {
        if (isRunning) return
        if (targets.isEmpty()) {
            onLog("⚠️ Keine Ziele definiert! Bitte zuerst Referenzbilder hinzufügen.")
            return
        }

        isRunning = true
        onLog("🚀 KI-Bot gestartet. Überwache Bildschirm...")

        botJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    val screenshot = screenshotProvider()
                    if (screenshot != null) {
                        for (target in targets.filter { it.enabled }) {
                            val match = TemplateMatcher.findTemplate(screenshot, target.templateBitmap, target.threshold)
                            if (match != null) {
                                onLog("🎯 Ziel '${target.name}' gefunden bei (${match.x}, ${match.y})! Tappe...")
                                onTapAction(match.x, match.y)
                                delay(800) // Cooldown after tap
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    onLog("❌ Bot-Fehler: ${e.message}")
                }
                delay(checkIntervalMs)
            }
        }
    }

    fun stopBot() {
        isRunning = false
        botJob?.cancel()
        botJob = null
        onLog("⏹️ KI-Bot gestoppt.")
    }

    fun release() {
        stopBot()
        scope.cancel()
    }
}
// V3.5.0
