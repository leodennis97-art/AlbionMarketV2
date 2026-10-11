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
    ARTIFACTS("🔮 Artefakte & Runen"),
    WOOD("🪵 Holz"),
    ORE("🪨 Erz"),
    HIDE("🐆 Leder"),
    FIBER("🌾 Faser"),
    ROCK("🧱 Stein"),
    STONE("🧱 Stein"),
    BAGS_CAPES("🎒 Taschen & Umhänge"),
    CONSUMABLES("🧪 Tränke & Essen");

    fun getDisplayName(langCode: String): String {
        return LanguageManager.translateUI(displayName, langCode)
    }
}

data class AlbionResource(
    val id: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val tier: Int = 1,
    val category: ResourceCategory = ResourceCategory.ALL,
    val enchantment: Int = 0,
    val description: String = "",
    val quality: Int = 1,
    val name: String = "",
    val healingRating: Int = 0,
    val maxTier: Int = 8,
    val estimatedDailyIncomeSilver: Long = 0L,
    val rarity: String = "Normal",
    val isRare: Boolean = false,
    val dropChancePercent: Double = 0.0
) {
    val cleanNameDe: String
        get() = stripTierFromName(if (nameDe.isNotBlank()) nameDe else name)

    val cleanNameEn: String
        get() = stripTierFromName(if (nameEn.isNotBlank()) nameEn else name)

    fun getName(langCode: String): String {
        val cleanLang = langCode.trim().uppercase()
        if (cleanLang == "DE") return if (nameDe.isNotBlank()) nameDe else name
        if (cleanLang == "EN") return if (nameEn.isNotBlank()) nameEn else cleanNameEn
        val baseName = if (nameEn.isNotBlank()) nameEn else if (nameDe.isNotBlank()) nameDe else name
        return LanguageManager.translateUI(baseName, cleanLang)
    }

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

            s = s.replace(Regex("^(T[1-8](\\.[0-4])?|Stufe\\s*[1-8](\\.[0-4])?|Tier\\s*[1-8](\\.[0-4])?)[\\s._-]+", RegexOption.IGNORE_CASE), "")
            s = s.replace(Regex("\\s*[(\\[]T[1-8](\\.[0-4])?[)\\]]$", RegexOption.IGNORE_CASE), "")

            return s.trim().ifEmpty { rawName }
        }
    }
}

val AlbionResource?.useCaseFunction: String get() = "Verwendung"
val AlbionResource?.potion: String get() = "Trank"
val AlbionResource?.food: String get() = "Essen"
val AlbionResource?.recommendedSkills: String get() = "Skills"
val AlbionResource?.headSkill: String get() = "Kopf"
val AlbionResource?.armorSkill: String get() = "Brust"
val AlbionResource?.shoesSkill: String get() = "Schuhe"
val AlbionResource?.weaponSkill: String get() = "Waffe"
val AlbionResource?.bossLootList: List<String> get() = emptyList()
val AlbionResource?.possibleChests: List<String> get() = emptyList()
val AlbionResource?.trendForecastDe: String get() = "Stabil"
val AlbionResource?.dailyLow: Int get() = 0
val AlbionResource?.monthlyLow: Int get() = 0
val AlbionResource?.dailyHigh: Int get() = 0
val AlbionResource?.monthlyHigh: Int get() = 0
val AlbionResource?.forecast3Days: String get() = "Stabil"
val AlbionResource?.forecast7Days: String get() = "Stabil"
val AlbionResource?.expectedNetProfitPerGold: Long get() = 0L
val AlbionResource?.expectedRoiPercent: Double get() = 0.0
val AlbionResource?.cityBonusDescEn: String get() = ""
