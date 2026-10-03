package com.example.albionmarketv2

import android.content.Context
import org.json.JSONObject

object DesktopBridgeManager {

    fun generateCompanionQrPayload(context: Context): String {
        val hwId = DeviceHardwareManager.getHardwareId(context)
        return JSONObject().apply {
            put("app", "AlbionDataPro")
            put("version", "1.3.18")
            put("hwId", hwId)
            put("bridgeUrl", "https://albionmarketv2.onrender.com/bridge/$hwId")
        }.toString()
    }
}
