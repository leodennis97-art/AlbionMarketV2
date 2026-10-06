package com.example.albionmarketv2

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ZoneSafety(val displayName: String, val isDangerous: Boolean) {
    SAFE_BLUE("Blaue Zone (Sicher - Kein PvP)", false),
    SAFE_YELLOW("Gelbe Zone (Gefahr - K.O. PvP)", false),
    DANGEROUS_RED("Rote Zone (Gefährlich - Full Loot PvP)", true),
    DANGEROUS_BLACK("Schwarze Zone (Outlands - Full Loot PvP)", true)
}

data class LiveEventItem(
    val id: String,
    val title: String,
    val category: String,
    val startDateFormatted: String,
    val endDateFormatted: String,
    val remainingMinutes: Long,
    val description: String,
    val rewardSummary: String,
    val zoneName: String,
    val zoneSafety: ZoneSafety,
    val isExpired: Boolean = false,
)

data class ChestDrop(
    val nameDe: String,
    val rarity: String, // Grünes Buch, Blauer Tresor, Violette Truhe, Legendäre Goldtruhe
    val dropChancePercent: Double
)

data class AiPlayerAdvice(
    val playerCategory: PlayerCategory,
    val aiRecommendationDe: String,
    val aiRecommendationEn: String,
    val recommendedActivities: List<String>,
    val estimatedProfitScore: String,
    val riskLevelDe: String
)

object AiEventAndBossAdvisor {
    fun getAdviceForCategory(category: PlayerCategory): AiPlayerAdvice {
        return when (category) {
            PlayerCategory.SOLO -> AiPlayerAdvice(
                playerCategory = category,
                aiRecommendationDe = "🤖 KI-Entscheidung (Solo): Konzentriere dich auf T8 Solo-Dungeons, Korrumpierte Dungeons (Slayer 1v1) und Die Nebel (The Mists) für maximalen Solo-Gewinn.",
                aiRecommendationEn = "🤖 AI Decision (Solo): Focus on T8 Solo Dungeons, Corrupted Dungeons (Slayer 1v1), and The Mists for maximum solo profit.",
                recommendedActivities = listOf("T8 Solo Dungeon (Schwarze Zone)", "Korrumpierte Dungeons (Slayer 1v1)", "The Mists / Feen-Drache", "Solo Expeditions"),
                estimatedProfitScore = "~1.250.000 - 3.200.000 S. / Std.",
                riskLevelDe = "Mittel bis Hoch (Full-Loot in Outlands)"
            )
            PlayerCategory.DUO -> AiPlayerAdvice(
                playerCategory = category,
                aiRecommendationDe = "🤖 KI-Entscheidung (Duo): 2v2 Höllentore (Lethal) und Straßen von Avalon (Roads Duo Chests) bieten den höchsten Duo-Profit.",
                aiRecommendationEn = "🤖 AI Decision (Duo): 2v2 Lethal Hellgates and Roads of Avalon Duo Chests offer the highest duo profit.",
                recommendedActivities = listOf("2v2 Höllentore (Lethal)", "Roads of Avalon Duo Chests", "Kristallspinne (Roaming Boss)", "Duo Nebel-Camps"),
                estimatedProfitScore = "~1.900.000 - 4.500.000 S. / Std.",
                riskLevelDe = "Sehr Hoch (Full-Loot PvPvE)"
            )
            PlayerCategory.GROUP -> AiPlayerAdvice(
                playerCategory = category,
                aiRecommendationDe = "🤖 KI-Entscheidung (Gruppe): T8 Statische Gruppen-Dungeons (Static) und HCE Stufe 18 (Hardcore Expeditions) sind die Meta für Fame & Silber.",
                aiRecommendationEn = "🤖 AI Decision (Group): T8 Static Group Dungeons and HCE Level 18 are the meta for Fame & Silver.",
                recommendedActivities = listOf("T8 Statische Gruppen-Dungeons", "Hardcore Expedition Level 18 (HCE)", "5v5 Höllentore", "Fraktionskrieg Vorposten-Sturm"),
                estimatedProfitScore = "~3.000.000 - 6.500.000 S. / Std.",
                riskLevelDe = "Sicher (HCE) bis Sehr Hoch (Statics)"
            )
            PlayerCategory.RAID -> AiPlayerAdvice(
                playerCategory = category,
                aiRecommendationDe = "🤖 KI-Entscheidung (Raid & Gilde): Avalonien 20-Spieler Gold-Raids, Weltbosse (Erdmutter & Mammut) und Schloss-Eroberungen (ZvZ) maximieren Gilden-Einnahmen.",
                aiRecommendationEn = "🤖 AI Decision (Raid & Guild): Avalonian 20-player gold raids, World Bosses and Castle ZvZ sieges maximize guild revenue.",
                recommendedActivities = listOf("T8.4 Avalonien Gold-Raid (20 Spieler)", "Uraltes Weißes Mammut (Old White)", "Hüter-Erdmutter World Boss", "Outlands Schloss-Eroberung (ZvZ)"),
                estimatedProfitScore = "~4.000.000 - 15.000.000+ S. / Std.",
                riskLevelDe = "Extrem (ZvZ & Großgilden PvP)"
            )
            PlayerCategory.ALL -> AiPlayerAdvice(
                playerCategory = category,
                aiRecommendationDe = "🤖 KI-Gesamtübersicht: Alle Albion Online Aktivitäten, Events, Dungeons und Boss-Loot basierend auf Echtzeit-Markt und Meta.",
                aiRecommendationEn = "🤖 AI Overview: All Albion Online activities, events, dungeons, and boss loot based on real-time market and meta.",
                recommendedActivities = listOf("Live Events (Fraktionen & Höllentore)", "Weltbosse & Raids", "Random Dungeons & Static", "Expeditions & HCE"),
                estimatedProfitScore = "Variabel (~1.000.000 - 15.000.000 S. / Std.)",
                riskLevelDe = "Variabel (Sicher bis Full-Loot)"
            )
        }
    }

