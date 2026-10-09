package com.example.albionmarketv2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
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

    fun clickAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = StrokeDescription(path, 0, 100)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        
        val gesture = builder.build()
        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("AutoClicker", "Click at ($x, $y) completed.")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d("AutoClicker", "Click at ($x, $y) cancelled.")
            }
        }, null)
        Log.d("AutoClicker", "Dispatched click at ($x, $y): $result")
    }
}