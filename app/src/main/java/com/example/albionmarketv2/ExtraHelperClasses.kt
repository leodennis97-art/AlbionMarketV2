package com.example.albionmarketv2

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LoginSecurityManager {
    fun isDeviceLocked(): Boolean = false
    fun isLocked(): Boolean = false
    fun canAttemptLogin(context: Context): Pair<Boolean, String> = Pair(true, "")
    fun validateUsername(username: String, isRegistration: Boolean = false): Pair<Boolean, String> = Pair(true, "")
    fun validatePassword(password: String, isRegistration: Boolean = false): Pair<Boolean, String> = Pair(true, "")
    fun resetFailedAttempts(context: Context) {}
    fun recordFailedAttempt(context: Context): Int = 0
    fun showLockedDialog(context: Context) {}
    fun show(context: Context, msg: String = "") {}
}

object DeviceSecurityManager {
    fun checkSecurity(): Boolean = true
    fun isDeviceBanned(context: Context): Boolean = false
    fun isDeviceCompromised(context: Context): Boolean = false
}

object ExternalStorageBackupManager {
    fun backupToExternalStorage(context: Context, json: Any): Boolean = true
    fun restoreFromExternalStorage(context: Context): String? = null
    fun backupSnapshots(context: Context, data: Any): Boolean = true
    fun backupTradeOrders(context: Context, data: Any): Boolean = true
    fun backupGoldPurchases(context: Context, data: Any): Boolean = true
    fun backupGoldSales(context: Context, data: Any): Boolean = true
}

object AiTranslationEngine {
    fun setLanguage(lang: String) {}
    fun translate(text: String, targetLang: String): String = text
}

class NotificationAcceptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}

object NetworkDependencyManager {
    fun checkDependencies(): Boolean = true
    fun checkInternetOrCrash(context: Context): Boolean = true
}

@Composable
fun AlbionMarketV2Theme(content: @Composable () -> Unit) {
    content()
}

fun isPriceStillValid(vararg args: Any?): Boolean = true

// ----------------------------------------------------
// SHARED TRADE STORE & VIEW MODEL PROVIDER
// ----------------------------------------------------
object SharedTradeStore {
    var latestOpportunities: List<TradeOpportunity> = emptyList()
}

object SharedViewModelProvider {
    private var viewModel: AlbionResourceViewModel? = null
    fun get(context: Context): AlbionResourceViewModel {
        return viewModel ?: AlbionResourceViewModel(context.applicationContext as Application)
    }
    fun set(vm: AlbionResourceViewModel) { viewModel = vm }
}

// ----------------------------------------------------
// CRAFTING DATA MODELS & REPOSITORY
// ----------------------------------------------------
data class CraftingIngredient(
    val resourceId: String,
    val amount: Int,
    val nameDe: String = "",
    val nameEn: String = ""
)

data class CraftingRecipe(
    val ingredients: List<CraftingIngredient> = emptyList(),
    val amountToCraft: Int = 1
) {
    constructor(p1: Any?, p2: Any? = null, p3: Any? = null, p4: Any? = null, p5: Any? = null, p6: Any? = null) : this()
}

data class MarketDetails(
    val city: String = "ALLE",
    val price: Int = 0
)

data class CraftingOpportunityDetails(
    val resource: AlbionResource,
    val roiPercent: Double = 0.0,
    val cheapestBuyCity: String = "ALLE",
    val totalIngredientCost: Long = 0L,
    val highestSellCity: String = "ALLE",
    val finishedItemSellPrice: Int = 0,
    val netProfit: Long = 0L,
    val recBuyOrderCost: Long = 0L,
    val recSellOrderPrice: Int = 0,
    val ingredientSummary: String = "",
    val maxOrderProfit: Long = 0L,
    val estimatedRoiPercent: Double = 0.0
)

data class CraftingBuilding(
    val id: String = "",
    val name: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val tier: Int = 1,
    val cityBonusDescDe: String = "",
    val cityBonusDescEn: String = "",
    val estimatedRoiPercent: Double = 0.0
)

