package com.example.albionmarketv2

enum class ResourceCategory(val displayName: String) {
    ALL("Alle Kategorien"),
    WEAPONS("⚔️ Waffen"),
    OFFHAND("🛡️ Off-Hand"),
    HELMETS("🪖 Helme"),
    ARMOR("🥋 Rüstung"),
    SHOES("🥾 Stiefel"),
    CAPE("🧥 Umhänge"),
    BAG("🎒 Taschen"),
    MOUNTS("🐎 Reittiere"),
    FOOD("🍖 Essen"),
    POTIONS("🧪 Tränke"),
    RESOURCES("⛏️ Rohstoffe"),
    REFINED("🧱 Veredelt"),
    ARTIFACTS("🔮 Artefakte & Runen")
}

data class AlbionResource(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val tier: Int,
    val category: ResourceCategory,
    val enchantment: Int = 0,
    val description: String = "",
    val quality: Int = 1,
) {
    val cleanNameDe: String
        get() = stripTierFromName(nameDe)

    val cleanNameEn: String
        get() = stripTierFromName(nameEn)

    val fullId: String
        get() = if (enchantment > 0) "$id@$enchantment" else id

    val imageUrl: String
        get() = "https://render.albiononline.com/v1/item/$fullId.png"

    val tierText: String
        get() = if (enchantment > 0) "T$tier.$enchantment" else "T$tier"

    val enchantmentText: String
        get() = if (enchantment > 0) ".$enchantment" else ""

    val qualityText: String
        get() = when (quality) {
            2 -> "Gut"
            3 -> "Hervorragend"
            4 -> "Ausgezeichnet"
            5 -> "Meisterhaft"
            else -> "Normal"
        }

    companion object {
        fun stripTierFromName(rawName: String): String {
            if (rawName.isBlank()) return rawName
            var s = rawName.trim()

            // Remove Tier/Stufe prefixes like "T4.1 ", "T4. ", "T4 ", "T4_ ", "T4 - ", "Stufe 4 ", "Tier 4 "
            s = s.replace(Regex("^(T[1-8](\\.[0-4])?|Stufe\\s*[1-8](\\.[0-4])?|Tier\\s*[1-8](\\.[0-4])?)[\\s._-]+", RegexOption.IGNORE_CASE), "")

            // Remove trailing "(T4.1)", "(T4)" or "[T4.1]"
            s = s.replace(Regex("\\s*[(\\[]T[1-8](\\.[0-4])?[)\\]]$", RegexOption.IGNORE_CASE), "")

            return s.trim().ifEmpty { rawName }
        }
    }
}
