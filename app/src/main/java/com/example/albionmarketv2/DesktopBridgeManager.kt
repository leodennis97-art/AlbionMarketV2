package com.example.albionmarketv2

import android.content.Context
import org.json.JSONObject

object DesktopBridgeManager {

    fun generateCompanionQrPayload(context: Context): String {
        val hwId = DeviceHardwareManager.getHardwareId(context)
        val versionName = OtaUpdateManager.getInstalledVersionName(context)
        return JSONObject().apply {
            put("app", "AlbionDataPro")
            put("version", versionName)
            put("hwId", hwId)
            put("bridgeUrl", "https://albionmarketv2-1.onrender.com/bridge/$hwId")
        }.toString()
    }
}
