package com.example.albionmarketv2

import android.content.Context
import org.json.JSONObject

object DesktopBridgeManager {

    fun generateCompanionQrPayload(context: Context): String {
        val hwId = DeviceHardwareManager.getHardwareId(context)
        return JSONObject().apply {
            put("app", "AlbionDataPro")
            put("version", "1.3.8")
            put("hwId", hwId)
            put("bridgeUrl", "https://witty-catfish-22.loca.lt/bridge/$hwId")
        }.toString()
    }
}
