package com.example.albionmarketv2

object CityDistanceCalculator {
    private val distanceMatrix = mapOf(
        "bridgewatch" to mapOf("bridgewatch" to 0, "martlock" to 3, "lymhurst" to 3, "fortsterling" to 4, "thetford" to 4, "caerleon" to 3, "brecilien" to 2, "blackmarket" to 3),
        "martlock" to mapOf("bridgewatch" to 3, "martlock" to 0, "lymhurst" to 4, "fortsterling" to 3, "thetford" to 5, "caerleon" to 3, "brecilien" to 2, "blackmarket" to 3),
        "lymhurst" to mapOf("bridgewatch" to 3, "martlock" to 4, "lymhurst" to 0, "fortsterling" to 3, "thetford" to 3, "caerleon" to 3, "brecilien" to 2, "blackmarket" to 3),
        "fortsterling" to mapOf("bridgewatch" to 4, "martlock" to 3, "lymhurst" to 3, "fortsterling" to 0, "thetford" to 3, "caerleon" to 3, "brecilien" to 2, "blackmarket" to 3),
        "thetford" to mapOf("bridgewatch" to 4, "martlock" to 5, "lymhurst" to 3, "fortsterling" to 3, "thetford" to 0, "caerleon" to 3, "brecilien" to 2, "blackmarket" to 3),
        "caerleon" to mapOf("bridgewatch" to 3, "martlock" to 3, "lymhurst" to 3, "fortsterling" to 3, "thetford" to 3, "caerleon" to 0, "brecilien" to 4, "blackmarket" to 0),
        "blackmarket" to mapOf("bridgewatch" to 3, "martlock" to 3, "lymhurst" to 3, "fortsterling" to 3, "thetford" to 3, "caerleon" to 0, "brecilien" to 4, "blackmarket" to 0),
        "brecilien" to mapOf("bridgewatch" to 2, "martlock" to 2, "lymhurst" to 2, "fortsterling" to 2, "thetford" to 2, "caerleon" to 4, "brecilien" to 0, "blackmarket" to 4),
    )

    fun getZonesDistance(cityA: String, cityB: String): Int {
        val normA = TradeCalculator.normalizeCityName(cityA)
        val normB = TradeCalculator.normalizeCityName(cityB)

        if (normA == normB) return 0

        val mapA = distanceMatrix[normA]
        if (mapA != null && mapA.containsKey(normB)) {
            return mapA[normB] ?: 2
        }
        val mapB = distanceMatrix[normB]
        if (mapB != null && mapB.containsKey(normA)) {
            return mapB[normA] ?: 2
        }
        return 3
    }
}
