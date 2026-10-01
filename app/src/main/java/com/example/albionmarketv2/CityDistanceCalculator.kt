package com.example.albionmarketv2

object CityDistanceCalculator {
    private val distanceMatrix = mapOf(
        "Bridgewatch" to mapOf("Bridgewatch" to 0, "Martlock" to 3, "Lymhurst" to 3, "Fort Sterling" to 4, "Thetford" to 4, "Caerleon" to 3, "Brecilien" to 2),
        "Martlock" to mapOf("Bridgewatch" to 3, "Martlock" to 0, "Lymhurst" to 4, "Fort Sterling" to 3, "Thetford" to 5, "Caerleon" to 3, "Brecilien" to 2),
        "Lymhurst" to mapOf("Bridgewatch" to 3, "Martlock" to 4, "Lymhurst" to 0, "Fort Sterling" to 3, "Thetford" to 3, "Caerleon" to 3, "Brecilien" to 2),
        "Fort Sterling" to mapOf("Bridgewatch" to 4, "Martlock" to 3, "Lymhurst" to 3, "Fort Sterling" to 0, "Thetford" to 3, "Caerleon" to 3, "Brecilien" to 2),
        "Thetford" to mapOf("Bridgewatch" to 4, "Martlock" to 5, "Lymhurst" to 3, "Fort Sterling" to 3, "Thetford" to 0, "Caerleon" to 3, "Brecilien" to 2),
        "Caerleon" to mapOf("Bridgewatch" to 3, "Martlock" to 3, "Lymhurst" to 3, "Fort Sterling" to 3, "Thetford" to 3, "Caerleon" to 0, "Brecilien" to 4),
        "Brecilien" to mapOf("Bridgewatch" to 2, "Martlock" to 2, "Lymhurst" to 2, "Fort Sterling" to 2, "Thetford" to 2, "Caerleon" to 4, "Brecilien" to 0),
    )

    fun getZonesDistance(cityA: String, cityB: String): Int {
        val mapA = distanceMatrix[cityA]
        if ((mapA != null) && mapA.containsKey(cityB)) {
            return mapA[cityB] ?: 2
        }
        val mapB = distanceMatrix[cityB]
        if ((mapB != null) && mapB.containsKey(cityA)) {
            return mapB[cityA] ?: 2
        }
        return 4
    }
}
