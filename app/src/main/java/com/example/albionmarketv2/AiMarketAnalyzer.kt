package com.example.albionmarketv2

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

data class PriceTrendPrediction(
    val badgeText: String,
    val isBottom: Boolean,
    val isPeak: Boolean,
)

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

    fun predictPriceTrend(currentPrice: Int, cityPrices: List<Int>): PriceTrendPrediction {
        if (cityPrices.isEmpty() || currentPrice <= 0) {
            return PriceTrendPrediction("🟡 Normalpreis", isBottom = false, isPeak = false)
        }
        val avg = cityPrices.average()
        return when {
            currentPrice <= (avg * 0.88) -> PriceTrendPrediction("🟢 Tiefstpreis (Kaufen!)", isBottom = true, isPeak = false)
            currentPrice >= (avg * 1.15) -> PriceTrendPrediction("🔴 Höchstpreis (Warten)", isBottom = false, isPeak = true)
            else -> PriceTrendPrediction("🟡 Normalpreis", isBottom = false, isPeak = false)
        }
    }

    /**
     * Quantitative High-End Volumen & VWAP-Berechnung (Volume Weighted Average Price)
     * Berechnet den echten Marktpreis gewichtet nach Handelsvolumen.
     */
    fun calculateVwapAndQuantMetrics(
        itemId: String,
        city: String,
        historyEntries: List<ItemHistoryEntry>,
        currentAskPrice: Int
    ): MarketQuantMetrics {
        val cityHistory = historyEntries.filter { (it.itemId == itemId) && TradeCalculator.citiesMatch(it.city, city) }
        if (cityHistory.isEmpty()) {
            return MarketQuantMetrics(
                itemId = itemId,
                city = city,
                vwap24h = currentAskPrice.toLong(),
                totalVolume24h = 0L,
                liquidityLevel = LiquidityLevel.LOW,
                isPriceManipulated = false,
                priceVolatilityPercent = 0.0,
                blackMarketSinkVelocity = "Unbekannt (Kein Historien-Volumen)"
            )
        }

        var totalVolume = 0L
        var totalWeightedSum = 0L

        for (entry in cityHistory) {
            totalVolume += entry.itemCount
            totalWeightedSum += (entry.avgPrice * entry.itemCount)
        }

        val vwap = if (totalVolume > 0) (totalWeightedSum / totalVolume) else currentAskPrice.toLong()

        val liquidity = when {
            totalVolume >= LiquidityLevel.HIGH.minDailyVolume -> LiquidityLevel.HIGH
            totalVolume >= LiquidityLevel.MEDIUM.minDailyVolume -> LiquidityLevel.MEDIUM
            totalVolume >= LiquidityLevel.LOW.minDailyVolume -> LiquidityLevel.LOW
            else -> LiquidityLevel.VERY_LOW
        }

        // Price manipulation detection: If current sell price is > 1.75x VWAP or < 0.4x VWAP
        val isManipulated = (vwap > 0) && ((currentAskPrice > vwap * 1.75) || (currentAskPrice < vwap * 0.4))

        val diffVwap = if (vwap > 0) abs((currentAskPrice - vwap).toDouble() / vwap * 100.0) else 0.0

        val sinkVelocityText = when {
            itemId.contains("T4") || itemId.contains("T5") || itemId.contains("T6") -> "⚡ Schneller System-Drain (Hohe Mob-Drop Rate)"
            itemId.contains("T7") -> "⏩ Moderater System-Drain (Ca. 2-6h Verkaufszeit)"
            itemId.contains("T8") -> "🐢 Träger System-Drain (Hohes Silber-Risiko)"
            else -> "🔄 Standard Markt-Rotation"
        }

        return MarketQuantMetrics(
            itemId = itemId,
            city = city,
            vwap24h = vwap,
            totalVolume24h = totalVolume,
            liquidityLevel = liquidity,
            isPriceManipulated = isManipulated,
            priceVolatilityPercent = diffVwap,
            blackMarketSinkVelocity = sinkVelocityText
        )
    }

    /**
     * KI-Bot Suchregeln für präzise Marktlücken:
     * 1. Zonen-Sicherheit: Ausschluss von Caerleon & Rotzonen wenn 'avoidDangerousZones' aktiv.
     * 2. Budget & Kapazität: Berücksichtigung von Silber-Budget und Traglast.
     * 3. Brecilien-Filter: Ausschluss von Brecilien wenn 'hideBrecilien' aktiv.
     * 4. Anomalie-Filter: Verwerfung unrealistischer Preisspitzen (> 4x oder < 10 Silber).
     * 5. Echtzeit-Aktualisierung: Laufender 5-Sekunden-Suchloop.
     */
    fun analyzeMarketPrices(
        pricesMap: Map<String, List<MarketPrice>>,
        minMarginPercent: Double = 5.0,
        avoidDangerousZones: Boolean = false,
        silverBudget: Long = 10_000_000L,
        carryCapacityKg: Double = 2000.0,
        hideBrecilien: Boolean = false,
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

            val validPrices = if (hideBrecilien) {
                noBm.filter { !TradeCalculator.isBrecilien(it.city) }
            } else {
                noBm
            }

            if (validPrices.size < 2) continue

            // Find lowest & highest price in different cities
            val lowest = validPrices.minByOrNull { it.sellPriceMin } ?: continue
            val validSellPrices = validPrices.filter { !TradeCalculator.citiesMatch(it.city, lowest.city) }
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

            // Top-of-Book Pricing Strategy for Instant Market Fill
            // 1. Verkauforder: -1 Silber unter das aktuell günstigste Angebot in der Zielstadt, um Platz 1 zu belegen.
            val recSellOrder = (sellPrice - 1).coerceAtLeast(buyPrice + 1)
            // 2. Kauforder: Biete konkurrenzfähig oder per Dip-Level (-7%)
            val recBuyOrder = if (lowest.buyPriceMax > 10) (lowest.buyPriceMax + 1) else (buyPrice * 0.93).toInt().coerceAtLeast(1)

            val recText = "🎯 Platz 1 Verkauforder: ${fmt.format(recSellOrder)} S. in ${highest.city} (-1 S. unter Konkurrenz für schnellen Verkauf) | 🛒 Kauforder: ${fmt.format(recBuyOrder)} S. in ${lowest.city}"

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
                    expectedPriceDropPercent = 7.0,
                    expectedPriceRisePercent = 8.5,
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

    fun calculateAdminMergeAndTradeOpportunities(
        pricesMap: Map<String, List<MarketPrice>>,
        silverBudget: Long = 10_000_000L,
        selectedCategoryName: String = "ALL",
        avoidDangerousZones: Boolean = false,
        enableMergeBot: Boolean = true,
        topN: Int = 10
    ): List<AdminMergeAndTradeOpportunity> {
        val analysis = analyzeMarketPrices(
            pricesMap = pricesMap,
            minMarginPercent = 1.0,
            avoidDangerousZones = avoidDangerousZones,
            silverBudget = silverBudget
        )

        return analysis.bestFlips.take(topN).map { flip ->
            AdminMergeAndTradeOpportunity(
                resourceId = flip.resourceId,
                resourceNameDe = flip.resourceNameDe,
                buyCity = flip.lowestCity,
                sellCity = flip.highestCity,
                predictedBuyOrderPrice = flip.lowestPrice,
                predictedSellOrderPrice = flip.highestPrice,
                maxMarginPercent = flip.marginPercent,
                totalExpectedProfit = flip.profitPerUnit.toLong() * (silverBudget / flip.lowestPrice.coerceAtLeast(1)).coerceAtLeast(1),
                isMergeOpportunity = enableMergeBot,
                strategyRecommendationDe = flip.aiRecommendationDe,
                zoneSafetyText = if (avoidDangerousZones) "🟢 Sichere Route" else "🟡 Standard Route",
                statisticalGuaranteeLabel = "KI Statistische Marge"
            )
        }
    }
}

