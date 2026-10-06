package com.example.albionmarketv2

enum class PlayerCategory(val displayNameDe: String, val displayNameEn: String, val iconEmoji: String) {
    ALL("Alle Spielerzahlen", "All Player Counts", "🌐"),
    SOLO("Solo (1 Spieler)", "Solo (1 Player)", "👤"),
    DUO("Duo / Small (2-3 Spieler)", "Duo / Small (2-3 Players)", "👥"),
    GROUP("Gruppe (4-10 Spieler)", "Group (4-10 Players)", "⚔️"),
    RAID("Raid & Gilde (10-20+ Spieler)", "Raid & Guild (10-20+ Players)", "🏰")
}

data class MonsterDropItem(
    val itemNameDe: String,
    val itemNameEn: String,
    val dropChancePercent: Double,
    val estimatedValueSilver: Long = 0L,
    val isRare: Boolean = false
)

enum class MonsterType(val displayNameDe: String, val displayNameEn: String) {
    WORLD_BOSS("Weltboss", "World Boss"),
    ROAMING_BOSS("Wanderboss", "Roaming Boss"),
    DUNGEON_BOSS("Dungeon Endboss", "Dungeon Boss"),
    STATIC_BOSS("Statisch Dungeon Boss", "Static Dungeon Boss"),
    ASPECT("Aspekt / Ressourcen-Titan", "Resource Aspect"),
    HELLGATE_BOSS("Höllentor Boss", "Hellgate Boss"),
    MISTS_BOSS("Nebel Boss", "Mists Boss"),
    ROADS_BOSS("Avalon-Pfade Boss", "Roads Boss"),
    ELITE_MOB("Elite / Veteran Wildtier", "Elite / Veteran Mob")
}

data class AlbionMonster(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val type: MonsterType,
    val playerCategory: PlayerCategory = PlayerCategory.GROUP,
    val recommendedPlayerCount: String = "4-10 Spieler",
    val tier: Int,
    val regionDe: String,
    val regionEn: String,
    val zoneLocationDe: String,
    val zoneLocationEn: String,
    val zoneSafety: ZoneSafety,
    val estimatedProfitSilver: Long,
    val estimatedSilverPerHour: String = "~850.000 - 2.500.000 S. / Std.",
    val chestDropSummaryDe: String = "🎁 Truhen: Legendäre Goldtruhe (~1.8M S.), Violette Schatulle (~450k S.), Blauer Tresor (~180k S.)",
    val fameAmount: Long,
    val respawnTimeDe: String,
    val respawnTimeEn: String,
    val difficultyDe: String,
    val difficultyEn: String,
    val drops: List<MonsterDropItem>,
    val imageUrl: String,
    val combatTipsDe: String,
    val combatTipsEn: String,
    val descriptionDe: String,
    val descriptionEn: String
)

object AlbionMonsterRepository {

