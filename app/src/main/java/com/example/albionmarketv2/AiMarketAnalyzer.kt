package com.example.albionmarketv2

import java.text.NumberFormat
import java.util.Locale

data class AiItemPriceComparison(
    val resourceId: String,
    val resourceNameDe: String,
    val lowestPrice: Int,
    val lowestCity: String,
    val highestPrice: Int,
    val highestCity: String,
    val profitPerUnit: Int,
    val marginPercent: Double,
    val updateTimeFormatted: String,
    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0,
    val expectedPriceDropPercent: Double = 0.0,
    val expectedPriceRisePercent: Double = 0.0,
    val aiRecommendationDe: String = ""
) {
    fun toTradeOpportunity(tradeUnits: Int = 10): TradeOpportunity {
        val resource = AlbionResourceRepository.resources.find { it.fullId == resourceId || it.id == resourceId }
            ?: AlbionResource(
                id = resourceId,
                nameDe = resourceNameDe,
                nameEn = resourceNameDe,
                tier = 4,
                category = ResourceCategory.ALL
            )
        val buy = if (recommendedBuyOrderPrice > 0) recommendedBuyOrderPrice else lowestPrice
        val sell = if (recommendedSellOrderPrice > 0) recommendedSellOrderPrice else highestPrice
        val units = tradeUnits.coerceAtLeast(1)
        val investment = buy.toLong() * units
        val netProfit = profitPerUnit.toLong() * units
        return TradeOpportunity(
            resource = resource,
            buyCity = lowestCity,
            buyPrice = buy,
            sellCity = highestCity,
            sellPrice = sell,
            unitNetProfit = profitPerUnit,
            unitWeightKg = TradeCalculator.getItemWeightKg(resource),
            maxUnitsBySilver = units,
            maxUnitsByWeight = units,
            tradeUnits = units,
            totalInvestment = investment,
            totalGrossRevenue = sell.toLong() * units,
            totalNetRevenue = netProfit,
            totalNetProfit = netProfit,
            totalWeightKg = TradeCalculator.getItemWeightKg(resource) * units,
            roiPercent = marginPercent,
            priorityScore = 100,
            recommendedBuyOrderPrice = recommendedBuyOrderPrice,
            recommendedSellOrderPrice = recommendedSellOrderPrice,
            aiOrderStrategy = aiRecommendationDe
        )
    }
}

