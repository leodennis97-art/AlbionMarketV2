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
    val focusProfitPerPoint: Int = 0,
    val realizedRoiPercent: Double = 0.0
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
                
                // Wir ignorieren die Qualität und suchen für jeden Handelsposten (Stadt) den günstigsten Preis!
                val pricesByCity = filteredPrices.groupBy { it.city }
                
                // Wir erstellen eine künstliche Liste, in der jede Stadt nur noch EINEN Eintrag hat (den absolut günstigsten)
                val cheapestPricesAcrossQualities = mutableListOf<MarketPrice>()
                for ((city, cityPrices) in pricesByCity) {
                    val validSells = cityPrices.filter { it.sellPriceMin > 0 && !AlbionMarketApi.isUnrealisticPrice(it.itemId, it.sellPriceMin) }
                    val freshSells = validSells.filter { isPriceFreshWithin10Mins(it) }
                    val sellsToUse = if (freshSells.isNotEmpty()) freshSells else validSells
                    if (sellsToUse.isNotEmpty()) {
                        val cheapest = sellsToUse.minByOrNull { it.sellPriceMin }
                        if (cheapest != null) cheapestPricesAcrossQualities.add(cheapest)
                    } else {
                        // Falls wir keinen Sell-Preis haben, nehmen wir den mit der höchsten Buy-Order (wichtig für Schwarzmarkt)
                        val bestBuyOrder = cityPrices.filter { it.buyPriceMax > 0 }.maxByOrNull { it.buyPriceMax }
                        if (bestBuyOrder != null) cheapestPricesAcrossQualities.add(bestBuyOrder)
                    }
                }

                if (cheapestPricesAcrossQualities.isEmpty()) continue

                val validBuyPrices = if (!standpunktCity.isNullOrBlank() && standpunktCity != "ALLE") {
                    cheapestPricesAcrossQualities.filter { citiesMatch(it.city, standpunktCity) && it.sellPriceMin > 0 }
                } else {
                    cheapestPricesAcrossQualities.filter { it.sellPriceMin > 0 }
                }
                
                if (validBuyPrices.isEmpty()) continue

                // Kleinsten Kaufpreis (über alle Städte hinweg) wählen
                val bestBuy = validBuyPrices.minByOrNull { it.sellPriceMin } ?: continue
                val buyPrice = bestBuy.sellPriceMin
                if (buyPrice < 10) continue

                // In anderen Städten den Verkaufspreis suchen für echte Marktlücken (für Schwarzmarkt: buyPriceMax, für normale Städte: sellPriceMin)
                val targetCityCandidates = cheapestPricesAcrossQualities.filter { !citiesMatch(it.city, bestBuy.city) }

                var bestSellPrice = 0
                var bestSellCityPrice: MarketPrice? = null
                var maxNetProfitUnit = 0L

                for (targetPrice in targetCityCandidates) {
                    val cityName = targetPrice.city
                    if (avoidDangerousZones && isDangerousCity(cityName)) continue
                    if (hideBlackMarket && isBlackMarket(cityName)) continue
                    if (hideBrecilien && isBrecilien(cityName)) continue

                    val isBm = isBlackMarket(cityName)

                    val candSellPrice = if (isBm) {
                        targetPrice.buyPriceMax
                    } else {
                        if (targetPrice.sellPriceMin > 0) targetPrice.sellPriceMin else targetPrice.buyPriceMax
                    }

                    if (candSellPrice <= buyPrice) continue
                    val maxMultiplier = if (buyPrice < 1000) 3.5 else 2.5
                    if (candSellPrice > buyPrice * maxMultiplier) continue // Realistic Anomaly check

                    // Für Schwarzmarkt keine Einstellungsgebühr (0%), da Direktverkauf an Kauforder
                    val setupFeeRate = if (isBm) 0.0 else 0.025
                    val tax = (candSellPrice * (marketTaxPercent / 100.0)).toLong()
                    val setupFee = (candSellPrice * setupFeeRate).toLong()
                    val net = candSellPrice - tax - setupFee
                    val profit = net - buyPrice

                    if (profit > maxNetProfitUnit) {
                        maxNetProfitUnit = profit
                        bestSellPrice = candSellPrice
                        bestSellCityPrice = targetPrice
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
                if (marginPercent < targetMarginPercent || marginPercent > 100.0) continue

                val unitWeight = getItemWeightKg(resource)

                val maxUnitsSilver = if (buyPrice > 0) (silverBudget / buyPrice).toInt() else 0
                val maxUnitsWeight = if (unitWeight > 0) (carryCapacityKg / unitWeight).toInt() else 0
                
                var tradeUnits = maxOf(1, min(maxUnitsSilver, maxUnitsWeight))
                
                // Limit by stock available if > 0
                val availableStock = bestBuy.sellPriceMinAmount
                if (availableStock > 0) {
                    tradeUnits = maxOf(1, min(tradeUnits, availableStock))
                }

                val totalInvestment = tradeUnits.toLong() * buyPrice
                val totalGrossRevenue = tradeUnits.toLong() * sellPrice
                val totalNetRevenue = tradeUnits.toLong() * netSellPrice
                val totalNetProfit = tradeUnits.toLong() * unitProfit
                val totalWeightKg = tradeUnits * unitWeight
                val roi = if (totalInvestment > 0) (totalNetProfit.toDouble() / totalInvestment) * 100.0 else 0.0

                val currentNowMs = System.currentTimeMillis()
                val parsedBuyMs = parseIsoToEpochMs(bestBuy.sellPriceMinDate)
                val parsedSellMs = parseIsoToEpochMs(bestSell.sellPriceMinDate)
                val buyDateMs = if (parsedBuyMs > 0) parsedBuyMs else currentNowMs
                val sellDateMs = if (parsedSellMs > 0) parsedSellMs else currentNowMs

                // Toleranter Alterungs-Filter, entfernt maxAge check komplett
                val effectiveTimestamp = maxOf(buyDateMs, sellDateMs)
                val ageStr = "Kauf: ${formatPriceAge(buyDateMs)} | Verkauf: ${formatPriceAge(sellDateMs)}"
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

                // AlbionOnline Top-of-Book Market Order Engine
                // 1. Verkaufen per Sell Order: Stellen für -1 Silber unter den günstigsten Verkäufer in Stadt B
                val recSellOrderPrice = (sellPrice - 1).coerceAtLeast(buyPrice + 1)

                // 2. Kaufen per Buy Order: Biete +1 Silber über die beste Kauforder in Stadt A
                val topBuyInBuyCity = bestBuy.buyPriceMax
                val recBuyOrderPrice = if (topBuyInBuyCity > 10) (topBuyInBuyCity + 1) else (buyPrice * 0.91).toInt().coerceAtLeast(1)

                val targetTax = marketTaxPercent / 100.0
                val orderNetSellPrice = (recSellOrderPrice * (1.0 - targetTax - 0.025)).toInt()
                val orderNetProfitUnit = orderNetSellPrice - buyPrice
                val orderNetMarginPercent = if (buyPrice > 0) (orderNetProfitUnit.toDouble() / buyPrice) * 100.0 else 0.0

                // AlbionOnline2D 24h Sales Volume & Liquidity Calculation
                val estimated24hVol = when (resource.category) {
                    ResourceCategory.RESOURCES, ResourceCategory.REFINED -> (120 + resource.tier * 35 - enc * 15).coerceAtLeast(20)
                    ResourceCategory.FOOD, ResourceCategory.POTIONS -> (180 + resource.tier * 40 - enc * 20).coerceAtLeast(30)
                    ResourceCategory.WEAPONS, ResourceCategory.ARMOR -> (30 + resource.tier * 10 - enc * 5).coerceAtLeast(4)
                    ResourceCategory.BAG, ResourceCategory.CAPE -> (50 + resource.tier * 12 - enc * 8).coerceAtLeast(10)
                    else -> (25 + resource.tier * 6).coerceAtLeast(5)
                }

                val estimatedSellTimeText = when {
                    estimated24hVol >= 80 -> "⚡ Blitz-Verkauf (~10-30 Min.)"
                    estimated24hVol >= 25 -> "⏩ Schneller Verkauf (~1-3 Std.)"
                    estimated24hVol >= 8 -> "🟡 Moderater Markt (~4-12 Std.)"
                    else -> "🐢 Langsam (Gefahr von Unterbietungs-Kriegen!)"
                }

                val buyProbability = if (estimated24hVol >= 30) "98% (Sehr schnell)" else "85%"
                val sellProbability = if (estimated24hVol >= 30) "99% (Sofortige Platzierung)" else "88%"

                val buyOrderRec = "🛒 KI Kauforder (${bestBuy.city}): ${fmtNum.format(recBuyOrderPrice)} S. (+1 S. über beste Kauforder | $buyProbability)"
                val sellOrderRec = "📈 KI Verkauforder (${bestSell.city}): ${fmtNum.format(recSellOrderPrice)} S. (-1 S. unter günstigstem Angebot | $sellProbability)"
                val strategy = "🎯 Platz 1 Strategie: Biete für ${fmtNum.format(recSellOrderPrice)} S. in ${bestSell.city} an, um als ERSTER gekauft zu werden! ($estimatedSellTimeText | Netto: +${fmtNum.format(orderNetProfitUnit)} S./Stk.)"

                val liquidityLabel = "🔥 $estimatedSellTimeText (~$estimated24hVol Stk./Tag)"

                // AI Scam & Slow Selling Warning
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

    fun isPriceFreshWithin10Mins(price: MarketPrice): Boolean {
        val dateStr = price.sellPriceMinDate
        if (dateStr.isBlank() || dateStr.contains("Echtzeit", ignoreCase = true) || dateStr.contains("Gerade", ignoreCase = true)) {
            return true
        }
        val dateMs = parseIsoToEpochMs(dateStr)
        if (dateMs <= 0) return true
        val tenMinsAgo = System.currentTimeMillis() - (10 * 60 * 1000L)
        return dateMs >= tenMinsAgo
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

    // Megabau Feature 1.2: Station Trading & Flipping Engine (Buy Order -> Sell Order in Same City)
    fun calculateStationTradingFlippingOpportunities(
        resources: List<AlbionResource>,
        pricesByItem: Map<String, List<MarketPrice>>,
        silverBudget: Long = 10_000_000L,
        marketTaxPercent: Double = 4.0
    ): List<TradeOpportunity> {
        val list = mutableListOf<TradeOpportunity>()
        val cities = listOf("Bridgewatch", "Caerleon", "Fort Sterling", "Lymhurst", "Martlock", "Thetford", "Brecilien")

        for (res in resources) {
            val prices = pricesByItem[res.fullId] ?: pricesByItem[res.id] ?: continue
            for (cityName in cities) {
                val cityPrice = prices.find { citiesMatch(it.city, cityName) } ?: continue
                val buyOrderPrice = cityPrice.buyPriceMax
                val sellOrderPrice = cityPrice.sellPriceMin
                if (buyOrderPrice <= 10 || sellOrderPrice <= buyOrderPrice) continue
                if (sellOrderPrice > buyOrderPrice * 3.0) continue // Exaggerated gap check

                val setupFee = (sellOrderPrice * 0.025).toLong()
                val tax = (sellOrderPrice * (marketTaxPercent / 100.0)).toLong()
                val netSell = sellOrderPrice - setupFee - tax
                val unitNetProfit = netSell - buyOrderPrice
                if (unitNetProfit <= 0) continue

                val marginPercent = (unitNetProfit.toDouble() / buyOrderPrice) * 100.0
                if (marginPercent < 2.5 || marginPercent > 100.0) continue

                val tradeUnits = maxOf(1, (silverBudget / buyOrderPrice).toInt())
                val totalInvestment = tradeUnits.toLong() * buyOrderPrice
                val totalNetProfit = tradeUnits.toLong() * unitNetProfit

                list.add(
                    TradeOpportunity(
                        resource = res,
                        enchantment = res.enchantment,
                        quality = cityPrice.quality,
                        buyCity = cityName,
                        buyPrice = buyOrderPrice,
                        sellCity = cityName,
                        sellPrice = sellOrderPrice,
                        unitNetProfit = unitNetProfit.toInt(),
                        unitWeightKg = getItemWeightKg(res),
                        maxUnitsBySilver = tradeUnits,
                        maxUnitsByWeight = tradeUnits,
                        tradeUnits = tradeUnits,
                        totalInvestment = totalInvestment,
                        totalGrossRevenue = tradeUnits.toLong() * sellOrderPrice,
                        totalNetRevenue = tradeUnits.toLong() * netSell,
                        totalNetProfit = totalNetProfit,
                        totalWeightKg = tradeUnits * getItemWeightKg(res),
                        roiPercent = marginPercent,
                        priorityScore = (marginPercent * 10).toInt(),
                        zonesWalkedCount = 0,
                        updatedTimestamp = System.currentTimeMillis(),
                        updatedDateFormatted = "Echtzeit",
                        recommendedBuyOrderPrice = buyOrderPrice + 1,
                        recommendedSellOrderPrice = sellOrderPrice - 1,
                        aiOrderStrategy = "🛒 Station Trading in $cityName: Setze Buy Order für $buyOrderPrice S. & Sell Order für $sellOrderPrice S."
                    )
                )
            }
        }
        return list.sortedByDescending { it.totalNetProfit }.take(30)
    }

    // Megabau Feature 1.3: High-End Refining Matrix Engine (Resource Return Rate)
    fun calculateRefiningOpportunities(
        priceMap: Map<String, List<MarketPrice>>,
        useFocus: Boolean = true,
        hasPremium: Boolean = true
    ): List<CraftingOpportunityDetails> {
        val list = mutableListOf<CraftingOpportunityDetails>()
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

        val rrr = when {
            useFocus && hasPremium -> 0.539 // 53.9% RRR with Focus + City Bonus
            hasPremium -> 0.367              // 36.7% RRR with City Bonus
            else -> 0.152                    // 15.2% Standard RRR
        }

        val rawTypes = listOf(
            Triple("T5_PLANKS", "T5_LOGS", "T4_PLANKS"),
            Triple("T6_PLANKS", "T6_LOGS", "T5_PLANKS"),
            Triple("T7_PLANKS", "T7_LOGS", "T6_PLANKS"),
            Triple("T8_PLANKS", "T8_LOGS", "T7_PLANKS"),
            Triple("T5_METALBAR", "T5_ORE", "T4_METALBAR"),
            Triple("T6_METALBAR", "T6_ORE", "T5_METALBAR"),
            Triple("T7_METALBAR", "T7_ORE", "T6_METALBAR"),
            Triple("T8_METALBAR", "T8_ORE", "T7_METALBAR"),
            Triple("T5_LEATHER", "T5_HIDE", "T4_LEATHER"),
            Triple("T6_LEATHER", "T6_HIDE", "T5_LEATHER"),
            Triple("T7_LEATHER", "T7_HIDE", "T6_LEATHER"),
            Triple("T8_LEATHER", "T8_HIDE", "T7_LEATHER")
        )

        for ((refinedId, rawId, prevRefinedId) in rawTypes) {
            val res = AlbionResourceRepository.resources.find { it.fullId == refinedId || it.id == refinedId } ?: continue
            val rawPrice = CraftingRepository.getPriceInCity(rawId, "Bridgewatch", priceMap)
            val prevRefinedPrice = CraftingRepository.getPriceInCity(prevRefinedId, "Bridgewatch", priceMap)
            val finishedPrice = CraftingRepository.getPriceInCity(refinedId, "Bridgewatch", priceMap)

            val rawAmount = res.tier - 1
            val grossMatCost = (rawPrice.toLong() * rawAmount) + prevRefinedPrice
            val effectiveMatCost = (grossMatCost * (1.0 - rrr)).toLong()
            val netSell = (finishedPrice * (if (hasPremium) 0.935 else 0.895)).toLong()
            val netProfit = netSell - effectiveMatCost
            if (netProfit <= 0) continue

            val roi = if (effectiveMatCost > 0) (netProfit.toDouble() / effectiveMatCost) * 100.0 else 0.0
            if (roi > 100.0) continue

            list.add(
                CraftingOpportunityDetails(
                    resource = res,
                    roiPercent = roi,
                    cheapestBuyCity = "Königsstädte",
                    totalIngredientCost = effectiveMatCost,
                    highestSellCity = "Königsstädte",
                    finishedItemSellPrice = finishedPrice,
                    netProfit = netProfit,
                    recBuyOrderCost = (effectiveMatCost * 0.9).toLong(),
                    recSellOrderPrice = finishedPrice - 1,
                    ingredientSummary = "$rawAmount x $rawId + 1x $prevRefinedId (RRR: ${String.format(Locale.GERMANY, "%.1f", rrr * 100)}%)",
                    maxOrderProfit = netProfit * 100,
                    estimatedRoiPercent = roi,
                    resourceReturnRate = rrr,
                    targetMarket = "Veredelung (Refining)"
                )
            )
        }
        return list.sortedByDescending { it.netProfit }.take(30)
    }

    // Megabau Feature 1.6: Laborer Journals Profit Engine (Filling Empty Journals)
    fun calculateLaborerJournalOpportunities(
        priceMap: Map<String, List<MarketPrice>>
    ): List<CraftingOpportunityDetails> {
        val list = mutableListOf<CraftingOpportunityDetails>()
        val journals = listOf(
            LaborerJournal("JOURNAL_MERCENARY_T5", "T5 Söldner-Tagebuch", 5, "Mercenary", 1200, 8500),
            LaborerJournal("JOURNAL_MERCENARY_T8", "T8 Söldner-Tagebuch", 8, "Mercenary", 12000, 95000),
            LaborerJournal("JOURNAL_WARRIOR_T8", "T8 Schmied-Tagebuch", 8, "Crafting", 15000, 125000),
            LaborerJournal("JOURNAL_MAGE_T8", "T8 Magier-Tagebuch", 8, "Crafting", 15000, 130000),
            LaborerJournal("JOURNAL_WOOD_T8", "T8 Holzfäller-Tagebuch", 8, "Gathering", 14000, 110000),
            LaborerJournal("JOURNAL_ORE_T8", "T8 Erzsammler-Tagebuch", 8, "Gathering", 14000, 115000)
        )

        for (j in journals) {
            val emptyPrice = CraftingRepository.getPriceInCity("T${j.tier}_JOURNAL_${j.jobType.uppercase()}_EMPTY", "Caerleon", priceMap).toLong().coerceAtLeast(j.emptyPriceSilver)
            val fullPrice = CraftingRepository.getPriceInCity("T${j.tier}_JOURNAL_${j.jobType.uppercase()}_FULL", "Caerleon", priceMap).toLong().coerceAtLeast(j.fullPriceSilver)
            val profit = fullPrice - emptyPrice
            if (profit <= 0) continue

            val roi = (profit.toDouble() / emptyPrice) * 100.0
            if (roi > 100.0) continue
            val mockRes = AlbionResource("T${j.tier}_JOURNAL_${j.jobType.uppercase()}", j.nameDe, j.nameDe, j.tier, ResourceCategory.REFINED)

            list.add(
                CraftingOpportunityDetails(
                    resource = mockRes,
                    roiPercent = roi,
                    cheapestBuyCity = "Alle Städte",
                    totalIngredientCost = emptyPrice,
                    highestSellCity = "Caerleon",
                    finishedItemSellPrice = fullPrice.toInt(),
                    netProfit = profit,
                    recBuyOrderCost = (emptyPrice * 0.9).toLong(),
                    recSellOrderPrice = (fullPrice - 1).toInt(),
                    ingredientSummary = "Kaufe leeres Buch für ${emptyPrice} S. ➔ Befülle es beim Spielen ➔ Verkaufe für ${fullPrice} S.",
                    maxOrderProfit = profit * 20,
                    estimatedRoiPercent = roi,
                    targetMarket = "Tagebücher (Laborer Journals)"
                )
            )
        }
        return list.sortedByDescending { it.netProfit }
    }

    // Megabau Feature 1.7: Faction Warfare Trade Engine
    fun calculateFactionWarfareOpportunities(
        priceMap: Map<String, List<MarketPrice>>
    ): List<CraftingOpportunityDetails> {
        val list = mutableListOf<CraftingOpportunityDetails>()
        val factionCapes = listOf(
            Triple("T4_CAPEITEM_FW_BRIDGEWATCH", "Bridgewatch Fraktionsumhang (T4)", "Bridgewatch"),
            Triple("T4_CAPEITEM_FW_LYMHURST", "Lymhurst Fraktionsumhang (T4)", "Lymhurst"),
            Triple("T4_CAPEITEM_FW_FORTSTERLING", "Fort Sterling Fraktionsumhang (T4)", "FortSterling"),
            Triple("T4_CAPEITEM_FW_MARTLOCK", "Martlock Fraktionsumhang (T4)", "Martlock"),
            Triple("T4_CAPEITEM_FW_THETFORD", "Thetford Fraktionsumhang (T4)", "Thetford"),
            Triple("T4_CAPEITEM_FW_CAERLEON", "Caerleon Fraktionsumhang (T4)", "Caerleon")
        )

        for ((capeId, capeName, city) in factionCapes) {
            var capePrice = CraftingRepository.getPriceInCity(capeId, city, priceMap)
            var baseCapePrice = CraftingRepository.getPriceInCity("T4_CAPE", city, priceMap)
            var heartPrice = CraftingRepository.getPriceInCity("T4_HEART_$city", city, priceMap)

            if (baseCapePrice <= 0) baseCapePrice = 18_500
            if (heartPrice <= 0) heartPrice = 42_000
            if (capePrice <= 0) capePrice = 82_500

            val ingredientCost = baseCapePrice.toLong() + heartPrice.toLong()
            val netSell = (capePrice * 0.935).toLong()
            val profit = netSell - ingredientCost
            if (profit <= 0) continue

            val roi = (profit.toDouble() / ingredientCost) * 100.0
            if (roi > 100.0) continue
            val mockRes = AlbionResource(capeId, capeName, capeName, 4, ResourceCategory.CAPE)

            list.add(
                CraftingOpportunityDetails(
                    resource = mockRes,
                    roiPercent = roi,
                    cheapestBuyCity = city,
                    totalIngredientCost = ingredientCost,
                    highestSellCity = city,
                    finishedItemSellPrice = capePrice,
                    netProfit = profit,
                    recBuyOrderCost = (ingredientCost * 0.9).toLong(),
                    recSellOrderPrice = capePrice - 1,
                    ingredientSummary = "1x T4 Umhang (${baseCapePrice} S.) + 1x $city Herz (${heartPrice} S.)",
                    maxOrderProfit = profit * 50,
                    estimatedRoiPercent = roi,
                    targetMarket = "Fraktions-Handel ($city)"
                )
            )
        }
        return list.sortedByDescending { it.netProfit }
    }

    fun formatEpochToIso(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    fun formatPriceAge(epochMs: Long): String {
        if (epochMs <= 0) return "Gerade eben"
        val diffMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
        val diffSec = diffMs / 1000L
        val diffMin = diffMs / 60000L

        return when {
            diffSec < 60 -> "vor $diffSec Sek."
            else -> "vor $diffMin Min."
        }
    }
}

object CityDistanceCalculator {
    fun getZonesDistance(city1: String, city2: String): Int {
        if (TradeCalculator.citiesMatch(city1, city2)) return 0
        val c1 = TradeCalculator.normalizeCityName(city1)
        val c2 = TradeCalculator.normalizeCityName(city2)
        if (c1 == c2) return 0
        if (c1 == "caerleon" || c2 == "caerleon" || c1 == "blackmarket" || c2 == "blackmarket") return 4
        if (c1 == "brecilien" || c2 == "brecilien") return 5
        val adjacent = setOf(
            setOf("bridgewatch", "lymhurst"),
            setOf("bridgewatch", "martlock"),
            setOf("fortsterling", "thetford"),
            setOf("fortsterling", "lymhurst"),
            setOf("martlock", "thetford")
        )
        return if (adjacent.contains(setOf(c1, c2))) 2 else 3
    }
}

