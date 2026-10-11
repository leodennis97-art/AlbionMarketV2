package com.example.albionmarketv2

enum class ZoneSafety(val displayName: String, val isDangerous: Boolean) {
    SAFE_BLUE("Blaue Zone (Sicher - Kein PvP)", false),
    SAFE_YELLOW("Gelbe Zone (Gefahr - K.O. PvP)", false),
    DANGEROUS_RED("Rote Zone (Gefährlich - Full Loot PvP)", true),
    DANGEROUS_BLACK("Schwarze Zone (Outlands - Full Loot PvP)", true)
}

data class LiveEvent(
    val title: String,
    val category: String,
    val schedule: String,
    val description: String,
    val rewardSummary: String,
    val zoneSafety: ZoneSafety
)

data class ZoneMonster(
    val name: String,
    val type: String,
    val tierRange: String,
    val mainZones: String,
    val dropItems: List<String>,
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

    val liveEvents = listOf(
        LiveEvent(
            title = "Fraktionskrieg (Faction Warfare)",
            category = "Königliches PvP & PvE",
            schedule = "Rund um die Uhr aktiv",
            description = "Schließe dich Bridgewatch, Fort Sterling, Lymhurst, Martlock, Thetford oder Caerleon an. Erobere Vorposten und Zonen.",
            rewardSummary = "Fraktions-Punkte, Frequenz-Truhen, Herzen & Spezial-Umhänge",
            zoneSafety = ZoneSafety.SAFE_YELLOW
        ),
        LiveEvent(
            title = "Höllentore (Hellgates 2v2, 5v5, 10v10)",
            category = "PvP & Dungeons",
            schedule = "Kontinuierlich zugänglich",
            description = "Kämpfe in abgegrenzten Höllenarenan gegen Dämonen und feindliche Spieler-Gruppen.",
            rewardSummary = "Massive Artefakte, T7/T8 Beute, Dämonen-Relikte & Silber",
            zoneSafety = ZoneSafety.DANGEROUS_RED
        ),
        LiveEvent(
            title = "Die Nebel von Brecilien (The Mists)",
            category = "Solo & Duo Erkundung",
            schedule = "Ständig wechselnd",
            description = "Betrete mystische Nebelpfade aus der gelben oder roten Zone. Entdecke versteckte Schätze und Wisps.",
            rewardSummary = "Brecilien-Ansehen, Feen-Drachen Schuppen, Artefakte, T8 Erze",
            zoneSafety = ZoneSafety.SAFE_YELLOW
        ),
        LiveEvent(
            title = "Schloss-Eroberung (Castles & Outposts)",
            category = "Gilden-Massen-PvP (ZvZ)",
            schedule = "Alle 6 Stunden (Burg-Timer)",
            description = "Erobere Burgen in den Schwarzen Landen für Territoriums-Herrschaft deiner Gilde.",
            rewardSummary = "T8 Legendäre Belohnungstruhen, Saison-Punkte, Unmengen Silber",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK
        )
    )

    val zoneMonsters = listOf(
        ZoneMonster(
            name = "Erz-Elementar (Ore Elemental)",
            type = "Ressourcen-Kreatur",
            tierRange = "Tier 4 bis Tier 8",
            mainZones = "Fort Sterling / Gebirgs-Biome",
            dropItems = listOf("Kupfererz", "Eisenerz", "Titanerz", "Runenerz", "Meteoritenerz", "Adamantiumerz"),
            description = "Nach dem Besiegen kann die Kreatur mit der Spitzhacke abgebaut werden."
        ),
        ZoneMonster(
            name = "Eichen-Hüter (Dryad / Sprite)",
            type = "Ressourcen-Kreatur",
            tierRange = "Tier 4 bis Tier 8",
            mainZones = "Lymhurst / Wald-Biome",
            dropItems = listOf("Kiefernstamm", "Zedernstamm", "Bluteichenstamm", "Eschenrindenstamm", "Weißholzstamm"),
            description = "Bewacht die Wälder. Kann nach dem Sieg mit der Holzfäller-Axt geerntet werden."
        ),
        ZoneMonster(
            name = "Mammut & Riesenhirsch",
            type = "Großwild-Tiere",
            tierRange = "Tier 5 bis Tier 8",
            mainZones = "Steppe (Bridgewatch) & Schnee",
            dropItems = listOf("Robuste Haut", "Verstärkte Haut", "Riesenhirsch-Kitz (Drop)", "Transportmammut-Kalb (Selten!)"),
            description = "Seltener Wildtier-Drop. Das Kalb kann auf der eigenen Insel gezüchtet werden."
        ),
        ZoneMonster(
            name = "Hüter der Erde (Keepers Bosses)",
            type = "PvE Fraktion",
            tierRange = "Tier 4 bis Tier 8",
            mainZones = "Statische Dungeons in Wäldern",
            dropItems = listOf("Hüter-Artefakte", "Axt- & Bogen-Ausrüstung", "Runen", "Gefallenes Holz"),
            description = "Mächtige Wald-Kreaturen mit hohen Artefakt-Dropchancen."
        ),
        ZoneMonster(
            name = "Kult der Morgana (Morgana Cultists)",
            type = "Dämonische Krieger",
            tierRange = "Tier 5 bis Tier 8",
            mainZones = "Morgana Statische Dungeons & Burgen",
            dropItems = listOf("Morgana-Relikte", "Zauberstäbe & Stoffroben", "Seelen", "Schwarzes Herz"),
            description = "Dunkle Magier und Krieger mit wertvollem Zauberer-Equipment."
        )
    )

    val worldRegions = listOf(
        WorldMapRegion(
            name = "Bridgewatch",
            type = "Hauptstadt (Steppe)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Wüsten-Königstadt. Zentrum für Lederverarbeitung und Ochsenzucht.",
            resourcesFound = listOf("Haut / Leder (Haupt)", "Erz", "Faser"),
            connectsTo = listOf("Fort Sterling", "Caerleon", "Thetford")
        ),
        WorldMapRegion(
            name = "Fort Sterling",
            type = "Hauptstadt (Gebirge)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Schnee-Bergstadt. Zentrum für Erzverhüttung und Bärenzucht.",
            resourcesFound = listOf("Erz (Haupt)", "Stein", "Holz"),
            connectsTo = listOf("Bridgewatch", "Lymhurst", "Caerleon")
        ),
        WorldMapRegion(
            name = "Lymhurst",
            type = "Hauptstadt (Wald)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Wald-Königstadt. Zentrum für Holzverarbeitung und Wildschweinzucht.",
            resourcesFound = listOf("Holz (Haupt)", "Haut", "Faser"),
            connectsTo = listOf("Fort Sterling", "Martlock", "Caerleon")
        ),
        WorldMapRegion(
            name = "Martlock",
            type = "Hauptstadt (Hochland)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Hochland-Stadt. Zentrum für Steinmetzarbeiten.",
            resourcesFound = listOf("Stein (Haupt)", "Erz", "Holz"),
            connectsTo = listOf("Lymhurst", "Thetford", "Caerleon")
        ),
        WorldMapRegion(
            name = "Thetford",
            type = "Hauptstadt (Sumpf)",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Sichere Sumpf-Stadt. Zentrum für Stoffweberei.",
            resourcesFound = listOf("Faser (Haupt)", "Holz", "Haut"),
            connectsTo = listOf("Martlock", "Bridgewatch", "Caerleon")
        ),
        WorldMapRegion(
            name = "Caerleon 🔴",
            type = "Gefährliche Stadt (Rot-Zone Zentrum)",
            safety = ZoneSafety.DANGEROUS_RED,
            description = "Zentraler Handels-Knotenpunkt umgeben von ROTEN ZONEN (Full Loot PvP!). Bietet den Schwarzmarkt (Black Market).",
            resourcesFound = listOf("Alle T6-T8 Ressourcen in den umliegenden Rot-Zonen"),
            connectsTo = listOf("Bridgewatch", "Fort Sterling", "Lymhurst", "Martlock", "Thetford")
        ),
        WorldMapRegion(
            name = "Brecilien 🔵",
            type = "Versteckte Nebel-Stadt",
            safety = ZoneSafety.SAFE_BLUE,
            description = "Mystische Zufluchtsstadt tief in den Nebeln. Nur über Feen-Portale erreichbar.",
            resourcesFound = listOf("Nebel-Artefakte", "Feen-Schuppen", "T7-T8 Nebelmaterialien"),
            connectsTo = listOf("Die Nebel (The Mists)")
        ),
        WorldMapRegion(
            name = "Die Schwarze Zone (Outlands) 🔴",
            type = "Full Loot PvP Kontinent",
            safety = ZoneSafety.DANGEROUS_BLACK,
            description = "Riesiger Kontinent ohne Stadtwachen. Maximale T8-Ressourcen, Gilden-Territorien & Schlösser.",
            resourcesFound = listOf("T7 & T8 Erze", "T8 Holz", "T8 Haut", "T8 Faser", "T8 Stein"),
            connectsTo = listOf("Realmsgate Portale in den Hauptstädten")
        )
    )
}