data class AiAnalysisResult(
    val totalItemsCompared: Int,
    val bestFlips: List<AiItemPriceComparison>,
    val summaryTextDe: String,
    val summaryTextEn: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

object AiMarketAnalyzer {

    /**
     * KI-Bot Suchregeln für präzise Marktlücken:
     * 1. Zonen-Sicherheit: Ausschluss von Caerleon & Rotzonen wenn 'avoidDangerousZones' aktiv.
     * 2. Budget & Kapazität: Berücksichtigung von Silber-Budget und Traglast.
     * 3. Brecilien-Filter: Ausschluss von Brecilien wenn 'includeBrecilien' inaktiv.
     * 4. Anomalie-Filter: Verwerfung unrealistischer Preisspitzen (> 4x oder < 10 Silber).
     * 5. Echtzeit-Aktualisierung: Laufender 5-Sekunden-Suchloop.
     */
    fun analyzeMarketPrices(
        pricesMap: Map<String, List<MarketPrice>>,
        minMarginPercent: Double = 5.0,
        avoidDangerousZones: Boolean = false,
        silverBudget: Long = 10_000_000L,
        carryCapacityKg: Double = 2000.0,
        includeBrecilien: Boolean = false,
        hideBlackMarket: Boolean = false,
    ): AiAnalysisResult {
        val comparisons = mutableListOf<AiItemPriceComparison>()
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

        for ((itemId, priceList) in pricesMap) {
            val baseFiltered = if (avoidDangerousZones) {
                priceList.filter { p -> (p.sellPriceMin > 10) && (!TradeCalculator.isDangerousCity(p.city)) }
            } else {
                priceList.filter { p -> p.sellPriceMin > 10 }
            }

            val noBm = if (hideBlackMarket) {
                baseFiltered.filter { !TradeCalculator.isBlackMarket(it.city) }
            } else {
                baseFiltered
            }

            val validPrices = if (!includeBrecilien) {
                noBm.filter { !it.city.equals("Brecilien", ignoreCase = true) }
            } else {
                noBm
            }

            if (validPrices.size < 2) continue

            // Find lowest & highest price in different cities
            val lowest = validPrices.minByOrNull { it.sellPriceMin } ?: continue
            val validSellPrices = validPrices.filter { !it.city.equals(lowest.city, ignoreCase = true) }
            val highest = validSellPrices.maxByOrNull { it.sellPriceMin } ?: continue

            val buyPrice = lowest.sellPriceMin
            val sellPrice = highest.sellPriceMin

            // Price anomaly filter
            if ((buyPrice < 10) || (sellPrice > (buyPrice * 4))) continue

            // Check silver budget constraint first (cannot buy even 1 unit)
            val maxUnitsSilver = (silverBudget / buyPrice).toInt()
            if (maxUnitsSilver <= 0) continue

            val taxPerUnit = (sellPrice * (5.0 / 100.0)).toInt()
            val setupFeePerUnit = (sellPrice * 0.025).toInt()
            val netSellPrice = sellPrice - taxPerUnit - setupFeePerUnit
            val netProfitPerUnit = netSellPrice - buyPrice
            if (netProfitPerUnit <= 0) continue

            val margin = (netProfitPerUnit.toDouble() / buyPrice) * 100.0
            if (margin < minMarginPercent) continue

            val resource = AlbionResourceRepository.resources.find { (it.fullId == itemId) || (it.id == itemId) }
            val nameDe = resource?.nameDe ?: itemId

            // Check capacity constraints
            val unitWeight = resource?.let { TradeCalculator.getItemWeightKg(it) } ?: 1.0
            val maxUnitsWeight = if (unitWeight > 0) (carryCapacityKg / unitWeight).toInt() else 0
            val tradeUnits = kotlin.math.min(maxUnitsSilver, maxUnitsWeight)
            if (tradeUnits <= 0) continue

            val dropPct = 6.5
            val risePct = 11.2
            val recBuyOrder = (buyPrice * 0.935).toInt().coerceAtLeast(1)
            val recSellOrder = maxOf((sellPrice * 1.112).toInt(), (recBuyOrder * 1.15).toInt())
            val recText = "🛒 Kauforder: ${fmt.format(recBuyOrder)} S. (-${String.format(Locale.GERMANY, "%.1f", dropPct)}% Dip) | 📈 Verkauforder: ${fmt.format(recSellOrder)} S. (+${String.format(Locale.GERMANY, "%.1f", risePct)}% Peak)"

            comparisons.add(
                AiItemPriceComparison(
                    resourceId = itemId,
                    resourceNameDe = nameDe,
                    lowestPrice = buyPrice,
                    lowestCity = lowest.city,
                    highestPrice = sellPrice,
                    highestCity = highest.city,
                    profitPerUnit = netProfitPerUnit,
                    marginPercent = margin,
                    updateTimeFormatted = lowest.sellPriceMinDate.ifBlank { "Echtzeit-Zyklus" },
                    recommendedBuyOrderPrice = recBuyOrder,
                    recommendedSellOrderPrice = recSellOrder,
                    expectedPriceDropPercent = dropPct,
                    expectedPriceRisePercent = risePct,
                    aiRecommendationDe = recText
                )
            )
        }

        val sortedFlips = comparisons.asSequence().sortedByDescending { it.marginPercent }.take(15).toList()

        val top = sortedFlips.firstOrNull()
        val summaryDe = if (top != null) {
            "🤖 KI-Bot Live-Suche aktiv: ${comparisons.size} Items geprüft. Top Marktlücke: ${top.resourceNameDe} in ${top.lowestCity} kaufen (${fmt.format(top.lowestPrice)} S.), in ${top.highestCity} verkaufen (${fmt.format(top.highestPrice)} S.) [+${String.format(Locale.GERMANY, "%.1f", top.marginPercent)}% Marge]!"
        } else {
            "🤖 KI-Bot Live-Suche: Keine Marktlücken mit mind. ${minMarginPercent.toInt()}% Marge unter Beachtung der Filterregeln gefunden."
        }

        val summaryEn = if (top != null) {
            "🤖 AI Bot Live Search: Checked ${comparisons.size} items. Top market gap: ${top.resourceNameDe} buy at ${top.lowestCity} (${fmt.format(top.lowestPrice)} S.), sell at ${top.highestCity} (${fmt.format(top.highestPrice)} S.) [+${String.format(Locale.GERMANY, "%.1f", top.marginPercent)}% margin]!"
        } else {
            "🤖 AI Bot Live Search: No market gaps found with at least ${minMarginPercent.toInt()}% margin respecting filter rules."
        }

        return AiAnalysisResult(
            totalItemsCompared = comparisons.size,
            bestFlips = sortedFlips,
            summaryTextDe = summaryDe,
            summaryTextEn = summaryEn
        )
    }
}