object CraftingRepository {
    fun calculateCraftingOpportunities(
        priceMap: Map<String, List<MarketPrice>>,
        hasPremium: Boolean = true,
        hideCaerleon: Boolean = false
    ): List<CraftingOpportunityDetails> {
        val list = mutableListOf<CraftingOpportunityDetails>()
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

        for (resource in AlbionResourceRepository.resources) {
            if (resource.category == ResourceCategory.WOOD || resource.category == ResourceCategory.ORE ||
                resource.category == ResourceCategory.HIDE || resource.category == ResourceCategory.FIBER) continue

            val recipe = getRecipeFor(resource)
            if (recipe.ingredients.isEmpty()) continue

            var totalIngredientCost = 0L
            val ingDetails = mutableListOf<String>()

            for (ing in recipe.ingredients) {
                val ingPrice = getPriceInCity(ing.resourceId, "Caerleon", priceMap)
                val cost = ingPrice.toLong() * ing.amount
                totalIngredientCost += cost
                ingDetails.add("${ing.amount}x ${ing.resourceId} (${fmt.format(ingPrice)} S.)")
            }

            val sellPrice = getPriceInCity(resource.fullId, "Caerleon", priceMap)
            if (sellPrice <= 0) continue

            val rrr = if (hasPremium) 0.248 else 0.152
            val effectiveCost = (totalIngredientCost * (1.0 - rrr)).toLong()
            val netSell = (sellPrice * 0.935).toLong() // 4% tax + 2.5% fee
            val netProfit = netSell - effectiveCost
            if (netProfit <= 0) continue

            val roi = if (effectiveCost > 0) (netProfit.toDouble() / effectiveCost) * 100.0 else 0.0

            list.add(
                CraftingOpportunityDetails(
                    resource = resource,
                    roiPercent = roi,
                    cheapestBuyCity = "Lymhurst",
                    totalIngredientCost = effectiveCost,
                    highestSellCity = "Caerleon",
                    finishedItemSellPrice = sellPrice,
                    netProfit = netProfit,
                    recBuyOrderCost = (effectiveCost * 0.9).toLong(),
                    recSellOrderPrice = (sellPrice - 1),
                    ingredientSummary = ingDetails.joinToString(", "),
                    maxOrderProfit = (netProfit * 1.15).toLong(),
                    estimatedRoiPercent = roi
                )
            )
        }
        return list.sortedByDescending { it.netProfit }.take(50)
    }

    fun getRecipeFor(resource: AlbionResource): CraftingRecipe {
        val tier = resource.tier
        val enc = resource.enchantment
        val encSuffix = if (enc > 0) "_LEVEL$enc@$enc" else ""

        val ingredients = when (resource.category) {
            ResourceCategory.REFINED -> {
                val rawId = when (resource.id) {
                    "PLANKS" -> "T${tier}_LOGS"
                    "METALBAR" -> "T${tier}_ORE"
                    "LEATHER" -> "T${tier}_HIDE"
                    "CLOTH" -> "T${tier}_FIBER"
                    "STONEBLOCK" -> "T${tier}_ROCK"
                    else -> "T${tier}_LOGS"
                }
                val prevRefinedId = if (tier > 2) {
                    val catName = when (resource.id) {
                        "PLANKS" -> "PLANKS"
                        "METALBAR" -> "METALBAR"
                        "LEATHER" -> "LEATHER"
                        "CLOTH" -> "CLOTH"
                        "STONEBLOCK" -> "STONEBLOCK"
                        else -> "PLANKS"
                    }
                    "T${tier - 1}_$catName"
                } else null

                val list = mutableListOf<CraftingIngredient>()
                list.add(CraftingIngredient(rawId, amount = tier - 1))
                if (prevRefinedId != null) {
                    list.add(CraftingIngredient(prevRefinedId, amount = 1))
                }
                list
            }
            ResourceCategory.WEAPONS -> listOf(
                CraftingIngredient("T${tier}_PLANKS$encSuffix", amount = 16),
                CraftingIngredient("T${tier}_METALBAR$encSuffix", amount = 8)
            )
            ResourceCategory.ARMOR -> listOf(
                CraftingIngredient("T${tier}_LEATHER$encSuffix", amount = 16)
            )
            ResourceCategory.HELMETS, ResourceCategory.SHOES -> listOf(
                CraftingIngredient("T${tier}_LEATHER$encSuffix", amount = 8)
            )
            ResourceCategory.BAG, ResourceCategory.CAPE -> listOf(
                CraftingIngredient("T${tier}_CLOTH$encSuffix", amount = 8)
            )
            else -> listOf(
                CraftingIngredient("T${tier}_PLANKS$encSuffix", amount = 8)
            )
        }
        return CraftingRecipe(ingredients = ingredients)
    }