    fun getFilteredEventsForCategory(events: List<LiveEventItem>, category: PlayerCategory): List<LiveEventItem> {
        if (category == PlayerCategory.ALL) return events
        val filtered = events.filter { event ->
            when (category) {
                PlayerCategory.SOLO -> event.title.contains("Nebel", ignoreCase = true) || event.category.contains("Solo", ignoreCase = true) || event.description.contains("Solo", ignoreCase = true)
                PlayerCategory.DUO -> event.title.contains("Höllentor", ignoreCase = true) || event.category.contains("Duo", ignoreCase = true) || event.description.contains("Duo", ignoreCase = true)
                PlayerCategory.GROUP -> event.title.contains("Fraktion", ignoreCase = true) || event.title.contains("Gruppe", ignoreCase = true) || event.title.contains("Höllentor", ignoreCase = true)
                PlayerCategory.RAID -> event.title.contains("Schloss", ignoreCase = true) || event.title.contains("Eroberung", ignoreCase = true) || event.title.contains("Raid", ignoreCase = true) || event.category.contains("Gilden", ignoreCase = true)
                else -> true
            }
        }
        return filtered.ifEmpty { events }
    }
}

data class BossLootBoss(
    val id: String,
    val name: String,
    val title: String,
    val categoryType: String, // "BOSS" or "DUNGEON"
    val tier: Int,
    val location: String,
    val zoneSafety: ZoneSafety,
    val respawnTime: String,
    val difficulty: String,
    val recommendedGroup: String,
    val estimatedLootSilver: Long,
    val guaranteedLoot: List<String>,
    val rareLoot: List<String>,
    val lootDropChances: Map<String, String>,
    val possibleChests: List<ChestDrop>,
    val combatMechanics: String,
    val description: String
)

data class WorldMapRegion(
    val name: String,
    val type: String, // City, Biome, BlackZone, Mists
    val safety: ZoneSafety,
    val description: String,
    val resourcesFound: List<String>,
    val connectsTo: List<String>
)

object AlbionWorldData {

    // Dynamic Live Events Manager - updates every 10 minutes & sorts most recent to top
    fun generateLiveEvents(): List<LiveEventItem> {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)

        val cycleMs = 10 * 60 * 1000L // 10 minutes refresh cycle
        val baseTime = (now / cycleMs) * cycleMs

