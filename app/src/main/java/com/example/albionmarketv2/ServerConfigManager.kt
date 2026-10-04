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
                    put("serverUrl", "https://speller-importer-captivate.ngrok-free.dev")
                    put("fallbackUrl", "https://albionmarketv2.onrender.com")
                    put("autoConnect", true)
                    put("note", "Central Live Cloud Server & Ngrok Tunnel")
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
                        put("serverUrl", "https://speller-importer-captivate.ngrok-free.dev")
                        put("fallbackUrl", "https://albionmarketv2.onrender.com")
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
                val customUrl = json.optString("serverUrl", "").ifBlank { "https://speller-importer-captivate.ngrok-free.dev" }
                if (customUrl.isNotBlank()) urls.add(customUrl.trimEnd('/'))
                val fallbackUrl = json.optString("fallbackUrl", "")
                if (fallbackUrl.isNotBlank()) urls.add(fallbackUrl.trimEnd('/'))
            } else {
                urls.add("https://speller-importer-captivate.ngrok-free.dev")
                urls.add("https://albionmarketv2.onrender.com")
            }
        } catch (_: Exception) {
            urls.add("https://speller-importer-captivate.ngrok-free.dev")
            urls.add("https://albionmarketv2.onrender.com")
        }

        try {
            val file = File(File(context.getExternalFilesDir(null), "AlbionDataPro"), "server_config.json")
            if (file.exists()) {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val customUrl = json.optString("serverUrl", "")
                if (customUrl.isNotBlank()) urls.add(customUrl.trimEnd('/'))
            }
        } catch (_: Exception) {}

        fun setCustomServerUrl(context: Context, url: String) {
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val albionFolder = File(docsDir, "AlbionDataPro")
            if (!albionFolder.exists()) albionFolder.mkdirs()
            val file = File(albionFolder, "server_config.json")
            val json = if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else JSONObject()
            json.put("serverUrl", url.trim())
            file.writeText(json.toString(4), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
