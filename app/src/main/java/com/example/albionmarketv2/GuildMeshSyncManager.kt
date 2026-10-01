package com.example.albionmarketv2

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GuildMeshSyncManager {

    suspend fun shareSnapshotWithGuild(context: Context, guildKey: String, snapshots: List<PriceSnapshot>): Boolean = withContext(Dispatchers.IO) {
        if (snapshots.isEmpty()) return@withContext false
        val arr = JSONArray()
        snapshots.takeLast(50).forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("itemId", s.itemId)
                    put("city", s.city)
                    put("sellPriceMin", s.sellPriceMin)
                    put("buyPriceMax", s.buyPriceMax)
                    put("timestampMs", s.timestampMs)
                },
            )
        }
        val payload = JSONObject().apply {
            put("guildKey", guildKey)
            put("hwId", DeviceHardwareManager.getHardwareId(context))
            put("snapshots", arr)
        }.toString()

        val syncUrls = ServerSyncManager.getServerBaseUrls(context).map { "$it/api/guild/mesh/sync" }

        for (syncUrl in syncUrls) {
            try {
                val url = URL(syncUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        false
    }
}
