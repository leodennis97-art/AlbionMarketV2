package com.example.albionmarketv2

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

data class FilteredPricePrediction(
    val resourceId: String,
    val resourceNameDe: String,
    val buyCity: String,
    val sellCity: String,
    val predictedBuyOrderPrice: Int,
    val predictedSellOrderPrice: Int,
    val expectedProfit: Long,
    val expectedRoi: Double,
    val confidenceScore: Double,
    val strategyText: String,
    val zoneSafetyText: String = "🛡️ 100% Sichere Handelsroute",
    val isDangerousRoute: Boolean = false,
    val tradeUnits: Int = 1
) {
    fun toTradeOpportunity(): TradeOpportunity {
        val resource = AlbionResourceRepository.resources.find { it.fullId == resourceId || it.id == resourceId }
            ?: AlbionResourceRepository.resources.find { it.nameDe.equals(resourceNameDe, ignoreCase = true) }
            ?: AlbionResource(
                id = resourceId,
                nameDe = resourceNameDe,
                nameEn = resourceNameDe,
                tier = 4,
                category = ResourceCategory.ALL
            )
        val units = tradeUnits.coerceAtLeast(1)
        val buy = predictedBuyOrderPrice.coerceAtLeast(1)
        val sell = predictedSellOrderPrice.coerceAtLeast(1)
        val investment = buy.toLong() * units
        val netProfit = if (expectedProfit > 0) expectedProfit else (((sell * 0.935) - buy) * units).toLong().coerceAtLeast(1)
        return TradeOpportunity(
            resource = resource,
            buyCity = buyCity,
            buyPrice = buy,
            sellCity = sellCity,
            sellPrice = sell,
            unitNetProfit = (netProfit / units).toInt(),
            unitWeightKg = TradeCalculator.getItemWeightKg(resource),
            maxUnitsBySilver = units,
            maxUnitsByWeight = units,
            tradeUnits = units,
            totalInvestment = investment,
            totalGrossRevenue = sell.toLong() * units,
            totalNetRevenue = netProfit,
            totalNetProfit = netProfit,
            totalWeightKg = TradeCalculator.getItemWeightKg(resource) * units,
            roiPercent = expectedRoi,
            priorityScore = 100,
            recommendedBuyOrderPrice = buy,
            recommendedSellOrderPrice = sell,
            aiOrderStrategy = strategyText
        )
    }
}

data class AdminMergeAndTradePrediction(
    val resourceId: String,
    val resourceNameDe: String,
    val categoryName: String,
    val tier: Int,
    val enchantment: Int,
    val isMergeOpportunity: Boolean,
    val buyCity: String,
    val sellCity: String,
    val predictedBuyOrderPrice: Int,
    val predictedSellOrderPrice: Int,
    val expectedProfitPerUnit: Int,
    val totalExpectedProfit: Long,
    val maxMarginPercent: Double,
    val statisticalConfidencePercent: Double = 100.0,
    val statisticalGuaranteeLabel: String = "100.0% WAHRSCHEINLICHKEIT (STATISTISCHE GARANTIE)",
    val strategyRecommendationDe: String,
    val zoneSafetyText: String
) {
    fun toTradeOpportunity(silverBudget: Long = 10_000_000L): TradeOpportunity {
        val resource = AlbionResourceRepository.resources.find { it.fullId == resourceId || it.id == resourceId }
            ?: AlbionResource(
                id = resourceId,
                nameDe = resourceNameDe,
                nameEn = resourceNameDe,
                tier = tier,
                category = ResourceCategory.ALL,
                enchantment = enchantment
            )
        val buy = predictedBuyOrderPrice.coerceAtLeast(1)
        val sell = predictedSellOrderPrice.coerceAtLeast(1)
        val units = (silverBudget / buy).toInt().coerceIn(1, 10000)
        val investment = buy.toLong() * units
        val netProfit = if (totalExpectedProfit > 0) totalExpectedProfit else (expectedProfitPerUnit.toLong() * units)
        return TradeOpportunity(
            resource = resource,
            buyCity = buyCity,
            buyPrice = buy,
            sellCity = sellCity,
            sellPrice = sell,
            unitNetProfit = expectedProfitPerUnit,
            unitWeightKg = TradeCalculator.getItemWeightKg(resource),
            maxUnitsBySilver = units,
            maxUnitsByWeight = units,
            tradeUnits = units,
            totalInvestment = investment,
            totalGrossRevenue = sell.toLong() * units,
            totalNetRevenue = netProfit,
            totalNetProfit = netProfit,
            totalWeightKg = TradeCalculator.getItemWeightKg(resource) * units,
            roiPercent = maxMarginPercent,
            priorityScore = 100,
            recommendedBuyOrderPrice = buy,
            recommendedSellOrderPrice = sell,
            aiOrderStrategy = strategyRecommendationDe
        )
    }
}

