package com.example.albionmarketv2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickerService : AccessibilityService() {
    
    companion object {
        var instance: AutoClickerService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("AutoClicker", "Accessibility Service connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We do not need to listen to events for simple clicking
    }

    override fun onInterrupt() {
        Log.d("AutoClicker", "Accessibility Service interrupted!")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun clickAt(x: Float, y: Float, humanize: Boolean = true) {
        // High-End Anti-Cheat Humanization: Add random micro jitter (+/- 4px) and random press duration (60ms - 140ms)
        val finalX = if (humanize) x + ((-4..4).random()) else x
        val finalY = if (humanize) y + ((-4..4).random()) else y
        val pressDuration = if (humanize) (60L..140L).random() else 80L

        val path = Path()
        path.moveTo(finalX.coerceAtLeast(0f), finalY.coerceAtLeast(0f))
        val stroke = StrokeDescription(path, 0, pressDuration)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        
        val gesture = builder.build()
        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("AutoClicker", "⚡ High-End Click at ($finalX, $finalY) completed ($pressDuration ms).")
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = getSystemService(VibratorManager::class.java)
                        vm?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (vibrator?.hasVibrator() == true) {
                        vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } catch (_: Exception) {}
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d("AutoClicker", "Click at ($finalX, $finalY) cancelled.")
            }
        }, null)
        Log.d("AutoClicker", "Dispatched click at ($finalX, $finalY): $result")
    }
}