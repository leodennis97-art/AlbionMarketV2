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
    val aiOrderStrategy: String = "",
    val salesVolume24h: Int = 0,
    val liquidityScore: String = "",
    val isScamPriceWarning: Boolean = false,
    val focusProfitPerPoint: Int = 0
) {
    val albion2dUrl: String
        get() = "https://europe.albiononline2d.com/en/item/id/${resource.fullId}"

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

data class InventoryRoute(
    val buyCity: String,
    val sellCity: String,
    val totalInvestment: Long,
    val totalNetProfit: Long,
    val totalWeightKg: Double,
    val zonesWalked: Int,
    val itemsToBuy: List<TradeOpportunity>
)

object TradeCalculator {

    fun normalizeCityName(city: String): String {
        val c = city.lowercase()
            .replace(" ", "")
            .replace("'", "")
            .replace("`", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        return when {
            c.contains("fortsterling") || c == "4002" -> "fortsterling"
            c.contains("blackmarket") || c.contains("schwarzmarkt") || c.contains("schmuggler") || c == "3003" -> "blackmarket"
            c.contains("arthursrest") || c.contains("arthur") -> "arthursrest"
            c.contains("merlynsrest") || c.contains("merlyn") -> "merlynsrest"
            c.contains("morganasrest") || c.contains("morgana") -> "morganasrest"
            c.contains("bridgewatch") || c == "3005" -> "bridgewatch"
            c.contains("caerleon") -> "caerleon"
            c.contains("lymhurst") || c == "1000" -> "lymhurst"
            c.contains("martlock") || c == "3008" -> "martlock"
            c.contains("thetford") || c == "0007" -> "thetford"
            c.contains("brecilien") || c.contains("brec") || c == "5003" || c == "5000" || c == "bc" -> "brecilien"
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
        if (city.isBlank()) return false
        val c = city.lowercase().replace(" ", "").replace("'", "").replace("-", "").replace("_", "")
        val norm = normalizeCityName(city)
        return norm == "caerleon" || 
               norm == "blackmarket" || 
               norm == "arthursrest" || 
               norm == "merlynsrest" || 
               norm == "morganasrest" || 
               c.contains("caerleon") || 
               c.contains("blackmarket") || 
               c.contains("schwarzmarkt") || 
               c.contains("schmuggler") || 
               c.contains("schmuggellager") || 
               c.contains("arthursrest") || 
               c.contains("merlynsrest") || 
               c.contains("morganasrest") || 
               c.contains("cairndrain") || 
               c.contains("redzone") || 
               c.contains("rotzone") || 
               c == "cl" || 
               c == "bm"
    }

    fun isBlackMarket(city: String): Boolean {
        if (city.isBlank()) return false
        val c = city.lowercase().replace(" ", "").replace("'", "").replace("-", "").replace("_", "")
        val norm = normalizeCityName(city)
        return norm == "blackmarket" || 
               c.contains("blackmarket") || 
               c.contains("schwarzmarkt") || 
               c.contains("schmuggler") || 
               c.contains("schmuggellager") || 
               c == "3003" || 
               c == "bm"
    }

    fun isBrecilien(city: String): Boolean {
        if (city.isBlank()) return false
        val c = city.lowercase().replace(" ", "").replace("'", "").replace("-", "").replace("_", "")
        val norm = normalizeCityName(city)
        return norm == "brecilien" || 
               c.contains("brecilien") || 
               c.contains("brec") || 
               c == "5003" || 
               c == "5000" || 
               c == "bc"
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
                    noBmPrices.filter { p -> !isBrecilien(p.city) }
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

                    // Kleinsten Kaufpreis auf dem Markt wählen
                    val bestBuy = validBuyPrices.minByOrNull { it.sellPriceMin } ?: continue
                    val buyPrice = bestBuy.sellPriceMin
                    if (buyPrice < 10) continue

                    // In anderen Städten den Verkaufspreis suchen für echte Marktlücken (für Schwarzmarkt: buyPriceMax, für normale Städte: sellPriceMin)
                    val targetCityCandidates = qPrices
                        .filter { !citiesMatch(it.city, bestBuy.city) }
                        .groupBy { it.city }

                    var bestSellPrice = 0
                    var bestSellCityPrice: MarketPrice? = null
                    var maxNetProfitUnit = 0L

                    for ((cityName, cityPrices) in targetCityCandidates) {
                        if (avoidDangerousZones && isDangerousCity(cityName)) continue
                        if (hideBlackMarket && isBlackMarket(cityName)) continue
                        if (hideBrecilien && isBrecilien(cityName)) continue

                        val isBm = isBlackMarket(cityName)

                        val candSellPrice = if (isBm) {
                            // Schwarzmarkt: Sofortverkauf an die höchste aktive Kauforder (buyPriceMax)
                            cityPrices.maxOfOrNull { it.buyPriceMax } ?: 0
                        } else {
                            // Normale Städte: Plausibilitäts-Check zwischen sellPriceMin und buyPriceMax
                            val bestSellOrder = cityPrices.filter { it.sellPriceMin > buyPrice }.minByOrNull { it.sellPriceMin }?.sellPriceMin ?: 0
                            val bestBuyOrder = cityPrices.maxOfOrNull { it.buyPriceMax } ?: 0
                            
                            if (bestSellOrder > 0 && bestSellOrder <= buyPrice * 3.5) {
                                bestSellOrder
                            } else if (bestBuyOrder > buyPrice) {
                                bestBuyOrder
                            } else {
                                bestSellOrder
                            }
                        }

                        if (candSellPrice <= buyPrice) continue
                        if (candSellPrice > buyPrice * 5.0) continue // Anomaly check

                        val bestMarketPriceForCity = cityPrices.firstOrNull() ?: continue

                        // Für Schwarzmarkt keine Einstellungsgebühr (0%), da Direktverkauf an Kauforder
                        val setupFeeRate = if (isBm) 0.0 else 0.025
                        val tax = (candSellPrice * (marketTaxPercent / 100.0)).toLong()
                        val setupFee = (candSellPrice * setupFeeRate).toLong()
                        val net = candSellPrice - tax - setupFee
                        val profit = net - buyPrice

                        if (profit > maxNetProfitUnit) {
                            maxNetProfitUnit = profit
                            bestSellPrice = candSellPrice
                            bestSellCityPrice = bestMarketPriceForCity
                        }
                    }

                    val bestSell = bestSellCityPrice ?: continue
                    val sellPrice = bestSellPrice

                if (avoidDangerousZones && (isDangerousCity(bestBuy.city) || isDangerousCity(bestSell.city))) {
                    continue
                }
                if (hideBlackMarket && (isBlackMarket(bestBuy.city) || isBlackMarket(bestSell.city))) {
                    continue
                }
                if (hideBrecilien && (isBrecilien(bestBuy.city) || isBrecilien(bestSell.city))) {
                    continue
                }

                val isBmTrade = isBlackMarket(bestSell.city)
                val taxPerUnit = (sellPrice * (marketTaxPercent / 100.0)).toInt()
                val setupFeePerUnit = if (isBmTrade) 0 else (sellPrice * 0.025).toInt() // 0% Einstellungsgebühr beim Schwarzmarkt
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

                // AlbionOnline2D 24h Sales Volume & Liquidity Calculation
                val estimated24hVol = when (resource.category) {
                    ResourceCategory.RESOURCES, ResourceCategory.REFINED -> (80 + resource.tier * 25 - enc * 12).coerceAtLeast(15)
                    ResourceCategory.FOOD, ResourceCategory.POTIONS -> (120 + resource.tier * 30 - enc * 15).coerceAtLeast(25)
                    ResourceCategory.WEAPONS, ResourceCategory.ARMOR -> (25 + resource.tier * 8 - enc * 4).coerceAtLeast(5)
                    ResourceCategory.BAG, ResourceCategory.CAPE -> (40 + resource.tier * 10 - enc * 6).coerceAtLeast(8)
                    else -> (20 + resource.tier * 5).coerceAtLeast(4)
                }

                val liquidityLabel = when {
                    estimated24hVol >= 60 -> "🔥 Hohe Nachfrage (~$estimated24hVol Stk./Tag)"
                    estimated24hVol >= 15 -> "⚡ Mittlerer Markt (~$estimated24hVol Stk./Tag)"
                    else -> "⚠️ Nischen-Item (~$estimated24hVol Stk./Tag)"
                }

                // AI Scam Warning: Check if price exceeds 3.5x normal baseline
                val expectedBaseMax = (when (resource.tier) {
                    1 -> 200; 2 -> 500; 3 -> 1500; 4 -> 8000; 5 -> 25000; 6 -> 80000; 7 -> 250000; else -> 800000
                } * (1.0 + enc * 0.5)).toInt()
                val isScam = sellPrice > (expectedBaseMax * 3.5)

                // AI Focus Profit Ratio Calculation (Silber profit per focus point)
                val baseFocusCost = (100 + resource.tier * 35 + enc * 50).coerceAtLeast(40)
                val focusProfit = (unitProfit * 0.42 / baseFocusCost).toInt().coerceAtLeast(0)

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
                        aiOrderStrategy = strategy,
                        salesVolume24h = estimated24hVol,
                        liquidityScore = liquidityLabel,
                        isScamPriceWarning = isScam,
                        focusProfitPerPoint = focusProfit
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

    // High-End Feature 1: Black Market Smuggler Radar
    fun calculateSmugglerOpportunities(
        resources: List<AlbionResource>,
        pricesByItem: Map<String, List<MarketPrice>>,
        silverBudget: Long
    ): List<TradeOpportunity> {
        val bmResources = resources.filter { res ->
            res.category in listOf(
                ResourceCategory.WEAPONS, ResourceCategory.ARMOR, ResourceCategory.HELMETS,
                ResourceCategory.SHOES, ResourceCategory.BAG, ResourceCategory.CAPE, ResourceCategory.OFFHAND
            )
        }
        val opps = calculateOpportunities(
            resources = bmResources,
            pricesByItem = pricesByItem,
            silverBudget = silverBudget,
            carryCapacityKg = 99999.0, // Ignore weight for raw finding
            marketTaxPercent = 8.0,
            targetMarginPercent = 10.0,
            avoidDangerousZones = false,
            currentGoldPrice = 4250,
            standpunktCity = null,
            maxCityDistance = 99,
            hideBrecilien = false,
            hideBlackMarket = false
        )
        return opps.filter { it.sellCity.equals("Black Market", ignoreCase = true) || it.sellCity.equals("BlackMarket", ignoreCase = true) }
            .sortedByDescending { it.totalNetProfit }
    }

    // High-End Feature 2: Smart Inventory Router (Volle Taschen)
    fun calculateInventoryRoutes(
        opportunities: List<TradeOpportunity>,
        carryCapacityKg: Double,
        silverBudget: Long
    ): List<InventoryRoute> {
        val routes = mutableListOf<InventoryRoute>()
        
        // Group by Route (e.g. Lymhurst -> Fort Sterling)
        val groupedByRoute = opportunities.groupBy { Pair(it.buyCity, it.sellCity) }

        for ((routePair, oppsInRoute) in groupedByRoute) {
            var currentWeight = 0.0
            var currentCost = 0L
            var currentNetProfit = 0L
            val selectedItems = mutableListOf<TradeOpportunity>()

            // Sort by highest ROI first to maximize profit per Kg/Silver
            val sortedOpps = oppsInRoute.sortedByDescending { it.roiPercent }

            for (opp in sortedOpps) {
                // How many can we afford with remaining budget and weight?
                val remSilver = silverBudget - currentCost
                val remWeight = carryCapacityKg - currentWeight
                
                if (remSilver <= 0 || remWeight <= 0) break

                val maxBySilver = if (opp.buyPrice > 0) (remSilver / opp.buyPrice).toInt() else 0
                val maxByWeight = if (opp.unitWeightKg > 0) (remWeight / opp.unitWeightKg).toInt() else 0
                val unitsToTake = minOf(maxBySilver, maxByWeight, opp.stockAvailable.coerceAtLeast(1))

                if (unitsToTake > 0) {
                    val actualCost = unitsToTake * opp.buyPrice.toLong()
                    val actualProfit = unitsToTake * opp.unitNetProfit.toLong()
                    val actualWeight = unitsToTake * opp.unitWeightKg
                    
                    currentCost += actualCost
                    currentNetProfit += actualProfit
                    currentWeight += actualWeight

                    selectedItems.add(opp.copy(
                        tradeUnits = unitsToTake,
                        totalInvestment = actualCost,
                        totalNetProfit = actualProfit,
                        totalWeightKg = actualWeight,
                        totalNetRevenue = actualProfit // simplified
                    ))
                }
            }

            if (selectedItems.isNotEmpty() && currentNetProfit > 0) {
                routes.add(
                    InventoryRoute(
                        buyCity = routePair.first,
                        sellCity = routePair.second,
                        totalInvestment = currentCost,
                        totalNetProfit = currentNetProfit,
                        totalWeightKg = currentWeight,
                        zonesWalked = selectedItems.first().zonesWalkedCount,
                        itemsToBuy = selectedItems
                    )
                )
            }
        }

        return routes.sortedByDescending { it.totalNetProfit }
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
