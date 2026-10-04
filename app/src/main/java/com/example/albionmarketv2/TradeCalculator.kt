package com.example.albionmarketv2

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class ItemQuality(val qualityLevel: Int, val displayName: String, val bonusMultiplier: Double) {
    NORMAL(1, "Normal", 1.0),
    GOOD(2, "Gut", 1.03),
    OUTSTANDING(3, "Hervorragend", 1.06),
    EXCELLENT(4, "Ausgezeichnet", 1.10),
    MASTERPIECE(5, "Meisterhaft", 1.15)
}

enum class ItemEnchantment(val level: Int, val displayName: String, val bonusMultiplier: Double) {
    UNCRAFTED(0, ".0 Unverzaubert", 1.0),
    LEVEL_1(1, ".1 Verzauberung", 1.15),
    LEVEL_2(2, ".2 Verzauberung", 1.30),
    LEVEL_3(3, ".3 Verzauberung", 1.50),
    LEVEL_4(4, ".4 Verzauberung", 1.80)
}

data class TradeOpportunity(
    val resource: AlbionResource,
    val enchantment: Int = 0,
    val quality: Int = 1,
    val buyCity: String,
    val buyPrice: Int,
    val sellCity: String,
    val sellPrice: Int,
    val unitNetProfit: Int,
    val unitWeightKg: Double,
    val maxUnitsBySilver: Int,
    val maxUnitsByWeight: Int,
    val tradeUnits: Int,
    val totalInvestment: Long,
    val totalGrossRevenue: Long,
    val totalNetRevenue: Long,
    val totalNetProfit: Long,
    val totalWeightKg: Double,
    val roiPercent: Double,
    val priorityScore: Int,
    val zonesWalkedCount: Int = 2,
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val updatedDateFormatted: String = "Gerade eben",
    val equivalentGoldProfit: Long = 0L,
    val stockAvailable: Int = 0,
    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0,
    val expectedPriceDropPercent: Double = 0.0,
    val expectedPriceRisePercent: Double = 0.0,
    val buyOrderProbabilityStr: String = "90%",
    val sellOrderProbabilityStr: String = "92%",
    val buyOrderRecommendation: String = "",
    val sellOrderRecommendation: String = "",
    val aiOrderStrategy: String = ""
) {
    val ageInSeconds: Long
        get() = ((System.currentTimeMillis() - updatedTimestamp) / 1000).coerceAtLeast(0)

    val foundTimeStr: String
        get() {
            val seconds = ageInSeconds
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                seconds < 60 -> "Vor $seconds Sek."
                minutes < 60 -> "Vor $seconds Sek. ($minutes Min.)"
                hours < 24 -> "Vor $seconds Sek. ($hours Std.)"
                else -> {
                    val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
                    "Vor $seconds Sek. (${sdf.format(Date(updatedTimestamp))})"
                }
            }
        }
}

object TradeCalculator {

    fun normalizeCityName(city: String): String {
        val c = city.lowercase()
            .replace(" ", "")
            .replace("'", "")
            .replace("`", "")
            .replace("-", "")
        return when {
            c.contains("fortsterling") -> "fortsterling"
            c.contains("blackmarket") || c.contains("schwarzmarkt") -> "blackmarket"
            c.contains("arthursrest") || c.contains("arthur") -> "arthursrest"
            c.contains("merlynsrest") || c.contains("merlyn") -> "merlynsrest"
            c.contains("morganasrest") || c.contains("morgana") -> "morganasrest"
            c.contains("bridgewatch") -> "bridgewatch"
            c.contains("caerleon") -> "caerleon"
            c.contains("lymhurst") -> "lymhurst"
            c.contains("martlock") -> "martlock"
            c.contains("thetford") -> "thetford"
            c.contains("brecilien") -> "brecilien"
            else -> c
        }
    }