data class AdminMergeAndTradeOpportunity(
    val resourceId: String,
    val resourceNameDe: String,
    val buyCity: String,
    val sellCity: String,
    val predictedBuyOrderPrice: Int,
    val predictedSellOrderPrice: Int,
    val maxMarginPercent: Double,
    val totalExpectedProfit: Long,
    val isMergeOpportunity: Boolean = false,
    val strategyRecommendationDe: String = "",
    val zoneSafetyText: String = "",
    val statisticalGuaranteeLabel: String = ""
) {
    fun toTradeOpportunity(silverBudget: Long): TradeOpportunity {
        val res = AlbionResourceRepository.resources.find { it.fullId == resourceId || it.id == resourceId }
            ?: AlbionResource(id = resourceId, nameDe = resourceNameDe, nameEn = resourceNameDe, tier = 4, category = ResourceCategory.ALL)
        val buy = predictedBuyOrderPrice.coerceAtLeast(1)
        val sell = predictedSellOrderPrice.coerceAtLeast(1)
        val units = (silverBudget / buy).toInt().coerceAtLeast(1)
        val unitProfit = (sell - buy).coerceAtLeast(0)
        val totalInv = buy.toLong() * units
        val totalRev = sell.toLong() * units
        val totalProf = unitProfit.toLong() * units
        val weight = TradeCalculator.getItemWeightKg(res)
        val totalWeight = weight * units
        val roi = if (totalInv > 0) (totalProf.toDouble() / totalInv) * 100.0 else 0.0

        return TradeOpportunity(
            resource = res,
            buyCity = buyCity,
            buyPrice = buy,
            sellCity = sellCity,
            sellPrice = sell,
            unitNetProfit = unitProfit,
            unitWeightKg = weight,
            maxUnitsBySilver = units,
            maxUnitsByWeight = units,
            tradeUnits = units,
            totalInvestment = totalInv,
            totalGrossRevenue = totalRev,
            totalNetRevenue = totalRev,
            totalNetProfit = totalProf,
            totalWeightKg = totalWeight,
            roiPercent = roi,
            priorityScore = 80
        )
    }
}

object AdvancedTradingBot {
    fun calculateAdminMergeAndTradeOpportunities(
        pricesMap: Map<String, List<MarketPrice>>,
        silverBudget: Long = 10_000_000L,
        selectedCategoryName: String = "ALL",
        avoidDangerousZones: Boolean = false,
        enableMergeBot: Boolean = true,
        topN: Int = 10
    ): List<AdminMergeAndTradeOpportunity> {
        return AiMarketAnalyzer.calculateAdminMergeAndTradeOpportunities(
            pricesMap, silverBudget, selectedCategoryName, avoidDangerousZones, enableMergeBot, topN
        )
    }
}

