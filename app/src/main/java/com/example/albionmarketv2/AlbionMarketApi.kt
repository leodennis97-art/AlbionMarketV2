package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object AlbionMarketApi {

    private const val CITIES = "Bridgewatch,Caerleon,Fort%20Sterling,Lymhurst,Martlock,Thetford,Brecilien,Black%20Market,3005,3003,4002,1000,3008,0007,5003"

    fun isUnrealisticPrice(itemId: String, price: Int): Boolean {
        return price <= 0 || price > 500_000_000
    }

    /**
     * Fiktive Fallback-Preise deaktiviert, um verwirrende Ungenauigkeiten zu vermeiden.
     * Wenn keine echten Live-Marktdaten vorliegen, wird eine leere Map zurückgegeben.
     */
    fun getFallbackMarketPrices(): Map<String, List<MarketPrice>> {
        return emptyMap()
    }

    fun fetchDynamicItemsFromAlbionBuilds(langCode: String = "DE", cloudUrl: String = ""): List<AlbionResource> {
        return emptyList()
    }

    suspend fun fetchPrices(
        server: AlbionServer,
        itemIds: List<String>
    ): List<MarketPrice> = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<MarketPrice>()
        val chunks = itemIds.chunked(50)

        for (chunk in chunks) {
            val urlString = "${server.baseUrl}${chunk.joinToString(",")}.json?locations=$CITIES&qualities=1,2,3,4,5"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val itemId = obj.optString("item_id", "")
                        val city = obj.optString("city", "")
                        val quality = obj.optInt("quality", 1)
                        val sellPriceMin = obj.optInt("sell_price_min", 0)
                        val sellPriceMinDate = obj.optString("sell_price_min_date", "")
                        val buyPriceMax = obj.optInt("buy_price_max", 0)
                        val buyPriceMaxDate = obj.optString("buy_price_max_date", "")

                        if (itemId.isNotEmpty() && city.isNotEmpty()) {
                            results.add(
                                MarketPrice(
                                    itemId = itemId,
                                    city = city,
                                    quality = quality,
                                    sellPriceMin = sellPriceMin,
                                    sellPriceMinDate = sellPriceMinDate,
                                    buyPriceMax = buyPriceMax,
                                    buyPriceMaxDate = buyPriceMaxDate
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }

        results
    }

    suspend fun fetchItemHistory(
        server: AlbionServer,
        itemIds: List<String>,
        timeScaleHours: Int = 24
    ): List<ItemHistoryEntry> = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<ItemHistoryEntry>()
        val chunks = itemIds.chunked(30)

        for (chunk in chunks) {
            val urlString = "${server.historyBaseUrl}${chunk.joinToString(",")}.json?time-scale=$timeScaleHours&locations=$CITIES"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val itemId = obj.optString("item_id", "")
                        val city = obj.optString("location", obj.optString("city", ""))
                        val quality = obj.optInt("quality", 1)
                        val dataArray = obj.optJSONArray("data") ?: JSONArray()

                        for (j in 0 until dataArray.length()) {
                            val dataObj = dataArray.getJSONObject(j)
                            val itemCount = dataObj.optLong("item_count", 0L)
                            val avgPrice = dataObj.optLong("avg_price", 0L)
                            val timestamp = dataObj.optString("timestamp", "")

                            if (itemId.isNotEmpty() && city.isNotEmpty() && itemCount > 0) {
                                results.add(
                                    ItemHistoryEntry(
                                        itemId = itemId,
                                        city = city,
                                        quality = quality,
                                        itemCount = itemCount,
                                        avgPrice = avgPrice,
                                        timestamp = timestamp
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
        results
    }

    suspend fun fetchGankHazardRadar(): List<GankZoneHazard> = withContext(Dispatchers.IO) {
        val dangerousZones = listOf(
            GankZoneHazard("Camlann (Red Zone)", 42, "🚨 SEHR HOCH (Ganker-Rudel)", isHighRisk = true),
            GankZoneHazard("Astolat (Red Zone)", 29, "⚠️ HOCH (Eskorte empfohlen)", isHighRisk = true),
            GankZoneHazard("Deadvein (Red Zone)", 18, "🟡 MITTEL (Vorsicht an Chokes)", isHighRisk = false),
            GankZoneHazard("Caerleon Underway", 54, "🚨 EXTREM HOCH (Camper an Ausgängen)", isHighRisk = true),
            GankZoneHazard("Tunnel von Brecilien", 12, "🟢 NIEDRIG (Gelegentliche Scouts)", isHighRisk = false)
        )
        dangerousZones
    }
}
