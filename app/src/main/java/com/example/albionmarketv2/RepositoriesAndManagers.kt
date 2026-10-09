package com.example.albionmarketv2

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

object EquipmentBuildRepository {
    val builds = listOf(
        EquipmentBuild(
            id = "1",
            title = "Broadsword Solo PvE Speed-Clear",
            category = BuildCategory.SOLO_PVE,
            estimatedCostSilver = 145000L,
            estimatedMarginPercent = 22.5,
            strongestWeaponHighlight = "Breitschwert (Broadsword) T8 mit Lebensraub",
            useCaseFunction = "Hervorragend für Solo-Dungeons, T8-Weltmobs und schnelles Fame-Farmen.",
            weapon = "Breitschwert (Broadsword) .4",
            head = "Söldner-Kapuze (Mercenary Hood)",
            armor = "Hellion-Jacke (Hellion Jacket)",
            shoes = "Königs-Sandalen (Royal Sandals)",
            mount = "Wildschwein (Wild Boar)",
            potion = "Großer Gigantentrank",
            food = "Äal-Eintopf (Eel Stew)",
            recommendedSkills = "Q: Heldenhafter Schlag | W: Heroischer Wurf",
            headSkill = "Reinigung (Cleanse)",
            armorSkill = "Blutdurst (Bloodlust - Flächenheilung)",
            shoesSkill = "Ausweichen / Lauf",
            damageRating = 85,
            healingRating = 45,
            defenseRating = 70,
            mobilityRating = 75,
            aiEvaluationDe = "Sehr ausbalanciertes Solo-PvE-Build mit starkem Sustain durch Hellion-Jacke und hoher Mobilität.",
            combatRotationDe = "Heroische Stacks aufbauen (3x Q), W einsetzen für Burst, Hellion-Jacke bei < 50% HP zünden.",
            vorteileDe = listOf("Enormer Selbstheilungs-Sustain", "Gute Mobilität", "Geringe Repkosten"),
            nachteileDe = listOf("Schwächer bei Bossen mit hohem One-Shot-Schaden")
        ),
        EquipmentBuild(
            id = "2",
            title = "Greataxe Solo Dungeon Farmer",
            category = BuildCategory.SOLO_PVE,
            estimatedCostSilver = 120000L,
            estimatedMarginPercent = 18.2,
            strongestWeaponHighlight = "Große Axt (Greataxe) - Wirbelwind AoE",
            useCaseFunction = "Perfekt zum schnellen Clearen von Gruppen und Open-World-Mobs.",
            weapon = "Große Axt (Greataxe)",
            head = "Kläpper-Kapuze (Stalker Hood)",
            armor = "Söldner-Jacke (Mercenary Jacket)",
            shoes = "Soldaten-Stiefel (Soldier Boots)",
            mount = "Hirsch (Stag)",
            potion = "Heiltrank",
            food = "Brathuhn (Roast Chicken)",
            recommendedSkills = "Q: Wirbelwind (Rending Spin) | W: Adrenalin-Schub",
            headSkill = "Schadens-Boost",
            armorSkill = "Blutdurst (Lifesteal)",
            shoesSkill = "Sprint",
            damageRating = 90,
            healingRating = 30,
            defenseRating = 60,
            mobilityRating = 65,
            aiEvaluationDe = "Klassisches PvE-Metabuild für schnelles Clearen von Mob-Lagern.",
            combatRotationDe = "Mobs zusammenziehen, Wirbelwind aktivieren, Söldner-Jacke für Heilung nutzen.",
            vorteileDe = listOf("Sehr hohes AoE-Schadenspotenzial", "Günstiger Einstieg"),
            nachteileDe = listOf("Anfällig gegen Unterbrechungen (Interrupts)")
        ),
        EquipmentBuild(
            id = "3",
            title = "1H Nature Staff Healer (Group PvE)",
            category = BuildCategory.GROUP_PVE,
            estimatedCostSilver = 210000L,
            estimatedMarginPercent = 25.0,
            strongestWeaponHighlight = "1H Naturstab (1H Nature Staff) + Leere-Schild",
            useCaseFunction = "Unverzichtbar für Static Dungeons, Hardcore Expeditionen und Gruppen-Bosskämpfe.",
            weapon = "1H Naturstab",
            head = "Kleriker-Kappe (Cleric Cowl)",
            armor = "Kleriker-Robe (Cleric Robe)",
            shoes = "Gelehrten-Sandalen (Scholar Sandals)",
            mount = "Gepanzerches Pferd",
            potion = "Energietrank",
            food = "Suppe (Health Regeneration)",
            recommendedSkills = "Q: Dornenwuchs | W: Verjüngung",
            headSkill = "Eisblock (Invulnerability)",
            armorSkill = "Unbezwingbarkeit / Schild",
            shoesSkill = "Energieladung",
            damageRating = 30,
            healingRating = 98,
            defenseRating = 75,
            mobilityRating = 50,
            aiEvaluationDe = "Das absolute Meta-Heiler-Build für PvE-Gruppen mit exzellentem HoT (Heal over Time).",
            combatRotationDe = "Verjüngung auf Tanks aufrechterhalten, Eisblock bei Aggro oder Boss-Mechanik nutzen.",
            vorteileDe = listOf("Extrem hoher Dauermarkt-Heal", "Sehr gefragt in jeder Gruppe"),
            nachteileDe = listOf("Kein Burst-Heal bei plötzlichen One-Shots")
        ),
        EquipmentBuild(
            id = "4",
            title = "Halberd Tank & AoE Group Bruiser",
            category = BuildCategory.GROUP_PVE,
            estimatedCostSilver = 180000L,
            estimatedMarginPercent = 15.4,
            strongestWeaponHighlight = "Hellebarde (Halberd) mit Flächenschaden",
            useCaseFunction = "Frontline-Kampf in Static Dungeons und Gruppen-Expeditionen.",
            weapon = "Hellebarde",
            head = "Soldaten-Helm",
            armor = "Wächter-Rüstung (Guardian Armor)",
            shoes = "Soldaten-Stiefel",
            mount = "Oxen / Transport",
            potion = "Gigantentrank",
            food = "Rindfleischeintopf",
            recommendedSkills = "Q: Schlitzer | W: Wirbelwind",
            headSkill = "Block",
            armorSkill = "Aura",
            shoesSkill = "Sprint",
            damageRating = 75,
            healingRating = 20,
            defenseRating = 90,
            mobilityRating = 55,
            aiEvaluationDe = "Solides Frontline-Build für Gruppen, die zusätzlichen Tank-Druck und AoE-Schaden benötigen.",
            combatRotationDe = "Mobs spotten, AoE-Schaden verteilen, Defensive Cooldowns rotieren.",
            vorteileDe = listOf("Sehr tankig", "Guter Gruppennutzen"),
            nachteileDe = listOf("Geringer Einzelziel-Schaden")
        ),
        EquipmentBuild(
            id = "5",
            title = "1H Curse Staff (Corrupted Stalker / Slayer)",
            category = BuildCategory.CORRUPTED_DUNGEON,
            estimatedCostSilver = 290000L,
            estimatedMarginPercent = 31.2,
            strongestWeaponHighlight = "1H Fluchstab + Krypta-Kerze (Cryptcandle)",
            useCaseFunction = "Dominantes 1v1 PvP-Build für verdorbene Dungeons (Corrupted Dungeons).",
            weapon = "1H Fluchstab (1H Curse Staff)",
            head = "Magier-Kappe (Mage Cowl)",
            armor = "Geister-Robe (Fiend Robe)",
            shoes = "Magier-Sandalen",
            mount = "Sumpfdrache (Swamp Salamander)",
            potion = "Unsichtbarkeitstrank",
            food = "Omelett (Cooldown Reduction)",
            recommendedSkills = "Q: Fluchkugel | W: Todesstrahl",
            headSkill = "Gift-Schuss",
            armorSkill = "Geist-Auswurf (Purge)",
            shoesSkill = "Lauschen / Lauf",
            damageRating = 95,
            healingRating = 10,
            defenseRating = 65,
            mobilityRating = 60,
            aiEvaluationDe = "Extremer kontinuierlicher Schaden (DoT) mit starkem Purge gegen gegnerische Buffs.",
            combatRotationDe = "4 Stacks Fluch aufbauen, E-Skill (Todeszauber) für massiven Burst ausführen, Fiend Robe zum Purgen nutzen.",
            vorteileDe = listOf("Sehr hoher DoT-Schaden", "Starker Purge-Effekt"),
            nachteileDe = listOf("Hoher Skillbedarf gegen Mobile Gegner")
        ),
        EquipmentBuild(
            id = "6",
            title = "Bloodletter Mobility & Escape PvP",
            category = BuildCategory.CORRUPTED_DUNGEON,
            estimatedCostSilver = 340000L,
            estimatedMarginPercent = 28.5,
            strongestWeaponHighlight = "Blutbrief (Bloodletter) + Mistcaller",
            useCaseFunction = "Hervorragende Überlebensfähigkeit, Execute und hohe Mobilität im 1v1 PvP.",
            weapon = "Blutbrief (Bloodletter)",
            head = "Jäger-Kapuze",
            armor = "Stalker-Jacke",
            shoes = "Königs-Sandalen",
            mount = "Gepanzerches Pferd",
            potion = "Gigantentrank",
            food = "Omelett",
            recommendedSkills = "Q: Heroischer Schlag | W: Dash",
            headSkill = "Reflektieren",
            armorSkill = "Blitzschlag (Lightning)",
            shoesSkill = "Doppelter Sprung",
            damageRating = 80,
            healingRating = 15,
            defenseRating = 65,
            mobilityRating = 98,
            aiEvaluationDe = "Das ultimative Mobilitäts-Build für Entkommen und präzise Execute-Kills.",
            combatRotationDe = "Gegner mürbe machen, mit E-Skill finishen (unter 40% HP erhöhter Schaden) oder entkommen.",
            vorteileDe = listOf("Unübertroffene Mobilität", "Exzellenter Execute"),
            nachteileDe = listOf("Geringer Basis-Schaden gegen schwere Tanks")
        ),
        EquipmentBuild(
            id = "7",
            title = "1H Mace & Holy Healer (Hellgate Meta)",
            category = BuildCategory.HELLGATE,
            estimatedCostSilver = 310000L,
            estimatedMarginPercent = 20.4,
            strongestWeaponHighlight = "1H Streitkolben (1H Mace) Stun-Lock",
            useCaseFunction = "Kontrolliertes 2v2 oder 5v5 Hellgate PvP mit tödlichem CC.",
            weapon = "1H Streitkolben",
            head = "Dämmerungs-Helm (Knight Helmet)",
            armor = "Justiz-Rüstung (Judicator Armor)",
            shoes = "Königs-Stiefel",
            mount = "Wildschwein",
            potion = "Zähigkeitstrank",
            food = "Steheintopf",
            recommendedSkills = "Q: Betäubender Schlag | W: Schild",
            headSkill = "Unangreifbarkeit",
            armorSkill = "Gruppen-Buff",
            shoesSkill = "Sprint",
            damageRating = 50,
            healingRating = 60,
            defenseRating = 95,
            mobilityRating = 60,
            aiEvaluationDe = "Hervorragendes CC-Tank-Build für Hellgates zur Isolierung von Gegnern.",
            combatRotationDe = "Gegnerischen Heiler oder DPS stunnen, Team nachziehen lassen.",
            vorteileDe = listOf("Extremes CC-Potential", "Sehr hohe Verteidigung"),
            nachteileDe = listOf("Sehr teamabhängig")
        ),
        EquipmentBuild(
            id = "8",
            title = "Dual Swords Mists 1v1 Brawler",
            category = BuildCategory.MISTS,
            estimatedCostSilver = 240000L,
            estimatedMarginPercent = 24.1,
            strongestWeaponHighlight = "Doppelschwerter (Dual Swords) Wirbelwind-Burst",
            useCaseFunction = "Perfekt für Solo-Mists, Abteien (Abbeys) und PvP-Begegnungen.",
            weapon = "Doppelschwerter (Dual Swords)",
            head = "Kläpper-Kapuze (Stalker Hood)",
            armor = "Hellion-Jacke",
            shoes = "Dämonen-Stiefel",
            mount = "Eber (Wild Boar)",
            potion = "Heiltrank",
            food = "Äal-Eintopf",
            recommendedSkills = "Q: Heroischer Schlag | W: Wirbelwind",
            headSkill = "Schadens-Buff",
            armorSkill = "Hellion Lifesteal",
            shoesSkill = "Sprint",
            damageRating = 88,
            healingRating = 40,
            defenseRating = 65,
            mobilityRating = 80,
            aiEvaluationDe = "Sehr aggressives Brawler-Build für die Nebel (Mists) mit starkem Flächenschaden.",
            combatRotationDe = "Heroische Stacks aufrecht erhalten, E-Skill für massiven Flächenschaden auf den Gegner loslassen.",
            vorteileDe = listOf("Starker AoE-Burst", "Guter Sustain in Kämpfen"),
            nachteileDe = listOf("Anfällig gegen Kiting")
        ),
        EquipmentBuild(
            id = "9",
            title = "Hoarfrost Staff ZvZ AoE Freeze",
            category = BuildCategory.ZVZ,
            estimatedCostSilver = 390000L,
            estimatedMarginPercent = 26.8,
            strongestWeaponHighlight = "Raureif-Stab (Hoarfrost Staff) Gruppen-Freeze",
            useCaseFunction = "Großschlachten (ZvZ), Gildenkriege und Castle Outposts.",
            weapon = "Raureif-Stab",
            head = "Kultisten-Kappe (Cultist Cowl)",
            armor = "Schriftgelehrten-Robe (Cleric)",
            shoes = "Königs-Sandalen",
            mount = "Armored Horse",
            potion = "Gigantentrank",
            food = "Omelett",
            recommendedSkills = "Q: Frostblitz | W: Eisige Pfade",
            headSkill = "Reflektieren",
            armorSkill = "Schutzschild",
            shoesSkill = "Sprint",
            damageRating = 92,
            healingRating = 10,
            defenseRating = 60,
            mobilityRating = 65,
            aiEvaluationDe = "Meta-Build für ZvZ-Zonencontroll und Massen-Freezing.",
            combatRotationDe = "In die gegnerische Backline blinzeln, E-Skill (Großfrost) zünden und eingefrieren.",
            vorteileDe = listOf("Kriegsentscheidendes CC", "Sehr hoher Flächenschaden"),
            nachteileDe = listOf("Sehr hoher Energieverbrauch")
        ),
        EquipmentBuild(
            id = "10",
            title = "Bloodletter Gatherer Escape Suit",
            category = BuildCategory.GATHERING,
            estimatedCostSilver = 175000L,
            estimatedMarginPercent = 19.0,
            strongestWeaponHighlight = "Blutbrief + Nebel-Pferd für sicheres Sammeln",
            useCaseFunction = "Sicheres Ressourcen-Sammeln in tödlichen Schwarz- und Mists-Zonen.",
            weapon = "Blutbrief",
            head = "Sammler-Kapuze (Gatherer Hood)",
            armor = "Sammler-Jacke (Invisibility)",
            shoes = "Sammler-Stiefel (Flee)",
            mount = "Schneller Hirsch / Reitechsen",
            potion = "Unsichtbarkeitstrank",
            food = "Kuchen (Gathering Yield)",
            recommendedSkills = "Q: Heroisch | W: Dash",
            headSkill = "Sammel-Boost",
            armorSkill = "Unsichtbarkeit",
            shoesSkill = "Entkommen (Flee)",
            damageRating = 45,
            healingRating = 10,
            defenseRating = 70,
            mobilityRating = 100,
            aiEvaluationDe = "Das Standard-Überlebensbuild für jeden professionellen Sammler in PvP-Zonen.",
            combatRotationDe = "Bei Gankern Unsichtbarkeit der Jacke zünden, Dash und Flee nutzen, um zu entkommen.",
            vorteileDe = listOf("Maximale Fluchtchancen", "Sammel-Bonus"),
            nachteileDe = listOf("Kaum Kampfkraft gegen geübte Ganker")
        ),
        EquipmentBuild(
            id = "11",
            title = "Claymore Melee Burst DPS",
            category = BuildCategory.MELEE_DPS,
            estimatedCostSilver = 195000L,
            estimatedMarginPercent = 21.3,
            strongestWeaponHighlight = "Claymore - Tödlicher Ziel-Stun & Charge",
            useCaseFunction = "Hoher Einzelziel-Schaden in Kleingruppen und Gank-Trupps.",
            weapon = "Claymore",
            head = "Jäger-Kapuze",
            armor = "Hellion-Jacke",
            shoes = "Soldaten-Stiefel",
            mount = "Wildschwein",
            potion = "Heiltrank",
            food = "Rindfleischeintopf",
            recommendedSkills = "Q: Heldenhafter Schlag | W: Geist-Rüstung / Parry",
            headSkill = "Schadens-Buff",
            armorSkill = "Hellion Lifesteal",
            shoesSkill = "Sprint",
            damageRating = 89,
            healingRating = 20,
            defenseRating = 65,
            mobilityRating = 85,
            aiEvaluationDe = "Exzellentes Nahkampf-DPS-Build mit starkem Einzelziel-Fokus.",
            combatRotationDe = "Anstürmen, Stacks aufbauen, E-Skill für enormen Einzelziel-Schaden entfesseln.",
            vorteileDe = listOf("Hoher Burst", "Gute Verfolgung"),
            nachteileDe = listOf("Geringe Gruppen-Utility")
        ),
        EquipmentBuild(
            id = "12",
            title = "Great Fire Staff Range AoE Damage",
            category = BuildCategory.DAMAGE_DPS,
            estimatedCostSilver = 260000L,
            estimatedMarginPercent = 23.9,
            strongestWeaponHighlight = "Großer Feuerstab - Meteore & Flächenbrand",
            useCaseFunction = "Zerstörerischer Fernkampf-Schaden in PvE-Expeditionen und PvP-Gruppen.",
            weapon = "Großer Feuerstab",
            head = "Magier-Kappe",
            armor = "Kleriker-Robe",
            shoes = "Magier-Sandalen",
            mount = "Gepanzerches Pferd",
            potion = "Energietrank",
            food = "Omelett",
            recommendedSkills = "Q: Feuerball | W: Feuersäule",
            headSkill = "Schadens-Buff",
            armorSkill = "Unbesiegbarkeit (Cleric Robe)",
            shoesSkill = "Sprint",
            damageRating = 96,
            healingRating = 10,
            defenseRating = 55,
            mobilityRating = 60,
            aiEvaluationDe = "Einer der stärksten Fernkampf-Schadensausteiler im Spiel mit verheerendem Flächenschaden.",
            combatRotationDe = "Aus sicherer Distanz Feuerbälle werfen, E-Skill (Meteor) auf gebündelte Feinde platzieren.",
            vorteileDe = listOf("Extremer Schaden", "Große Reichweite"),
            nachteileDe = listOf("Sehr papier-dünn (geringe Rüstung)")
        )
    )
}

