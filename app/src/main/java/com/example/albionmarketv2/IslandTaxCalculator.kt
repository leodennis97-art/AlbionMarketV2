package com.example.albionmarketv2

data class IslandTaxAnalysis(
    val buildingName: String,
    val tier: Int,
    val dailyNutritionConsumed: Long,
    val feedingFoodCostSilver: Long,
    val recommendedUsageFeePercent: Double,
    val estimatedDailyGrossRevenue: Long,
    val estimatedDailyNetProfit: Long
)

object IslandTaxCalculator {

    fun calculatePlotTax(buildingName: String, tier: Int, feedingFoodPriceSilver: Long): IslandTaxAnalysis {
        val baseNutrition = (tier * 1500L) + 2500L
        val foodCost = (baseNutrition / 100L) * (feedingFoodPriceSilver / 10L)
        val grossRevenue = tier * 75000L
        val optimalFee = when (tier) {
            in 1..3 -> 5.0
            in 4..6 -> 8.0
            else -> 12.0
        }
        val netProfit = grossRevenue - foodCost

        return IslandTaxAnalysis(
            buildingName = buildingName,
            tier = tier,
            dailyNutritionConsumed = baseNutrition,
            feedingFoodCostSilver = foodCost,
            recommendedUsageFeePercent = optimalFee,
            estimatedDailyGrossRevenue = grossRevenue,
            estimatedDailyNetProfit = netProfit.coerceAtLeast(0L)
        )
    }
}