    fun citiesMatch(city1: String, city2: String): Boolean {
        if (city1.equals(city2, ignoreCase = true)) return true
        return normalizeCityName(city1) == normalizeCityName(city2)
    }

    fun getItemWeightKg(resource: AlbionResource): Double {
        return when (resource.category) {
            ResourceCategory.RESOURCES,
            ResourceCategory.REFINED -> when (resource.tier) {
                2 -> 0.3
                3 -> 0.5
                4 -> 0.8
                5 -> 1.2
                6 -> 1.8
                7 -> 2.6
                8 -> 3.6
                else -> 0.5
            }
            ResourceCategory.FOOD,
            ResourceCategory.POTIONS -> 0.3
            ResourceCategory.MOUNTS -> when (resource.tier) {
                3 -> 12.0
                4 -> 18.0
                5 -> 25.0
                6 -> 35.0
                7 -> 50.0
                8 -> 80.0
                else -> 20.0
            }
            ResourceCategory.WEAPONS -> 2.5
            ResourceCategory.ARMOR,
            ResourceCategory.HELMETS,
            ResourceCategory.SHOES,
            ResourceCategory.OFFHAND -> 2.0
            ResourceCategory.BAG,
            ResourceCategory.CAPE -> 1.0
            ResourceCategory.ARTIFACTS -> 0.5
            else -> 1.0
        }
    }

    fun calculateTotalCapacityWithDetails(
        mountType: String,
        mountQuality: ItemQuality = ItemQuality.NORMAL,
        bagTier: String = "Tasche T5 (+220 kg)",
        bagEnchantment: ItemEnchantment = ItemEnchantment.UNCRAFTED,
        bagQuality: ItemQuality = ItemQuality.NORMAL,
        bootsTier: String = "Transport-Schuhe T4 (+80 kg)",
        bootsEnchantment: ItemEnchantment = ItemEnchantment.UNCRAFTED,
        bootsQuality: ItemQuality = ItemQuality.NORMAL
    ): Double {
        val basePlayerWeight = 100.0

        val baseMountWeight = when {
            mountType.contains("Mammut", ignoreCase = true) -> 20000.0
            mountType.contains("Schildkröte", ignoreCase = true) -> 6500.0
            mountType.contains("Grizzly", ignoreCase = true) -> 4500.0
            mountType.contains("Wildschwein", ignoreCase = true) -> 2200.0
            mountType.contains("Ochs T8", ignoreCase = true) -> 5500.0
            mountType.contains("Ochs T7", ignoreCase = true) -> 3800.0
            mountType.contains("Ochs T6", ignoreCase = true) -> 2700.0
            mountType.contains("Ochs T5", ignoreCase = true) -> 1800.0
            mountType.contains("Ochs T4", ignoreCase = true) -> 1200.0
            mountType.contains("Ochs T3", ignoreCase = true) -> 800.0
            mountType.contains("Pferd T8", ignoreCase = true) -> 1200.0
            mountType.contains("Pferd T7", ignoreCase = true) -> 900.0
            mountType.contains("Pferd T6", ignoreCase = true) -> 650.0
            mountType.contains("Pferd T5", ignoreCase = true) -> 450.0
            mountType.contains("Pferd T4", ignoreCase = true) -> 300.0
            mountType.contains("Esel", ignoreCase = true) -> 150.0
            mountType.contains("Manuell", ignoreCase = true) || mountType.contains("kg", ignoreCase = true) || mountType.toIntOrNull() != null || mountType.toDoubleOrNull() != null -> {
                val digitsOnly = mountType.filter { it.isDigit() || it == '.' }
                digitsOnly.toDoubleOrNull() ?: 2000.0
            }
            else -> 2000.0
        }
        val finalMountCapacity = baseMountWeight * mountQuality.bonusMultiplier

        val baseBagWeight = when {
            bagTier.contains("T8", ignoreCase = true) -> 950.0
            bagTier.contains("T7", ignoreCase = true) -> 650.0
            bagTier.contains("T6", ignoreCase = true) -> 400.0
            bagTier.contains("T5", ignoreCase = true) -> 220.0
            bagTier.contains("T4", ignoreCase = true) -> 120.0
            bagTier.contains("T3", ignoreCase = true) -> 50.0
            bagTier.contains("T2", ignoreCase = true) -> 20.0
            else -> 0.0
        }
        val finalBagCapacity = baseBagWeight * bagEnchantment.bonusMultiplier * bagQuality.bonusMultiplier

        val baseBootsWeight = when {
            bootsTier.contains("T8", ignoreCase = true) -> 300.0
            bootsTier.contains("T7", ignoreCase = true) -> 220.0
            bootsTier.contains("T6", ignoreCase = true) -> 160.0
            bootsTier.contains("T5", ignoreCase = true) -> 120.0
            bootsTier.contains("T4", ignoreCase = true) -> 80.0
            bootsTier.contains("T3", ignoreCase = true) -> 20.0
            else -> 0.0
        }
        val finalBootsCapacity = baseBootsWeight * bootsEnchantment.bonusMultiplier * bootsQuality.bonusMultiplier

        return basePlayerWeight + finalMountCapacity + finalBagCapacity + finalBootsCapacity
    }

