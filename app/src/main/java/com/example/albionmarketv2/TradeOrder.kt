package com.example.albionmarketv2

enum class OrderStatus {
    ACTIVE,
    COMPLETED,
    DISCARDED
}

data class TradeOrder(
    val id: String,
    val resourceId: String,
    val resourceNameDe: String,
    val resourceNameEn: String,
    val tier: Int,
    val enchantment: Int = 0,
    val buyCity: String,
    val buyPrice: Int,
    val sellCity: String,
    val sellPrice: Int,
    val plannedUnits: Int,
    val targetNetProfit: Long,
    val targetInvestment: Long,
    val acceptedDate: String,
    val status: OrderStatus = OrderStatus.ACTIVE,

    // Realized Trade Results when completed
    val actualSilverSpent: Long? = null,
    val actualSilverEarned: Long? = null,
    val completedDate: String? = null,
    val actualUnits: Int = 0,
    val actualBuyPrice: Int = 0,
    val actualSellPrice: Int = 0,
    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0
) {
    val tierText: String
        get() = if (enchantment > 0) "T$tier.$enchantment" else "T$tier"

    val effectiveUnits: Int
        get() = if (actualUnits > 0) actualUnits else plannedUnits

    val effectiveBuyPrice: Int
        get() = if (actualBuyPrice > 0) actualBuyPrice else buyPrice

    val effectiveSellPrice: Int
        get() = if (actualSellPrice > 0) actualSellPrice else sellPrice

    fun isPriceStillValid(marketPrices: Any? = null): Boolean = true

    val realizedNetProfit: Long
        get() {
            if (actualSilverSpent != null && actualSilverEarned != null) {
                return actualSilverEarned - actualSilverSpent
            }
            return targetNetProfit
        }

    val realizedRoiPercent: Double
        get() {
            val spent = actualSilverSpent ?: targetInvestment
            if (spent <= 0) return 0.0
            return (realizedNetProfit.toDouble() / spent) * 100.0
        }
}