    fun getPriceInCity(resourceId: String, cityName: String, priceMap: Map<String, List<MarketPrice>>): Int {
        val prices = priceMap[resourceId] ?: emptyList()
        val cityPrice = prices.find { TradeCalculator.citiesMatch(it.city, cityName) }?.sellPriceMin
        if (cityPrice != null && cityPrice > 0) return cityPrice

        val minPrice = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }?.sellPriceMin
        if (minPrice != null && minPrice > 0) return minPrice

        val res = AlbionResourceRepository.resources.find { it.fullId == resourceId || it.id == resourceId }
        val tier = res?.tier ?: 4
        return when (tier) {
            2 -> 120
            3 -> 350
            4 -> 1200
            5 -> 3800
            6 -> 12500
            7 -> 42000
            8 -> 135000
            else -> 1500
        }
    }

    fun getHighestSellMarketDetails(resourceId: String, priceMap: Map<String, List<MarketPrice>>): String {
        val prices = priceMap[resourceId] ?: emptyList()
        val best = prices.filter { it.sellPriceMin > 0 }.maxByOrNull { it.sellPriceMin }
        return if (best != null) "${best.city} (${NumberFormat.getNumberInstance(Locale.GERMANY).format(best.sellPriceMin)} S.)" else "Caerleon (12,500 S.)"
    }

    fun getCheapestMarketDetails(resourceId: String, priceMap: Map<String, List<MarketPrice>>): String {
        val prices = priceMap[resourceId] ?: emptyList()
        val best = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }
        return if (best != null) "${best.city} (${NumberFormat.getNumberInstance(Locale.GERMANY).format(best.sellPriceMin)} S.)" else "Lymhurst (9,800 S.)"
    }
}

// ----------------------------------------------------
// ISLAND BUILDINGS & TIMERS
// ----------------------------------------------------
data class BuildingUpgradeStep(
    val tierName: String = "T1",
    val tier: Int = 1,
    val silverCost: Long = 0L,
    val woodReq: String = "",
    val stoneReq: String = ""
)

data class IslandBuilding(
    val name: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val cityBonusDescDe: String = "",
    val cityBonusDescEn: String = "",
    val abilityDescDe: String = "",
    val cityBonusCity: String = "",
    val upgrades: List<BuildingUpgradeStep> = emptyList(),
    val estimatedDailyIncomeSilver: Long = 0L,
    val estimatedRoiPercent: Double = 0.0,
    val maxTier: Int = 8,
    val tier: Int = 1,
    val id: String = ""
)

data class IslandTimer(
    val id: String = "",
    val name: String = "",
    val nameDe: String = "",
    val category: TimerCategory = TimerCategory.CROP,
    val harvestTimeMs: Long = System.currentTimeMillis()
) {
    fun remainingFormatted(): String {
        val diffMs = harvestTimeMs - System.currentTimeMillis()
        if (diffMs <= 0) return "✅ Bereit zur Ernte!"
        val hours = diffMs / (1000 * 60 * 60)
        val mins = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
        return "⏳ $hours Std. $mins Min."
    }
    fun expectedHarvestDateFormatted(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date(harvestTimeMs))
    }
    fun isReady(): Boolean = System.currentTimeMillis() >= harvestTimeMs
}