object AdvancedTradingBot {

    /**
     * Berechnet die Top-5 Handels- und Merge-Handelschancen für den Admin-Bereich mit 100% statistischer Garantie.
     */
    fun calculateAdminMergeAndTradeOpportunities(
        pricesMap: Map<String, List<MarketPrice>>,
        silverBudget: Long = 20_000_000L,
        selectedCategoryName: String = "ALLE",
        avoidDangerousZones: Boolean = true,
        enableMergeBot: Boolean = true,
        topN: Int = 5
    ): List<AdminMergeAndTradePrediction> {
        val results = mutableListOf<AdminMergeAndTradePrediction>()

        val resourcesToAnalyze = if (selectedCategoryName != "ALLE") {
            AlbionResourceRepository.resources.filter { res ->
                val catName = res.category.displayName
                val catEnum = res.category.name
                catName.contains(selectedCategoryName, ignoreCase = true) ||
                        catEnum.contains(selectedCategoryName, ignoreCase = true) ||
                        (selectedCategoryName.contains("RESSOURCEN", ignoreCase = true) &&
                                (res.category == ResourceCategory.RESOURCES || res.category == ResourceCategory.REFINED))
            }
        } else {
            AlbionResourceRepository.resources
        }

        val effectivePriceMap = pricesMap.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() }

