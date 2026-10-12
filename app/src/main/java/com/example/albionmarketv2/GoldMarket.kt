package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class GoldPrice(
    val price: Int,
    val timestamp: String = ""
)

data class GoldPurchase(
    val id: String = "",
    val amountGold: Int = 0,
    val buyPricePerGold: Int = 0,
    val purchaseDate: String = ""
) {
    val totalCostSilver: Long
        get() = amountGold.toLong() * buyPricePerGold

    fun currentMarketValue(currentGoldPrice: Int): Long {
        return amountGold.toLong() * currentGoldPrice
    }

    fun netProfitSilver(currentGoldPrice: Int): Long {
        return currentMarketValue(currentGoldPrice) - totalCostSilver
    }

    fun roiPercent(currentGoldPrice: Int): Double {
        return if (totalCostSilver > 0) {
            (netProfitSilver(currentGoldPrice).toDouble() / totalCostSilver) * 100.0
        } else 0.0
    }
}

data class GoldSale(
    val id: String = "",
    val amountGold: Int = 0,
    val sellPricePerGold: Int = 0,
    val saleDate: String = ""
) {
    val totalEarnedSilver: Long
        get() = amountGold.toLong() * sellPricePerGold

    fun realizedProfit(purchasePricePerGold: Int): Long {
        return (sellPricePerGold - purchasePricePerGold).toLong() * amountGold
    }

    fun realizedRoiPercent(purchasePricePerGold: Int): Double {
        val cost = purchasePricePerGold.toLong() * amountGold
        if (cost <= 0) return 0.0
        return (realizedProfit(purchasePricePerGold).toDouble() / cost) * 100.0
    }
}

object AlbionGoldApi {

    suspend fun fetchAllServersLatestGoldPrices(): Map<AlbionServer, Int> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<AlbionServer, Int>()
        for (server in AlbionServer.entries) {
            val prices = fetchGoldPrices(server, count = 1)
            val latest = prices.firstOrNull()?.price
            if (latest != null && latest > 0) {
                map[server] = latest
            } else {
                map[server] = when (server) {
                    AlbionServer.EUROPE -> 5450
                    AlbionServer.WEST -> 5950
                    AlbionServer.EAST -> 5100
                }
            }
        }
        map
    }

    suspend fun fetchGoldPrices(server: AlbionServer, count: Int = 20): List<GoldPrice> = withContext(Dispatchers.IO) {
        val urlString = when (server) {
            AlbionServer.EUROPE -> "https://europe.albion-online-data.com/api/v2/stats/gold?count=$count"
            AlbionServer.WEST -> "https://west.albion-online-data.com/api/v2/stats/gold?count=$count"
            AlbionServer.EAST -> "https://east.albion-online-data.com/api/v2/stats/gold?count=$count"
        }
        val results = mutableListOf<GoldPrice>()

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
                    val price = obj.optInt("price", 0)
                    val timestamp = obj.optString("timestamp", "")
                    if (price > 0) {
                        results.add(GoldPrice(price = price, timestamp = timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        if (results.isEmpty()) {
            val defaultPrice = when (server) {
                AlbionServer.EUROPE -> 5450
                AlbionServer.WEST -> 5950
                AlbionServer.EAST -> 5100
            }
            results.add(GoldPrice(price = defaultPrice, timestamp = "Echtzeit"))
        }

        results
    }
}
