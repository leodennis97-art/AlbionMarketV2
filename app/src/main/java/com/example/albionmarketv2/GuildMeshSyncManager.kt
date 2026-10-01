package com.example.albionmarketv2

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SharedGuildOrder(
    val id: String,
    val author: String,
    val resourceId: String,
    val resourceName: String,
    val buyCity: String,
    val buyPrice: Int,
    val sellCity: String,
    val sellPrice: Int,
    val netProfit: Int,
    val timestamp: String
)

object GuildMeshSyncManager {

    suspend fun shareTradeOrderWithGuild(
        context: Context,
        resourceId: String,
        resourceName: String,
        buyCity: String,
        buyPrice: Int,
        sellCity: String,
        sellPrice: Int,
        netProfit: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val prefs = AppPreferences(context)
        val author = prefs.savedUsername.ifBlank { "Unbekannt" }
        val payload = JSONObject().apply {
            put("author", author)
            put("resourceId", resourceId)
            put("resourceName", resourceName)
            put("buyCity", buyCity)
            put("buyPrice", buyPrice)
            put("sellCity", sellCity)
            put("sellPrice", sellPrice)
            put("netProfit", netProfit)
        }.toString()

        val urls = ServerSyncManager.getServerBaseUrls(context).map { "$it/api/guild/order/share" }
        for (urlStr in urls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.doOutput = true
                conn.outputStream.use { os -> os.write(payload.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) return@withContext true
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        false
    }

    suspend fun fetchGuildSharedOrders(context: Context): List<SharedGuildOrder> = withContext(Dispatchers.IO) {
        val urls = ServerSyncManager.getServerBaseUrls(context).map { "$it/api/guild/orders" }
        val list = mutableListOf<SharedGuildOrder>()
        for (urlStr in urls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(response)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            SharedGuildOrder(
                                id = obj.optString("id", ""),
                                author = obj.optString("author", "Gilden-Mitglied"),
                                resourceId = obj.optString("resourceId", ""),
                                resourceName = obj.optString("resourceName", "Rohstoff"),
                                buyCity = obj.optString("buyCity", ""),
                                buyPrice = obj.optInt("buyPrice", 0),
                                sellCity = obj.optString("sellCity", ""),
                                sellPrice = obj.optInt("sellPrice", 0),
                                netProfit = obj.optInt("netProfit", 0),
                                timestamp = obj.optString("timestamp", "")
                            )
                        )
                    }
                    return@withContext list
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        emptyList()
    }

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