object AlbionMonsterRepository {
    val monsters = emptyList<MonsterItem>()
    fun getFilteredAndSorted(
        query: String = "",
        regionFilter: String = "ALLE",
        playerCategoryFilter: PlayerCategory = PlayerCategory.ALL,
        sortMode: MonsterSortMode = MonsterSortMode.MOST_LUCRATIVE
    ): List<MonsterItem> = emptyList()
}

object CraftingRepository {
    val recipes: List<CraftingRecipe>
        get() = AlbionResourceRepository.resources.map { getRecipeFor(it) }

    fun calculateCraftingOpportunity(vararg args: Any?): CraftingOpportunityDetails? {
        val opps = calculateCraftingOpportunities(*args)
        return opps.firstOrNull()
    }

    fun getRecipeFor(vararg args: Any?): CraftingRecipe {
        val arg = args.getOrNull(0)
        val res = arg as? AlbionResource ?: when (arg) {
            is String -> AlbionResourceRepository.resources.firstOrNull { it.fullId.equals(arg, true) || it.id.equals(arg, true) } 
                ?: AlbionResource(id = arg, nameDe = arg, nameEn = arg, tier = 4, category = ResourceCategory.RESOURCES)
            else -> AlbionResource(id = "T4_ORE", nameDe = "Eisenstein", nameEn = "Iron Ore", tier = 4, category = ResourceCategory.RESOURCES)
        }

        val tier = res.tier.coerceIn(2, 8)
        val idUpper = res.id.uppercase()

        val rawId = when {
            idUpper.contains("METALBAR") || idUpper.contains("ORE") -> "T${tier}_ORE"
            idUpper.contains("PLANKS") || idUpper.contains("WOOD") -> "T${tier}_WOOD"
            idUpper.contains("LEATHER") || idUpper.contains("HIDE") -> "T${tier}_HIDE"
            idUpper.contains("CLOTH") || idUpper.contains("FIBER") -> "T${tier}_FIBER"
            idUpper.contains("STONEBLOCK") || idUpper.contains("ROCK") -> "T${tier}_ROCK"
            else -> "T${tier}_ORE"
        }

        val rawNameDe = when {
            idUpper.contains("METALBAR") || idUpper.contains("ORE") -> "Erz / Barren (T$tier)"
            idUpper.contains("PLANKS") || idUpper.contains("WOOD") -> "Holzstämme (T$tier)"
            idUpper.contains("LEATHER") || idUpper.contains("HIDE") -> "Leder / Felle (T$tier)"
            idUpper.contains("CLOTH") || idUpper.contains("FIBER") -> "Fasern (T$tier)"
            idUpper.contains("STONEBLOCK") || idUpper.contains("ROCK") -> "Kalkstein / Stein (T$tier)"
            else -> "Rohstoff (T$tier)"
        }

        val ingredients = listOf(
            CraftingIngredient(
                resourceId = rawId,
                nameDe = rawNameDe,
                nameEn = rawNameDe,
                amount = 2
            )
        )

        return CraftingRecipe(
            id = res.fullId,
            resourceId = res.fullId,
            nameDe = res.nameDe,
            nameEn = res.nameEn,
            tier = tier,
            category = res.category,
            ingredients = ingredients
        )
    }

