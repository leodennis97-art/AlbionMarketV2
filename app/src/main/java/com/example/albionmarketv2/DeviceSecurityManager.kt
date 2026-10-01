package com.example.albionmarketv2

import android.content.Context
import android.os.Build
import java.io.File

object DeviceSecurityManager {

    /**
     * Checks if the device is rooted, running in an emulator, or compromised.
     */
    fun isDeviceCompromised(context: Context): Boolean {
        return isRooted() || isEmulator()
    }

    private fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/fulls/su",
            "/bin/su"
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

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
}
