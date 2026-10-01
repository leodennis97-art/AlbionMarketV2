package com.example.albionmarketv2

data class BuildingUpgradeStep(
    val tier: Int,
    val tierName: String,
    val silverCost: Long,
    val woodReq: String,
    val stoneReq: String
)

data class IslandBuilding(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val iconEmoji: String,
    val category: String, // LANDWIRTSCHAFT or HERSTELLUNG
    val maxTier: Int,
    val estimatedDailyIncomeSilver: Long,
    val estimatedRoiPercent: Double,
    val abilityDescDe: String,
    val cityBonusCity: String,
    val cityBonusDescDe: String,
    val cityBonusDescEn: String,
    val upgrades: List<BuildingUpgradeStep>
)

object IslandRepository {

    private fun generateStandardUpgrades(maxTier: Int, isFarm: Boolean = false): List<BuildingUpgradeStep> {
        val list = mutableListOf<BuildingUpgradeStep>()
        if (isFarm) {
            list.add(BuildingUpgradeStep(1, "Stufe T1 (Feld / Weide errichten)", 2_500L, "15x T1 Raues Holz", "15x T1 Rauer Stein"))
            list.add(BuildingUpgradeStep(2, "Stufe T2 (Feld Upgrade)", 10_000L, "50x T2 Birkenplanken", "50x T2 Kalksteinblock"))
            list.add(BuildingUpgradeStep(3, "Stufe T3 (Max Feld)", 40_000L, "150x T3 Kastanienplanken", "150x T3 Sandsteinblock"))
            return list
        }

        val silverCosts = mapOf(
            1 to 2_500L,
            2 to 5_000L,
            3 to 25_000L,
            4 to 75_000L,
            5 to 220_000L,
            6 to 650_000L,
            7 to 1_800_000L,
            8 to 5_000_000L
        )

        for (t in 1..maxTier) {
            val tName = when (t) {
                1 -> "Stufe T1 (Bauplatz erschließen)"
                2 -> "Stufe T2 (Fundament & Neubau)"
                else -> "Stufe T$t (Gebäude-Upgrade)"
            }
            val cost = silverCosts[t] ?: 0L
            val woodStr = ""
            val stoneStr = ""

            list.add(BuildingUpgradeStep(t, tName, cost, woodStr, stoneStr))
        }
        return list
    }