        val events = listOf(
            LiveEventItem(
                id = "evt_faction_${baseTime}_1",
                title = "🔥 Fraktionskrieg: Vorposten-Sturm",
                category = "Königliches PvP & PvE",
                startDateFormatted = sdf.format(Date(baseTime)),
                endDateFormatted = sdf.format(Date(baseTime + cycleMs)),
                remainingMinutes = (((baseTime + cycleMs - now) / 60000L)).coerceIn(0, 10),
                description = "Erobere feindliche Vorposten in der Steppe und im Wald. Erhalte doppelte Fraktions-Punkte!",
                rewardSummary = "Fraktions-Punkte, Frequenz-Truhen, Herzen & Spezial-Umhänge",
                zoneName = "Königliche Steppen & Wälder (Bridgewatch / Lymhurst Outskirts)",
                zoneSafety = ZoneSafety.SAFE_YELLOW,
                isExpired = false
            ),
            LiveEventItem(
                id = "evt_hellgate_${baseTime}_2",
                title = "🌋 Höllentor-Invasion (5v5 & 10v10)",
                category = "PvP & Dungeons",
                startDateFormatted = sdf.format(Date(baseTime)),
                endDateFormatted = sdf.format(Date(baseTime + cycleMs)),
                remainingMinutes = (((baseTime + cycleMs - now) / 60000L)).coerceIn(0, 10),
                description = "Dämonen horden sich in den Höllentoren. Höhere Artefakt-Dropchancen für 10 Minuten!",
                rewardSummary = "T7/T8 Dämonen-Artefakte, Inferno-Ausrüstung, 500k+ Silber",
                zoneName = "Outlands Höllentore & Schwarze Zone Dungeons",
                zoneSafety = ZoneSafety.DANGEROUS_RED,
                isExpired = false
            ),
            LiveEventItem(
                id = "evt_mists_${baseTime}_3",
                title = "✨ Nebelschätze von Brecilien (The Mists)",
                category = "Solo & Duo Erkundung",
                startDateFormatted = sdf.format(Date(baseTime)),
                endDateFormatted = sdf.format(Date(baseTime + cycleMs)),
                remainingMinutes = (((baseTime + cycleMs - now) / 60000L)).coerceIn(0, 10),
                description = "Gefangene Wisps und seltene Feen-Drachen erscheinen gehäuft in den Nebeln.",
                rewardSummary = "Brecilien-Ansehen, Feen-Drachen Schuppen, T8 Nebelmaterialien",
                zoneName = "Die Nebel (The Mists - Brecilien Outlands Portal)",
                zoneSafety = ZoneSafety.SAFE_YELLOW,
                isExpired = false
            ),
            LiveEventItem(
                id = "evt_castle_${baseTime}_4",
                title = "⚔️ Outlands Schloss-Eroberung (Castles & Outposts)",
                category = "Gilden-Massen-PvP (ZvZ)",
                startDateFormatted = sdf.format(Date(baseTime)),
                endDateFormatted = sdf.format(Date(baseTime + cycleMs)),
                remainingMinutes = (((baseTime + cycleMs - now) / 60000L)).coerceIn(0, 10),
                description = "Burg-Timer aktiv! Erobere gegnerische Burgen für saisonale Gildentruhen.",
                rewardSummary = "T8 Legendäre Belohnungstruhen, Saison-Punkte",
                zoneName = "Schwarze Zone Burgen & Außenposten (Outlands)",
                zoneSafety = ZoneSafety.DANGEROUS_BLACK,
                isExpired = false
            )
        )