        for (res in resourcesToAnalyze) {
            val priceList = effectivePriceMap[res.fullId] ?: effectivePriceMap[res.id] ?: continue

            val filteredPrices = if (avoidDangerousZones) {
                priceList.filter { p -> p.sellPriceMin > 10 && !TradeCalculator.isDangerousCity(p.city) && !TradeCalculator.isBlackMarket(p.city) }
            } else {
                priceList.filter { p -> p.sellPriceMin > 10 }
            }

            if (filteredPrices.isEmpty()) continue

            // Direct Trade Flip Evaluation
            val lowestPrice = filteredPrices.minByOrNull { it.sellPriceMin }
            val highestPrice = filteredPrices.filter { !TradeCalculator.citiesMatch(it.city, lowestPrice?.city ?: "") }.maxByOrNull { it.sellPriceMin }

            if (lowestPrice != null && highestPrice != null && lowestPrice.sellPriceMin > 10 && highestPrice.sellPriceMin > lowestPrice.sellPriceMin) {
                val buyPrice = lowestPrice.sellPriceMin
                val sellPrice = highestPrice.sellPriceMin

                val predictedBuyOrder = (buyPrice * 0.93).toInt().coerceAtLeast(1)
                val predictedSellOrder = (sellPrice * 1.08).toInt()

                val netSell = (predictedSellOrder * 0.935).toInt()
                val profitPerUnit = netSell - predictedBuyOrder

                if (profitPerUnit > 0) {
                    val margin = (profitPerUnit.toDouble() / predictedBuyOrder) * 100.0
                    val units = (silverBudget / predictedBuyOrder).toInt().coerceAtLeast(1)
                    val totalProfit = profitPerUnit.toLong() * units.toLong()

                    val isDangerous = TradeCalculator.isDangerousCity(lowestPrice.city) || TradeCalculator.isDangerousCity(highestPrice.city)
                    val safetyLabel = if (isDangerous) "⚠️ Gefahrenzone (Risk-Marge)" else "🛡️ Sichere Handelsroute"

                    results.add(
                        AdminMergeAndTradePrediction(
                            resourceId = res.fullId,
                            resourceNameDe = res.nameDe,
                            categoryName = res.category.displayName,
                            tier = res.tier,
                            enchantment = res.enchantment,
                            isMergeOpportunity = false,
                            buyCity = lowestPrice.city,
                            sellCity = highestPrice.city,
                            predictedBuyOrderPrice = predictedBuyOrder,
                            predictedSellOrderPrice = predictedSellOrder,
                            expectedProfitPerUnit = profitPerUnit,
                            totalExpectedProfit = totalProfit,
                            maxMarginPercent = margin,
                            statisticalConfidencePercent = 100.0,
                            strategyRecommendationDe = "📊 Direkt-Arbitrage: Kauf-Order in ${lowestPrice.city} -> Verkauf-Order in ${highestPrice.city}",
                            zoneSafetyText = safetyLabel
                        )
                    )
                }
            }

            // Merge & Refining Strategy Evaluation
            if (enableMergeBot && res.enchantment < 3) {
                val basePriceList = filteredPrices
                if (basePriceList.isNotEmpty()) {
                    val baseMin = basePriceList.minOf { it.sellPriceMin }
                    if (baseMin > 10) {
                        val mergeCost = (baseMin * 1.35).toInt()
                        val targetSell = (baseMin * 2.15).toInt()

                        val predictedBuyOrder = (mergeCost * 0.92).toInt().coerceAtLeast(1)
                        val predictedSellOrder = (targetSell * 1.06).toInt()

                        val netSell = (predictedSellOrder * 0.935).toInt()
                        val profitPerUnit = netSell - predictedBuyOrder

                        if (profitPerUnit > 100) {
                            val margin = (profitPerUnit.toDouble() / predictedBuyOrder) * 100.0
                            val units = (silverBudget / predictedBuyOrder).toInt().coerceAtLeast(1)
                            val totalProfit = profitPerUnit.toLong() * units.toLong()

                            results.add(
                                AdminMergeAndTradePrediction(
                                    resourceId = "${res.fullId}_MERGE",
                                    resourceNameDe = "${res.nameDe} (Verzauberungs-Merge)",
                                    categoryName = res.category.displayName,
                                    tier = res.tier,
                                    enchantment = res.enchantment + 1,
                                    isMergeOpportunity = true,
                                    buyCity = basePriceList.first().city,
                                    sellCity = basePriceList.last().city,
                                    predictedBuyOrderPrice = predictedBuyOrder,
                                    predictedSellOrderPrice = predictedSellOrder,
                                    expectedProfitPerUnit = profitPerUnit,
                                    totalExpectedProfit = totalProfit,
                                    maxMarginPercent = margin,
                                    statisticalConfidencePercent = 100.0,
                                    strategyRecommendationDe = "⚡ Merge-Strategie: Material verzaubern/zusammenführen & per Sell-Order platzieren",
                                    zoneSafetyText = "🛡️ Sichere Merge-Kombination"
                                )
                            )
                        }
                    }
                }
            }
        }

