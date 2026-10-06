package com.example.albionmarketv2

enum class AlbionServer(val displayName: String, val baseUrl: String, val webUrl: String, val serverId: String) {
    EUROPE("Europa", "https://europe.albion-online-data.com/api/v2/stats/prices/", "https://europe.albiononline2d.com/", "europe"),
    AMERICAS("Americas", "https://www.albion-online-data.com/api/v2/stats/prices/", "https://albiononline2d.com/", "americas"),
    ASIA("Asia", "https://east.albion-online-data.com/api/v2/stats/prices/", "https://east.albiononline2d.com/", "asia")
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