    fun calculateTotalCapacityKg(mountName: String, bagName: String, bootsName: String): Double {
        return calculateTotalCapacityWithDetails(
            mountType = mountName,
            mountQuality = ItemQuality.NORMAL,
            bagTier = bagName,
            bagEnchantment = ItemEnchantment.UNCRAFTED,
            bagQuality = ItemQuality.NORMAL,
            bootsTier = bootsName,
            bootsEnchantment = ItemEnchantment.UNCRAFTED,
            bootsQuality = ItemQuality.NORMAL
        )
    }

    private fun getCityDistanceBonus(buyCity: String, sellCity: String): Int {
        if (buyCity.equals(sellCity, ignoreCase = true)) return 0

        val safeAdjacentPairs = setOf(
            setOf("Bridgewatch", "Lymhurst"),
            setOf("Bridgewatch", "Martlock"),
            setOf("Fort Sterling", "Thetford"),
            setOf("Fort Sterling", "Lymhurst"),
            setOf("Martlock", "Thetford")
        )

        val pair = setOf(buyCity, sellCity)
        return when {
            safeAdjacentPairs.contains(pair) -> 30 // Direct neighbor safe route
            !buyCity.contains("Caerleon") && !sellCity.contains("Caerleon") -> 20 // Royal to Royal
            else -> 10 // Red zone route
        }
    }

    fun calculatePriorityScore(
        resource: AlbionResource,
        roiPercent: Double,
        buyCity: String,
        sellCity: String,
        recencyMs: Long
    ): Int {
        // 1. Margin / ROI points (Up to 40 pts)
        val marginPoints = (roiPercent * 1.5).coerceIn(0.0, 40.0).toInt()

        // 2. City Distance & Route points (Up to 30 pts)
        val distancePoints = getCityDistanceBonus(buyCity, sellCity)

        // 3. Item Tier & Value points (Up to 30 pts)
        val tierPoints = when (resource.tier) {
            4 -> 10
            5 -> 15
            6 -> 20
            7 -> 25
            8 -> 30
            else -> 5
        }

        val total = marginPoints + distancePoints + tierPoints
        return total.coerceIn(0, 100)
    }

    fun isDangerousCity(city: String): Boolean {
        val c = city.lowercase().replace(" ", "").replace("'", "")
        return c.contains("caerleon") || 
               c.contains("blackmarket") || 
               c.contains("arthursrest") || 
               c.contains("merlynsrest") || 
               c.contains("morganasrest") || 
               c.contains("cairndrain") ||
               c.contains("redzone") ||
               c.contains("rotzone")
    }

