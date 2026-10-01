package com.example.albionmarketv2

enum class TimeFrame(val labelDe: String, val hours: Int) {
    DAY("1 Tag", 24),
    WEEK("1 Woche", 168),
    MONTH("1 Monat", 720),
    YEAR("1 Jahr", 8760)
}

data class PriceSnapshot(
    val itemId: String,
    val city: String,
    val sellPriceMin: Int,
    val buyPriceMax: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ItemPriceHistory(
    val itemId: String,
    val itemNameDe: String,
    val tier: Int,
    val timeFrame: TimeFrame,
    val lowestSellPrice: Int,
    val highestSellPrice: Int,
    val avgSellPrice: Int,
    val lowestBuyPrice: Int,
    val highestBuyPrice: Int,
    val avgBuyPrice: Int,
    val sampleCount: Int,
    val lastUpdatedStr: String
) {
    val priceRatioPercent: Int
        get() = if (avgSellPrice > 0) ((lowestSellPrice.toDouble() / avgSellPrice) * 100).toInt() else 100
}
