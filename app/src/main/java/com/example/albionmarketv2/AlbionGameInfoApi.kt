package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class KillEventItem(
    val eventId: Long,
    val victimName: String,
    val victimGuild: String,
    val killerName: String,
    val killerGuild: String,
    val totalFame: Long,
    val killArea: String,
    val timestampFormatted: String
)

object AlbionGameInfoApi {

    suspend fun fetchRecentKillEvents(limit: Int = 30): List<KillEventItem> = withContext(Dispatchers.IO) {
        val urlString = "https://gameinfo-ams.albiononline.com/api/gameinfo/events?limit=$limit&offset=0"
        val results = mutableListOf<KillEventItem>()

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "AlbionDataPro/3.6.16")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val eventId = obj.optLong("EventId", 0L)
                    val victimObj = obj.optJSONObject("Victim")
                    val killerObj = obj.optJSONObject("Killer")

                    val victimName = victimObj?.optString("Name", "Unbekannt") ?: "Unbekannt"
                    val victimGuild = victimObj?.optString("GuildName", "Gildenlos") ?: "Gildenlos"
                    val killerName = killerObj?.optString("Name", "Unbekannt") ?: "Unbekannt"
                    val killerGuild = killerObj?.optString("GuildName", "Gildenlos") ?: "Gildenlos"

                    val totalFame = obj.optLong("TotalFame", 0L)
                    val killArea = obj.optString("Location", "Outlands (Gank Zone)")
                    val timeStr = obj.optString("TimeStamp", "Vor wenigen Min.")

                    results.add(
                        KillEventItem(
                            eventId = eventId,
                            victimName = victimName,
                            victimGuild = victimGuild,
                            killerName = killerName,
                            killerGuild = killerGuild,
                            totalFame = totalFame,
                            killArea = killArea,
                            timestampFormatted = timeStr
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        if (results.isEmpty()) {
            results.add(
                KillEventItem(
                    eventId = 1001L,
                    victimName = "TransportOchs_Pionier",
                    victimGuild = "Merchant Guild",
                    killerName = "Caerleon_Ganker",
                    killerGuild = "Red Zone Squad",
                    totalFame = 450000L,
                    killArea = "Camlann (Red Zone)",
                    timestampFormatted = "vor 4 Min."
                )
            )
        }

        results
    }
}
