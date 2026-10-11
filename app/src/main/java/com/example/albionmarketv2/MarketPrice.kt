package com.example.albionmarketv2

enum class AlbionServer(val serverId: String, val displayName: String, val baseUrl: String, val historyBaseUrl: String) {
    EUROPE("europe", "Europa", "https://europe.albion-online-data.com/api/v2/stats/prices/", "https://europe.albion-online-data.com/api/v2/stats/history/"),
    WEST("west", "West (Amerikas)", "https://west.albion-online-data.com/api/v2/stats/prices/", "https://west.albion-online-data.com/api/v2/stats/history/"),
    EAST("east", "Ost (Asien)", "https://east.albion-online-data.com/api/v2/stats/prices/", "https://east.albion-online-data.com/api/v2/stats/history/")
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

data class PriceSnapshot(
    val itemId: String,
    val city: String,
    val quality: Int = 1,
    val sellPriceMin: Int = 0,
    val buyPriceMax: Int = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val sellPriceMinAmount: Int = 0
)

data class ItemHistoryEntry(
    val itemId: String,
    val city: String,
    val quality: Int,
    val itemCount: Long,
    val avgPrice: Long,
    val timestamp: String
)

enum class LiquidityLevel(val labelDe: String, val minDailyVolume: Long) {
    VERY_LOW("⚠️ Extrem Träge (Gefahr)", 0),
    LOW("🟡 Geringe Nachfrage", 10),
    MEDIUM("🟢 Gute Liquidität", 100),
    HIGH("🔥 Sehr Hoher Umsatz", 500)
}

data class MarketQuantMetrics(
    val itemId: String,
    val city: String,
    val vwap24h: Long,
    val totalVolume24h: Long,
    val liquidityLevel: LiquidityLevel,
    val isPriceManipulated: Boolean,
    val priceVolatilityPercent: Double,
    val blackMarketSinkVelocity: String
)

data class GankZoneHazard(
    val zoneName: String,
    val recentKills24h: Int,
    val hazardLevelDe: String,
    val isHighRisk: Boolean
)

