package com.example.albionmarketv2

object AlbionResourceRepository {

    private val _dynamicResources = mutableListOf<AlbionResource>()

    private val _baseResources: List<AlbionResource> = listOf(
        // ESSEN & PFLANZEN (Pflanzen, Kräuter & Produkte)
        AlbionResource("T1_CARROT", "Karotte", "Carrot", 1, ResourceCategory.FOOD, 0, "Bauernhof Saatgut & Ernte"),
        AlbionResource("T3_WHEAT", "Weizen", "Wheat", 3, ResourceCategory.FOOD, 0, "Bauernhof Getreide"),
        AlbionResource("T4_TURNIP", "Steckrübe", "Turnip", 4, ResourceCategory.FOOD, 0, "Bauernhof Gemüse"),
        AlbionResource("T5_CABBAGE", "Kohl", "Cabbage", 5, ResourceCategory.FOOD, 0, "Bauernhof Gemüse"),
        AlbionResource("T6_POTATO", "Kartoffel", "Potato", 6, ResourceCategory.FOOD, 0, "Bauernhof Gemüse"),
        AlbionResource("T7_CORN", "Mais", "Corn", 7, ResourceCategory.FOOD, 0, "Bauernhof Getreide"),
        AlbionResource("T8_PUMPKIN", "Kürbis", "Pumpkin", 8, ResourceCategory.FOOD, 0, "Bauernhof Gemüse"),
        AlbionResource("T3_HERB", "Klettenwurzel", "Burdock", 3, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T4_HERB", "Drachenteufel", "Dragon Teasel", 4, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T5_HERB", "Elfenbein-Fingerhut", "Elusive Foxglove", 5, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T6_HERB", "Feuerkraut", "Firepit Vanilla", 6, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T7_HERB", "Ghillie-Kraut", "Ghoul Yarrow", 7, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T8_HERB", "Mondlicht-Mohn", "Moonshine Poppy", 8, ResourceCategory.FOOD, 0, "Kräutergarten Plantage"),
        AlbionResource("T3_MILK", "Ziegenmilch", "Goat Milk", 3, ResourceCategory.FOOD, 0, "Tierprodukt"),
        AlbionResource("T4_MILK", "Kuhmilch", "Cow Milk", 4, ResourceCategory.FOOD, 0, "Tierprodukt"),
        AlbionResource("T6_EGG", "Gänseei", "Goose Egg", 6, ResourceCategory.FOOD, 0, "Tierprodukt"),
        AlbionResource("T8_BUTTER", "Butter", "Butter", 8, ResourceCategory.FOOD, 0, "Tierprodukt"),

        // RESOURCES (Holz, Erz, Haut, Faser, Stein)
        AlbionResource("T2_WOOD", "Birkenstamm", "Birch Logs", 2, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 2"),
        AlbionResource("T3_WOOD", "Kastanienstamm", "Chestnut Logs", 3, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 3"),
        AlbionResource("T4_WOOD", "Kiefernstamm", "Pine Logs", 4, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 4"),
        AlbionResource("T5_WOOD", "Zedernstamm", "Cedar Logs", 5, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 5"),
        AlbionResource("T6_WOOD", "Bluteichenstamm", "Bloodoak Logs", 6, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 6"),
        AlbionResource("T7_WOOD", "Eschenrindenstamm", "Ashenbark Logs", 7, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 7"),
        AlbionResource("T8_WOOD", "Weißholzstamm", "Whitewood Logs", 8, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 8"),

        AlbionResource("T2_ORE", "Kupfererz", "Copper Ore", 2, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 2"),
        AlbionResource("T3_ORE", "Zinnerz", "Tin Ore", 3, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 3"),
        AlbionResource("T4_ORE", "Eisenerz", "Iron Ore", 4, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 4"),
        AlbionResource("T5_ORE", "Titanerz", "Titanium Ore", 5, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 5"),
        AlbionResource("T6_ORE", "Runenerz", "Runite Ore", 6, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 6"),
        AlbionResource("T7_ORE", "Meteoritenerz", "Meteorite Ore", 7, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 7"),
        AlbionResource("T8_ORE", "Adamantiumerz", "Adamantium Ore", 8, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 8"),

        AlbionResource("T2_HIDE", "Rauleder-Haut", "Rugged Hide", 2, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 2"),
        AlbionResource("T3_HIDE", "Dicke Haut", "Thick Hide", 3, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 3"),
        AlbionResource("T4_HIDE", "Schwere Haut", "Heavy Hide", 4, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 4"),
        AlbionResource("T5_HIDE", "Robuste Haut", "Robust Hide", 5, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 5"),
        AlbionResource("T6_HIDE", "Makellose Haut", "Pristine Hide", 6, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 6"),
        AlbionResource("T7_HIDE", "Gehärtete Haut", "Hardened Hide", 7, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 7"),
        AlbionResource("T8_HIDE", "Verstärkte Haut", "Fortified Hide", 8, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 8"),

        AlbionResource("T2_FIBER", "Baumwolle", "Cotton", 2, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 2"),
        AlbionResource("T3_FIBER", "Flachs", "Flax", 3, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 3"),
        AlbionResource("T4_FIBER", "Hanf", "Hemp", 4, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 4"),
        AlbionResource("T5_FIBER", "Himmelsblume", "Skyflower", 5, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 5"),
        AlbionResource("T6_FIBER", "Rotblatt-Baumwolle", "Redleaf Cotton", 6, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 6"),
        AlbionResource("T7_FIBER", "Sonnenblüte", "Sunblossom", 7, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 7"),
        AlbionResource("T8_FIBER", "Geisterhanf", "Ghost Hemp", 8, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 8"),

        AlbionResource("T2_STONE", "Kalkstein", "Limestone", 2, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 2"),
        AlbionResource("T3_STONE", "Sandstein", "Sandstone", 3, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 3"),
        AlbionResource("T4_STONE", "Travertin", "Travertine", 4, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 4"),
        AlbionResource("T5_STONE", "Granit", "Granite", 5, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 5"),
        AlbionResource("T6_STONE", "Schiefer", "Slate", 6, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 6"),
        AlbionResource("T7_STONE", "Basalt", "Basalt", 7, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 7"),
        AlbionResource("T8_STONE", "Marmor", "Marble", 8, ResourceCategory.RESOURCES, 0, "Rohstoff Stufe 8"),

        // REFINED (Planks, Bars, Leather, Cloth, Stoneblocks)
        AlbionResource("T2_PLANKS", "Birkenbretter", "Birch Planks", 2, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T3_PLANKS", "Kastanienbretter", "Chestnut Planks", 3, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T4_PLANKS", "Kiefernbretter", "Pine Planks", 4, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T5_PLANKS", "Zedernbretter", "Cedar Planks", 5, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T6_PLANKS", "Bluteichenbretter", "Bloodoak Planks", 6, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T7_PLANKS", "Eschenrindenbretter", "Ashenbark Planks", 7, ResourceCategory.REFINED, 0, "Veredeltes Holz"),
        AlbionResource("T8_PLANKS", "Weißholzbretter", "Whitewood Planks", 8, ResourceCategory.REFINED, 0, "Veredeltes Holz"),

        AlbionResource("T2_METALBAR", "Kupferbarren", "Copper Bar", 2, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T3_METALBAR", "Bronzebarren", "Bronze Bar", 3, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T4_METALBAR", "Eisenbarren", "Steel Bar", 4, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T5_METALBAR", "Titanbarren", "Titanium Bar", 5, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T6_METALBAR", "Runenbarren", "Runite Bar", 6, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T7_METALBAR", "Meteoritenbarren", "Meteorite Bar", 7, ResourceCategory.REFINED, 0, "Veredeltes Erz"),
        AlbionResource("T8_METALBAR", "Adamantiunbarren", "Adamantium Bar", 8, ResourceCategory.REFINED, 0, "Veredeltes Erz"),

        AlbionResource("T2_LEATHER", "Steifes Leder", "Stiff Leather", 2, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T3_LEATHER", "Dickes Leder", "Thick Leather", 3, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T4_LEATHER", "Gearbeitetes Leder", "Worked Leather", 4, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T5_LEATHER", "Cured Leder", "Cured Leather", 5, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T6_LEATHER", "Gehärtetes Leder", "Hardened Leather", 6, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T7_LEATHER", "Verstärktes Leder", "Reinforced Leather", 7, ResourceCategory.REFINED, 0, "Veredelte Haut"),
        AlbionResource("T8_LEATHER", "Makelloses Leder", "Pristine Leather", 8, ResourceCategory.REFINED, 0, "Veredelte Haut"),

        AlbionResource("T2_CLOTH", "Einfacher Stoff", "Simple Cloth", 2, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T3_CLOTH", "Nesselstoff", "Neat Cloth", 3, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T4_CLOTH", "Feinstoff", "Fine Cloth", 4, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T5_CLOTH", "Ornatstoff", "Ornate Cloth", 5, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T6_CLOTH", "Seidenstoff", "Silk Cloth", 6, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T7_CLOTH", "Opulenter Stoff", "Opulent Cloth", 7, ResourceCategory.REFINED, 0, "Veredelte Faser"),
        AlbionResource("T8_CLOTH", "Barocker Stoff", "Baroque Cloth", 8, ResourceCategory.REFINED, 0, "Veredelte Faser"),

        AlbionResource("T2_STONEBLOCK", "Kalksteinblock", "Limestone Block", 2, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T3_STONEBLOCK", "Sandsteinblock", "Sandstone Block", 3, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T4_STONEBLOCK", "Travertinblock", "Travertine Block", 4, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T5_STONEBLOCK", "Granitblock", "Granite Block", 5, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T6_STONEBLOCK", "Schieferblock", "Slate Block", 6, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T7_STONEBLOCK", "Basaltblock", "Basalt Block", 7, ResourceCategory.REFINED, 0, "Veredelter Stein"),
        AlbionResource("T8_STONEBLOCK", "Marmorblock", "Marble Block", 8, ResourceCategory.REFINED, 0, "Veredelter Stein"),

        // ARTIFACTS / RUNES / SOULS / RELICS
        AlbionResource("T4_RUNE", "Rune (T4)", "Rune (T4)", 4, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial zum Verzaubern"),
        AlbionResource("T5_RUNE", "Rune (T5)", "Rune (T5)", 5, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial zum Verzaubern"),
        AlbionResource("T6_RUNE", "Rune (T6)", "Rune (T6)", 6, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial zum Verzaubern"),
        AlbionResource("T7_RUNE", "Rune (T7)", "Rune (T7)", 7, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial zum Verzaubern"),
        AlbionResource("T8_RUNE", "Rune (T8)", "Rune (T8)", 8, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial zum Verzaubern"),

        AlbionResource("T4_SOUL", "Seele (T4)", "Soul (T4)", 4, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 4"),
        AlbionResource("T5_SOUL", "Seele (T5)", "Soul (T5)", 5, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 5"),
        AlbionResource("T6_SOUL", "Seele (T6)", "Soul (T6)", 6, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 6"),
        AlbionResource("T7_SOUL", "Seele (T7)", "Soul (T7)", 7, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 7"),
        AlbionResource("T8_SOUL", "Seele (T8)", "Soul (T8)", 8, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 8"),

        AlbionResource("T4_RELIC", "Relikt (T4)", "Relic (T4)", 4, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 4"),
        AlbionResource("T5_RELIC", "Relikt (T5)", "Relic (T5)", 5, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 5"),
        AlbionResource("T6_RELIC", "Relikt (T6)", "Relic (T6)", 6, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 6"),
        AlbionResource("T7_RELIC", "Relikt (T7)", "Relic (T7)", 7, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 7"),
        AlbionResource("T8_RELIC", "Relikt (T8)", "Relic (T8)", 8, ResourceCategory.ARTIFACTS, 0, "Artefaktmaterial Stufe 8"),

        // FOOD & POTIONS
        AlbionResource("T4_POTION_HEAL", "Heiltrank (Gering)", "Minor Healing Potion", 4, ResourceCategory.POTIONS, 0, "Stellt Gesundheit wieder her"),
        AlbionResource("T6_POTION_HEAL", "Heiltrank (Groß)", "Major Healing Potion", 6, ResourceCategory.POTIONS, 0, "Stellt Gesundheit stark wieder her"),
        AlbionResource("T4_POTION_ENERGY", "Energietrank (Gering)", "Minor Energy Potion", 4, ResourceCategory.POTIONS, 0, "Stellt Energie wieder her"),
        AlbionResource("T6_POTION_ENERGY", "Energietrank (Groß)", "Major Energy Potion", 6, ResourceCategory.POTIONS, 0, "Stellt Energie stark wieder her"),
        AlbionResource("T5_POTION_REVIVE", "Gigantifizierungstrank", "Gigantify Potion", 5, ResourceCategory.POTIONS, 0, "Erhöht max. Leben und Traglast"),
        AlbionResource("T7_POTION_CLEANSE", "Unverwundbarkeitstrank", "Resistance Potion", 7, ResourceCategory.POTIONS, 0, "Erhöht Widerstände stark"),

        AlbionResource("T4_MEAT_STEW", "Rindfleischeintopf", "Beef Stew", 4, ResourceCategory.FOOD, 0, "Erhöht Schaden um 13.5%"),
        AlbionResource("T6_MEAT_STEW", "Schweinefleischeintopf", "Pork Stew", 6, ResourceCategory.FOOD, 0, "Erhöht Schaden um 15%"),
        AlbionResource("T8_MEAT_STEW", "Rinder-Spezialeintopf", "Deadwater Eel Stew", 8, ResourceCategory.FOOD, 0, "Erhöht Schaden und Abklingzeit-Rückgang"),
        AlbionResource("T5_OMELETTE", "Schweine-Omelett", "Pork Omelette", 5, ResourceCategory.FOOD, 0, "Reduziert Abklingzeiten und Zauberzeiten"),
        AlbionResource("T7_OMELETTE", "Gänse-Omelett", "Goose Omelette", 7, ResourceCategory.FOOD, 0, "Stark reduzierte Abklingzeiten"),

        // MOUNTS
        AlbionResource("T3_MOUNT_RIDING_HORSE", "Reitpferd (T3)", "Riding Horse (T3)", 3, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),
        AlbionResource("T4_MOUNT_RIDING_HORSE", "Reitpferd (T4)", "Riding Horse (T4)", 4, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),
        AlbionResource("T5_MOUNT_RIDING_HORSE", "Reitpferd (T5)", "Riding Horse (T5)", 5, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),
        AlbionResource("T6_MOUNT_RIDING_HORSE", "Reitpferd (T6)", "Riding Horse (T6)", 6, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),
        AlbionResource("T7_MOUNT_RIDING_HORSE", "Reitpferd (T7)", "Riding Horse (T7)", 7, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),
        AlbionResource("T8_MOUNT_RIDING_HORSE", "Reitpferd (T8)", "Riding Horse (T8)", 8, ResourceCategory.MOUNTS, 0, "Schnelles Reittier"),

        AlbionResource("T3_MOUNT_TRANSPORT_OX", "Transportochse (T3)", "Transport Ox (T3)", 3, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),
        AlbionResource("T4_MOUNT_TRANSPORT_OX", "Transportochse (T4)", "Transport Ox (T4)", 4, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),
        AlbionResource("T5_MOUNT_TRANSPORT_OX", "Transportochse (T5)", "Transport Ox (T5)", 5, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),
        AlbionResource("T6_MOUNT_TRANSPORT_OX", "Transportochse (T6)", "Transport Ox (T6)", 6, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),
        AlbionResource("T7_MOUNT_TRANSPORT_OX", "Transportochse (T7)", "Transport Ox (T7)", 7, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),
        AlbionResource("T8_MOUNT_TRANSPORT_OX", "Transportochse (T8)", "Transport Ox (T8)", 8, ResourceCategory.MOUNTS, 0, "Hohe Traglast"),

        AlbionResource("T5_MOUNT_DIREWOLF", "Direwolf", "Direwolf", 5, ResourceCategory.MOUNTS, 0, "Sehr schnelles Raubtier"),
        AlbionResource("T8_MOUNT_DIREBEAR", "Direbear", "Direbear", 8, ResourceCategory.MOUNTS, 0, "Mächtiges Kampfreittier"),
        AlbionResource("T8_MOUNT_MAMMOTH_TRANSPORT", "Transportmammut", "Command Mammoth", 8, ResourceCategory.MOUNTS, 0, "Maximale Traglast in Albion Online"),

        // WEAPONS (Waffen)
        AlbionResource("T3_MAIN_SWORD", "Breitschwert (T3)", "Broadsword (T3)", 3, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),
        AlbionResource("T4_MAIN_SWORD", "Breitschwert (T4)", "Broadsword (T4)", 4, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),
        AlbionResource("T5_MAIN_SWORD", "Breitschwert (T5)", "Broadsword (T5)", 5, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),
        AlbionResource("T6_MAIN_SWORD", "Breitschwert (T6)", "Broadsword (T6)", 6, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),
        AlbionResource("T7_MAIN_SWORD", "Breitschwert (T7)", "Broadsword (T7)", 7, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),
        AlbionResource("T8_MAIN_SWORD", "Breitschwert (T8)", "Broadsword (T8)", 8, ResourceCategory.WEAPONS, 0, "Einhand-Schwert"),

        AlbionResource("T4_2H_CLAYMORE", "Claymore (T4)", "Claymore (T4)", 4, ResourceCategory.WEAPONS, 0, "Zweihand-Schwert"),
        AlbionResource("T5_2H_CLAYMORE", "Claymore (T5)", "Claymore (T5)", 5, ResourceCategory.WEAPONS, 0, "Zweihand-Schwert"),
        AlbionResource("T6_2H_CLAYMORE", "Claymore (T6)", "Claymore (T6)", 6, ResourceCategory.WEAPONS, 0, "Zweihand-Schwert"),
        AlbionResource("T7_2H_CLAYMORE", "Claymore (T7)", "Claymore (T7)", 7, ResourceCategory.WEAPONS, 0, "Zweihand-Schwert"),
        AlbionResource("T8_2H_CLAYMORE", "Claymore (T8)", "Claymore (T8)", 8, ResourceCategory.WEAPONS, 0, "Zweihand-Schwert"),

        AlbionResource("T3_2H_BOW", "Bogen (T3)", "Bow (T3)", 3, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),
        AlbionResource("T4_2H_BOW", "Bogen (T4)", "Bow (T4)", 4, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),
        AlbionResource("T5_2H_BOW", "Bogen (T5)", "Bow (T5)", 5, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),
        AlbionResource("T6_2H_BOW", "Bogen (T6)", "Bow (T6)", 6, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),
        AlbionResource("T7_2H_BOW", "Bogen (T7)", "Bow (T7)", 7, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),
        AlbionResource("T8_2H_BOW", "Bogen (T8)", "Bow (T8)", 8, ResourceCategory.WEAPONS, 0, "Fernkampfwaffe"),

        AlbionResource("T4_2H_WARBOW", "Kriegsbogen (T4)", "Warbow (T4)", 4, ResourceCategory.WEAPONS, 0, "Schneller Fernkampf-Bogen"),
        AlbionResource("T5_2H_WARBOW", "Kriegsbogen (T5)", "Warbow (T5)", 5, ResourceCategory.WEAPONS, 0, "Schneller Fernkampf-Bogen"),
        AlbionResource("T6_2H_WARBOW", "Kriegsbogen (T6)", "Warbow (T6)", 6, ResourceCategory.WEAPONS, 0, "Schneller Fernkampf-Bogen"),
        AlbionResource("T7_2H_WARBOW", "Kriegsbogen (T7)", "Warbow (T7)", 7, ResourceCategory.WEAPONS, 0, "Schneller Fernkampf-Bogen"),
        AlbionResource("T8_2H_WARBOW", "Kriegsbogen (T8)", "Warbow (T8)", 8, ResourceCategory.WEAPONS, 0, "Schneller Fernkampf-Bogen"),

        AlbionResource("T3_2H_CROSSBOW", "Armbrust (T3)", "Crossbow (T3)", 3, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),
        AlbionResource("T4_2H_CROSSBOW", "Armbrust (T4)", "Crossbow (T4)", 4, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),
        AlbionResource("T5_2H_CROSSBOW", "Armbrust (T5)", "Crossbow (T5)", 5, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),
        AlbionResource("T6_2H_CROSSBOW", "Armbrust (T6)", "Crossbow (T6)", 6, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),
        AlbionResource("T7_2H_CROSSBOW", "Armbrust (T7)", "Crossbow (T7)", 7, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),
        AlbionResource("T8_2H_CROSSBOW", "Armbrust (T8)", "Crossbow (T8)", 8, ResourceCategory.WEAPONS, 0, "Schwere Fernkampfwaffe"),

        AlbionResource("T3_MAIN_DAGGER", "Dolch (T3)", "Dagger (T3)", 3, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),
        AlbionResource("T4_MAIN_DAGGER", "Dolch (T4)", "Dagger (T4)", 4, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),
        AlbionResource("T5_MAIN_DAGGER", "Dolch (T5)", "Dagger (T5)", 5, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),
        AlbionResource("T6_MAIN_DAGGER", "Dolch (T6)", "Dagger (T6)", 6, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),
        AlbionResource("T7_MAIN_DAGGER", "Dolch (T7)", "Dagger (T7)", 7, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),
        AlbionResource("T8_MAIN_DAGGER", "Dolch (T8)", "Dagger (T8)", 8, ResourceCategory.WEAPONS, 0, "Schnelle Nahkampfwaffe"),

        AlbionResource("T4_MAIN_RAPIER_KATAR", "Blutklinge", "Bloodletter", 4, ResourceCategory.WEAPONS, 0, "Beliebte Assassinen-Waffe"),
        AlbionResource("T5_MAIN_RAPIER_KATAR", "Blutklinge", "Bloodletter", 5, ResourceCategory.WEAPONS, 0, "Beliebte Assassinen-Waffe"),
        AlbionResource("T6_MAIN_RAPIER_KATAR", "Blutklinge", "Bloodletter", 6, ResourceCategory.WEAPONS, 0, "Beliebte Assassinen-Waffe"),
        AlbionResource("T7_MAIN_RAPIER_KATAR", "Blutklinge", "Bloodletter", 7, ResourceCategory.WEAPONS, 0, "Beliebte Assassinen-Waffe"),
        AlbionResource("T8_MAIN_RAPIER_KATAR", "Blutklinge", "Bloodletter", 8, ResourceCategory.WEAPONS, 0, "Beliebte Assassinen-Waffe"),

        AlbionResource("T3_MAIN_FIRESTAFF", "Feuerstab (T3)", "Fire Staff (T3)", 3, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),
        AlbionResource("T4_MAIN_FIRESTAFF", "Feuerstab (T4)", "Fire Staff (T4)", 4, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),
        AlbionResource("T5_MAIN_FIRESTAFF", "Feuerstab (T5)", "Fire Staff (T5)", 5, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),
        AlbionResource("T6_MAIN_FIRESTAFF", "Feuerstab (T6)", "Fire Staff (T6)", 6, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),
        AlbionResource("T7_MAIN_FIRESTAFF", "Feuerstab (T7)", "Fire Staff (T7)", 7, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),
        AlbionResource("T8_MAIN_FIRESTAFF", "Feuerstab (T8)", "Fire Staff (T8)", 8, ResourceCategory.WEAPONS, 0, "Magische Feuerwaffe"),

        AlbionResource("T4_MAIN_FROSTSTAFF", "Eisstab (T4)", "Frost Staff (T4)", 4, ResourceCategory.WEAPONS, 0, "Magische Eiswaffe"),
        AlbionResource("T5_MAIN_FROSTSTAFF", "Eisstab (T5)", "Frost Staff (T5)", 5, ResourceCategory.WEAPONS, 0, "Magische Eiswaffe"),
        AlbionResource("T6_MAIN_FROSTSTAFF", "Eisstab (T6)", "Frost Staff (T6)", 6, ResourceCategory.WEAPONS, 0, "Magische Eiswaffe"),
        AlbionResource("T7_MAIN_FROSTSTAFF", "Eisstab (T7)", "Frost Staff (T7)", 7, ResourceCategory.WEAPONS, 0, "Magische Eiswaffe"),
        AlbionResource("T8_MAIN_FROSTSTAFF", "Eisstab (T8)", "Frost Staff (T8)", 8, ResourceCategory.WEAPONS, 0, "Magische Eiswaffe"),

        AlbionResource("T4_MAIN_HOLYSTAFF", "Heilstab (T4)", "Holy Staff (T4)", 4, ResourceCategory.WEAPONS, 0, "Magische Heilwaffe"),
        AlbionResource("T5_MAIN_HOLYSTAFF", "Heilstab (T5)", "Holy Staff (T5)", 5, ResourceCategory.WEAPONS, 0, "Magische Heilwaffe"),
        AlbionResource("T6_MAIN_HOLYSTAFF", "Heilstab (T6)", "Holy Staff (T6)", 6, ResourceCategory.WEAPONS, 0, "Magische Heilwaffe"),
        AlbionResource("T7_MAIN_HOLYSTAFF", "Heilstab (T7)", "Holy Staff (T7)", 7, ResourceCategory.WEAPONS, 0, "Magische Heilwaffe"),
        AlbionResource("T8_MAIN_HOLYSTAFF", "Heilstab (T8)", "Holy Staff (T8)", 8, ResourceCategory.WEAPONS, 0, "Magische Heilwaffe"),

        // ARMOR (Rüstung, Helme, Stiefel)
        AlbionResource("T4_HEAD_PLATE_SET1", "Soldaten-Helm (T4)", "Soldier Helmet (T4)", 4, ResourceCategory.HELMETS, 0, "Platten-Kopfbedeckung"),
        AlbionResource("T5_HEAD_PLATE_SET1", "Soldaten-Helm (T5)", "Soldier Helmet (T5)", 5, ResourceCategory.HELMETS, 0, "Platten-Kopfbedeckung"),
        AlbionResource("T6_HEAD_PLATE_SET1", "Soldaten-Helm (T6)", "Soldier Helmet (T6)", 6, ResourceCategory.HELMETS, 0, "Platten-Kopfbedeckung"),
        AlbionResource("T7_HEAD_PLATE_SET1", "Soldaten-Helm (T7)", "Soldier Helmet (T7)", 7, ResourceCategory.HELMETS, 0, "Platten-Kopfbedeckung"),
        AlbionResource("T8_HEAD_PLATE_SET1", "Soldaten-Helm (T8)", "Soldier Helmet (T8)", 8, ResourceCategory.HELMETS, 0, "Platten-Kopfbedeckung"),

        AlbionResource("T4_ARMOR_PLATE_SET1", "Soldaten-Rüstung (T4)", "Soldier Armor (T4)", 4, ResourceCategory.ARMOR, 0, "Platten-Brustrüstung"),
        AlbionResource("T5_ARMOR_PLATE_SET1", "Soldaten-Rüstung (T5)", "Soldier Armor (T5)", 5, ResourceCategory.ARMOR, 0, "Platten-Brustrüstung"),
        AlbionResource("T6_ARMOR_PLATE_SET1", "Soldaten-Rüstung (T6)", "Soldier Armor (T6)", 6, ResourceCategory.ARMOR, 0, "Platten-Brustrüstung"),
        AlbionResource("T7_ARMOR_PLATE_SET1", "Soldaten-Rüstung (T7)", "Soldier Armor (T7)", 7, ResourceCategory.ARMOR, 0, "Platten-Brustrüstung"),
        AlbionResource("T8_ARMOR_PLATE_SET1", "Soldaten-Rüstung (T8)", "Soldier Armor (T8)", 8, ResourceCategory.ARMOR, 0, "Platten-Brustrüstung"),

        AlbionResource("T4_SHOES_PLATE_SET1", "Soldaten-Stiefel (T4)", "Soldier Boots (T4)", 4, ResourceCategory.SHOES, 0, "Platten-Schuhe"),
        AlbionResource("T5_SHOES_PLATE_SET1", "Soldaten-Stiefel (T5)", "Soldier Boots (T5)", 5, ResourceCategory.SHOES, 0, "Platten-Schuhe"),
        AlbionResource("T6_SHOES_PLATE_SET1", "Soldaten-Stiefel (T6)", "Soldier Boots (T6)", 6, ResourceCategory.SHOES, 0, "Platten-Schuhe"),
        AlbionResource("T7_SHOES_PLATE_SET1", "Soldaten-Stiefel (T7)", "Soldier Boots (T7)", 7, ResourceCategory.SHOES, 0, "Platten-Schuhe"),
        AlbionResource("T8_SHOES_PLATE_SET1", "Soldaten-Stiefel (T8)", "Soldier Boots (T8)", 8, ResourceCategory.SHOES, 0, "Platten-Schuhe"),

        AlbionResource("T4_HEAD_LEATHER_SET1", "Söldner-Kapuze (T4)", "Mercenary Hood (T4)", 4, ResourceCategory.HELMETS, 0, "Leder-Kopfbedeckung"),
        AlbionResource("T5_HEAD_LEATHER_SET1", "Söldner-Kapuze (T5)", "Mercenary Hood (T5)", 5, ResourceCategory.HELMETS, 0, "Leder-Kopfbedeckung"),
        AlbionResource("T6_HEAD_LEATHER_SET1", "Söldner-Kapuze (T6)", "Mercenary Hood (T6)", 6, ResourceCategory.HELMETS, 0, "Leder-Kopfbedeckung"),
        AlbionResource("T7_HEAD_LEATHER_SET1", "Söldner-Kapuze (T7)", "Mercenary Hood (T7)", 7, ResourceCategory.HELMETS, 0, "Leder-Kopfbedeckung"),
        AlbionResource("T8_HEAD_LEATHER_SET1", "Söldner-Kapuze (T8)", "Mercenary Hood (T8)", 8, ResourceCategory.HELMETS, 0, "Leder-Kopfbedeckung"),

        AlbionResource("T4_ARMOR_LEATHER_SET1", "Söldner-Jacke (T4)", "Mercenary Jacket (T4)", 4, ResourceCategory.ARMOR, 0, "Leder-Brustrüstung"),
        AlbionResource("T5_ARMOR_LEATHER_SET1", "Söldner-Jacke (T5)", "Mercenary Jacket (T5)", 5, ResourceCategory.ARMOR, 0, "Leder-Brustrüstung"),
        AlbionResource("T6_ARMOR_LEATHER_SET1", "Söldner-Jacke (T6)", "Mercenary Jacket (T6)", 6, ResourceCategory.ARMOR, 0, "Leder-Brustrüstung"),
        AlbionResource("T7_ARMOR_LEATHER_SET1", "Söldner-Jacke (T7)", "Mercenary Jacket (T7)", 7, ResourceCategory.ARMOR, 0, "Leder-Brustrüstung"),
        AlbionResource("T8_ARMOR_LEATHER_SET1", "Söldner-Jacke (T8)", "Mercenary Jacket (T8)", 8, ResourceCategory.ARMOR, 0, "Leder-Brustrüstung"),

        AlbionResource("T4_SHOES_LEATHER_SET1", "Söldner-Schuhe (T4)", "Mercenary Shoes (T4)", 4, ResourceCategory.SHOES, 0, "Leder-Schuhe"),
        AlbionResource("T5_SHOES_LEATHER_SET1", "Söldner-Schuhe (T5)", "Mercenary Shoes (T5)", 5, ResourceCategory.SHOES, 0, "Leder-Schuhe"),
        AlbionResource("T6_SHOES_LEATHER_SET1", "Söldner-Schuhe (T6)", "Mercenary Shoes (T6)", 6, ResourceCategory.SHOES, 0, "Leder-Schuhe"),
        AlbionResource("T7_SHOES_LEATHER_SET1", "Söldner-Schuhe (T7)", "Mercenary Shoes (T7)", 7, ResourceCategory.SHOES, 0, "Leder-Schuhe"),
        AlbionResource("T8_SHOES_LEATHER_SET1", "Söldner-Schuhe (T8)", "Mercenary Shoes (T8)", 8, ResourceCategory.SHOES, 0, "Leder-Schuhe"),

        AlbionResource("T4_HEAD_CLOTH_SET1", "Magier-Haube (T4)", "Mage Cowl (T4)", 4, ResourceCategory.HELMETS, 0, "Stoff-Kopfbedeckung"),
        AlbionResource("T5_HEAD_CLOTH_SET1", "Magier-Haube (T5)", "Mage Cowl (T5)", 5, ResourceCategory.HELMETS, 0, "Stoff-Kopfbedeckung"),
        AlbionResource("T6_HEAD_CLOTH_SET1", "Magier-Haube (T6)", "Mage Cowl (T6)", 6, ResourceCategory.HELMETS, 0, "Stoff-Kopfbedeckung"),
        AlbionResource("T7_HEAD_CLOTH_SET1", "Magier-Haube (T7)", "Mage Cowl (T7)", 7, ResourceCategory.HELMETS, 0, "Stoff-Kopfbedeckung"),
        AlbionResource("T8_HEAD_CLOTH_SET1", "Magier-Haube (T8)", "Mage Cowl (T8)", 8, ResourceCategory.HELMETS, 0, "Stoff-Kopfbedeckung"),

        AlbionResource("T4_ARMOR_CLOTH_SET1", "Magier-Robe (T4)", "Mage Robe (T4)", 4, ResourceCategory.ARMOR, 0, "Stoff-Brustkleidung"),
        AlbionResource("T5_ARMOR_CLOTH_SET1", "Magier-Robe (T5)", "Mage Robe (T5)", 5, ResourceCategory.ARMOR, 0, "Stoff-Brustkleidung"),
        AlbionResource("T6_ARMOR_CLOTH_SET1", "Magier-Robe (T6)", "Mage Robe (T6)", 6, ResourceCategory.ARMOR, 0, "Stoff-Brustkleidung"),
        AlbionResource("T7_ARMOR_CLOTH_SET1", "Magier-Robe (T7)", "Mage Robe (T7)", 7, ResourceCategory.ARMOR, 0, "Stoff-Brustkleidung"),
        AlbionResource("T8_ARMOR_CLOTH_SET1", "Magier-Robe (T8)", "Mage Robe (T8)", 8, ResourceCategory.ARMOR, 0, "Stoff-Brustkleidung"),

        AlbionResource("T4_SHOES_CLOTH_SET1", "Magier-Sandalen (T4)", "Mage Sandals (T4)", 4, ResourceCategory.SHOES, 0, "Stoff-Sandalen"),
        AlbionResource("T5_SHOES_CLOTH_SET1", "Magier-Sandalen (T5)", "Mage Sandals (T5)", 5, ResourceCategory.SHOES, 0, "Stoff-Sandalen"),
        AlbionResource("T6_SHOES_CLOTH_SET1", "Magier-Sandalen (T6)", "Mage Sandals (T6)", 6, ResourceCategory.SHOES, 0, "Stoff-Sandalen"),
        AlbionResource("T7_SHOES_CLOTH_SET1", "Magier-Sandalen (T7)", "Mage Sandals (T7)", 7, ResourceCategory.SHOES, 0, "Stoff-Sandalen"),
        AlbionResource("T8_SHOES_CLOTH_SET1", "Magier-Sandalen (T8)", "Mage Sandals (T8)", 8, ResourceCategory.SHOES, 0, "Stoff-Sandalen"),

        // BAGS & CAPES
        AlbionResource("T2_BAG", "Tasche (T2)", "Bag (T2)", 2, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T3_BAG", "Tasche (T3)", "Bag (T3)", 3, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T4_BAG", "Tasche (T4)", "Bag (T4)", 4, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T5_BAG", "Tasche (T5)", "Bag (T5)", 5, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T6_BAG", "Tasche (T6)", "Bag (T6)", 6, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T7_BAG", "Tasche (T7)", "Bag (T7)", 7, ResourceCategory.BAG, 0, "Traglast-Tasche"),
        AlbionResource("T8_BAG", "Tasche (T8)", "Bag (T8)", 8, ResourceCategory.BAG, 0, "Traglast-Tasche"),

        AlbionResource("T2_CAPE", "Umhang (T2)", "Cape (T2)", 2, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T3_CAPE", "Umhang (T3)", "Cape (T3)", 3, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T4_CAPE", "Umhang (T4)", "Cape (T4)", 4, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T5_CAPE", "Umhang (T5)", "Cape (T5)", 5, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T6_CAPE", "Umhang (T6)", "Cape (T6)", 6, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T7_CAPE", "Umhang (T7)", "Cape (T7)", 7, ResourceCategory.CAPE, 0, "Einfacher Umhang"),
        AlbionResource("T8_CAPE", "Umhang (T8)", "Cape (T8)", 8, ResourceCategory.CAPE, 0, "Einfacher Umhang"),

        AlbionResource("T4_CAPEITEM_FW_BRIDGEWATCH", "Bridgewatch-Umhang", "Bridgewatch Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt"),
        AlbionResource("T4_CAPEITEM_FW_FORTSTERLING", "Fort Sterling-Umhang", "Fort Sterling Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt"),
        AlbionResource("T4_CAPEITEM_FW_LYMHURST", "Lymhurst-Umhang", "Lymhurst Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt"),
        AlbionResource("T4_CAPEITEM_FW_MARTLOCK", "Martlock-Umhang", "Martlock Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt"),
        AlbionResource("T4_CAPEITEM_FW_THETFORD", "Thetford-Umhang", "Thetford Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt"),
        AlbionResource("T4_CAPEITEM_FW_CAERLEON", "Caerleon-Umhang", "Caerleon Cape", 4, ResourceCategory.CAPE, 0, "Fraktions-Umhang mit Spezialeffekt")
    )

    val resources: List<AlbionResource>
        get() = (_baseResources + _dynamicResources).distinctBy { it.fullId }

    fun addDynamicResources(items: List<AlbionResource>) {
        _dynamicResources.clear()
        _dynamicResources.addAll(items)
    }

    fun filterResources(
        category: ResourceCategory = ResourceCategory.ALL,
        tier: Int = 0,
        enchantment: Int = 0,
        query: String = ""
    ): List<AlbionResource> {
        return resources.filter { res ->
            val matchCategory = category == ResourceCategory.ALL || res.category == category
            val matchTier = tier == 0 || res.tier == tier
            val matchEnchantment = res.enchantment == enchantment
            val matchQuery = query.isBlank() ||
                    res.nameDe.contains(query, ignoreCase = true) ||
                    res.nameEn.contains(query, ignoreCase = true) ||
                    res.id.contains(query, ignoreCase = true)

            matchCategory && matchTier && matchEnchantment && matchQuery
        }
    }
}