    val monsters: List<AlbionMonster> = listOf(
        // ----------------------------------------------------
        // 1. SOLO AKTIVITÄTEN & DUNGEONS (1 SPIELER)
        // ----------------------------------------------------
        AlbionMonster(
            id = "dungeon_solo_t8",
            nameDe = "T8 Random Solo Dungeon (Schwarze Zone)",
            nameEn = "T8 Random Solo Dungeon",
            type = MonsterType.DUNGEON_BOSS,
            playerCategory = PlayerCategory.SOLO,
            recommendedPlayerCount = "1 Spieler (Solo)",
            tier = 8,
            regionDe = "Outlands (Schwarze Zone)",
            regionEn = "Outlands (Black Zone)",
            zoneLocationDe = "T8 Schwarze Zone Portale & Gebiete",
            zoneLocationEn = "T8 Black Zone Entrances",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 1_250_000L,
            estimatedSilverPerHour = "~850.000 - 1.800.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: Legendäre Goldtruhe (~1.2M S.), Violette Schatulle (~420k S.), Blauer Tresor (~160k S.)",
            fameAmount = 85_000L,
            respawnTimeDe = "Sofortiger Spawn in Open World",
            respawnTimeEn = "Instant Spawn",
            difficultyDe = "Solo PvE (Einfach bis Mittel)",
            difficultyEn = "Solo PvE (Easy to Medium)",
            drops = listOf(
                MonsterDropItem("T8.3 Meister-Ausrüstung", "T8.3 Master Gear", 5.5, 1_800_000L, isRare = true),
                MonsterDropItem("T8 Foliant des Wissens", "T8 Tome of Insight", 100.0, 180_000L),
                MonsterDropItem("Dungeon-Silberbeutel", "Dungeon Silver Bag", 100.0, 250_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_BAG.png",
            combatTipsDe = "Naturstab oder Curse Waffe nutzen für schnelle Clears. Eingang nach 90 Sek. geschlossen!",
            combatTipsEn = "Use Nature Staff or Curse for fast clears. Entrance closes after 90 sec!",
            descriptionDe = "Bester Solo-Dungeon in den Schwarzen Landen für hohe Kampferfahrung, Silberbeutel und Gold-Truhen.",
            descriptionEn = "Best Solo Dungeon in the Black Zones for high combat fame, silver bags, and gold chests."
        ),
        AlbionMonster(
            id = "dungeon_corrupted_slayer",
            nameDe = "Korrumpierte Dungeons (Slayer Tier 1v1)",
            nameEn = "Corrupted Dungeon (Slayer 1v1)",
            type = MonsterType.DUNGEON_BOSS,
            playerCategory = PlayerCategory.SOLO,
            recommendedPlayerCount = "1 Spieler (Solo 1v1)",
            tier = 8,
            regionDe = "Solo Korrumpierte Welten",
            regionEn = "Solo Corrupted Realms",
            zoneLocationDe = "Open World Dämonen-Eingänge (Rot & Schwarz)",
            zoneLocationEn = "Open World Entrances",
            zoneSafety = ZoneSafety.DANGEROUS_RED,
            estimatedProfitSilver = 2_100_000L,
            estimatedSilverPerHour = "~1.500.000 - 3.200.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: Dämonische Infamy-Goldtruhe (~2.2M S.), Teufels-Tresor (~650k S.)",
            fameAmount = 110_000L,
            respawnTimeDe = "Sofortiger Spawn bei neuem Run",
            respawnTimeEn = "Instant on new run",
            difficultyDe = "Solo PvPvE Hardcore (1v1)",
            difficultyEn = "Solo PvPvE Hardcore (1v1)",
            drops = listOf(
                MonsterDropItem("Dämonischer Fangzahn Artefakt", "Demonic Fang Artifact", 18.0, 650_000L, isRare = true),
                MonsterDropItem("Verfluchte Runen & Seelen", "Cursed Runes & Souls", 100.0, 380_000L),
                MonsterDropItem("T8.2 Waffen-Drop", "T8.2 Weapon Drop", 12.0, 850_000L, isRare = true)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_MAIN_CURSEDSTAFF_HELL.png",
            combatTipsDe = "Infamy-Punkte erhöhen Loot-Multiplier! Dämonenfallen nutzen, um Invasoren im PvP zu überraschen.",
            combatTipsEn = "Infamy points boost loot multiplier! Use demonic traps to outplay invaders in PvP.",
            descriptionDe = "Ultra-lukrativer Solo 1v1 Dämonen-Dungeon. Gewinne Kämpfe für epische Goldtruhen und maximale Infamy.",
            descriptionEn = "Ultra-lucrative Solo 1v1 Demonic Dungeon. Win fights for epic gold chests and maximum infamy."
        ),
        AlbionMonster(
            id = "monster_fey_dragon",
            nameDe = "Feen-Drache (Die Nebel / The Mists)",
            nameEn = "Fey Dragon (The Mists)",
            type = MonsterType.MISTS_BOSS,
            playerCategory = PlayerCategory.SOLO,
            recommendedPlayerCount = "1 Spieler (Solo Nebel)",
            tier = 8,
            regionDe = "The Mists (Die Nebel)",
            regionEn = "The Mists",
            zoneLocationDe = "Brecilien Portal / Wisps in Gelben & Schwarzen Zonen",
            zoneLocationEn = "Brecilien Portal / Wisps",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 1_650_000L,
            estimatedSilverPerHour = "~1.200.000 - 2.800.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: Nebel-Feentruhe (~1.4M S.), Brecilien-Ansehen Tresor (~400k S.)",
            fameAmount = 85_000L,
            respawnTimeDe = "Zufällig in Nebel-Camps",
            respawnTimeEn = "Random in Mists Camps",
            difficultyDe = "Solo / Duo PvPvE",
            difficultyEn = "Solo / Duo PvPvE",
            drops = listOf(
                MonsterDropItem("Feen-Drachen Schuppen", "Fey Dragon Scales", 100.0, 520_000L),
                MonsterDropItem("T8 Feen-Artefakt", "T8 Fey Artifact", 25.0, 380_000L, isRare = true),
                MonsterDropItem("Brecilien-Ansehen (+1000)", "Brecilien Standing (+1000)", 100.0, 200_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_ARMOR_LEATHER_FEY.png",
            combatTipsDe = "Feuerkegel-Attacken ausweichen. Vorsicht vor hinterhaltigen Solo-Gankern im Nebel!",
            combatTipsEn = "Dodge fire cone attacks. Watch for sneaky solo gankers in the mist!",
            descriptionDe = "Seltenes Nebel-Fabelwesen. Liefert wertvolles Brecilien-Ansehen und seltene Feen-Drachen Schuppen.",
            descriptionEn = "Rare Mists creature. Delivers valuable Brecilien Standing and rare Fey Dragon scales."
        ),

        // ----------------------------------------------------
        // 2. DUO & SMALL GROUP (2-3 SPIELER)
        // ----------------------------------------------------
        AlbionMonster(
            id = "dungeon_hellgate_2v2",
            nameDe = "2v2 Höllentore (Lethal Hellgate)",
            nameEn = "2v2 Lethal Hellgate",
            type = MonsterType.HELLGATE_BOSS,
            playerCategory = PlayerCategory.DUO,
            recommendedPlayerCount = "2 Spieler (Duo)",
            tier = 8,
            regionDe = "Outlands / Schwarze Zone",
            regionEn = "Outlands / Black Zone",
            zoneLocationDe = "Open World Höllentor-Portale (Rot & Schwarz)",
            zoneLocationEn = "Open World Hellgate Portals",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 2_800_000L,
            estimatedSilverPerHour = "~2.000.000 - 4.500.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: Dämonen-Höllentruhe (~2.8M S.), Dämonischer Beute-Tresor (~900k S.)",
            fameAmount = 140_000L,
            respawnTimeDe = "Sofort nach Portal-Eintritt",
            respawnTimeEn = "Instant on Portal Entry",
            difficultyDe = "Duo PvPvE Hardcore (2v2)",
            difficultyEn = "Duo PvPvE Hardcore (2v2)",
            drops = listOf(
                MonsterDropItem("T8 Dämonen-Herz", "T8 Demon Heart", 100.0, 650_000L),
                MonsterDropItem("Dämonenprinz-Flügel", "Demon Prince Wings Artifact", 15.0, 1_200_000L, isRare = true),
                MonsterDropItem("T8.2 Seelen (10x)", "T8.2 Souls", 80.0, 350_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_CAPEITEM_DEMON.png",
            combatTipsDe = "Synergie aus DPS + Heiler / Support zwingend erforderlich. Kontrolliere das Zentrum!",
            combatTipsEn = "DPS + Healer / Support synergy required. Control the center area!",
            descriptionDe = "Bestes Duo-Gespann PvPvE Event. Besiege gegnerische Duos für legendäre Dämonen-Schatztruhen.",
            descriptionEn = "Best Duo PvPvE Event. Defeat rival duos for legendary demon chests."
        ),
        AlbionMonster(
            id = "dungeon_roads_avalon_duo",
            nameDe = "Straßen von Avalon (Roads Duo Chests)",
            nameEn = "Roads of Avalon Duo Chests",
            type = MonsterType.ROADS_BOSS,
            playerCategory = PlayerCategory.DUO,
            recommendedPlayerCount = "2-3 Spieler (Duo/Small)",
            tier = 7,
            regionDe = "Roads of Avalon (Avalon-Pfade)",
            regionEn = "Roads of Avalon",
            zoneLocationDe = "Avalon-Tunnel & Grüne/Blaue Truhen-Camps",
            zoneLocationEn = "Avalon Tunnels & Chest Camps",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 1_900_000L,
            estimatedSilverPerHour = "~1.400.000 - 3.000.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: Avalonien-Raidtruhe (~1.9M S.), Mystischer Pfad-Tresor (~550k S.)",
            fameAmount = 125_000L,
            respawnTimeDe = "30 - 60 Minuten",
            respawnTimeEn = "30 - 60 Minutes",
            difficultyDe = "Small Group (2-3 Spieler)",
            difficultyEn = "Small Group (2-3 Players)",
            drops = listOf(
                MonsterDropItem("Avalonische Energie (30x)", "Avalonian Energy", 100.0, 240_000L),
                MonsterDropItem("T7 Avalonisches Artefakt", "T7 Avalonian Artifact", 20.0, 480_000L, isRare = true),
                MonsterDropItem("T7.2 Ausrüstung", "T7.2 Equipment Drop", 15.0, 520_000L, isRare = true)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_ARTEFACT_ARMOR_CLOTH_AVALON.png",
            combatTipsDe = "Dungeon-Camps gemeinsam pullen. Achtung vor feindlichen Gankern in den Pfad-Tunneln!",
            combatTipsEn = "Pull dungeon camps together. Watch out for enemy gankers in tunnel corridors!",
            descriptionDe = "Hervorragende Duo- & Small-Group Aktivität in den Pfaden von Avalon mit hoher Beute.",
            descriptionEn = "Excellent Duo & Small Group activity in the Roads of Avalon with high loot."
        ),
        AlbionMonster(
            id = "monster_crystal_spider",
            nameDe = "Kristallspinne (Crystal Spider)",
            nameEn = "Crystal Spider",
            type = MonsterType.ROAMING_BOSS,
            playerCategory = PlayerCategory.DUO,
            recommendedPlayerCount = "2-3 Spieler (Duo/Small)",
            tier = 7,
            regionDe = "Outlands (Schwarze Zone)",
            regionEn = "Outlands (Black Zone)",
            zoneLocationDe = "Zufälliger Spawn in allen T7/T8 Outlands Zonen",
            zoneLocationEn = "Random Spawn across T7/T8 Outlands",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 1_500_000L,
            estimatedSilverPerHour = "~1.500.000 S. pro Kill",
            chestDropSummaryDe = "🎁 Truhen: Kristall-Schatulle (~1.5M S.), Saisonpunkte-Tresor",
            fameAmount = 95_000L,
            respawnTimeDe = "Zufällig (1 - 4 Std)",
            respawnTimeEn = "Random (1 - 4 Hours)",
            difficultyDe = "Small-Scale (2-4 Spieler)",
            difficultyEn = "Small-Scale (2-4 Players)",
            drops = listOf(
                MonsterDropItem("Kristallspinne-Ei", "Crystal Spider Egg", 2.5, 3_500_000L, isRare = true),
                MonsterDropItem("Kristallierte Spinnenseide", "Crystallized Spider Silk", 100.0, 450_000L),
                MonsterDropItem("Gilden-Saisonpunkte (100pt)", "Guild Season Points", 100.0, 500_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_ARTEFACT_OFF_TOWERSHIELD_UNDEAD.png",
            combatTipsDe = "Zieht Spieler mit Spinnenfäden heran. Bei 30% HP versprüht sie Kristall-Gift. Mobil bleiben!",
            combatTipsEn = "Pulls players with webs. At 30% HP sprays crystal poison. Stay mobile!",
            descriptionDe = "Sehr begehrter Wanderboss in den Schwarzen Landen. Verleiht wertvolle Saisonpunkte für Gilden.",
            descriptionEn = "Highly sought-after roaming boss in the Outlands. Grants valuable Guild Season points."
        ),

        // ----------------------------------------------------
        // 3. GRUPPE (4-10 SPIELER)
        // ----------------------------------------------------
        AlbionMonster(
            id = "dungeon_group_static_t8",
            nameDe = "T8 Statische Gruppen-Dungeons (Static Dungeon)",
            nameEn = "T8 Static Group Dungeon",
            type = MonsterType.STATIC_BOSS,
            playerCategory = PlayerCategory.GROUP,
            recommendedPlayerCount = "5-10 Spieler (Gruppe)",
            tier = 8,
            regionDe = "Outlands Statische Dungeons",
            regionEn = "Outlands Static Dungeons",
            zoneLocationDe = "T8 Schwarze Zone Statische Dungeons (z.B. Lymhurst/Martlock Outlands)",
            zoneLocationEn = "T8 Black Zone Static Dungeons",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 4_500_000L,
            estimatedSilverPerHour = "~3.000.000 - 6.500.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: T8.3 Legendäre Gruppen-Goldtruhe (~3.8M S.), Violette Schatulle (~1.2M S.)",
            fameAmount = 350_000L,
            respawnTimeDe = "45 Minuten",
            respawnTimeEn = "45 Minutes",
            difficultyDe = "Gruppe Hardcore (5-10 Spieler)",
            difficultyEn = "Group Hardcore (5-10 Players)",
            drops = listOf(
                MonsterDropItem("T8.3 Legendäre Truhe", "T8.3 Legendary Chest", 100.0, 3_800_000L),
                MonsterDropItem("T8 Foliant des Wissens (5x)", "T8 Tome of Insight", 100.0, 450_000L),
                MonsterDropItem("Avalonische Splitter", "Avalonian Shards", 60.0, 300_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_CHEST_LEGENDARY.png",
            combatTipsDe = "Enorme Mob-Dichte vor dem Boss. Tank und Holy Healer absolut Pflicht!",
            combatTipsEn = "Massive mob density before boss. Tank and Holy Healer absolute must!",
            descriptionDe = "Der beste Fame-Farm Ort im Spiel mit gigantischen Truppen-Truhen am Ende.",
            descriptionEn = "The best fame farming spot in the game with massive group chests at the end."
        ),
        AlbionMonster(
            id = "dungeon_hce_lvl18",
            nameDe = "Hardcore Expedition Level 18 (HCE)",
            nameEn = "Hardcore Expedition Level 18 (HCE)",
            type = MonsterType.DUNGEON_BOSS,
            playerCategory = PlayerCategory.GROUP,
            recommendedPlayerCount = "5 Spieler (HCE Meta)",
            tier = 8,
            regionDe = "Königliche Hauptstädte (Expedition)",
            regionEn = "Royal Cities Expedition Portal",
            zoneLocationDe = "Hauptstadt Expedition-Portal (Sichere Zone)",
            zoneLocationEn = "Royal City Expedition Portal",
            zoneSafety = ZoneSafety.SAFE_BLUE,
            estimatedProfitSilver = 3_800_000L,
            estimatedSilverPerHour = "~2.500.000 - 5.000.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: HCE Stufe-18 Gold-Tresor (~3.5M S.), Luxusgut-Schatulle (~1.0M S.)",
            fameAmount = 280_000L,
            respawnTimeDe = "Sofort per Karte startbar",
            respawnTimeEn = "Instant via Map",
            difficultyDe = "Sichere 5er Meta-Expedition",
            difficultyEn = "Safe 5-man Meta Expedition",
            drops = listOf(
                MonsterDropItem("T8.3 HCE Beutebeutel", "T8.3 HCE Loot Pouch", 100.0, 2_200_000L),
                MonsterDropItem("HCE Luxusgüter-Drop", "HCE Luxury Goods Drop", 100.0, 1_100_000L),
                MonsterDropItem("T8 Meister-Runen (25x)", "T8 Master Runes", 100.0, 500_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_MAP_EXPEDITION_HCE.png",
            combatTipsDe = "Erfordert 1900+ Item Power und exakte CC-Rotationen. Null Risiko, da in der Stadt!",
            combatTipsEn = "Requires 1900+ Item Power and precise CC rotations. Zero risk in city!",
            descriptionDe = "Sicherste High-End Aktivität im Spiel ohne Full-Loot Risiko. Bietet enormes Silber und Ruhm.",
            descriptionEn = "Safest High-End activity in the game with zero full-loot risk. Offers massive silver and fame."
        ),
        AlbionMonster(
            id = "monster_demon_prince",
            nameDe = "Dämonenprinz von Morgana (10v10 Hellgates)",
            nameEn = "Morgana Demon Prince (10v10 Hellgates)",
            type = MonsterType.HELLGATE_BOSS,
            playerCategory = PlayerCategory.GROUP,
            recommendedPlayerCount = "10 Spieler Meta-Gruppe",
            tier = 8,
            regionDe = "Outlands / Schwarze Zone",
            regionEn = "Outlands / Black Zone",
            zoneLocationDe = "T8 10v10 Höllentore & Morgana Schlösser",
            zoneLocationEn = "T8 10v10 Hellgates & Morgana Castles",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 3_200_000L,
            estimatedSilverPerHour = "~3.200.000 S. pro Run",
            chestDropSummaryDe = "🎁 Truhen: Legendäre Dämonen-Goldtruhe (~3.5M S.), Morgana-Gilden-Tresor (~1.2M S.)",
            fameAmount = 210_000L,
            respawnTimeDe = "Kontinuierlich in 10v10 Hellgates",
            respawnTimeEn = "Continuous in 10v10 Hellgates",
            difficultyDe = "10v10 PvPvE Meta-Gruppe",
            difficultyEn = "10v10 PvPvE Meta Group",
            drops = listOf(
                MonsterDropItem("Dämonenprinz-Flügel Artefakt", "Demon Prince Wings Artifact", 20.0, 1_400_000L, isRare = true),
                MonsterDropItem("T8 Dämonen-Herz", "T8 Demon Heart", 100.0, 650_000L),
                MonsterDropItem("Morgana-Umhang Stoff", "Morgana Cape Cloth", 100.0, 380_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_CAPEITEM_DEMON.png",
            combatTipsDe = "Verwandelt sich bei 50% HP. Schleudert Feuerstürme auf das ganze Schlachtfeld – Heiler müssen defensive Cooldowns zünden!",
            combatTipsEn = "Transforms at 50% HP. Casts firestorms over the battlefield - healers must cycle defensive cooldowns!",
            descriptionDe = "Der ultimative Endboss in den 10v10 Höllentoren. Verlangt perfekte Gruppen-Koordination unter PvP-Druck.",
            descriptionEn = "The ultimate end boss in 10v10 Hellgates. Requires perfect group coordination under PvP pressure."
        ),

        // ----------------------------------------------------
        // 4. RAID & GILDE (10-20+ SPIELER)
        // ----------------------------------------------------
        AlbionMonster(
            id = "dungeon_avalonian_raid_20",
            nameDe = "T8.4 Avalonien Gold-Raid (20-Spieler Raid)",
            nameEn = "T8.4 Avalonian Gold Raid (20-Player Raid)",
            type = MonsterType.DUNGEON_BOSS,
            playerCategory = PlayerCategory.RAID,
            recommendedPlayerCount = "15-20 Spieler (Gold-Raid)",
            tier = 8,
            regionDe = "Outlands Avalonien-Tore",
            regionEn = "Outlands Avalonian Portals",
            zoneLocationDe = "T8 Schwarze Zone Avalonien-Raid Dungeons",
            zoneLocationEn = "T8 Black Zone Avalonian Dungeons",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 6_500_000L,
            estimatedSilverPerHour = "~4.000.000 - 8.500.000 S. / Std.",
            chestDropSummaryDe = "🎁 Truhen: T8.4 Goldene Avalon-Kaiser-Truhe (~6.5M S.), Platin-Tresor (~2.8M S.)",
            fameAmount = 650_000L,
            respawnTimeDe = "24 Stunden",
            respawnTimeEn = "24 Hours",
            difficultyDe = "Gilden-Raid Hardcore (15-20 Spieler)",
            difficultyEn = "Guild Raid Hardcore (15-20 Players)",
            drops = listOf(
                MonsterDropItem("T8.4 Avalonien Artefakt", "T8.4 Avalonian Artifact", 25.0, 4_500_000L, isRare = true),
                MonsterDropItem("Avalonien-Segen (+10% Fame Buff)", "Avalonian Fame Buff", 100.0, 1_500_000L),
                MonsterDropItem("Avalonische Energie (150x)", "Avalonian Energy", 100.0, 1_200_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_ARMOR_CLOTH_AVALON.png",
            combatTipsDe = "Benötigt 2 Off-Tanks, 4 Raid-Heiler & Lymhurst-Umhänge. Gibt den berühmten 1-wöchigen 10% Fame-Buff!",
            combatTipsEn = "Requires 2 Off-Tanks, 4 Raid Healers & Lymhurst Capes. Grants the famous 1-week 10% Fame Buff!",
            descriptionDe = "Der wertvollste PvE-Raid in Albion Online. Gewährt allen 20 Raid-Teilnehmern den weltberühmten Avalonien-Buff.",
            descriptionEn = "The most valuable PvE Raid in Albion Online. Grants all 20 raid members the world-famous Avalonian Buff."
        ),
        AlbionMonster(
            id = "monster_old_white",
            nameDe = "Uraltes Weißes Mammut (Old White)",
            nameEn = "Old White Mammoth",
            type = MonsterType.WORLD_BOSS,
            playerCategory = PlayerCategory.RAID,
            recommendedPlayerCount = "10-20 Spieler Raid",
            tier = 8,
            regionDe = "Steppe (Bridgewatch Outlands)",
            regionEn = "Steppe (Bridgewatch Outlands)",
            zoneLocationDe = "T8 Schwarze Zone Steppe (Bridgewatch Outlands)",
            zoneLocationEn = "T8 Black Zone Steppe (Bridgewatch Outlands)",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 15_000_000L,
            estimatedSilverPerHour = "~15.000.000 S. bei Kalb-Drop",
            chestDropSummaryDe = "🎁 Truhen: Uraltes Mammut-Kadaver (~15M - 150M S.)",
            fameAmount = 250_000L,
            respawnTimeDe = "24 - 48 Stunden",
            respawnTimeEn = "24 - 48 Hours",
            difficultyDe = "Großwild-Raid (10-20 Spieler / Kürschner)",
            difficultyEn = "Raid (10-20 Hide Gatherers)",
            drops = listOf(
                MonsterDropItem("Transportmammut-Kalb", "Grandmaster's Transport Mammoth Calf", 1.2, 150_000_000L, isRare = true),
                MonsterDropItem("220x T8 Uralte Mammuthaut", "220x T8 Elder's Hide", 100.0, 1_320_000L),
                MonsterDropItem("T8 Robustes Mammutfleisch", "T8 Elder's Raw Pork/Mammoth Meat", 100.0, 180_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_MOUNT_MAMMOTH_TRANSPORT.png",
            combatTipsDe = "Flächendeckender Ansturm & Trampelschaden. Tank muss Aggro halten, Kürschner brauchen T8 Häutungsmesser!",
            combatTipsEn = "Massive AoE charge & trample. Tank must hold aggro, gatherers require T8 Skinning Knives!",
            descriptionDe = "Das wertvollste Großwild in Albion Online. Bei einem Kalb-Drop winken über 150.000.000 Silber Gewinn!",
            descriptionEn = "The most valuable big game creature in Albion Online. A calf drop yields over 150,000,000 Silver!"
        ),
        AlbionMonster(
            id = "monster_earthmother",
            nameDe = "Hüter-Erdmutter (Earthmother World Boss)",
            nameEn = "Keeper Earthmother",
            type = MonsterType.WORLD_BOSS,
            playerCategory = PlayerCategory.RAID,
            recommendedPlayerCount = "10-20 Spieler Raid",
            tier = 8,
            regionDe = "Wald (Lymhurst Outlands)",
            regionEn = "Forest (Lymhurst Outlands)",
            zoneLocationDe = "T8 Schwarze Zone Wälder & Statische Dungeons (Lymhurst)",
            zoneLocationEn = "T8 Black Zone Forest & Static Dungeons (Lymhurst)",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 3_500_000L,
            estimatedSilverPerHour = "~3.500.000 S. pro Kill",
            chestDropSummaryDe = "🎁 Truhen: Legendäre Hüter-Goldtruhe (~2.5M S.), Violette Hüter-Schatulle (~850k S.)",
            fameAmount = 180_000L,
            respawnTimeDe = "24 Stunden",
            respawnTimeEn = "24 Hours",
            difficultyDe = "Raid-Gilde (10-20 Spieler)",
            difficultyEn = "Guild Raid (10-20 Players)",
            drops = listOf(
                MonsterDropItem("T8.3 Reliktschatulle", "T8.3 Relic Reliquary", 8.0, 2_200_000L, isRare = true),
                MonsterDropItem("Hüter-Wurzeltrieb Artefakt", "Keeper Rootfury Artifact", 15.0, 850_000L, isRare = true),
                MonsterDropItem("T8 Gefallenes Holz (20-40x)", "T8 Fallen Wood Logs", 100.0, 450_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_MAIN_HOLYSTAFF_KEEPER.png",
            combatTipsDe = "Beschwört Erdbeben-Flächenschaden & Baumstamm-Wellen. Boss immer von der Gruppe weg drehen!",
            combatTipsEn = "Summons earthquake AoE & log waves. Always face the boss away from the group!",
            descriptionDe = "Mächtigster Hüter-Weltboss. Liefert hochkarätige T8.3 Relikte, Holzfäller-Artefakte und Runen.",
            descriptionEn = "Most powerful Keeper World Boss. Yields high-tier T8.3 relics, woodcutting artifacts, and runes."
        )
    )

    fun getFilteredAndSorted(
        query: String = "",
        regionFilter: String = "ALLE",
        playerCategoryFilter: PlayerCategory = PlayerCategory.ALL,
        safetyFilter: ZoneSafety? = null,
        sortMode: MonsterSortMode = MonsterSortMode.MOST_LUCRATIVE
    ): List<AlbionMonster> {
        var list = monsters

        if (playerCategoryFilter != PlayerCategory.ALL) {
            list = list.filter { it.playerCategory == playerCategoryFilter }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.nameDe.lowercase().contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.regionDe.lowercase().contains(q) ||
                it.zoneLocationDe.lowercase().contains(q) ||
                it.chestDropSummaryDe.lowercase().contains(q) ||
                it.drops.any { d -> d.itemNameDe.lowercase().contains(q) || d.itemNameEn.lowercase().contains(q) }
            }
        }

        if (regionFilter != "ALLE" && regionFilter.isNotBlank()) {
            list = list.filter { it.regionDe.contains(regionFilter, ignoreCase = true) || it.regionEn.contains(regionFilter, ignoreCase = true) }
        }

        if (safetyFilter != null) {
            list = list.filter { it.zoneSafety == safetyFilter }
        }

        return when (sortMode) {
            MonsterSortMode.MOST_LUCRATIVE -> list.sortedByDescending { it.estimatedProfitSilver }
            MonsterSortMode.BY_REGION -> list.sortedBy { it.regionDe }
            MonsterSortMode.BY_SAFETY -> list.sortedByDescending { it.zoneSafety.ordinal }
            MonsterSortMode.BY_TIER -> list.sortedByDescending { it.tier }
        }
    }
}

enum class MonsterSortMode(val labelDe: String, val labelEn: String) {
    MOST_LUCRATIVE("💰 Höchster Gewinn (Silber)", "💰 Most Lucrative (Silver)"),
    BY_REGION("📍 Nach Region", "📍 By Region"),
    BY_SAFETY("🛡️ Nach Sicherheitszone", "🛡️ By Safety Zone"),
    BY_TIER("⭐ Nach Stufe (Tier T1 - T8)", "⭐ By Tier (T1 - T8)")
}
