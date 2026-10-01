package com.example.albionmarketv2

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

object ServerConfigManager {

    fun initServerConfig(context: Context) {
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val albionFolder = File(docsDir, "AlbionDataPro")
            if (!albionFolder.exists()) {
                albionFolder.mkdirs()
            }

            val configFile = File(albionFolder, "server_config.json")
            if (!configFile.exists()) {
                val defaultConfig = JSONObject().apply {
                    put("serverUrl", "https://albionmarketv2-1.onrender.com")
                    put("ngrokUrl", "https://albionmarketv2-1.onrender.com")
                    put("autoConnect", true)
                    put("note", "24/7 Central Render Cloud Server")
                }
                configFile.writeText(defaultConfig.toString(4), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val altFolder = File(context.getExternalFilesDir(null), "AlbionDataPro")
                if (!altFolder.exists()) {
                    altFolder.mkdirs()
                }
                val configFile = File(altFolder, "server_config.json")
                if (!configFile.exists()) {
                    val defaultConfig = JSONObject().apply {
                        put("serverUrl", "https://albionmarketv2-1.onrender.com")
                        put("ngrokUrl", "https://albionmarketv2-1.onrender.com")
                        put("autoConnect", true)
                    }
                    configFile.writeText(defaultConfig.toString(4), Charsets.UTF_8)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getCustomServerUrl(context: Context): String? {
        return getCustomServerUrls(context).firstOrNull()?.let { "$it/api/devices/ping" }
    }

    fun getCustomServerUrls(context: Context): List<String> {
        val urls = mutableListOf<String>()
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(File(docsDir, "AlbionDataPro"), "server_config.json")
            if (file.exists()) {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val customUrl = json.optString("serverUrl", "")
                if (customUrl.isNotBlank()) urls.add(customUrl.trimEnd('/'))
                val ngrokUrl = json.optString("ngrokUrl", "")
                if (ngrokUrl.isNotBlank()) urls.add(ngrokUrl.trimEnd('/'))
            }
        } catch (_: Exception) {}

        try {
            val file = File(File(context.getExternalFilesDir(null), "AlbionDataPro"), "server_config.json")
            if (file.exists()) {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val customUrl = json.optString("serverUrl", "")
                if (customUrl.isNotBlank()) urls.add(customUrl.trimEnd('/'))
                val ngrokUrl = json.optString("ngrokUrl", "")
                if (ngrokUrl.isNotBlank()) urls.add(ngrokUrl.trimEnd('/'))
            }
        } catch (_: Exception) {}

        return urls.distinct()
    }
}
