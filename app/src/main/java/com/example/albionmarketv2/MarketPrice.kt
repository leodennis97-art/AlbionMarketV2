package com.example.albionmarketv2

data class MarketPrice(
    val itemId: String = "",
    val city: String = "",
    val sellPriceMin: Int = 0,
    val sellPriceMinAmount: Int = 1,
    val buyPriceMax: Int = 0,
    val buyPriceMaxAmount: Int = 1,
    val timestampMs: Long = System.currentTimeMillis(),
    val sellPriceMinDate: String = "",
    val buyPriceMaxDate: String = "",
    val quality: Int = 1
) {
    fun isUnrealisticPrice(vararg args: Any?): Boolean = sellPriceMin <= 0 || buyPriceMax <= 0
}