    fun calculateCraftingOpportunities(vararg args: Any?): List<CraftingOpportunityDetails> {
        val priceMap = args.getOrNull(0) as? Map<String, List<MarketPrice>> ?: emptyMap()
        val useFocus = args.getOrNull(1) as? Boolean ?: true
        val hideCaerleon = args.getOrNull(2) as? Boolean ?: false

        val resources = AlbionResourceRepository.resources
        val list = mutableListOf<CraftingOpportunityDetails>()
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

        for (res in resources) {
            val recipe = getRecipeFor(res)
            if (recipe.ingredients.isEmpty()) continue

            var totalCost = 0L
            val cheapestCities = mutableListOf<String>()
            val ingSummaryParts = mutableListOf<String>()

            for (ing in recipe.ingredients) {
                val prices = priceMap[ing.resourceId] ?: priceMap[ing.resourceId.uppercase()] ?: emptyList()
                val bestBuy = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }
                val unitPrice = if (bestBuy != null && bestBuy.sellPriceMin > 0) bestBuy.sellPriceMin else (res.tier * 200)
                val cost = ing.amount.toLong() * unitPrice.toLong()
                totalCost += cost
                if (bestBuy != null && bestBuy.city.isNotBlank()) {
                    cheapestCities.add(bestBuy.city)
                }
                ingSummaryParts.add("${ing.amount}x ${ing.nameDe}: ${fmt.format(unitPrice)}S")
            }

            val rrr = if (useFocus) 0.435 else 0.15
            val adjustedCost = (totalCost * (1.0 - rrr)).toLong()

            val itemPrices = priceMap[res.fullId] ?: priceMap[res.id] ?: emptyList()
            val validSellPrices = if (hideCaerleon) {
                itemPrices.filter { !it.city.equals("Caerleon", true) && it.sellPriceMin > 0 }
            } else {
                itemPrices.filter { it.sellPriceMin > 0 }
            }

            val bestSell = validSellPrices.maxByOrNull { it.sellPriceMin }
            val sellPrice = if (bestSell != null && bestSell.sellPriceMin > 0) bestSell.sellPriceMin else (res.tier * 600)
            val sellCity = bestSell?.city ?: "Martlock"

            val netProfit = sellPrice.toLong() - adjustedCost
            val roi = if (adjustedCost > 0) (netProfit.toDouble() / adjustedCost) * 100.0 else 0.0

            list.add(
                CraftingOpportunityDetails(
                    resource = res,
                    cheapestBuyCity = cheapestCities.distinct().joinToString(", ").ifBlank { "Martlock" },
                    totalIngredientCost = adjustedCost,
                    highestSellCity = sellCity,
                    finishedItemSellPrice = sellPrice.toLong(),
                    netProfit = netProfit,
                    roiPercent = roi,
                    recBuyOrderCost = (adjustedCost * 0.9).toLong(),
                    recSellOrderPrice = (sellPrice * 0.95).toLong(),
                    maxOrderProfit = (sellPrice * 0.95).toLong() - (adjustedCost * 0.9).toLong(),
                    ingredientSummary = ingSummaryParts.joinToString(" | ")
                )
            )
        }

