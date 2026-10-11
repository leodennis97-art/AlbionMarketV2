package com.example.albionmarketv2

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceHardwareManager {

    fun getHardwareId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, "android_id")
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") androidId else Build.FINGERPRINT
        } catch (_: Exception) {
            Build.FINGERPRINT ?: "UNKNOWN_DEVICE_ID"
        }
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.trim().ifBlank { "Android Device" }
    }
}