        return events.asSequence().filter { !it.isExpired && it.remainingMinutes > 0 }
            .sortedByDescending { it.remainingMinutes }.toList()
    }

    // Boss & Dungeon Loot Detailed Database
    val bossLootList = listOf(
        // 1. WELTBOSSE (BOSS)
        BossLootBoss(
            id = "boss_earthmother",
            name = "Hüter-Erdmutter (Earthmother)",
            title = "Uralter Wald-Weltboss",
            categoryType = "BOSS",
            tier = 8,
            location = "Lymhurst Statische Dungeons & Schwarze Zone Wälder",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            respawnTime = "24 Stunden",
            difficulty = "Großgilden Raid (10-20 Spieler)",
            recommendedGroup = "2 Tanks, 4 Heiler, 12+ DPS",
            estimatedLootSilver = 2_500_000L,
            guaranteedLoot = listOf("T8 Gefallenes Holz (100%)", "Hüter-Segen-Relikte (100%)", "T8 Meister-Runen (100%)"),
            rareLoot = listOf("Hüter-Artefakt (Wurzelstab)", "Großes Bären-Reittier-Fell", "T8.3 Reliktschatulle"),
            lootDropChances = mapOf(
                "Hüter-Artefakt (Wurzelstab)" to "8.5%",
                "Großes Bären-Reittier-Fell" to "3.2%",
                "T8.3 Reliktschatulle" to "2.1%"
            ),
            possibleChests = listOf(
                ChestDrop("Legendäre Hüter-Goldtruhe", "Legendär", 15.0),
                ChestDrop("Violette Hüter-Schatulle", "Episch", 35.0),
                ChestDrop("Blaue Holzfäller-Kiste", "Selten", 50.0)
            ),
            combatMechanics = "Beschwört Baumstamm-Wellen und Erdbeben-Flächenschaden. Tanks müssen den Boss umdrehen!",
            description = "Mächtigster Hüter-Boss in Albion Online. Besitzt extrem wertvolles T8.3 Equipment und seltene Holzfäller-Artefakte."
        ),
        BossLootBoss(
            id = "boss_harvester",
            name = "Erntebringer der Seelen",
            title = "Untoter Katakomben-König",
            categoryType = "BOSS",
            tier = 8,
            location = "Caerleon Rot-Zone & Schwarze Zone Dungeons",
            zoneSafety = ZoneSafety.DANGEROUS_RED,
            respawnTime = "12 Stunden",
            difficulty = "Group Dungeon Boss (5-10 Spieler)",
            recommendedGroup = "1 Tank, 2 Heiler, 5 DPS",
            estimatedLootSilver = 1_800_000L,
            guaranteedLoot = listOf("T8 Untote Seelen (100%)", "Dunkle Roben-Stoffe (100%)"),
            rareLoot = listOf("Seelenernter-Artefakt", "Skelett-Kampfross Panzerung", "T8 Phiole des Todes"),
            lootDropChances = mapOf(
                "Seelenernter-Artefakt" to "12.0%",
                "Skelett-Kampfross Panzerung" to "4.5%",
                "T8 Phiole des Todes" to "6.0%"
            ),
            possibleChests = listOf(
                ChestDrop("Untote Schatztruhe", "Episch", 40.0),
                ChestDrop("Skelett-Tresor", "Selten", 60.0)
            ),
            combatMechanics = "Nutzt Seelenentzug und Flächen-Giftwolken. Heiler müssen sofort Debuffs reinigen!",
            description = "Herrscher der Untoten-Katakomben. Bekannt für extrem hohe Silber-Drops und Waffen-Artefakte."
        ),
        BossLootBoss(
            id = "boss_demon_prince",
            name = "Dämonenprinz von Morgana",
            title = "Lord der Höllentore",
            categoryType = "BOSS",
            tier = 8,
            location = "10v10 Höllentore & Morgana Schlösser",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            respawnTime = "Kontinuierlich in 10v10 Hellgates",
            difficulty = "Endboss 10v10 PvPvE",
            recommendedGroup = "Strikte 10-Spieler Meta-Gruppe",
            estimatedLootSilver = 3_200_000L,
            guaranteedLoot = listOf("T8 Dämonen-Herz (100%)", "Morgana-Umhang-Stoffe (100%)", "T8.2 Seelen (100%)"),
            rareLoot = listOf("Dämonenprinz-Flügel (Artefakt)", "Höchste Morgana-Rüstung", "Legendäre Schatztruhe"),
            lootDropChances = mapOf(
                "Dämonenprinz-Flügel (Artefakt)" to "18.5%",
                "Höchste Morgana-Rüstung" to "14.2%",
                "Legendäre Schatztruhe" to "9.0%"
            ),
            possibleChests = listOf(
                ChestDrop("Legendäre Dämonen-Goldtruhe", "Legendär", 25.0),
                ChestDrop("Morgana-Gilden-Tresor", "Episch", 75.0)
            ),
            combatMechanics = "Verwandelt sich bei 50% HP und schleudert Feuerstürme auf das ganze Schlachtfeld.",
            description = "Der ultimative Endboss in den 10v10 Höllentoren. Verlangt perfekte Gruppenkoordination."
        ),

        // 2. DUNGEONS & SCHATZTRUHEN (DUNGEON)
        BossLootBoss(
            id = "dungeon_solo_t8",
            name = "T8 Legendärer Solo-Dungeon",
            title = "Schwarze Zone Random Solo Dungeon",
            categoryType = "DUNGEON",
            tier = 8,
            location = "Schwarze Zone (Outlands) T8 Gebiete",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            respawnTime = "Zufällige Portale überall in T8 Zonen",
            difficulty = "Solo PvE (1 Spieler)",
            recommendedGroup = "Solo Build (Naturstab / Brawler / Curse)",
            estimatedLootSilver = 850_000L,
            guaranteedLoot = listOf("T8 Runen & Seelen (100%)", "Dungeon-Silberbeutel (100%)"),
            rareLoot = listOf("T8.3 Meister-Ausrüstung", "T8 Luxusgüter-Artefakte", "Reittier-Fohlen"),
            lootDropChances = mapOf(
                "T8.3 Meister-Ausrüstung" to "5.5%",
                "T8 Luxusgüter-Artefakte" to "14.0%",
                "Reittier-Fohlen" to "2.8%"
            ),
            possibleChests = listOf(
                ChestDrop("Legendäre Goldene Buch-Truhe", "Legendär (~1.2M Silber)", 8.0),
                ChestDrop("Violette Zauber-Schatulle", "Episch (~450k Silber)", 22.0),
                ChestDrop("Blaue Holzfäller-Kiste", "Selten (~180k Silber)", 40.0),
                ChestDrop("Grüner Holzfäller-Sack", "Gewöhnlich (~60k Silber)", 30.0)
            ),
            combatMechanics = "Enthält 3-5 Ebenen mit Mini-Bossen & Endboss. Vorsicht vor gegnerischen Solo-Divern!",
            description = "Bester Solo-Dungeon für maximale Kampfspezialisierung, T8 Ausrüstung und wertvolle Buch-Drop-Truhen."
        ),
        BossLootBoss(
            id = "dungeon_hellgate_10v10",
            name = "10v10 Dämonen-Höllentor",
            title = "Inferno PvPvE Hellgate",
            categoryType = "DUNGEON",
            tier = 8,
            location = "Schwarze Zone Höllentor-Portale",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            respawnTime = "Kontinuierlich bei Portal-Spawn",
            difficulty = "Hardcore 10v10 PvPvE",
            recommendedGroup = "Strikte 10-Spieler Meta-Gilden-Raid",
            estimatedLootSilver = 3_500_000L,
            guaranteedLoot = listOf("T8.2 Dämonen-Herzen (100%)", "10x T8 Seelen (100%)"),
            rareLoot = listOf("Dämonenprinz-Flügel", "T8.3 Artefakte", "Höllenpferd-Haut"),
            lootDropChances = mapOf(
                "Dämonenprinz-Flügel" to "18.5%",
                "T8.3 Artefakte" to "25.0%",
                "Höllenpferd-Haut" to "6.2%"
            ),
            possibleChests = listOf(
                ChestDrop("Legendäre Dämonen-Schatztruhe", "Legendär (~3.5M Silber)", 30.0),
                ChestDrop("Inferno-Gilden-Tresor", "Episch (~1.2M Silber)", 70.0)
            ),
            combatMechanics = "Zwingt zwei 10er Gruppen zum Kampf um die Schatztruhen im Zentrum der Dämonen-Festung.",
            description = "Höchstbezahlter PvPvE-Dungeon in Albion Online. Verlangt perfekte Gruppenkoordination."
        )
    )

    // Complete Albion World Map Database with ALL Royal Cities & Regions
    val worldRegions = listOf(
        WorldMapRegion(
            name = "Bridgewatch",
            type = "Hauptstadt (Steppe / Wüste)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Wüsten-Königstadt. Zentrum für Steinmetz-Veredelung (+56% RRR) und Ochsen- & Reittierzucht.",
            resourcesFound = listOf("Haut / Leder (Haupt)", "Erz", "Faser"),
            connectsTo = listOf("Fort Sterling", "Caerleon", "Thetford", "Martlock")
        ),
        WorldMapRegion(
            name = "Fort Sterling",
            type = "Hauptstadt (Gebirge / Schnee)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Schnee-Bergstadt. Zentrum für Faser- & Stoff-Weberei (+56% RRR) und Bärenzucht.",
            resourcesFound = listOf("Erz (Haupt)", "Stein", "Holz"),
            connectsTo = listOf("Bridgewatch", "Lymhurst", "Caerleon", "Thetford")
        ),
        WorldMapRegion(
            name = "Lymhurst",
            type = "Hauptstadt (Wald / Forst)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Wald-Königstadt. Zentrum für Rauholz- & Planken-Veredelung (+56% RRR) und Bogenbau.",
            resourcesFound = listOf("Holz (Haupt)", "Stein", "Haut"),
            connectsTo = listOf("Fort Sterling", "Bridgewatch", "Caerleon")
        ),
        WorldMapRegion(
            name = "Martlock",
            type = "Hauptstadt (Hochland / Klippen)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Klippen-Königstadt. Zentrum für Tierhaut- & Leder-Gerberei (+56% RRR) und Axt- & Dolchherstellung.",
            resourcesFound = listOf("Stein (Haupt)", "Erz", "Holz"),
            connectsTo = listOf("Bridgewatch", "Thetford", "Caerleon")
        ),
        WorldMapRegion(
            name = "Thetford",
            type = "Hauptstadt (Sumpf / Moor)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Sumpf-Königstadt. Zentrum für Eisenerz-Schmelzerei (+56% RRR) und Magier-Staff-Herstellung.",
            resourcesFound = listOf("Faser (Haupt)", "Holz", "Erz"),
            connectsTo = listOf("Fort Sterling", "Martlock", "Caerleon")
        ),
        WorldMapRegion(
            name = "Caerleon",
            type = "Zentrum-Königstadt (Rot-Zone / Schwarzmarkt)",
            safety = ZoneSafety.DANGEROUS_RED,
            description = "Gefährliche Stadt im Zentrum des königlichen Kontinents. Beimatet den berühmten SCHWARZMARKT und bietet +15% RRR Bonus auf Tränke & Nahrung.",
            resourcesFound = listOf("Schwarzmarkt (System-Verkauf)", "Trank-Herstellung", "Nahrungsmittel-Kochstelle"),
            connectsTo = listOf("Bridgewatch", "Fort Sterling", "Lymhurst", "Martlock", "Thetford")
        ),
        WorldMapRegion(
            name = "Brecilien",
            type = "Mystische Nebelstadt (The Mists)",
            safety = ZoneSafety.SAFE_YELLOW,
            description = "Versteckte Nebelstadt außerhalb der normalen Weltkarte. Bietet Direktzugang zu den Nebeln (The Mists) & Feen-Drachen Artefakte.",
            resourcesFound = listOf("Nebel-Materialien", "Wisps Schätze", "Brecilien-Ansehen"),
            connectsTo = listOf("Die Nebel (The Mists)", "Avalon-Pfade", "Königliche Kontinental-Portale")
        ),
        WorldMapRegion(
            name = "Outlands (Schwarze Zonen)",
            type = "Full-Loot PvP Kontinent",
            safety = ZoneSafety.DANGEROUS_BLACK,
            description = "Die feindseligen Schwarzen Lande. Heimat von T8 High-Tier Ressourcen, Gilden-Burgen, Territorien & Arthur's Rest Portalen.",
            resourcesFound = listOf("T8 Rauholz / Eisenerz / Haut", "T8.3 Relikte", "Saison-Schatztruhen"),
            connectsTo = listOf("Portal-Städte (Bridgewatch, Fort Sterling, Lymhurst, Martlock, Thetford Portale)")
        ),
        WorldMapRegion(
            name = "Roads of Avalon (Avalon-Pfade)",
            type = "Dynamische Pfade & Tunnel",
            safety = ZoneSafety.DANGEROUS_BLACK,
            description = "Mystisches Höhlen- & Tunnelsystem. Verknüpft zufällige Zonen in Albion Online für Abkürzungen & T8 Aspekte.",
            resourcesFound = listOf("Avalonien-Erztropfen", "T8 Rohstoff-Aspekte", "Versteckte Truhen"),
            connectsTo = listOf("Königliche Zonen", "Schwarze Zonen", "Brecilien")
        ),
        WorldMapRegion(
            name = "The Mists (Die Nebel)",
            type = "Solo & Duo Wisps-Region",
            safety = ZoneSafety.DANGEROUS_BLACK,
            description = "Magische Nebelwelten für Solo- & Duo-Erkundung. Bietet gefangene Wisps, Fee-Drachen und Brecilien-Ansehen.",
            resourcesFound = listOf("Feen-Schuppen", "Wisps-Mischgut", "T8 Nebelholz"),
            connectsTo = listOf("Brecilien", "Zufällige Outlands-Portale")
        )
    )
}
