package com.example.albionmarketv2

data class TradingRouteLeg(
    val fromCity: String,
    val toCity: String,
    val resource: AlbionResource,
    val buyPrice: Int,
    val sellPrice: Int,
    val netProfit: Long,
    val roiPercent: Double
)

data class OptimizedTradingLoop(
    val routeDescription: String,
    val legs: List<TradingRouteLeg>,
    val totalEstimatedNetProfit: Long,
    val totalWeightKg: Double,
    val estimatedMinutes: Int
)

data class RouteRiskDetails(
    val riskBadge: String,
    val riskColorHex: Long,
    val zoneDescription: String,
    val peakTimeWarning: String
)

object TradeRouteOptimizer {

    fun calculateOptimalLoop(
        resources: List<AlbionResource>,
        pricesByItem: Map<String, List<MarketPrice>>,
        silverBudget: Long,
        carryCapacityKg: Double
    ): List<OptimizedTradingLoop> {
        val loops = mutableListOf<OptimizedTradingLoop>()
        val cities = listOf("Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Caerleon", "Brecilien")

        for (sourceCity in cities) {
            for (destCity in cities) {
                if (sourceCity == destCity) continue
                val bestLegs = mutableListOf<TradingRouteLeg>()

                for (res in resources.take(100)) {
                    val prices = pricesByItem[res.fullId] ?: continue
                    val buyObj = prices.firstOrNull { it.city.equals(sourceCity, ignoreCase = true) && it.sellPriceMin > 0 }
                    val sellObj = prices.firstOrNull { it.city.equals(destCity, ignoreCase = true) && it.buyPriceMax > 0 }

                    if (buyObj != null && sellObj != null && sellObj.buyPriceMax > buyObj.sellPriceMin) {
                        val profit = (sellObj.buyPriceMax - buyObj.sellPriceMin).toLong()
                        val roi = (profit.toDouble() / buyObj.sellPriceMin) * 100.0
                        if (roi >= 10.0) {
                            bestLegs.add(
                                TradingRouteLeg(
                                    fromCity = sourceCity,
                                    toCity = destCity,
                                    resource = res,
                                    buyPrice = buyObj.sellPriceMin,
                                    sellPrice = sellObj.buyPriceMax,
                                    netProfit = profit,
                                    roiPercent = roi
                                )
                            )
                        }
                    }
                }

                val sortedLegs = bestLegs.sortedByDescending { it.netProfit }.take(2)
                if (sortedLegs.isNotEmpty()) {
                    val totalProfit = sortedLegs.sumOf { it.netProfit }
                    loops.add(
                        OptimizedTradingLoop(
                            routeDescription = "$sourceCity ➔ ${sortedLegs.first().toCity} ➔ $sourceCity (Multi-City Loop)",
                            legs = sortedLegs,
                            totalEstimatedNetProfit = totalProfit,
                            totalWeightKg = sortedLegs.size * 25.0,
                            estimatedMinutes = 15 * sortedLegs.size
                        )
                    )
                }
            }
        }

        return loops.sortedByDescending { it.totalEstimatedNetProfit }.take(5)
    }

    fun assessRouteRisk(fromCity: String, toCity: String): RouteRiskDetails {
        val f = fromCity.lowercase()
        val t = toCity.lowercase()
        return when {
            f.contains("caerleon") || t.contains("caerleon") -> RouteRiskDetails(
                riskBadge = "💀 HOHE GEFAHR (Rot-Zone)",
                riskColorHex = 0xFFEF4444,
                zoneDescription = "Rot-Zonen Engpass. Full-Loot PvP Gefahr durch Ganker-Gruppen.",
                peakTimeWarning = "⚠️ Peak-Time Warnung: Höchste Aktivität zwischen 18:00 - 23:00 Uhr UTC."
            )
            f.contains("brecilien") || t.contains("brecilien") -> RouteRiskDetails(
                riskBadge = "🌀 NEBEL / AVALON (Mists)",
                riskColorHex = 0xFFA855F7,
                zoneDescription = "Transport über Pfade von Avalon oder Nebel-Portale.",
                peakTimeWarning = "💡 Tipp: Grüne/Blaue Avalon-Portale bieten temporären Schutz."
            )
            else -> RouteRiskDetails(
                riskBadge = "🟢 SICHER (Gelb/Blau-Zone)",
                riskColorHex = 0xFF10B981,
                zoneDescription = "Sicherer Transport auf dem Royal-Kontinent ohne Item-Verlust.",
                peakTimeWarning = "✅ Jederzeit sicher befahrbar."
            )
        }
    }
}