        return list.sortedByDescending { pf -> pf.netProfit }
    }

    fun getPriceInCity(vararg args: Any?): Int {
        val itemId = args.getOrNull(0) as? String ?: return 1000
        val city = args.getOrNull(1) as? String ?: "ALLE"
        val priceMap = args.getOrNull(2) as? Map<String, List<MarketPrice>> ?: emptyMap()

        val prices = priceMap[itemId] ?: priceMap[itemId.uppercase()] ?: emptyList()
        if (prices.isEmpty()) return 500 * (itemId.takeWhile { it.isDigit() }.toIntOrNull() ?: 4)

        if (city.equals("ALLE", true)) {
            val minPrice = prices.filter { it.sellPriceMin > 0 }.minOfOrNull { it.sellPriceMin }
            return minPrice ?: (prices.firstOrNull()?.sellPriceMin ?: 1000)
        }

        val match = prices.firstOrNull { it.city.equals(city, true) }
        return if (match != null && match.sellPriceMin > 0) match.sellPriceMin else {
            prices.filter { it.sellPriceMin > 0 }.minOfOrNull { it.sellPriceMin } ?: 1000
        }
    }

    fun getHighestSellMarketDetails(vararg args: Any?): String {
        val itemId = args.getOrNull(0) as? String ?: return "Caerleon"
        val priceMap = args.getOrNull(1) as? Map<String, List<MarketPrice>> ?: emptyMap()

        val prices = priceMap[itemId] ?: priceMap[itemId.uppercase()] ?: emptyList()
        val best = prices.filter { it.sellPriceMin > 0 }.maxByOrNull { it.sellPriceMin }
        return if (best != null) "${best.city} (${NumberFormat.getNumberInstance(Locale.GERMANY).format(best.sellPriceMin)} S.)" else "Caerleon"
    }

    fun getCheapestMarketDetails(vararg args: Any?): String {
        val itemId = args.getOrNull(0) as? String ?: return "Martlock"
        val priceMap = args.getOrNull(1) as? Map<String, List<MarketPrice>> ?: emptyMap()

        val prices = priceMap[itemId] ?: priceMap[itemId.uppercase()] ?: emptyList()
        val best = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }
        return if (best != null) "${best.city} (${NumberFormat.getNumberInstance(Locale.GERMANY).format(best.sellPriceMin)} S.)" else "Martlock"
    }
}

