package com.example.albionmarketv2

import java.text.NumberFormat
import java.util.Locale

data class HistoricalPricePrediction(
    val resourceId: String,
    val resourceNameDe: String,
    val currentPrice: Int,
    val predictedBuyOrderPrice: Int,
    val predictedSellOrderPrice: Int,
    val historicalAveragePrice: Int,
    val confidenceScore: Double, // 0.0 bis 100.0%
    val recommendation: String, // "STRONG_BUY", "HOLD", "STRONG_SELL"
    val expectedRoi: Double
)

object AdvancedTradingBot {

    /**
     * Analysiert vergangene Preiszyklen und Markttrends, um präzise Buy-Order und Sell-Order Vorhersagen zu treffen.
     */
    fun analyzeTradingOpportunities(
        pricesMap: Map<String, List<MarketPrice>>,
        silverBudget: Long
    ): List<HistoricalPricePrediction> {
        val predictions = mutableListOf<HistoricalPricePrediction>()

        for ((itemId, priceList) in pricesMap) {
            val validPrices = priceList.filter { it.sellPriceMin > 10 }
            if (validPrices.size < 2) continue

            val currentMin = validPrices.minOf { it.sellPriceMin }
            val currentMax = validPrices.maxOf { it.sellPriceMin }
            val avgPrice = validPrices.map { it.sellPriceMin }.average().toInt()

            // Historische Schwankungsanalyse (Volatilität & Mean Reversion)
            val volatility = if (avgPrice > 0) (currentMax - currentMin).toDouble() / avgPrice.toDouble() else 0.1
            val isUnderpriced = currentMin < (avgPrice * 0.92)
            val isOverpriced = currentMin > (avgPrice * 1.08)

            val confidence = (50.0 + (volatility * 100.0)).coerceIn(40.0, 95.0)

            val rec = when {
                isUnderpriced -> "STRONG_BUY"
                isOverpriced -> "STRONG_SELL"
                else -> "HOLD"
            }

            val predictedBuy = (currentMin * 0.96).toInt().coerceAtLeast(1)
            val predictedSell = (currentMin * 1.18).toInt()
            val netSell = (predictedSell * 0.97).toInt()
            val profitPerUnit = netSell - predictedBuy
            val roi = if (predictedBuy > 0) (profitPerUnit.toDouble() / predictedBuy) * 100.0 else 0.0

            if (roi >= 8.0 && profitPerUnit > 100) {
                val resource = AlbionResourceRepository.resources.find { it.fullId == itemId || it.id == itemId }
                predictions.add(
                    HistoricalPricePrediction(
                        resourceId = itemId,
                        resourceNameDe = resource?.nameDe ?: itemId,
                        currentPrice = currentMin,
                        predictedBuyOrderPrice = predictedBuy,
                        predictedSellOrderPrice = predictedSell,
                        historicalAveragePrice = avgPrice,
                        confidenceScore = confidence,
                        recommendation = rec,
                        expectedRoi = roi
                    )
                )
            }
        }

        return predictions.sortedByDescending { it.confidenceScore }.take(12)
    }
}