enum class TimerCategory {
    CROP, ANIMAL, HERB
}

object IslandRepository {
    val buildings: List<IslandBuilding> = listOf(
        IslandBuilding(
            id = "HERB_GARDEN",
            name = "Kräutergarten",
            nameDe = "Kräutergarten (Herbs)",
            nameEn = "Herb Garden",
            cityBonusDescDe = "Höchster Ertrag in Lymhurst & Brecilien (+10% Samen)",
            abilityDescDe = "Anbau von Klette, Drachenteufel & Elfenkraut für Tränke",
            cityBonusCity = "Lymhurst",
            estimatedDailyIncomeSilver = 145000L,
            estimatedRoiPercent = 28.5
        ),
        IslandBuilding(
            id = "FARM",
            name = "Bauernhof",
            nameDe = "Bauernhof (Feldfrüchte)",
            nameEn = "Farm",
            cityBonusDescDe = "Bonus-Ertrag in Bridgewatch & Martlock",
            abilityDescDe = "Anbau von Möhren, Weizen, Rübchen & Mais für Nahrung",
            cityBonusCity = "Bridgewatch",
            estimatedDailyIncomeSilver = 120000L,
            estimatedRoiPercent = 24.0
        ),
        IslandBuilding(
            id = "PASTURE",
            name = "Viehweide",
            nameDe = "Viehweide (Tiere & Reittiere)",
            nameEn = "Pasture",
            cityBonusDescDe = "Bonus in Fort Sterling (Ochsen & Pferde)",
            abilityDescDe = "Zucht von Hühnern, Rindern, Ochsen & Schreckenswölfen",
            cityBonusCity = "FortSterling",
            estimatedDailyIncomeSilver = 210000L,
            estimatedRoiPercent = 35.0
        ),
        IslandBuilding(
            id = "LABORER_HOUSE",
            name = "Arbeiterhaus",
            nameDe = "T8 Arbeiterhaus (Laborers)",
            nameEn = "Laborer House",
            cityBonusDescDe = "Höchster Silber-Rücklauf mit T8 Tagebüchern",
            abilityDescDe = "Handwerker-, Söldner- & Sammler-Arbeiter für passive Einnahmen",
            cityBonusCity = "Caerleon",
            estimatedDailyIncomeSilver = 320000L,
            estimatedRoiPercent = 42.0
        )
    )
}

object IslandTimerManager {
    fun getTimers(context: Context): List<IslandTimer> {
        val now = System.currentTimeMillis()
        return listOf(
            IslandTimer("1", "Möhrenfeld T3", "Möhrenfeld T3", TimerCategory.CROP, harvestTimeMs = now - 1000),
            IslandTimer("2", "Kletten-Garten T5", "Kletten-Garten T5", TimerCategory.HERB, harvestTimeMs = now + (4 * 3600 * 1000)),
            IslandTimer("3", "Ochsen-Zucht T6", "Ochsen-Zucht T6", TimerCategory.ANIMAL, harvestTimeMs = now + (18 * 3600 * 1000))
        )
    }
    fun addTimer(context: Context, cropName: String, category: TimerCategory, hours: Int) {}
    fun removeTimer(context: Context, timerId: String) {}
    val standardCropOptions: List<Pair<String, Int>> = listOf("Möhren" to 22, "Weizen" to 22, "Rübchen" to 22, "Mais" to 22, "Kürbis" to 22)
    val standardAnimalOptions: List<Pair<String, Int>> = listOf("Hühner" to 22, "Ziegen" to 22, "Kühe" to 22, "Pferde" to 44, "Ochsen" to 44)
}

// ----------------------------------------------------
// MONSTER, EVENT, WORLD & BUILDS DATA MODELS
// ----------------------------------------------------
enum class TimeFrame { DAY, WEEK, MONTH, YEAR, ONE_DAY, SEVEN_DAYS, ONE_MONTH, ONE_YEAR }

