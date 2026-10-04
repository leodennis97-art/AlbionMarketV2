package com.example.albionmarketv2

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

object ServerConfigManager {

    private const val RENDER_PRIMARY_URL = "https://albionmarketv2-1.onrender.com"

    fun initServerConfig(context: Context) {
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val albionFolder = File(docsDir, "AlbionDataPro")
            if (!albionFolder.exists()) {
                albionFolder.mkdirs()
            }

            val configFile = File(albionFolder, "server_config.json")
            val defaultConfig = JSONObject().apply {
                put("serverUrl", RENDER_PRIMARY_URL)
                put("fallbackUrl", RENDER_PRIMARY_URL)
                put("autoConnect", true)
                put("note", "Render Cloud Live Server")
            }
            configFile.writeText(defaultConfig.toString(4), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCustomServerUrl(context: Context): String {
        return "$RENDER_PRIMARY_URL/api/devices/ping"
    }

    fun getCustomServerUrls(context: Context): List<String> {
        val urls = mutableListOf<String>()
        urls.add(RENDER_PRIMARY_URL)

        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(File(docsDir, "AlbionDataPro"), "server_config.json")
            if (file.exists()) {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val customUrl = json.optString("serverUrl", "")
                if (customUrl.isNotBlank() && customUrl != RENDER_PRIMARY_URL) {
                    urls.add(customUrl.trimEnd('/'))
                }
            }
        } catch (_: Exception) {}

        return urls.distinct()
    }

    fun updateServerUrl(context: Context, url: String) {
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val albionFolder = File(docsDir, "AlbionDataPro")
            if (!albionFolder.exists()) albionFolder.mkdirs()
            val configFile = File(albionFolder, "server_config.json")
            val json = if (configFile.exists()) JSONObject(configFile.readText(Charsets.UTF_8)) else JSONObject()
            json.put("serverUrl", url.trim())
            configFile.writeText(json.toString(4), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
