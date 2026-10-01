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
    val fullId: String
        get() = if (enchantment > 0) "$id@$enchantment" else id

    val imageUrl: String
        get() = "https://render.albiononline.com/v1/item/$fullId.png"

    val tierText: String
        get() = "T$tier"

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
}