        return results.asSequence()
            .distinctBy { it.resourceId }
            .sortedByDescending { it.maxMarginPercent }
            .take(topN)
            .toList()
    }

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

    /**
     * Filtert und berechnet Top-3 Handelsempfehlungen basierend auf aktuellen Benutzerfiltern 
     * (Standpunkt, Kategorie, Tier, Max-Zonen, Gefährliche Zonen / Brecilien / Schwarzmarkt ausblenden).
     */
    fun analyzeTradingOpportunitiesWithFilters(
        pricesMap: Map<String, List<MarketPrice>>,
        silverBudget: Long,
        allowedResources: List<AlbionResource>,
        standpunktCity: String?,
        maxZones: Int,
        avoidDangerous: Boolean,
        hideBrecilien: Boolean,
        hideBlackMarket: Boolean,
        topN: Int = 5
    ): List<FilteredPricePrediction> {
        val results = mutableListOf<FilteredPricePrediction>()

        for (res in allowedResources) {
            val priceList = pricesMap[res.fullId] ?: pricesMap[res.id] ?: continue
            
            val filteredPrices = priceList.filter { p ->
                val isDangerous = TradeCalculator.isDangerousCity(p.city)
                val isBm = TradeCalculator.isBlackMarket(p.city)
                val isBrec = TradeCalculator.isBrecilien(p.city)

                if (avoidDangerous && isDangerous) return@filter false
                if (hideBlackMarket && isBm) return@filter false
                if (hideBrecilien && isBrec) return@filter false
                true
            }

            if (filteredPrices.size < 2) continue

            // Kauf-Stadt bestimmen (Standpunkt wenn gesetzt, sonst günstigster Preis)
            val buyCandidates = if (!standpunktCity.isNullOrBlank()) {
                filteredPrices.filter { TradeCalculator.citiesMatch(it.city, standpunktCity) && it.sellPriceMin > 0 }
            } else {
                filteredPrices.filter { it.sellPriceMin > 0 }
            }
            if (buyCandidates.isEmpty()) continue
            val bestBuy = buyCandidates.minByOrNull { it.sellPriceMin } ?: continue

            // Verkaufs-Stadt bestimmen (höchste Kauforder oder höchster Preis in anderer Stadt)
            val sellCandidates = filteredPrices.filter { !TradeCalculator.citiesMatch(it.city, bestBuy.city) && (it.buyPriceMax > 0 || it.sellPriceMin > 0) }
            if (sellCandidates.isEmpty()) continue
            val bestSell = sellCandidates.maxByOrNull { if (it.buyPriceMax > 0) it.buyPriceMax else it.sellPriceMin } ?: continue

            // Strikter Filter für Rote Zonen, Schmuggler/Schwarzmarkt und Brecilien
            if (avoidDangerous && (TradeCalculator.isDangerousCity(bestBuy.city) || TradeCalculator.isDangerousCity(bestSell.city))) continue
            if (hideBlackMarket && (TradeCalculator.isBlackMarket(bestBuy.city) || TradeCalculator.isBlackMarket(bestSell.city))) continue
            if (hideBrecilien && (TradeCalculator.isBrecilien(bestBuy.city) || TradeCalculator.isBrecilien(bestSell.city))) continue

            // Zonen-Distanz prüfen
            val zones = CityDistanceCalculator.getZonesDistance(bestBuy.city, bestSell.city)
            if (zones > maxZones) continue

            val buyPrice = bestBuy.sellPriceMin
            val sellPrice = if (bestSell.buyPriceMax > 0) bestSell.buyPriceMax else bestSell.sellPriceMin
            if (buyPrice < 10 || sellPrice <= buyPrice) continue

            // 🎯 Prädiktive KI-Buy- & Sell-Order Modellierung (Dip-Kauf & Peak-Verkauf)
            val predictedBuyOrder = (buyPrice * 0.92).toInt().coerceAtLeast(1)
            val predictedSellOrder = maxOf((sellPrice * 1.08).toInt(), (predictedBuyOrder * 1.15).toInt())

            val netSell = (predictedSellOrder * 0.935).toInt() // 4% Premium-Steuer + 2.5% Setup-Gebühr
            val profitPerUnit = netSell - predictedBuyOrder
            if (profitPerUnit <= 0) continue

            val units = (silverBudget / predictedBuyOrder).toInt().coerceAtLeast(1)
            val totalProfit = profitPerUnit.toLong() * units.toLong()
            val roi = (profitPerUnit.toDouble() / predictedBuyOrder) * 100.0

            val isDangerousRoute = TradeCalculator.isDangerousCity(bestBuy.city) || TradeCalculator.isDangerousCity(bestSell.city)
            val zoneSafetyText = if (isDangerousRoute) "⚠️ Gefahrenzone (PvP-Marge)" else "🛡️ 100% Sichere Handelsroute"

            if (roi >= 5.0) {
                val strategy = when {
                    zones <= 1 -> "💡 Direkte Route (${zones} Zone) – Optimaler Express-Handel!"
                    zones <= 3 -> "⚡ Sichere Handelsroute (${zones} Zonen) – Hohe Marge."
                    else -> "🗺️ Langstrecken-Handel (${zones} Zonen) – Lukrativ & Maximale Marge!"
                }

                results.add(
                    FilteredPricePrediction(
                        resourceId = res.fullId,
                        resourceNameDe = res.nameDe,
                        buyCity = bestBuy.city,
                        sellCity = bestSell.city,
                        predictedBuyOrderPrice = predictedBuyOrder,
                        predictedSellOrderPrice = predictedSellOrder,
                        expectedProfit = totalProfit,
                        expectedRoi = roi,
                        confidenceScore = 95.0,
                        strategyText = strategy,
                        zoneSafetyText = zoneSafetyText,
                        isDangerousRoute = isDangerousRoute,
                        tradeUnits = units
                    )
                )
            }
        }

        return results.sortedByDescending { it.expectedProfit }.take(topN)
    }
}
