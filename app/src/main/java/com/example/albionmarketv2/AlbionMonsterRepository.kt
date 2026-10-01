package com.example.albionmarketv2

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
    val tier: Int,
    val regionDe: String,
    val regionEn: String,
    val zoneLocationDe: String,
    val zoneLocationEn: String,
    val zoneSafety: ZoneSafety,
    val estimatedProfitSilver: Long,
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
        AlbionMonster(
            id = "monster_old_white",
            nameDe = "Uraltes Weißes Mammut (Old White)",
            nameEn = "Old White Mammoth",
            type = MonsterType.WORLD_BOSS,
            tier = 8,
            regionDe = "Steppe (Bridgewatch)",
            regionEn = "Steppe (Bridgewatch)",
            zoneLocationDe = "T8 Schwarze Zone Steppe (Bridgewatch Outlands)",
            zoneLocationEn = "T8 Black Zone Steppe (Bridgewatch Outlands)",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 15_000_000L,
            fameAmount = 250_000L,
            respawnTimeDe = "24 - 48 Stunden",
            respawnTimeEn = "24 - 48 Hours",
            difficultyDe = "Großwild-Raid (5-10 Kürschner)",
            difficultyEn = "Raid (5-10 Hide Gatherers)",
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
            nameDe = "Hüter-Erdmutter (Earthmother)",
            nameEn = "Keeper Earthmother",
            type = MonsterType.WORLD_BOSS,
            tier = 8,
            regionDe = "Wald (Lymhurst)",
            regionEn = "Forest (Lymhurst)",
            zoneLocationDe = "T8 Schwarze Zone Wälder & Statische Dungeons (Lymhurst)",
            zoneLocationEn = "T8 Black Zone Forest & Static Dungeons (Lymhurst)",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 3_500_000L,
            fameAmount = 180_000L,
            respawnTimeDe = "24 Stunden",
            respawnTimeEn = "24 Hours",
            difficultyDe = "Raid-Gilde (10-20 Spieler)",
            difficultyEn = "Guild Raid (10-20 Players)",
            drops = listOf(
                MonsterDropItem("T8.3 Reliktschatulle", "T8.3 Relic Reliquary", 8.0, 2_200_000L, isRare = true),
                MonsterDropItem("Hüter-Wurzeltrieb Artefakt", "Keeper Rootfury Artifact", 15.0, 850_000L, isRare = true),
                MonsterDropItem("T8 Gefallenes Holz (20-40x)", "T8 Fallen Wood Logs", 100.0, 450_000L),
                MonsterDropItem("T8 Meister-Runen (15x)", "T8 Master's Runes", 100.0, 300_000L),
                MonsterDropItem("T8 Buch der Einsicht", "T8 Tome of Insight", 45.0, 180_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_MAIN_HOLYSTAFF_KEEPER.png",
            combatTipsDe = "Beschwört Erdbeben-Flächenschaden & Baumstamm-Wellen. Boss immer von der Gruppe weg drehen!",
            combatTipsEn = "Summons earthquake AoE & log waves. Always face the boss away from the group!",
            descriptionDe = "Mächtigster Hüter-Weltboss. Liefert hochkarätige T8.3 Relikte, Holzfäller-Artefakte und Runen.",
            descriptionEn = "Most powerful Keeper World Boss. Yields high-tier T8.3 relics, woodcutting artifacts, and runes."
        ),
        AlbionMonster(
            id = "monster_demon_prince",
            nameDe = "Dämonenprinz von Morgana",
            nameEn = "Morgana Demon Prince",
            type = MonsterType.HELLGATE_BOSS,
            tier = 8,
            regionDe = "Outlands / Schwarze Zone",
            regionEn = "Outlands / Black Zone",
            zoneLocationDe = "T8 10v10 Höllentore (Hellgates) & Morgana Schlösser",
            zoneLocationEn = "T8 10v10 Hellgates & Morgana Castles",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 3_200_000L,
            fameAmount = 210_000L,
            respawnTimeDe = "Kontinuierlich in 10v10 Hellgates",
            respawnTimeEn = "Continuous in 10v10 Hellgates",
            difficultyDe = "10v10 PvPvE Meta-Gruppe",
            difficultyEn = "10v10 PvPvE Meta Group",
            drops = listOf(
                MonsterDropItem("Dämonenprinz-Flügel Artefakt", "Demon Prince Wings Artifact", 20.0, 1_400_000L, isRare = true),
                MonsterDropItem("T8 Dämonen-Herz", "T8 Demon Heart", 100.0, 650_000L),
                MonsterDropItem("Morgana-Umhang Stoff", "Morgana Cape Cloth", 100.0, 380_000L),
                MonsterDropItem("T8.2 Dämonenseele (15x)", "T8.2 Demon Souls", 60.0, 420_000L),
                MonsterDropItem("Legendäre Hellgate-Schatztruhe", "Legendary Hellgate Chest", 100.0, 950_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_CAPEITEM_DEMON.png",
            combatTipsDe = "Verwandelt sich bei 50% HP. Schleudert Feuerstürme auf das ganze Schlachtfeld – Heiler müssen defensive Cooldowns zünden!",
            combatTipsEn = "Transforms at 50% HP. Casts firestorms over the battlefield - healers must cycle defensive cooldowns!",
            descriptionDe = "Der ultimative Endboss in den 10v10 Höllentoren. Verlangt perfekte Gruppen-Koordination unter PvP-Druck.",
            descriptionEn = "The ultimate end boss in 10v10 Hellgates. Requires perfect group coordination under PvP pressure."
        ),
        AlbionMonster(
            id = "monster_avalonian_archon",
            nameDe = "Avalonischer Hohepriester (Archon)",
            nameEn = "Avalonian Archon",
            type = MonsterType.ROADS_BOSS,
            tier = 8,
            regionDe = "Roads of Avalon (Avalon-Pfade)",
            regionEn = "Roads of Avalon",
            zoneLocationDe = "T8 Avalon-Pfade Statische Gold-Dungeons",
            zoneLocationEn = "T8 Roads of Avalon Static Gold Dungeons",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 2_800_000L,
            fameAmount = 320_000L,
            respawnTimeDe = "12 - 24 Stunden",
            respawnTimeEn = "12 - 24 Hours",
            difficultyDe = "Avalonian Raid (10-20 Spieler)",
            difficultyEn = "Avalonian Raid (10-20 Players)",
            drops = listOf(
                MonsterDropItem("T8.3 Avalonische Ausrüstung", "T8.3 Avalonian Gear Drop", 12.0, 1_800_000L, isRare = true),
                MonsterDropItem("T8 Avalonisches Artefakt", "T8 Avalonian Artifact", 30.0, 650_000L, isRare = true),
                MonsterDropItem("Avalonische Energie (50-100x)", "Avalonian Energy", 100.0, 400_000L),
                MonsterDropItem("T8 Buch des Wissens", "T8 Avalonian Tome of Insight", 75.0, 250_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_ARMOR_CLOTH_AVALON.png",
            combatTipsDe = "Lichtstrahl-Attacken ausweichen! Tank benötigt 2.500+ IP, um die Strahlungsstöße zu überleben.",
            combatTipsEn = "Dodge beam attacks! Tank needs 2,500+ IP to withstand the radiation bursts.",
            descriptionDe = "Ehrfurchtgebietender Herrscher der Avalonischen Gewölbe. Liefert gigantische Ruhm-Punkte und Avalonische Energie.",
            descriptionEn = "Awe-inspiring ruler of Avalonian vaults. Delivers massive Fame and Avalonian Energy."
        ),
        AlbionMonster(
            id = "monster_harvester",
            nameDe = "Erntebringer der Seelen",
            nameEn = "Harvester of Souls",
            type = MonsterType.DUNGEON_BOSS,
            tier = 8,
            regionDe = "Rot-Zone (Caerleon)",
            regionEn = "Red Zone (Caerleon)",
            zoneLocationDe = "Caerleon Rot-Zone & Schwarze Zone Katakomben",
            zoneLocationEn = "Caerleon Red Zone & Black Zone Catacombs",
            zoneSafety = ZoneSafety.DANGEROUS_RED,
            estimatedProfitSilver = 1_800_000L,
            fameAmount = 140_000L,
            respawnTimeDe = "12 Stunden",
            respawnTimeEn = "12 Hours",
            difficultyDe = "Group Dungeon (5-10 Spieler)",
            difficultyEn = "Group Dungeon (5-10 Players)",
            drops = listOf(
                MonsterDropItem("Skelett-Kampfross Panzerung", "Skeletal Couser Armor", 5.0, 1_200_000L, isRare = true),
                MonsterDropItem("Seelenernter-Artefakt", "Soul Harvester Artifact", 18.0, 480_000L, isRare = true),
                MonsterDropItem("T8 Untote Seelen (20x)", "T8 Undead Souls", 100.0, 320_000L),
                MonsterDropItem("Dunkler Robenstoff", "Dark Robe Cloth", 100.0, 180_000L),
                MonsterDropItem("T8 Phiole des Todes", "T8 Phial of Death", 35.0, 140_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_MOUNT_ARMORED_HORSE_UNDEAD.png",
            combatTipsDe = "Macht Seelenentzug & Giftwolken. Heiler müssen sofort Debuffs reinigen!",
            combatTipsEn = "Uses soul drain & poison clouds. Healers must cleanse debuffs immediately!",
            descriptionDe = "Herrscher der Untoten-Katakomben. Bekannt für extrem hohe Silber-Beutel und seltene Skelett-Mount Rüstungen.",
            descriptionEn = "Ruler of undead catacombs. Known for massive silver bags and rare skeletal mount armor."
        ),
        AlbionMonster(
            id = "monster_crystal_spider",
            nameDe = "Kristallspinne (Crystal Spider)",
            nameEn = "Crystal Spider",
            type = MonsterType.ROAMING_BOSS,
            tier = 7,
            regionDe = "Outlands (Schwarze Zone)",
            regionEn = "Outlands (Black Zone)",
            zoneLocationDe = "Zufälliger Spawn in allen T7/T8 Outlands Zonen",
            zoneLocationEn = "Random Spawn across T7/T8 Outlands Zones",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 1_500_000L,
            fameAmount = 95_000L,
            respawnTimeDe = "Zufällig (1 - 4 Std)",
            respawnTimeEn = "Random (1 - 4 Hours)",
            difficultyDe = "Small-Scale (3-5 Spieler)",
            difficultyEn = "Small-Scale (3-5 Players)",
            drops = listOf(
                MonsterDropItem("Kristallspinne-Ei", "Crystal Spider Egg", 2.5, 3_500_000L, isRare = true),
                MonsterDropItem("Kristallierte Spinnenseide", "Crystallized Spider Silk", 100.0, 450_000L),
                MonsterDropItem("Gilden-Saisonpunkte (100pt)", "Guild Season Points", 100.0, 500_000L),
                MonsterDropItem("T7 Artefakte", "T7 Artifact Drop", 40.0, 380_000L),
                MonsterDropItem("T8 Nebelhaut", "T8 Mist Hide", 15.0, 220_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_ARTEFACT_OFF_TOWERSHIELD_UNDEAD.png",
            combatTipsDe = "Zieht Spieler mit Spinnenfäden heran. Bei 30% HP versprüht sie Kristall-Gift. Mobil bleiben!",
            combatTipsEn = "Pulls players with webs. At 30% HP sprays crystal poison. Stay mobile!",
            descriptionDe = "Sehr begehrter Wanderboss in den Schwarzen Landen. Verleiht wertvolle Saisonpunkte für Gilden.",
            descriptionEn = "Highly sought-after roaming boss in the Outlands. Grants valuable Guild Season points."
        ),
        AlbionMonster(
            id = "monster_fey_dragon",
            nameDe = "Feen-Drache / Schleierdrache",
            nameEn = "Fey Dragon",
            type = MonsterType.MISTS_BOSS,
            tier = 8,
            regionDe = "The Mists (Die Nebel)",
            regionEn = "The Mists",
            zoneLocationDe = "Legendäre & Seltene Nebel (Brecilien / Safe & Unsafe Mists)",
            zoneLocationEn = "Legendary & Rare Mists (Brecilien)",
            zoneSafety = ZoneSafety.SAFE_YELLOW,
            estimatedProfitSilver = 1_100_000L,
            fameAmount = 85_000L,
            respawnTimeDe = "Zufällig in den Nebeln",
            respawnTimeEn = "Random in Mists",
            difficultyDe = "Solo / Duo PvPvE",
            difficultyEn = "Solo / Duo PvPvE",
            drops = listOf(
                MonsterDropItem("Feen-Drachen Schuppen", "Fey Dragon Scales", 100.0, 520_000L),
                MonsterDropItem("T8 Feen-Artefakt", "T8 Fey Artifact", 25.0, 380_000L, isRare = true),
                MonsterDropItem("Brecilien-Ansehen (+500)", "Brecilien Standing (+500)", 100.0, 150_000L),
                MonsterDropItem("Feenblüte (20x)", "Fey Blossom", 60.0, 120_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T8_ARTEFACT_ARMOR_LEATHER_FEY.png",
            combatTipsDe = "Speit Nebelfeuer in Kegelform. Achte auf hinterhaltige Solo-Ganker während des Kampfes!",
            combatTipsEn = "Breathes mist fire in a cone. Watch out for sneaky solo gankers during combat!",
            descriptionDe = "Seltenes Nebel-Fabelwesen. Liefert wertvolles Brecilien-Ansehen und seltene Feen-Drachen Schuppen.",
            descriptionEn = "Rare Mists mythical creature. Delivers valuable Brecilien Standing and rare Fey Dragon scales."
        ),
        AlbionMonster(
            id = "monster_swamp_aspect",
            nameDe = "Faser-Riese (Sumpf-Aspekt)",
            nameEn = "Swamp Fiber Aspect",
            type = MonsterType.ASPECT,
            tier = 7,
            regionDe = "Sumpf (Thetford)",
            regionEn = "Swamp (Thetford)",
            zoneLocationDe = "T7/T8 Rot- & Schwarze Zone Sumpf-Biome (Thetford)",
            zoneLocationEn = "T7/T8 Red & Black Zone Swamp Biomes (Thetford)",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 950_000L,
            fameAmount = 65_000L,
            respawnTimeDe = "6 - 12 Stunden",
            respawnTimeEn = "6 - 12 Hours",
            difficultyDe = "Gruppe (3-5 Sammler mit T7 Sichel)",
            difficultyEn = "Group (3-5 Gatherers with T7 Sickle)",
            drops = listOf(
                MonsterDropItem("500x T7 Drachenfaser", "500x T7 Dragon Fiber", 100.0, 750_000L),
                MonsterDropItem("Sumpf-Faser Essenz", "Swamp Fiber Essence", 100.0, 120_000L),
                MonsterDropItem("Seltenes Sumpf-Elixier", "Rare Swamp Elixir", 8.0, 80_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_FIBER.png",
            combatTipsDe = "Umschlingt Spieler mit Ranken. Sammler benötigen T7/T8 Ernte-Sicheln für maximalen Ertrag!",
            combatTipsEn = "Entangles players with vines. Gatherers require T7/T8 Sickles for maximum yield!",
            descriptionDe = "Gigantischer Ressourcen-Titan im Sumpf. Kann von mehreren Sammlern für hunderte Fasern abgebaut werden.",
            descriptionEn = "Gigantic swamp resource titan. Can be harvested by multiple gatherers for hundreds of fibers."
        ),
        AlbionMonster(
            id = "monster_ore_titan",
            nameDe = "Erz-Titan (Metall-Aspekt)",
            nameEn = "Ore Titan Aspect",
            type = MonsterType.ASPECT,
            tier = 7,
            regionDe = "Steppe (Bridgewatch)",
            regionEn = "Steppe (Bridgewatch)",
            zoneLocationDe = "T7/T8 Rot- & Schwarze Zone Steppen-Biome",
            zoneLocationEn = "T7/T8 Red & Black Zone Steppe Biomes",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 900_000L,
            fameAmount = 62_000L,
            respawnTimeDe = "6 - 12 Stunden",
            respawnTimeEn = "6 - 12 Hours",
            difficultyDe = "Gruppe (3-5 Bergmänner mit T7 Spitzhacke)",
            difficultyEn = "Group (3-5 Miners with T7 Pickaxe)",
            drops = listOf(
                MonsterDropItem("500x T7 Titan-Erz", "500x T7 Titanium Ore", 100.0, 700_000L),
                MonsterDropItem("Titan-Erz Essenz", "Titan Ore Essence", 100.0, 110_000L),
                MonsterDropItem("Titanen-Kristall", "Titan Crystal", 10.0, 90_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_ORE.png",
            combatTipsDe = "Verursacht Erdbeben-Stöße. Tank muss Aggro halten, Bergmänner bauen nach dem Kill ab.",
            combatTipsEn = "Causes earthquake impacts. Tank holds aggro, miners harvest after the kill.",
            descriptionDe = "Mächtiger Erz-Gigant in den Steppen. Ein geschlagener Titan liefert über 500 Einheiten Titan-Erz.",
            descriptionEn = "Mighty ore giant in the steppes. A defeated titan yields over 500 units of Titanium Ore."
        ),
        AlbionMonster(
            id = "monster_rock_giant",
            nameDe = "Stein-Koloss (Stein-Aspekt)",
            nameEn = "Rock Giant Aspect",
            type = MonsterType.ASPECT,
            tier = 7,
            regionDe = "Gebirge (Fort Sterling)",
            regionEn = "Mountain (Fort Sterling)",
            zoneLocationDe = "T7/T8 Rot- & Schwarze Zone Gebirgs-Biome",
            zoneLocationEn = "T7/T8 Red & Black Zone Mountain Biomes",
            zoneSafety = ZoneSafety.DANGEROUS_BLACK,
            estimatedProfitSilver = 850_000L,
            fameAmount = 60_000L,
            respawnTimeDe = "6 - 12 Stunden",
            respawnTimeEn = "6 - 12 Hours",
            difficultyDe = "Gruppe (3-5 Steinmetze mit T7 Hammer)",
            difficultyEn = "Group (3-5 Quarrymen with T7 Hammer)",
            drops = listOf(
                MonsterDropItem("500x T7 Granitstein", "500x T7 Granite Stone", 100.0, 680_000L),
                MonsterDropItem("Stein-Koloss Essenz", "Rock Giant Essence", 100.0, 100_000L),
                MonsterDropItem("Koloss-Splitter", "Colossus Shard", 12.0, 70_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T7_STONE.png",
            combatTipsDe = "Schleudert Felsbrocken. Steinmetze müssen T7 Hämmer ausgerüstet haben.",
            combatTipsEn = "Hurls boulders. Quarrymen must have T7 Stone Hammers equipped.",
            descriptionDe = "Uralter Gebirgs-Koloss. Bietet massive Stein-Vorkommen für den Gilden- & Inselausbau.",
            descriptionEn = "Ancient mountain colossus. Offers massive stone deposits for guild & island upgrades."
        ),
        AlbionMonster(
            id = "monster_governor",
            nameDe = "Verdammter Gouverneur",
            nameEn = "Condemned Governor",
            type = MonsterType.STATIC_BOSS,
            tier = 6,
            regionDe = "Hochland (Martlock)",
            regionEn = "Highland (Martlock)",
            zoneLocationDe = "Martlock Gelb- & Rot-Zone Statische Dungeons",
            zoneLocationEn = "Martlock Yellow & Red Zone Static Dungeons",
            zoneSafety = ZoneSafety.SAFE_YELLOW,
            estimatedProfitSilver = 650_000L,
            fameAmount = 48_000L,
            respawnTimeDe = "2 - 4 Stunden",
            respawnTimeEn = "2 - 4 Hours",
            difficultyDe = "Gruppe (3-5 Spieler)",
            difficultyEn = "Group (3-5 Players)",
            drops = listOf(
                MonsterDropItem("T6 Kettenrüstungs-Artefakt", "T6 Chainmail Artifact", 25.0, 280_000L),
                MonsterDropItem("T6 Meister-Runen (10x)", "T6 Master's Runes", 100.0, 180_000L),
                MonsterDropItem("T6 Silberbeutel (Groß)", "T6 Large Silver Bag", 100.0, 120_000L),
                MonsterDropItem("T6 Buch der Einsicht", "T6 Tome of Insight", 80.0, 70_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T6_ARTEFACT_ARMOR_PLATE_KEEPER.png",
            combatTipsDe = "Ruft Wachen herbei. Flächenschaden (AoE) nutzen, um die Wachen schnell zu besiegen!",
            combatTipsEn = "Summons adds. Use AoE damage to clear the guards quickly!",
            descriptionDe = "Endboss in statischen Hochland-Dungeons. Sehr gutes Silber-/Aufwand-Verhältnis in Gelben Zonen.",
            descriptionEn = "End boss in static highland dungeons. Excellent silver-to-effort ratio in Yellow Zones."
        ),
        AlbionMonster(
            id = "monster_forest_bear",
            nameDe = "Veteran Wald-Bär",
            nameEn = "Veteran Forest Bear",
            type = MonsterType.ELITE_MOB,
            tier = 6,
            regionDe = "Wald (Lymhurst)",
            regionEn = "Forest (Lymhurst)",
            zoneLocationDe = "Gelb- & Rot-Zone Wälder (Lymhurst)",
            zoneLocationEn = "Yellow & Red Zone Forests (Lymhurst)",
            zoneSafety = ZoneSafety.SAFE_YELLOW,
            estimatedProfitSilver = 450_000L,
            fameAmount = 28_000L,
            respawnTimeDe = "30 - 60 Minuten",
            respawnTimeEn = "30 - 60 Minutes",
            difficultyDe = "Solo / Duo Kürschner",
            difficultyEn = "Solo / Duo Hide Gatherer",
            drops = listOf(
                MonsterDropItem("Bären-Junges Reittier", "Direbear Cub Mount", 3.5, 3_200_000L, isRare = true),
                MonsterDropItem("80x T6 Bärenhaut", "80x T6 Heavy Hide", 100.0, 240_000L),
                MonsterDropItem("T6 Bärenfleisch", "T6 Raw Bear Meat", 100.0, 40_000L)
            ),
            imageUrl = "https://render.albiononline.com/v1/item/T6_MOUNT_DIREBEAR.png",
            combatTipsDe = "Betäubt Ziel mit Prankenhieb. Kürschner-Set für Bonus-Ertrag tragen!",
            combatTipsEn = "Stuns target with paw strike. Wear skinning set for bonus yield!",
            descriptionDe = "Mächtiges Wildtier im Wald. Besitzt die Chance auf einen wertvollen Bärenjungen-Drop (Direbear Cub)!",
            descriptionEn = "Powerful forest creature. Has a chance to drop a valuable Direbear Cub!"
        )
    )

    fun getFilteredAndSorted(
        query: String = "",
        regionFilter: String = "ALLE",
        safetyFilter: ZoneSafety? = null,
        sortMode: MonsterSortMode = MonsterSortMode.MOST_LUCRATIVE
    ): List<AlbionMonster> {
        var list = monsters

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.nameDe.lowercase().contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.regionDe.lowercase().contains(q) ||
                it.zoneLocationDe.lowercase().contains(q) ||
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
    MOST_LUCRATIVE("🔥 Lukrativste zuerst", "🔥 Most Lucrative First"),
    BY_REGION("📍 nach Region", "📍 By Region"),
    BY_SAFETY("🛡️ nach Zone/Sicherheit", "🛡️ By Zone Safety"),
    BY_TIER("⭐ nach Tier (T3-T8)", "⭐ By Tier (T3-T8)")
}
