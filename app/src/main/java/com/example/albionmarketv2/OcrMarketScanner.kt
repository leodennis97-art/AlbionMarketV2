package com.example.albionmarketv2

import java.util.Locale

data class OcrItemMatchResult(
    val detectedRawText: String,
    val matchedResource: AlbionResource?,
    val detectedPriceSilver: Int,
    val detectedCity: String,
    val confidencePercent: Double
)

object OcrMarketScanner {

    private val TIER_REGEX = Regex("T([2-8])|Tier\\s*([2-8])", RegexOption.IGNORE_CASE)
    private val PRICE_REGEX = Regex("([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})\\s*(?:Silber|Silver|S)", RegexOption.IGNORE_CASE)

    fun parseMarketTextFromOcr(rawOcrText: String): OcrItemMatchResult {
        if (rawOcrText.isBlank()) {
            return OcrItemMatchResult(rawOcrText, null, 0, "Caerleon", 0.0)
        }

        val cleaned = rawOcrText.replace("\n", " ").trim()

        // Extract Tier
        val tierMatch = TIER_REGEX.find(cleaned)
        val tierNum = tierMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: tierMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 4

        // Extract Price
        val priceMatch = PRICE_REGEX.find(cleaned)
        val rawPriceStr = priceMatch?.groupValues?.getOrNull(1)?.replace(".", "")?.replace(",", "") ?: ""
        val priceVal = rawPriceStr.toIntOrNull() ?: 0

        // Match Resource in AlbionResourceRepository
        val matchedRes = AlbionResourceRepository.resources.find { res ->
            res.tier == tierNum && (
                    cleaned.lowercase(Locale.GERMANY).contains(res.nameDe.lowercase(Locale.GERMANY)) ||
                            cleaned.lowercase(Locale.ENGLISH).contains(res.nameEn.lowercase(Locale.ENGLISH))
                    )
        } ?: AlbionResourceRepository.resources.find { it.tier == tierNum }

        val detectedCity = when {
            cleaned.contains("Bridgewatch", true) -> "Bridgewatch"
            cleaned.contains("Caerleon", true) -> "Caerleon"
            cleaned.contains("Fort Sterling", true) -> "FortSterling"
            cleaned.contains("Lymhurst", true) -> "Lymhurst"
            cleaned.contains("Martlock", true) -> "Martlock"
            cleaned.contains("Thetford", true) -> "Thetford"
            cleaned.contains("Brecilien", true) -> "Brecilien"
            cleaned.contains("Black Market", true) -> "BlackMarket"
            else -> "Caerleon"
        }

        val confidence = if (matchedRes != null && priceVal > 0) 92.5 else 45.0

        return OcrItemMatchResult(
            detectedRawText = rawOcrText,
            matchedResource = matchedRes,
            detectedPriceSilver = priceVal,
            detectedCity = detectedCity,
            confidencePercent = confidence
        )
    }
}
