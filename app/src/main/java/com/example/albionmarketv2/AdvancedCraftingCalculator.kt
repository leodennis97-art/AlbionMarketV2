package com.example.albionmarketv2

data class CityCraftingBonus(
    val cityName: String,
    val primaryRefiningCategory: ResourceCategory,
    val primaryCraftingCategory: ResourceCategory,
    val refiningBonusNoFocusRrr: Double = 0.367, // 36.7%
    val refiningBonusFocusRrr: Double = 0.539,   // 53.9%
    val craftingBonusNoFocusRrr: Double = 0.248, // 24.8%
    val craftingBonusFocusRrr: Double = 0.435    // 43.5%
)

data class LaborerJournalReward(
    val tier: Int,
    val journalNameDe: String,
    val fillFameRequired: Int,
    val estimatedFilledJournalValueSilver: Int
)

data class FocusEfficiencyCalculation(
    val itemId: String,
    val itemNameDe: String,
    val city: String,
    val grossProfitNoFocus: Long,
    val grossProfitWithFocus: Long,
    val focusCostPoints: Int,
    val silverPerFocusPoint: Double,
    val laborerJournalSilverProfit: Long,
    val rrrPercent: Double,
    val isRecommendedCraft: Boolean
)

object AdvancedCraftingCalculator {

    val CITY_BONUSES = listOf(
        CityCraftingBonus("Martlock", ResourceCategory.HIDE, ResourceCategory.STONE),
        CityCraftingBonus("Thetford", ResourceCategory.ORE, ResourceCategory.WOOD),
        CityCraftingBonus("FortSterling", ResourceCategory.WOOD, ResourceCategory.ORE),
        CityCraftingBonus("Lymhurst", ResourceCategory.FIBER, ResourceCategory.HIDE),
        CityCraftingBonus("Bridgewatch", ResourceCategory.STONE, ResourceCategory.FIBER),
        CityCraftingBonus("Caerleon", ResourceCategory.REFINED, ResourceCategory.ARTIFACTS),
        CityCraftingBonus("Brecilien", ResourceCategory.REFINED, ResourceCategory.ALL)
    )

    val LABORER_JOURNALS = listOf(
        LaborerJournalReward(4, "T4 Handwerker-Tagebuch", 1200, 4500),
        LaborerJournalReward(5, "T5 Handwerker-Tagebuch", 2400, 11000),
        LaborerJournalReward(6, "T6 Handwerker-Tagebuch", 4800, 24000),
        LaborerJournalReward(7, "T7 Handwerker-Tagebuch", 9600, 52000),
        LaborerJournalReward(8, "T8 Handwerker-Tagebuch", 19200, 120000)
    )

    fun getResourceReturnRate(
        city: String,
        category: ResourceCategory,
        isRefining: Boolean,
        useFocus: Boolean,
        hasSaladBuff: Boolean = true
    ): Double {
        val cityBonus = CITY_BONUSES.find { TradeCalculator.citiesMatch(it.cityName, city) }
            ?: CityCraftingBonus(city, ResourceCategory.ALL, ResourceCategory.ALL)

        var rrr = if (isRefining) {
            if (cityBonus.primaryRefiningCategory == category || cityBonus.primaryRefiningCategory == ResourceCategory.ALL) {
                if (useFocus) 0.539 else 0.367
            } else {
                if (useFocus) 0.435 else 0.248
            }
        } else {
            if (cityBonus.primaryCraftingCategory == category || cityBonus.primaryCraftingCategory == ResourceCategory.ALL) {
                if (useFocus) 0.435 else 0.248
            } else {
                if (useFocus) 0.435 else 0.152
            }
        }

        if (hasSaladBuff) {
            rrr = (rrr * 1.02).coerceAtMost(0.58) // +2% effektiv durch Krautsalat-Qualität & Ertrag
        }
        return rrr
    }

    fun calculateFocusEfficiency(
        resource: AlbionResource,
        city: String,
        ingredientCostsSilver: Long,
        itemSellPriceSilver: Long,
        baseFocusCost: Int = 1000,
        specLevel: Int = 100
    ): FocusEfficiencyCalculation {
        // Efficiency cost reduction per spec level (100 spec = 50% focus cost reduction)
        val actualFocusCost = (baseFocusCost * (1.0 - (specLevel * 0.005))).toInt().coerceAtLeast(120)

        val rrrNoFocus = getResourceReturnRate(city, resource.category, resource.category == ResourceCategory.REFINED, useFocus = false)
        val rrrFocus = getResourceReturnRate(city, resource.category, resource.category == ResourceCategory.REFINED, useFocus = true)

        val effectiveCostNoFocus = (ingredientCostsSilver * (1.0 - rrrNoFocus)).toLong()
        val effectiveCostFocus = (ingredientCostsSilver * (1.0 - rrrFocus)).toLong()

        val netTax = (itemSellPriceSilver * 0.065).toLong() // 4% Steuer + 2.5% Einstellgebühr
        val profitNoFocus = (itemSellPriceSilver - netTax) - effectiveCostNoFocus
        val profitFocus = (itemSellPriceSilver - netTax) - effectiveCostFocus

        val focusDeltaSilver = profitFocus - profitNoFocus
        val silverPerFocus = if (actualFocusCost > 0) (focusDeltaSilver.toDouble() / actualFocusCost) else 0.0

        val journalReward = LABORER_JOURNALS.find { it.tier == resource.tier }?.estimatedFilledJournalValueSilver?.toLong() ?: 0L

        return FocusEfficiencyCalculation(
            itemId = resource.fullId,
            itemNameDe = resource.nameDe,
            city = city,
            grossProfitNoFocus = profitNoFocus,
            grossProfitWithFocus = profitFocus,
            focusCostPoints = actualFocusCost,
            silverPerFocusPoint = silverPerFocus,
            laborerJournalSilverProfit = journalReward,
            rrrPercent = rrrFocus * 100.0,
            isRecommendedCraft = silverPerFocus >= 30.0 // > 30 Silver pro Fokus ist exzellent
        )
    }
}
