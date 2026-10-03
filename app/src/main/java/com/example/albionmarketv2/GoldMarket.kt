package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class GoldPrice(
    val price: Int,
    val timestamp: String,
)

data class GoldPurchase(
    val id: String,
    val amountGold: Int,
    val buyPricePerGold: Int,
    val purchaseDate: String
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
    val id: String,
    val amountGold: Int,
    val sellPricePerGold: Int,
    val saleDate: String
) {
    val totalEarnedSilver: Long
        get() = amountGold.toLong() * sellPricePerGold

    fun realizedProfit(avgBuyPrice: Int): Long {
        val cost = amountGold.toLong() * avgBuyPrice
        return totalEarnedSilver - cost
    }

    fun realizedRoiPercent(avgBuyPrice: Int): Double {
        val cost = amountGold.toLong() * avgBuyPrice
        return if (cost > 0) {
            ((totalEarnedSilver - cost).toDouble() / cost) * 100.0
        } else 0.0
    }
}

data class GoldBotAnalysis(
    val dailyLow: Int,
    val dailyHigh: Int,
    val dailyAvg: Int,
    val monthlyLow: Int,
    val monthlyHigh: Int,
    val forecast3Days: Int,
    val forecast7Days: Int,
    val trendForecastDe: String,
    val recommendedBuyOrderPrice: Int,
    val recommendedSellOrderPrice: Int,
    val expectedNetProfitPerGold: Int,
    val expectedRoiPercent: Double,
    val recommendationSummaryDe: String
)

object GoldBotCalculator {

    fun analyzeGoldMarket(goldPrices: List<GoldPrice>, currentPrice: Int): GoldBotAnalysis {
        val validPrices = goldPrices.asSequence().map { it.price }.filter { it > 0 }.toList()
        val low = if (validPrices.isNotEmpty()) validPrices.minOrNull() ?: currentPrice else currentPrice
        val high = if (validPrices.isNotEmpty()) validPrices.maxOrNull() ?: currentPrice else currentPrice
        val avg = if (validPrices.isNotEmpty()) validPrices.average().toInt() else currentPrice

        val monthlyLow = (low * 0.96).toInt()
        val monthlyHigh = (high * 1.05).toInt()

        val momentum = if (validPrices.size >= 2) {
            val oldest = validPrices.last()
            val newest = validPrices.first()
            (newest.toDouble() - oldest.toDouble()) / oldest.toDouble()
        } else 0.01

        val forecast3Days = (currentPrice * (1.0 + (momentum * 1.5))).toInt()
        val forecast7Days = (currentPrice * (1.0 + (momentum * 3.0))).toInt()

        val trendStr = when {
            momentum > 0.03 -> "STARK STEIGEND 🚀"
            momentum > 0.0 -> "LEICHT STEIGEND 📈"
            momentum > -0.03 -> "SEITWÄRTS ➡️"
            else -> "FALLEND 📉"
        }

        val recBuyOrder = if (low > 0) (low * 1.002).toInt() else (currentPrice * 0.98).toInt()
        val recSellOrder = maxOf((high * 1.01).toInt(), (recBuyOrder * 1.06).toInt())

        val netProfitPerGold = (recSellOrder * 0.97).toInt() - recBuyOrder
        val roi = if (recBuyOrder > 0) (netProfitPerGold.toDouble() / recBuyOrder) * 100.0 else 0.0

        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
        val summary = "Tiefpunkt: ${fmt.format(low)} S. | " +
                "Kauf-Order: ${fmt.format(recBuyOrder)} S. | " +
                "Verkauf-Order: ${fmt.format(recSellOrder)} S. (+${String.format(Locale.GERMANY, "%.1f", roi)}% Marge)"

        return GoldBotAnalysis(
            dailyLow = low,
            dailyHigh = high,
            dailyAvg = avg,
            monthlyLow = monthlyLow,
            monthlyHigh = monthlyHigh,
            forecast3Days = forecast3Days,
            forecast7Days = forecast7Days,
            trendForecastDe = trendStr,
            recommendedBuyOrderPrice = recBuyOrder,
            recommendedSellOrderPrice = recSellOrder,
            expectedNetProfitPerGold = netProfitPerGold,
            expectedRoiPercent = roi,
            recommendationSummaryDe = summary
        )
    }
}

object AlbionGoldApi {

    suspend fun fetchGoldPrices(server: AlbionServer, count: Int = 24): List<GoldPrice> = withContext(Dispatchers.IO) {
        val baseUrl = server.baseUrl.replace("/stats/prices/", "/stats/gold.json")
        val results = mutableListOf<GoldPrice>()

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl?count=$count")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                if (responseText.trim().startsWith("[")) {
                    val jsonArray = JSONArray(responseText)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        val price = obj.optInt("price", 0)
                        val timestamp = obj.optString("timestamp", "")
                        if (price > 0) {
                            results.add(GoldPrice(price = price, timestamp = timestamp))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        results
    }

    suspend fun fetchAllServersLatestGoldPrices(): Map<AlbionServer, Int> = coroutineScope {
        val map = mutableMapOf<AlbionServer, Int>()
        val deferreds = AlbionServer.entries.map { server ->
            async {
                val prices = fetchGoldPrices(server, count = 1)
                val latest = prices.firstOrNull()?.price
                if (latest != null && latest > 0) {
                    server to latest
                } else null
            }
        }
        deferreds.forEach { deferred ->
            deferred.await()?.let { (server, price) ->
                map[server] = price
            }
        }
        map
    }
}
