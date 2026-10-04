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
    val buyCity: String,
    val buyPrice: Int, // Planned Unit Buy Price in Silber
    val sellCity: String,
    val sellPrice: Int, // Planned Unit Sell Price in Silber
    val plannedUnits: Int,
    val targetNetProfit: Long,
    val targetInvestment: Long,
    val acceptedDate: String,
    val status: OrderStatus = OrderStatus.ACTIVE,

    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0,

    // Realized Trade Results when completed
    val actualSilverSpent: Long? = null,
    val actualSilverEarned: Long? = null,
    val actualBuyPrice: Int? = null, // Realized Unit Buy Price
    val actualSellPrice: Int? = null, // Realized Unit Sell Price
    val actualUnits: Int? = null, // Realized Sold Units
    val completedDate: String? = null
) {
    fun isPriceStillValid(allPrices: Map<String, List<MarketPrice>>): Boolean {
        if (status != OrderStatus.ACTIVE) return false
        val prices = allPrices[resourceId] ?: return false
        // Check if there is still any price <= buyPrice in buyCity, and >= sellPrice in sellCity
        val buyValid = prices.any { it.city == buyCity && it.sellPriceMin > 0 && it.sellPriceMin <= buyPrice }
        val sellValid = prices.any { it.city == sellCity && it.sellPriceMin > 0 && it.sellPriceMin >= sellPrice }
        return buyValid && sellValid
    }
    val effectiveBuyPrice: Int
        get() = actualBuyPrice ?: buyPrice

    val effectiveSellPrice: Int
        get() = actualSellPrice ?: sellPrice

    val effectiveUnits: Int
        get() = actualUnits ?: plannedUnits

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
