package com.example.albionmarketv2

enum class AlbionServer(val displayName: String, val baseUrl: String, val webUrl: String) {
    EUROPE("Europa", "https://europe.albion-online-data.com/api/v2/stats/prices/", "https://europe.albiononline2d.com/")
}

data class MarketPrice(
    val itemId: String,
    val city: String,
    val quality: Int,
    val sellPriceMin: Int,
    val sellPriceMinDate: String,
    val buyPriceMax: Int,
    val buyPriceMaxDate: String,
    val sellPriceMinAmount: Int = 0
)