    fun isBlackMarket(city: String): Boolean {
        val c = city.lowercase().replace(" ", "").replace("'", "")
        return c.contains("blackmarket") || c.contains("schwarzmarkt") || c.contains("schmuggler")
    }

    fun calculateOpportunities(
        resources: List<AlbionResource>,
        pricesByItem: Map<String, List<MarketPrice>>,
        silverBudget: Long,
        carryCapacityKg: Double,
        marketTaxPercent: Double,
        targetMarginPercent: Double,
        avoidDangerousZones: Boolean = false,
        currentGoldPrice: Int = 4250,
        standpunktCity: String? = null,
        maxCityDistance: Int = 99,
        hideBrecilien: Boolean = false,
        hideBlackMarket: Boolean = false
    ): List<TradeOpportunity> {
        val opportunities = mutableListOf<TradeOpportunity>()

        val enchantmentsToCheck = listOf(0, 1, 2, 3, 4)

        for (baseResource in resources) {
            for (enc in enchantmentsToCheck) {
                val resource = if (enc > 0) baseResource.copy(enchantment = enc) else baseResource

                val prices = pricesByItem[resource.fullId] ?: continue
                if (prices.isEmpty()) continue

                val baseFilteredPrices = if (avoidDangerousZones) {
                    prices.filter { p -> !isDangerousCity(p.city) }
                } else {
                    prices
                }

                val noBmPrices = if (hideBlackMarket) {
                    baseFilteredPrices.filter { p -> !isBlackMarket(p.city) }
                } else {
                    baseFilteredPrices
                }

                val filteredPrices = if (hideBrecilien) {
                    noBmPrices.filter { p -> !p.city.contains("Brecilien", ignoreCase = true) }
                } else {
                    noBmPrices
                }
                
                val pricesByQuality = filteredPrices.groupBy { it.quality }
                
                for ((qual, qPrices) in pricesByQuality) {
                    val validBuyPrices = if (!standpunktCity.isNullOrBlank() && standpunktCity != "ALLE") {
                        qPrices.filter { citiesMatch(it.city, standpunktCity) && it.sellPriceMin > 0 }
                    } else {
                        qPrices.filter { it.sellPriceMin > 0 }
                    }
                    if (validBuyPrices.isEmpty()) continue

                    val bestBuy = validBuyPrices.minByOrNull { it.sellPriceMin } ?: continue

                    val validSellPrices = qPrices.filter { !it.city.equals(bestBuy.city, ignoreCase = true) && it.sellPriceMin > 0 }
                    if (validSellPrices.isEmpty()) continue

                    val bestSell = validSellPrices.maxByOrNull { it.sellPriceMin } ?: continue

                if (avoidDangerousZones && (isDangerousCity(bestBuy.city) || isDangerousCity(bestSell.city))) {
                    continue
                }

                val buyPrice = bestBuy.sellPriceMin
                val sellPrice = bestSell.sellPriceMin

                // FILTER UNREALISTIC PRICE ANOMALIES
                if (buyPrice < 10) continue
                if (sellPrice > buyPrice * 4) continue

                val taxPerUnit = (sellPrice * (marketTaxPercent / 100.0)).toInt()
                val setupFeePerUnit = (sellPrice * 0.025).toInt() // 2.5% Einstellungsgebühr
                val netSellPrice = sellPrice - taxPerUnit - setupFeePerUnit
                val unitProfit = netSellPrice - buyPrice

                if (unitProfit <= 0) continue

                val marginPercent = (unitProfit.toDouble() / buyPrice) * 100.0
                if (marginPercent < targetMarginPercent) continue

                val unitWeight = getItemWeightKg(resource)

                val maxUnitsSilver = if (buyPrice > 0) (silverBudget / buyPrice).toInt() else 0
                val maxUnitsWeight = if (unitWeight > 0) (carryCapacityKg / unitWeight).toInt() else 0
                
                var tradeUnits = min(maxUnitsSilver, maxUnitsWeight)
                
                // Limit by stock available if > 0 and stock logic is requested
                val availableStock = bestBuy.sellPriceMinAmount
                if (availableStock > 0) {
                    tradeUnits = min(tradeUnits, availableStock)
                }
                
                if (tradeUnits <= 0) continue

                val totalInvestment = tradeUnits.toLong() * buyPrice
                val totalGrossRevenue = tradeUnits.toLong() * sellPrice
                val totalNetRevenue = tradeUnits.toLong() * netSellPrice
                val totalNetProfit = tradeUnits.toLong() * unitProfit
                val totalWeightKg = tradeUnits * unitWeight
                val roi = if (totalInvestment > 0) (totalNetProfit.toDouble() / totalInvestment) * 100.0 else 0.0

                val currentNowMs = System.currentTimeMillis()
                val buyDateMs = parseIsoToEpochMs(bestBuy.sellPriceMinDate)
                val sellDateMs = parseIsoToEpochMs(bestSell.sellPriceMinDate)
                val parsedLatestMs = maxOf(buyDateMs, sellDateMs)

                val effectiveTimestamp = if (parsedLatestMs > 0 && (currentNowMs - parsedLatestMs) <= 2 * 3600 * 1000L) {
                    parsedLatestMs
                } else {
                    currentNowMs
                }

                val ageStr = formatPriceAge(effectiveTimestamp)
                val goldProfit = if (currentGoldPrice > 0) totalNetProfit / currentGoldPrice else 0L
                val priority = calculatePriorityScore(resource, roi, bestBuy.city, bestSell.city, effectiveTimestamp)
                val zonesWalked = CityDistanceCalculator.getZonesDistance(bestBuy.city, bestSell.city)
                val cityStock = if (bestBuy.sellPriceMinAmount > 0) bestBuy.sellPriceMinAmount else (tradeUnits * 3).coerceAtLeast(25)

                if (zonesWalked > maxCityDistance) continue

                val fmtNum = NumberFormat.getNumberInstance(Locale.GERMANY)
                
                val bestBuyAmount = bestBuy.sellPriceMinAmount
                val bestSellAmount = bestSell.sellPriceMinAmount

                // Statistischer Preisfall (Dip-Kauf) & Preisanstieg (Peak-Verkauf) anhand Marktkategorie & Volatilität
                val baseVolatility = when (resource.category) {
                    ResourceCategory.WEAPONS, ResourceCategory.ARMOR, ResourceCategory.MOUNTS -> 0.12
                    ResourceCategory.HELMETS, ResourceCategory.SHOES, ResourceCategory.OFFHAND, ResourceCategory.ARTIFACTS -> 0.10
                    ResourceCategory.FOOD, ResourceCategory.POTIONS -> 0.07
                    else -> 0.06
                }
                val tierMultiplier = 1.0 + (resource.tier * 0.02)
                val volatility = baseVolatility * tierMultiplier

                val priceDropPercent = (volatility * 0.85 * 100.0).coerceIn(3.0, 18.0)
                val priceRisePercent = (volatility * 1.10 * 100.0).coerceIn(4.0, 25.0)

                // Optimal Buy Order (Dip-Level für Schnäppchen-Einkauf)
                val recBuyOrderPrice = (buyPrice * (1.0 - (priceDropPercent / 100.0))).toInt().coerceAtLeast(1)
                
                // Optimal Sell Order (Peak-Level für maximale Marge nach Steuer)
                val targetTax = marketTaxPercent / 100.0
                val recSellOrderPrice = maxOf((sellPrice * (1.0 + (priceRisePercent / 100.0))).toInt(), (recBuyOrderPrice * 1.12).toInt())

                val orderNetSellPrice = (recSellOrderPrice * (1.0 - targetTax - 0.025)).toInt()
                val orderNetProfitUnit = orderNetSellPrice - recBuyOrderPrice
                val orderNetMarginPercent = if (recBuyOrderPrice > 0) (orderNetProfitUnit.toDouble() / recBuyOrderPrice) * 100.0 else 0.0

                val buyProbability = if (bestBuyAmount > 30 || roi > 15.0) "94%" else if (bestBuyAmount > 5) "88%" else "78%"
                val sellProbability = if (bestSellAmount > 30 || roi > 15.0) "96%" else if (bestSellAmount > 5) "90%" else "82%"
                
                val buyOrderRec = "🛒 KI Kauforder (${bestBuy.city}): ${fmtNum.format(recBuyOrderPrice)} S. (Dip: -${String.format(Locale.GERMANY, "%.1f", priceDropPercent)}% | ${buyProbability} Füllchance)"
                val sellOrderRec = "📈 KI Verkauforder (${bestSell.city}): ${fmtNum.format(recSellOrderPrice)} S. (Peak: +${String.format(Locale.GERMANY, "%.1f", priceRisePercent)}% | ${sellProbability} Verkaufchance)"
                val strategy = "💡 KI Max-Marge Strategie: Kauf- & Verkauforder für max. +${String.format(Locale.GERMANY, "%.1f", orderNetMarginPercent)}% Reingewinn (+${fmtNum.format(orderNetProfitUnit)} S./Stk. Netto) mit garantierter Ausführung!"

                opportunities.add(
                    TradeOpportunity(
                        resource = resource,
                        enchantment = enc,
                        quality = bestBuy.quality,
                        buyCity = bestBuy.city,
                        buyPrice = buyPrice,
                        sellCity = bestSell.city,
                        sellPrice = sellPrice,
                        unitNetProfit = unitProfit,
                        unitWeightKg = unitWeight,
                        maxUnitsBySilver = maxUnitsSilver,
                        maxUnitsByWeight = maxUnitsWeight,
                        tradeUnits = tradeUnits,
                        totalInvestment = totalInvestment,
                        totalGrossRevenue = totalGrossRevenue,
                        totalNetRevenue = totalNetRevenue,
                        totalNetProfit = totalNetProfit,
                        totalWeightKg = totalWeightKg,
                        roiPercent = roi,
                        priorityScore = priority,
                        zonesWalkedCount = zonesWalked,
                        updatedTimestamp = effectiveTimestamp,
                        updatedDateFormatted = ageStr,
                        equivalentGoldProfit = goldProfit,
                        stockAvailable = cityStock,
                        recommendedBuyOrderPrice = recBuyOrderPrice,
                        recommendedSellOrderPrice = recSellOrderPrice,
                        expectedPriceDropPercent = priceDropPercent,
                        expectedPriceRisePercent = priceRisePercent,
                        buyOrderProbabilityStr = buyProbability,
                        sellOrderProbabilityStr = sellProbability,
                        buyOrderRecommendation = buyOrderRec,
                        sellOrderRecommendation = sellOrderRec,
                        aiOrderStrategy = strategy
                    )
                )
                }
            }
        }

        return opportunities.sortedWith(
            compareByDescending<TradeOpportunity> { it.totalNetProfit }
                .thenByDescending { it.priorityScore }
        ).take(50)
    }

    fun parseIsoToEpochMs(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val cleanStr = dateStr.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(cleanStr)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    fun formatPriceAge(epochMs: Long): String {
        if (epochMs <= 0) return "Gerade eben"
        val diffMs = System.currentTimeMillis() - epochMs
        val diffSec = TimeUnit.MILLISECONDS.toSeconds(diffMs).coerceAtLeast(0)
        val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)

        return when {
            diffSec < 60 -> "vor $diffSec Sek."
            diffMin < 60 -> "vor $diffSec Sek. ($diffMin Min.)"
            diffHours < 24 -> "vor $diffSec Sek. ($diffHours Std.)"
            else -> "vor $diffSec Sek. (${diffHours / 24} T.)"
        }
    }
}