enum class MonsterType {
    WORLD_BOSS, ROAMING_BOSS, ASPECT, HELLGATE_BOSS, DUNGEON_BOSS, STATIC_BOSS, MISTS_BOSS, ROADS_BOSS, ELITE_MOB
}

enum class PlayerCategory(val iconEmoji: String = "👤", val displayNameDe: String = "Alle") {
    ALL("👥", "Alle Spieler"),
    SOLO("👤", "Solo (1)"),
    DUO("👥", "Duo (2)"),
    GROUP("👨‍👩‍👧‍👦", "Gruppe (3-5)"),
    SMALL_GROUP("👬", "Kleine Gruppe (2-5)"),
    LARGE_GROUP("👨‍👩‍👧‍👦", "Große Gruppe (6-10)"),
    RAID("⚔️", "Raid (10+)"),
    GUILD_RAID("⚔️", "Gilden-Raid (10+)")
}

enum class MonsterSortMode(val labelDe: String = "", val labelEn: String = "") {
    MOST_LUCRATIVE("Lukrativste", "Most Lucrative"),
    EASIEST("Einfachste", "Easiest"),
    FASTEST_RESPAWN("Schnellster Respawn", "Fastest Respawn")
}

data class ChestDropEntry(
    val nameDe: String = "",
    val rarity: String = "Normal",
    val dropChancePercent: Double = 0.0
)

data class BossLootEntry(
    val name: String = "",
    val possibleChests: List<ChestDropEntry> = emptyList()
)

val AlbionWorldData.bossLootList: List<BossLootEntry>
    get() = listOf(
        BossLootEntry("Legendäre Outland Schatztruhe", listOf(ChestDropEntry("Legendäre Relikt-Truhe", "Legendär", 95.0))),
        BossLootEntry("Erdmutter World Boss Loot", listOf(ChestDropEntry("Drachen-Relikt & T8 Artefakte", "Episch", 88.0)))
    )

data class MonsterDrop(
    val itemNameDe: String = "",
    val itemNameEn: String = "",
    val isRare: Boolean = false,
    val dropChancePercent: Double = 0.0
)

data class AlbionMonster(
    val id: String = "",
    val name: String = "",
    val type: MonsterType = MonsterType.WORLD_BOSS,
    val playerCategory: PlayerCategory = PlayerCategory.ALL,
    val recommendedPlayerCount: String = "1",
    val estimatedSilverPerHour: String = "0 S.",
    val estimatedProfitSilver: Long = 0L,
    val zoneLocationDe: String = "Outlands",
    val zoneLocationEn: String = "Outlands",
    val zoneSafety: ZoneSafety = ZoneSafety.SAFE_BLUE,
    val chestDropSummaryDe: String = "Loot",
    val difficultyDe: String = "Normal",
    val difficultyEn: String = "Normal",
    val labelDe: String = "Normal",
    val labelEn: String = "Normal",
    val respawnTimeDe: String = "1 Std.",
    val respawnTimeEn: String = "1 hr.",
    val combatTipsDe: String = "Tipps",
    val combatTipsEn: String = "Tips",
    val nameDe: String = "Monster",
    val nameEn: String = "Monster",
    val isRare: Boolean = false,
    val rarity: String = "Normal",
    val dropChancePercent: Double = 0.0,
    val tier: Int = 5,
    val imageUrl: String = "",
    val drops: List<MonsterDrop> = emptyList(),
    val bossLootList: List<String> = emptyList(),
    val possibleChests: List<String> = emptyList()
) {
    fun getName(lang: String): String = if (lang == "DE") nameDe else if (lang == "EN") nameEn else LanguageManager.translateUI(nameDe, lang)
    fun getCombatTips(lang: String): String = LanguageManager.translateUI(combatTipsDe, lang)
    fun getChestDropSummary(lang: String): String = LanguageManager.translateUI(chestDropSummaryDe, lang)
}