    val buildings = listOf(
        IslandBuilding(
            id = "BUILDING_COOK",
            nameDe = "🍳 Kochstelle (Cook)",
            nameEn = "🍳 Cook",
            iconEmoji = "🍳",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 350_000L,
            estimatedRoiPercent = 28.5,
            abilityDescDe = "Herstellung aller Albion-Nahrungsmitteln (Eintopf, Omelett, Fleischkuchen, Salat) mit Ernährungs-Aura.",
            cityBonusCity = "Caerleon",
            cityBonusDescDe = "+15% Ressourcen-Rückerstattung bei Nahrung in Caerleon",
            cityBonusDescEn = "+15% Resource Return Rate for Food in Caerleon",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_ALCHEMIST",
            nameDe = "🧪 Alchemielabor (Alchemist)",
            nameEn = "🧪 Alchemist Lab",
            iconEmoji = "🧪",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 280_000L,
            estimatedRoiPercent = 24.0,
            abilityDescDe = "Herstellung aller Kampf- & Heiltränke (Gifttrank, Heiltrank, Unsichtbarkeit) aus Kräutern.",
            cityBonusCity = "Caerleon",
            cityBonusDescDe = "+15% Trank-Herstellung & Kräuter-Rückerstattung in Caerleon",
            cityBonusDescEn = "+15% Potion Return Rate in Caerleon",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_FARM",
            nameDe = "🌾 Bauernhof (Farm Field)",
            nameEn = "🌾 Farm Field",
            iconEmoji = "🌾",
            category = "LANDWIRTSCHAFT",
            maxTier = 1,
            estimatedDailyIncomeSilver = 120_000L,
            estimatedRoiPercent = 18.0,
            abilityDescDe = "Anbau von Karotten, Bohnen, Weizen, Kürbis & Mais mit Fokus-Gießen für Samen-Ertrag.",
            cityBonusCity = "Caerleon & Brecilien",
            cityBonusDescDe = "+15% Ertrag auf Samen, Gemüse & Getreide",
            cityBonusDescEn = "+15% Yield on Seeds, Crop & Grains",
            upgrades = generateStandardUpgrades(1, isFarm = true)
        ),
        IslandBuilding(
            id = "BUILDING_HERB",
            nameDe = "🌿 Kräutergarten (Herb Garden)",
            nameEn = "🌿 Herb Garden",
            iconEmoji = "🌿",
            category = "LANDWIRTSCHAFT",
            maxTier = 1,
            estimatedDailyIncomeSilver = 110_000L,
            estimatedRoiPercent = 16.5,
            abilityDescDe = "Anbau aller Alchemie-Kräuter (Klettenwurz, Drachendistel, Magenwurz) für Tränke.",
            cityBonusCity = "Caerleon",
            cityBonusDescDe = "+15% Ertrag auf Kräuter & Zaubersamen in Caerleon",
            cityBonusDescEn = "+15% Yield on Herbs in Caerleon",
            upgrades = generateStandardUpgrades(1, isFarm = true)
        ),
        IslandBuilding(
            id = "BUILDING_PASTURE",
            nameDe = "🐄 Viehweide (Pasture)",
            nameEn = "🐄 Pasture",
            iconEmoji = "🐄",
            category = "LANDWIRTSCHAFT",
            maxTier = 1,
            estimatedDailyIncomeSilver = 150_000L,
            estimatedRoiPercent = 15.0,
            abilityDescDe = "Zucht von Reittieren (Pferde, Ochsen, Wildschweine) sowie Milch- & Ei-Produktion.",
            cityBonusCity = "Caerleon",
            cityBonusDescDe = "+15% Wuchsgeschwindigkeit bei Tieren & Milch in Caerleon",
            cityBonusDescEn = "+15% Animal Growth & Milk Speed in Caerleon",
            upgrades = generateStandardUpgrades(1, isFarm = true)
        ),
        IslandBuilding(
            id = "BUILDING_BUTCHER",
            nameDe = "🔪 Schlächterei (Butcher)",
            nameEn = "🔪 Butcher",
            iconEmoji = "🔪",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 90_000L,
            estimatedRoiPercent = 12.0,
            abilityDescDe = "Schlachtung von Vieh zu Rohfleisch für die Nahrungsmittel-Herstellung an der Kochstelle.",
            cityBonusCity = "Caerleon",
            cityBonusDescDe = "+15% Fleisch-Ertrag & Schlacht-Bonus in Caerleon",
            cityBonusDescEn = "+15% Meat Yield in Caerleon",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_LUMBERMILL",
            nameDe = "🪵 Sägewerk (Lumbermill)",
            nameEn = "🪵 Lumbermill",
            iconEmoji = "🪵",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 200_000L,
            estimatedRoiPercent = 14.5,
            abilityDescDe = "Veredelung von Rauholz zu Holzplanken (T1 bis T8) für Bogen, Stäbe & Werkzeuge.",
            cityBonusCity = "Kein Veredelungs-Bonus auf Inseln",
            cityBonusDescDe = "Achtung: Insel-Gebäude erhalten KEINEN Veredelungs-Rückerstattung-Bonus (RRR). Dieser gilt nur in Lymhurst!",
            cityBonusDescEn = "Note: Island buildings do NOT receive refining return rate bonuses.",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_SMELTER",
            nameDe = "⚒️ Schmelze (Smelter)",
            nameEn = "⚒️ Smelter",
            iconEmoji = "⚒️",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 210_000L,
            estimatedRoiPercent = 14.0,
            abilityDescDe = "Veredelung von Eisenerz zu Metallbarren (T1 bis T8) für Plattenrüstungen & Schwerter.",
            cityBonusCity = "Kein Veredelungs-Bonus auf Inseln",
            cityBonusDescDe = "Achtung: Insel-Gebäude erhalten KEINEN Veredelungs-Rückerstattung-Bonus (RRR). Dieser gilt nur in Thetford!",
            cityBonusDescEn = "Note: Island buildings do NOT receive refining return rate bonuses.",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_TANNER",
            nameDe = "🧥 Gerberei (Tanner)",
            nameEn = "🧥 Tanner",
            iconEmoji = "🧥",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 195_000L,
            estimatedRoiPercent = 13.8,
            abilityDescDe = "Veredelung von Tierhäuten zu Leder (T1 bis T8) für Lederrüstung & Dolche.",
            cityBonusCity = "Kein Veredelungs-Bonus auf Inseln",
            cityBonusDescDe = "Achtung: Insel-Gebäude erhalten KEINEN Veredelungs-Rückerstattung-Bonus (RRR). Dieser gilt nur in Martlock!",
            cityBonusDescEn = "Note: Island buildings do NOT receive refining return rate bonuses.",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_WEAVER",
            nameDe = "🧵 Weberei (Weaver)",
            nameEn = "🧵 Weaver",
            iconEmoji = "🧵",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 185_000L,
            estimatedRoiPercent = 13.2,
            abilityDescDe = "Veredelung von Hanf & Fasern zu Stoffballen (T1 bis T8) für Stoffroben & Magierhüte.",
            cityBonusCity = "Kein Veredelungs-Bonus auf Inseln",
            cityBonusDescDe = "Achtung: Insel-Gebäude erhalten KEINEN Veredelungs-Rückerstattung-Bonus (RRR). Dieser gilt nur in Fort Sterling!",
            cityBonusDescEn = "Note: Island buildings do NOT receive refining return rate bonuses.",
            upgrades = generateStandardUpgrades(8)
        ),
        IslandBuilding(
            id = "BUILDING_STONECUTTER",
            nameDe = "🪨 Steinmetz (Stonecutter)",
            nameEn = "🪨 Stonecutter",
            iconEmoji = "🪨",
            category = "HERSTELLUNG & VEREDELUNG",
            maxTier = 8,
            estimatedDailyIncomeSilver = 160_000L,
            estimatedRoiPercent = 11.5,
            abilityDescDe = "Veredelung von Feldsteinen zu Steinblöcken (T1 bis T8) zum Bau & Aufwerten von Inseln.",
            cityBonusCity = "Kein Veredelungs-Bonus auf Inseln",
            cityBonusDescDe = "Achtung: Insel-Gebäude erhalten KEINEN Veredelungs-Rückerstattung-Bonus (RRR). Dieser gilt nur in Bridgewatch!",
            cityBonusDescEn = "Note: Island buildings do NOT receive refining return rate bonuses.",
            upgrades = generateStandardUpgrades(8)
        )
    )
}
