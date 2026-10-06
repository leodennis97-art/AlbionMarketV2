package com.example.albionmarketv2

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

object DeviceHardwareManager {

    fun getHardwareId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_ID"
        } catch (e: Exception) {
            "UNKNOWN_ANDROID_ID"
        }

        val rawId = "ALBION_HW_${Build.BOARD}_${Build.BRAND}_${Build.HARDWARE}_${Build.MANUFACTURER}_${Build.MODEL}_${Build.PRODUCT}_$androidId"
        return sha256(rawId).take(16).uppercase()
    }

    private fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }
}