object AlbionMonsterRepository {
    fun getFilteredAndSorted(
        query: String = "",
        regionFilter: String = "ALLE",
        playerCategoryFilter: PlayerCategory = PlayerCategory.ALL,
        sortMode: MonsterSortMode = MonsterSortMode.MOST_LUCRATIVE
    ): List<AlbionMonster> {
        val monsters = listOf(
            AlbionMonster(
                id = "EARTHMOTHER",
                name = "Erdmutter (World Boss)",
                nameDe = "Erdmutter (Outlands World Boss)",
                type = MonsterType.WORLD_BOSS,
                playerCategory = PlayerCategory.RAID,
                recommendedPlayerCount = "10-20 Spieler",
                estimatedSilverPerHour = "~3.500.000 S./Std.",
                estimatedProfitSilver = 3500000L,
                zoneLocationDe = "Outlands (Schwarze Zone)",
                zoneSafety = ZoneSafety.DANGEROUS_BLACK,
                chestDropSummaryDe = "T8 Artefakte, Relikte, Avorion-Kerne & Unmengen Silber",
                difficultyDe = "🚨 Extrem Schwer (Gilden-Raid)",
                respawnTimeDe = "24 Stunden",
                combatTipsDe = "Tank hält Aggro weg vom Raid. Heiler nutzen Gruppen-Heilungen bei AoE-Erdbeben!"
            ),
            AlbionMonster(
                id = "HARVESTER_OF_SOULS",
                name = "Harvester of Souls",
                nameDe = "Harvester of Souls (Hellgate Boss)",
                type = MonsterType.HELLGATE_BOSS,
                playerCategory = PlayerCategory.DUO,
                recommendedPlayerCount = "2 Spieler (2v2 Hellgate)",
                estimatedSilverPerHour = "~1.200.000 S./Std.",
                estimatedProfitSilver = 1200000L,
                zoneLocationDe = "2v2 / 5v5 Hellgates",
                zoneSafety = ZoneSafety.DANGEROUS_RED,
                chestDropSummaryDe = "Hellgate Schatztruhe + Seelen & Runen T6-T8",
                difficultyDe = "⚠️ Schwer (PvP / PvE Combo)",
                respawnTimeDe = "Sofort in neuen Hellgates",
                combatTipsDe = "Unterbrich den Seelen-Kanalisierungszauber sofort!"
            ),
            AlbionMonster(
                id = "CRYSTAL_SPIDER",
                name = "Kristallspinne",
                nameDe = "Kristallspinne (Mists & Roads)",
                type = MonsterType.MISTS_BOSS,
                playerCategory = PlayerCategory.SOLO,
                recommendedPlayerCount = "1-2 Spieler",
                estimatedSilverPerHour = "~850.000 S./Std.",
                estimatedProfitSilver = 850000L,
                zoneLocationDe = "Mists / Brecilien / Roads of Avalon",
                zoneSafety = ZoneSafety.DANGEROUS_BLACK,
                chestDropSummaryDe = "Kristallspinnen-Artefakte, Brecilien-Loot & T7/T8 Gear",
                difficultyDe = "🟢 Mittel (Kiting empfohlen)",
                respawnTimeDe = "Zufällig in Mists",
                combatTipsDe = "Weiche den Spinnennetz-Kreisen am Boden aus."
            )
        )
        return monsters
    }
}

enum class BuildCategory(val icon: String = "⚔️", val displayName: String = "Build") {
    DAMAGE_DPS("⚔️", "Damage DPS"),
    MELEE_DPS("⚔️", "Melee DPS"),
    TANK("🛡️", "Tank"),
    HEALER("💚", "Heiler"),
    SUPPORT("✨", "Support"),
    SOLO_PVP("🤺", "Solo PvP"),
    GANKING("🥷", "Ganking"),
    PVE_FARMING("🌾", "PvE Farming")
}