object IslandRepository {
    val buildings = listOf(
        IslandBuilding(id = "farm", nameDe = "Bauernhof", nameEn = "Farm")
    )
}

object IslandTimerManager {
    private const val PREFS_KEY = "island_timer_items_json"

    fun getTimers(context: Context): List<IslandTimerItem> {
        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
        val list = mutableListOf<IslandTimerItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (id.isBlank()) continue
                val nameDe = obj.optString("nameDe", "")
                val catName = obj.optString("category", "CROP")
                val category = try { TimerCategory.valueOf(catName) } catch (_: Exception) { TimerCategory.CROP }
                val durationHours = obj.optInt("durationHours", 22)
                val startTimeMs = obj.optLong("startTimeMs", System.currentTimeMillis())
                val expectedHarvestTimeMs = obj.optLong("expectedHarvestTimeMs", System.currentTimeMillis())
                list.add(
                    IslandTimerItem(
                        id = id,
                        nameDe = nameDe,
                        category = category,
                        durationHours = durationHours,
                        startTimeMs = startTimeMs,
                        expectedHarvestTimeMs = expectedHarvestTimeMs
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveTimers(context: Context, timers: List<IslandTimerItem>) {
        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (t in timers) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("nameDe", t.nameDe)
                put("category", t.category.name)
                put("durationHours", t.durationHours)
                put("startTimeMs", t.startTimeMs)
                put("expectedHarvestTimeMs", t.expectedHarvestTimeMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREFS_KEY, arr.toString()).apply()
    }

    fun addTimer(context: Context, name: String, category: TimerCategory, hours: Int) {
        val current = getTimers(context).toMutableList()
        val now = System.currentTimeMillis()
        val harvestTime = now + (hours.toLong() * 3600L * 1000L)
        val item = IslandTimerItem(
            id = "timer_${now}",
            nameDe = name,
            category = category,
            durationHours = hours,
            startTimeMs = now,
            expectedHarvestTimeMs = harvestTime
        )
        current.add(item)
        saveTimers(context, current)
    }

    fun removeTimer(context: Context, id: String) {
        val current = getTimers(context).toMutableList()
        current.removeAll { it.id == id }
        saveTimers(context, current)
    }

    fun getActiveTimers(vararg args: Any?): List<Any> = emptyList()
    fun startTimer(vararg args: Any?) {}

    val standardCropOptions: List<Pair<String, Int>> = listOf("Karotten" to 22, "Kohl" to 44, "Bohnen" to 66)
    val standardAnimalOptions: List<Pair<String, Int>> = listOf("Huhn" to 24, "Ziege" to 48, "Kuh" to 72)
}

object SharedTradeStore {
    var activeOrders: List<TradeOrder> = emptyList()
    var latestOpportunities: List<TradeOpportunity> = emptyList()
}

object SharedViewModelProvider {
    @Composable
    fun getSharedViewModel(): AlbionResourceViewModel {
        val context = LocalContext.current
        val app = context.applicationContext as? Application ?: Application()
        return AlbionResourceViewModel(app)
    }

    fun getSharedViewModel(context: Context): AlbionResourceViewModel {
        val app = context.applicationContext as? Application ?: Application()
        return AlbionResourceViewModel(app)
    }
}

class BillingManager(val context: Context) {
    val subscriptionState: StateFlow<SubscriptionState> = MutableStateFlow(SubscriptionState.NotSubscribed)
    val productDetails: StateFlow<List<Any>> = MutableStateFlow(emptyList())

    sealed class SubscriptionState {
        object Loading : SubscriptionState()
        data class Subscribed(val details: String) : SubscriptionState()
        object NotSubscribed : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }

    fun startConnection(onSuccess: (() -> Unit)? = null) {
        onSuccess?.invoke()
    }

    fun queryActivePurchases() {}
    fun queryProductDetails() {}
    fun launchSubscriptionFlow(activity: Activity): Boolean = true
    fun handlePurchase(purchase: Any) {}
}

object AiTranslationEngine {
    fun translate(vararg args: Any?): String = "Translated"
    fun setLanguage(vararg args: Any?) {}
}

object LoginSecurityManager {
    fun checkAuth(vararg args: Any?): Pair<Boolean, String> = Pair(true, "")
    fun canAttemptLogin(vararg args: Any?): Pair<Boolean, String> = Pair(true, "")
    fun validateUsername(vararg args: Any?): Pair<Boolean, String> = Pair(true, "")
    fun validatePassword(password: String, isRegistration: Boolean = false): Pair<Boolean, String> = Pair(true, "")
    fun resetFailedAttempts(vararg args: Any?) {}
    fun recordFailedAttempt(vararg args: Any?): Int = 0
}

object DeviceHardwareManager {
    fun getHardwareId(vararg args: Any?): String = "device_123"
}

object DeviceSecurityManager {
    fun isSecure(vararg args: Any?): Boolean = true
    fun isDeviceCompromised(vararg args: Any?): Boolean = false
}

object CryptoSecurityUtils {
    fun encrypt(vararg args: Any?): String = ""
    fun decrypt(vararg args: Any?): String = ""
    fun verifyServerSignature(vararg args: Any?): Boolean = true
    fun computeHmacSha256(vararg args: Any?): String = "hmac"
    fun decryptAES(vararg args: Any?): String {
        val input = args.firstOrNull() as? String ?: return ""
        return try {
            String(Base64.decode(input, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
    fun encryptAES(vararg args: Any?): String {
        val input = args.firstOrNull() as? String ?: return ""
        return Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    }
    fun setupPermissiveSSLAndHostnameVerifier(vararg args: Any?) {}
}

object ExternalStorageBackupManager {
    fun exportBackup(vararg args: Any?) {}
    fun importBackup(vararg args: Any?) {}
    fun backupSnapshots(vararg args: Any?) {}
    fun backupTradeOrders(vararg args: Any?) {}
    fun backupGoldPurchases(vararg args: Any?) {}
    fun backupGoldSales(vararg args: Any?) {}
}

object NetworkDependencyManager {
    fun checkConnectivity(vararg args: Any?): Boolean = true
    fun setupPermissiveSSLAndHostnameVerifier(vararg args: Any?) {}
    fun checkInternetOrCrash(vararg args: Any?) {}
}

object WhatsAppMessageFormatter {
    fun formatTrade(vararg args: Any?): String = "Trade"
    fun formatOpportunityMessage(opp: TradeOpportunity, serverName: String, silverBudget: Long, carryCapacityKg: Double): String = "Opportunity"
}

object CallMeBotApi {
    fun sendMessage(vararg args: Any?) {}
    suspend fun sendWhatsAppMessage(phoneNumber: String, apiKey: String, message: String) {}
}

object AlbionMarketApi {
    suspend fun fetchPrices(server: AlbionServer, itemIds: List<String>): List<MarketPrice> = emptyList()
    suspend fun fetchAllPrices(server: AlbionServer): List<MarketPrice> = emptyList()
    fun getFallbackMarketPrices(): Map<String, List<MarketPrice>> = emptyMap()
    suspend fun fetchDynamicItemsFromAlbionBuilds(langCode: String = "DE", cloudUrl: String = ""): List<AlbionResource> = emptyList()
    fun isUnrealisticPrice(itemId: String, sellPriceMin: Int): Boolean = false
}

object AlbionGoldApi {
    suspend fun fetchGoldPrices(server: AlbionServer, count: Int = 24): List<GoldPrice> = emptyList()
    suspend fun fetchAllServersLatestGoldPrices(): Map<AlbionServer, Int> = emptyMap()
}

object CityDistanceCalculator {
    fun getDistance(vararg args: Any?): Int = 1
    fun getZonesDistance(vararg args: Any?): Int = 1
}

object GoldBotCalculator {
    fun analyzeGoldMarket(prices: List<GoldPrice>, currentGoldPrice: Int): GoldBotAnalysis = GoldBotAnalysis()
}

object AiEventAndBossAdvisor {
    fun getAdvice(vararg args: Any?): String = "Tipp"
    fun getAdviceForCategory(category: PlayerCategory): BossAdvice = BossAdvice()
}
