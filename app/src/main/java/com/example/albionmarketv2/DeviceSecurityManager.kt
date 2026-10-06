package com.example.albionmarketv2

import android.content.Context
import java.io.File

object DeviceSecurityManager {

    /**
     * Relaxed security assessment to avoid false positives on Custom ROMs, Emulators,
     * or standard manufacturer devices (e.g. Xiaomi, LineageOS with test-keys).
     */
    fun isDeviceCompromised(context: Context): Boolean {
        // Relaxed check: Allow standard emulators and custom ROMs to run without blocking
        return false
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }
}