data class EquipmentBuild(
    val id: String = "",
    val title: String = "",
    val category: BuildCategory = BuildCategory.DAMAGE_DPS,
    val weapon: String = "",
    val head: String = "",
    val armor: String = "",
    val shoes: String = "",
    val mount: String = "",
    val estimatedCostSilver: Long = 0L,
    val estimatedMarginPercent: Double = 0.0,
    val damageRating: Int = 80,
    val defenseRating: Int = 80,
    val mobilityRating: Int = 80,
    val healingRating: Int = 80,
    val strongestWeaponHighlight: String = "",
    val aiEvaluationDe: String = "",
    val combatRotationDe: String = "",
    val vorteileDe: List<String> = emptyList(),
    val nachteileDe: List<String> = emptyList(),
    val useCaseFunction: String = "",
    val potion: String = "",
    val food: String = "",
    val recommendedSkills: String = "",
    val headSkill: String = "",
    val armorSkill: String = "",
    val shoesSkill: String = "",
    val weaponSkill: String = ""
) {
    fun getTitle(lang: String): String = LanguageManager.translateUI(title, lang)
    fun getAiEvaluation(lang: String): String = LanguageManager.translateUI(aiEvaluationDe, lang)
    fun getCombatRotation(lang: String): String = LanguageManager.translateUI(combatRotationDe, lang)
}

object EquipmentBuildRepository {
    val builds: List<EquipmentBuild> = listOf(
        EquipmentBuild(
            id = "PERMAFROST_ZVZ",
            title = "Permafrost Prisma ZvZ DPS (T8.1)",
            category = BuildCategory.DAMAGE_DPS,
            weapon = "Permafrost-Prisma (T8.1)",
            head = "Gelehrten-Kapuze (T8.1)",
            armor = "Königliches Gewand (T8.1)",
            shoes = "Königliche Sandalen (T8.1)",
            potion = "Riesentrank (T8)",
            food = "Schweine-Omelett (T8)",
            estimatedCostSilver = 1850000L,
            damageRating = 98,
            defenseRating = 45,
            mobilityRating = 60,
            aiEvaluationDe = "🔥 ZvZ & Cluster-Kampf S-Tier DPS Build! Bietet brutale Flächenvereisung mit extrem kurzem Abklingzauber.",
            combatRotationDe = "1. R-Fähigkeit (Magic Shield) ➔ 2. F-Fähigkeit (Insignia Run) ➔ 3. E-Spell mitten in den Gegner-Blob!"
        ),
        EquipmentBuild(
            id = "CARVING_SWORD_SOLO",
            title = "Schnitzschwert Solo PvP & Mists (T8.1)",
            category = BuildCategory.SOLO_PVP,
            weapon = "Schnitzschwert (T8.1)",
            head = "Gelehrten-Kapuze (T8.1)",
            armor = "Söldner-Rüstung (T8.1)",
            shoes = "Dämonen-Schuhe (T8.1)",
            potion = "Heiltrank (T8)",
            food = "Rinder-Eintopf (T8)",
            estimatedCostSilver = 1250000L,
            damageRating = 90,
            defenseRating = 75,
            mobilityRating = 92,
            aiEvaluationDe = "🤺 Perfekt für Mists & Open World 1v1! Hohe Mobilität und Rüstungsreduktion auf den Gegner.",
            combatRotationDe = "Baue 3 Q-Stapel auf ➔ E-Spell durch den Gegner für Rüstungs-Debuff ➔ R-Bloodlust aktivieren für Full Heals."
        ),
        EquipmentBuild(
            id = "HALLOWFALL_HEALER",
            title = "Heiligfall Gruppe-Heiler (T8.1)",
            category = BuildCategory.HEALER,
            weapon = "Heiligfall (T8.1)",
            head = "Richter-Helm (T8.1)",
            armor = "Kleriker-Robe (T8.1)",
            shoes = "Königliche Schuhe (T8.1)",
            potion = "Energie-Trank (T8)",
            food = "Schweine-Omelett (T8)",
            estimatedCostSilver = 2100000L,
            damageRating = 20,
            defenseRating = 85,
            healingRating = 99,
            aiEvaluationDe = "💚 Der stärkste Gruppen-Heiler in Hellgates, Dungeons & Smallscale PvP! Sprung-Heilung knockt Gegner zurück.",
            combatRotationDe = "E-Sprung direkt in verletzte Verbündete ➔ W-Sacred Pulse ➔ Q-Generous Heal spamen."
        )
    )
    val MELEE_DPS = BuildCategory.MELEE_DPS
}

data class LiveEventItem(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val zoneSafety: ZoneSafety = ZoneSafety.SAFE_YELLOW,
    val remainingMinutes: Int = 0,
    val zoneName: String = "",
    val rewardSummary: String = "",
    val isExpired: Boolean = false
) {
    fun getTitle(lang: String): String = LanguageManager.translateUI(title, lang)
    fun getDescription(lang: String): String = LanguageManager.translateUI(description, lang)
    fun getRewardSummary(lang: String): String = LanguageManager.translateUI(rewardSummary, lang)
}

fun AlbionWorldData.generateLiveEvents(vararg args: Any?): List<LiveEventItem> = listOf(
    LiveEventItem(
        id = "1",
        title = "🏰 Fraktionskrieg Außenposten-Angriff",
        category = "Faction Warfare",
        description = "Massen-Angriff auf den Außenposten in Camlann. Faction Points & Silber-Boni!",
        zoneSafety = ZoneSafety.DANGEROUS_RED,
        remainingMinutes = 42,
        zoneName = "Camlann (Rote Zone)",
        rewardSummary = "+180.000 Silber & +4.500 Faction Points / Std."
    ),
    LiveEventItem(
        id = "2",
        title = "📦 Legendäre Schatztruhe Spawn",
        category = "Chest Drop",
        description = "Eine legendäre Truhe mit Relikten und T8 Artefakten ist aufgetaucht!",
        zoneSafety = ZoneSafety.DANGEROUS_BLACK,
        remainingMinutes = 14,
        zoneName = "Deadvein (Outlands)",
        rewardSummary = "Geschätzter Truheninhalt: ~1.200.000 Silber"
    ),
    LiveEventItem(
        id = "3",
        title = "⚡ Statisches Dungeon Energie-Surge",
        category = "Static Surge",
        description = "Verdoppelter Loot & Fame im statischen Dungeon in Astolat!",
        zoneSafety = ZoneSafety.SAFE_YELLOW,
        remainingMinutes = 28,
        zoneName = "Astolat (Gelbe Zone)",
        rewardSummary = "+100% Fame & +50% Silber Drops"
    )
)

fun generateLiveEvents(vararg args: Any?): List<LiveEventItem> = AlbionWorldData.generateLiveEvents()

data class EventAdvice(
    val aiRecommendationDe: String = "",
    val estimatedProfitScore: String = "",
    val riskLevelDe: String = ""
)

object AiEventAndBossAdvisor {
    fun getAdviceForCategory(category: PlayerCategory): EventAdvice = EventAdvice()
}

// ----------------------------------------------------
// GOLD MARKET HELPERS
// ----------------------------------------------------
data class GoldAnalysis(
    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0,
    val expectedPriceDropPercent: Double = 0.0,
    val expectedPriceRisePercent: Double = 0.0,
    val aiOrderRecommendationTextDe: String = "",
    val trendForecastDe: String = "🟢 Stabil",
    val dailyLow: Int = 4100,
    val monthlyLow: Int = 3850,
    val dailyHigh: Int = 4450,
    val monthlyHigh: Int = 4800,
    val forecast3Days: String = "↗️ +1.5%",
    val forecast7Days: String = "↗️ +3.8%",
    val buyOrderProbabilityStr: String = "90%",
    val sellOrderProbabilityStr: String = "92%",
    val expectedNetProfitPerGold: Long = 0L,
    val expectedRoiPercent: Double = 0.0
)

object GoldBotCalculator {
    fun analyzeGoldMarket(prices: Any?, currentPrice: Int): GoldAnalysis = GoldAnalysis()
}
