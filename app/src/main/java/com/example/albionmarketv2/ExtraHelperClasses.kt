package com.example.albionmarketv2

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
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
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF00A2FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF0369A1),
        onPrimaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFF38BDF8),
        onSecondary = Color.Black,
        tertiary = Color(0xFFF59E0B),
        background = Color(0xFF050B14),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF0F172A),
        onSurface = Color(0xFFF8FAFC),
        surfaceContainer = Color(0xFF1E293B),
        surfaceContainerHigh = Color(0xFF1E293B),
        outline = Color(0xFF334155),
        error = Color(0xFFEF4444)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
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
    val estimatedRoiPercent: Double = 0.0,
    // Add additional metrics for 1.3 High-End Veredelung and 1.4 Meister-Handwerk:
    val resourceReturnRate: Double = 0.152,
    val dailyVolume: Int = 0,
    val targetMarket: String = "" // "Königsstädte", "Black Market" or "Station Trading"
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
            cityBonusDescDe = "Höchster Ertrag in Lymhurst & Brecilien (+10% Samen-Rücklauf)",
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
            cityBonusDescDe = "Bonus-Ertrag in Bridgewatch & Martlock (+10% Pflanzen-Ertrag)",
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
            cityBonusDescDe = "Bonus in Fort Sterling & Thetford (Ochsen & Reittiere)",
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
            cityBonusDescDe = "Höchster Silber-Rücklauf mit T8 Tagebüchern in Caerleon & Hauptstädten",
            abilityDescDe = "Handwerker-, Söldner- & Sammler-Arbeiter für passive Einnahmen",
            cityBonusCity = "Caerleon",
            estimatedDailyIncomeSilver = 320000L,
            estimatedRoiPercent = 42.0
        ),
        IslandBuilding(
            id = "WARRIORS_FORGE",
            name = "Kriegerschmiede",
            nameDe = "Kriegerschmiede (Warrior's Forge)",
            nameEn = "Warrior's Forge",
            cityBonusDescDe = "Höchster Herstellungs-Bonus in Fort Sterling (+15% Material-Rückvergütung)",
            abilityDescDe = "Herstellung von Schwertern, Äxten, Streitkolben, Platte & Schilden",
            cityBonusCity = "FortSterling",
            estimatedDailyIncomeSilver = 280000L,
            estimatedRoiPercent = 38.0
        ),
        IslandBuilding(
            id = "MAGES_TOWER",
            name = "Magierturm",
            nameDe = "Magierturm (Mage's Tower)",
            nameEn = "Mage's Tower",
            cityBonusDescDe = "Höchster Herstellungs-Bonus in Caerleon & Lymhurst (+15% Magie-Rückvergütung)",
            abilityDescDe = "Herstellung von Feuer-, Frost-, Fluch-Stäben & Stoffrüstungen",
            cityBonusCity = "Caerleon",
            estimatedDailyIncomeSilver = 310000L,
            estimatedRoiPercent = 40.0
        ),
        IslandBuilding(
            id = "HUNTERS_LODGE",
            name = "Jägerhütte",
            nameDe = "Jägerhütte (Hunter's Lodge)",
            nameEn = "Hunter's Lodge",
            cityBonusDescDe = "Höchster Herstellungs-Bonus in Bridgewatch & Thetford (+15% Fernkampf-Bonus)",
            abilityDescDe = "Herstellung von Bögen, Armbrüsten, Dolchen, Speeren & Lederrüstung",
            cityBonusCity = "Bridgewatch",
            estimatedDailyIncomeSilver = 260000L,
            estimatedRoiPercent = 34.0
        ),
        IslandBuilding(
            id = "TOOLMAKER",
            name = "Werkstatt",
            nameDe = "Werkstatt (Toolmaker)",
            nameEn = "Toolmaker",
            cityBonusDescDe = "Höchster Bonus in Martlock (+15% Werkzeug-Rückvergütung)",
            abilityDescDe = "Herstellung von T8 Spitzhacken, Äxten, Angeln & Umhängen/Taschen",
            cityBonusCity = "Martlock",
            estimatedDailyIncomeSilver = 240000L,
            estimatedRoiPercent = 31.0
        ),
        IslandBuilding(
            id = "SMELTER",
            name = "Schmelze",
            nameDe = "Schmelze (Erz & Barren)",
            nameEn = "Smelter",
            cityBonusDescDe = "Höchste Erze-Veredelung in Thetford (+36.7% Barren-Rückvergütung)",
            abilityDescDe = "Veredelung von Kupfer, Eisen, Titanerz zu Metallbarren",
            cityBonusCity = "Thetford",
            estimatedDailyIncomeSilver = 290000L,
            estimatedRoiPercent = 37.5
        ),
        IslandBuilding(
            id = "TANNERY",
            name = "Gerberei",
            nameDe = "Gerberei (Leder-Veredelung)",
            nameEn = "Tannery",
            cityBonusDescDe = "Höchste Leder-Veredelung in Martlock (+36.7% Leder-Rückvergütung)",
            abilityDescDe = "Gerben von Tierhäuten zu feinstem T8 Leder",
            cityBonusCity = "Martlock",
            estimatedDailyIncomeSilver = 275000L,
            estimatedRoiPercent = 36.0
        ),
        IslandBuilding(
            id = "WEAVERY",
            name = "Weberei",
            nameDe = "Weberei (Stoff-Veredelung)",
            nameEn = "Weaver's Mill",
            cityBonusDescDe = "Höchste Stoff-Veredelung in Lymhurst (+36.7% Stoff-Rückvergütung)",
            abilityDescDe = "Weben von Baumwolle & Hanf zu T8 Stoffballen",
            cityBonusCity = "Lymhurst",
            estimatedDailyIncomeSilver = 265000L,
            estimatedRoiPercent = 33.5
        ),
        IslandBuilding(
            id = "LUMBERMILL",
            name = "Sägewerk",
            nameDe = "Sägewerk (Holz-Veredelung)",
            nameEn = "Lumbermill",
            cityBonusDescDe = "Höchste Holz-Veredelung in Fort Sterling (+36.7% Planken-Bonus)",
            abilityDescDe = "Verarbeitung von Birken-, Eichen- & Holzstämmen zu Planken",
            cityBonusCity = "FortSterling",
            estimatedDailyIncomeSilver = 285000L,
            estimatedRoiPercent = 36.8
        ),
        IslandBuilding(
            id = "STONE_MASON",
            name = "Steinmetz",
            nameDe = "Steinmetz (Stein-Veredelung)",
            nameEn = "Stone Mason",
            cityBonusDescDe = "Höchste Stein-Veredelung in Bridgewatch (+36.7% Blöcke-Bonus)",
            abilityDescDe = "Haauen von Feldsteinen zu T8 Steinblöcken für Gebäude",
            cityBonusCity = "Bridgewatch",
            estimatedDailyIncomeSilver = 230000L,
            estimatedRoiPercent = 30.0
        )
    )
}

object IslandTimerManager {
    private val inMemoryTimers = mutableListOf<IslandTimer>()
    private var isInitialized = false

    private fun ensureInit() {
        if (!isInitialized) {
            val now = System.currentTimeMillis()
            inMemoryTimers.add(IslandTimer("1", "Möhrenfeld T3", "Möhrenfeld T3", TimerCategory.CROP, harvestTimeMs = now - 1000))
            inMemoryTimers.add(IslandTimer("2", "Kletten-Garten T5", "Kletten-Garten T5", TimerCategory.HERB, harvestTimeMs = now + (4 * 3600 * 1000)))
            inMemoryTimers.add(IslandTimer("3", "Ochsen-Zucht T6", "Ochsen-Zucht T6", TimerCategory.ANIMAL, harvestTimeMs = now + (18 * 3600 * 1000)))
            isInitialized = true
        }
    }

    fun getTimers(context: Context): List<IslandTimer> {
        ensureInit()
        return inMemoryTimers.toList()
    }

    fun addTimer(context: Context, cropName: String, category: TimerCategory, hours: Int) {
        ensureInit()
        val newId = System.currentTimeMillis().toString()
        val harvestTimeMs = System.currentTimeMillis() + (hours * 3600 * 1000L)
        inMemoryTimers.add(IslandTimer(newId, cropName, cropName, category, harvestTimeMs = harvestTimeMs))
    }

    fun removeTimer(context: Context, timerId: String) {
        ensureInit()
        inMemoryTimers.removeAll { it.id == timerId }
    }

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
        val filtered = monsters.filter { 
            (playerCategoryFilter == PlayerCategory.ALL || it.playerCategory == playerCategoryFilter) &&
            (regionFilter == "ALLE" || it.zoneLocationDe.contains(regionFilter, ignoreCase = true)) &&
            (query.isBlank() || it.nameDe.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true))
        }

        return when (sortMode) {
            MonsterSortMode.MOST_LUCRATIVE -> filtered.sortedByDescending { it.estimatedProfitSilver }
            MonsterSortMode.EASIEST -> filtered.sortedBy { it.difficultyDe }
            MonsterSortMode.FASTEST_RESPAWN -> filtered.sortedBy { it.respawnTimeDe }
        }
    }
}

data class FactionItem(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val factionCity: String,
    val pointsCost: Int,
    val estimatedMarketPriceSilver: Long = 0L
)

data class LaborerJournal(
    val id: String,
    val nameDe: String,
    val tier: Int,
    val jobType: String, // e.g. "Gathering", "Crafting", "Mercenary"
    val emptyPriceSilver: Long = 0L,
    val fullPriceSilver: Long = 0L
) {
    val estimatedProfit: Long get() = fullPriceSilver - emptyPriceSilver
}

enum class BuildCategory(val icon: String = "⚔️", val displayName: String = "Build") {
    DAMAGE_DPS("⚔️", "Damage DPS"),
    MELEE_DPS("⚔️", "Melee DPS"),
    TANK("🛡️", "Tank"),
    HEALER("💚", "Heiler"),
    SUPPORT("✨", "Support"),
    SOLO_PVP("🤺", "Solo PvP"),
    GANKING("🥷", "Ganking"),
    PVE_FARMING("🌾", "PvE Farming"),
    SWORDS("🗡️", "Schwerter"),
    AXES("🪓", "Äxte"),
    MACES_HAMMERS("🔨", "Keulen & Hämmer"),
    DAGGER_SPEAR("🔱", "Dolche & Speere"),
    BOWS_CROSSBOWS("🏹", "Bögen & Armbrüste"),
    FIRE_FROST_STAFF("🔥", "Feuer & Frost"),
    CURSED_ARCANE_STAFF("🔮", "Fluch & Arkan"),
    HOLY_NATURE_STAFF("🌿", "Heilig & Natur"),
    SHAPESHIFTER("🐺", "Gestaltwandler")
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
        // S-TIER GOD BUILD 1: KINGMAKER ONE-SHOT BURST
        EquipmentBuild(
            id = "KINGMAKER_GOD_BURST",
            title = "👑 Kingmaker GOD Unbeatable Burst (T8.4 Masterwork)",
            category = BuildCategory.DAMAGE_DPS,
            weapon = "Königsmacher (T8.4 Masterwork)",
            head = "Stalker-Kapuze (T8.4)",
            armor = "Hellion-Rüstung (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            mount = "Schreckenswolf / Direwolf",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 4850000L,
            estimatedMarginPercent = 28.5,
            damageRating = 100,
            defenseRating = 90,
            mobilityRating = 95,
            healingRating = 85,
            strongestWeaponHighlight = "⚡ EXTREM-BURST: 3-Stack Heroic Charge + E-Knockup vernichtet Gegner-Blobs augenblicklich!",
            aiEvaluationDe = "👑 UNBESIEGBARE PVP & ZVZ MACHT: Höchster Instant-AoE-Schaden im gesamten Spiel! Bietet unschlagbare Armor-Shred Synergy mit Hellion-Aura für unendliche Lifesteal-Gefechte.",
            combatRotationDe = "1. 3x Q-Charge aufladen ➔ 2. Stalker-Kapuze D-Spell zünden (-35% Rüstung) ➔ 3. E-Mortal Leap in das Zielzentrum für 2200+ Instant Damage!",
            vorteileDe = listOf("Absoluter One-Shot Burst gegen leicht gerüstete Ziele", "Starker AoE Lifesteal durch Hellion-Rüstung", "Hohe Mobilität durch Heroic Charges"),
            nachteileDe = listOf("Hohe Anschaffungskosten", "Erfordert präzises Timing beim E-Mortal Leap")
        ),

        // S-TIER GOD BUILD 2: DEATHGIVERS MISTS EXECUTIONER
        EquipmentBuild(
            id = "DEATHGIVERS_MISTS_GOD",
            title = "☠️ Deathgivers GOD Unkillable Executioner (T8.4)",
            category = BuildCategory.SOLO_PVP,
            weapon = "Totengeber / Deathgivers (T8.4 Masterwork)",
            head = "Kultisten-Kapuze (T8.4)",
            armor = "Feen-Robe / Feenskult-Robe (T8.4)",
            shoes = "Söldner-Schuhe (T8.4)",
            mount = "Schneller Panther",
            potion = "Unsichtbarkeitstrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 6200000L,
            estimatedMarginPercent = 32.0,
            damageRating = 100,
            defenseRating = 85,
            mobilityRating = 100,
            healingRating = 75,
            strongestWeaponHighlight = "🗡️ PERMANENT-RESET: E-Ghost Blades setzt sich bei Treffern von hinten sofort zurück!",
            aiEvaluationDe = "🏆 UNGESCHLAGENER 1v1 MISTS GOD: Unendliche Invisibility-Resets durch Ghost Blades. Erlaubt ständiges Disengagen und Ausweichen von gegnerischen Cooldowns.",
            combatRotationDe = "1. Q-Assassin Stacks sammeln ➔ 2. W-Shadow Edge Dash ➔ 3. E-Ghost Blades von HINTEN ausführen für SOFORTIGEN E-RESET ➔ Wiederholen!",
            vorteileDe = listOf("1v1 Unbesiegbar bei korrekter Ghost Blades Reset Rotation", "Unbegrenzte Ausweichmöglichkeiten durch Invisible Flips", "Extremer Burst gegen Einzelziele"),
            nachteileDe = listOf("Sehr hoher Skill-Cap", "Fehlschlag des Reset macht anfällig")
        ),

        // S-TIER GOD BUILD 3: HALLOWFALL UNKILLABLE HEALER
        EquipmentBuild(
            id = "HALLOWFALL_UNKILLABLE_GOD",
            title = "💚 Hallowfall GOD Unkillable Support (T8.4)",
            category = BuildCategory.HEALER,
            weapon = "Heiligfall / Hallowfall (T8.4 Masterwork)",
            head = "Richter-Helm (T8.4)",
            armor = "Kleriker-Robe (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            mount = "Gepanzerter Bär",
            potion = "Energie-Trank (T8)",
            food = "Gänse-Omelett (T8.3)",
            estimatedCostSilver = 3900000L,
            estimatedMarginPercent = 24.0,
            damageRating = 50,
            defenseRating = 100,
            mobilityRating = 95,
            healingRating = 100,
            strongestWeaponHighlight = "✨ UNENDLICHE HEILUNG: E-Sacred Hop heilt 1500+ Leben und stößt Feinde zurück!",
            aiEvaluationDe = "💚 GOD-TIER HEILER: Hält selbst gegen 10+ Angreifer stand. Der E-Hop kombiniert Airborne-Knockup mit extremer Selbstheilung.",
            combatRotationDe = "1. Q-Generous Heal dauerspamen ➔ 2. Bei Crowd-Control Kleriker-Robe zünden ➔ 3. E-Sacred Hop direkt auf dich & Teammates zünden!",
            vorteileDe = listOf("Unübertroffene Gruppen- und Selbstheilung", "Knockup unterbricht gegnerische Zauber", "Extrem hoher Überlebenswert"),
            nachteileDe = listOf("Hoher Mana-Verbrauch ohne Omelett/Energietrank")
        ),

        // S-TIER GOD BUILD 4: DOUBLE-BLADED STAFF ONE-SHOT GANK
        EquipmentBuild(
            id = "DOUBLE_BLADED_GANK_GOD",
            title = "🥷 Double-Bladed Staff GOD Dismount Gank (T8.4)",
            category = BuildCategory.GANKING,
            weapon = "Zwiefacher Stab / Double-Bladed Staff (T8.4)",
            head = "Purpur-Kapuze / Fiend Cowl (T8.4)",
            armor = "Mascara / Dämonen-Rüstung (T8.4)",
            shoes = "Königliche Sandalen (T8.4)",
            mount = "Direwolf / Schreckenswolf",
            potion = "Gifttrank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3100000L,
            estimatedMarginPercent = 35.0,
            damageRating = 95,
            defenseRating = 80,
            mobilityRating = 100,
            healingRating = 50,
            strongestWeaponHighlight = "⚡ DISMOUNT-KING: Entmountet Reittiere in unter 2 Sekunden durch Verlangsamungs-Aura!",
            aiEvaluationDe = "🥷 EXTREMES GANKING: Kein Opfer entkommt diesem Build. Kombiniert Massen-Slow, Purge (Buff-Entfernung) und Dämonen-Reflekt.",
            combatRotationDe = "1. Gifttrank auf Reittier werfen ➔ 2. E-Overhead Slam zum Entmounten ➔ 3. Purging-Kapuze aktivieren, um Flucht-Buffs zu löschen!",
            vorteileDe = listOf("Maximale Reittier-Entmountungsrate", "Unschlagbare Verfolgungsreichweite", "Verhindert gegnerische Unsichtbarkeit"),
            nachteileDe = listOf("Geringe Rüstung in langgezogenen 1v1 Kämpfen")
        ),

        // S-TIER GOD BUILD 5: REALMBREAKER ARMOR SHREDDER
        EquipmentBuild(
            id = "REALMBREAKER_SHREDDER_GOD",
            title = "🪓 Realmbreaker GOD Mass Armor-Shredder (T8.3)",
            category = BuildCategory.MELEE_DPS,
            weapon = "Weltenbrecher / Realmbreaker (T8.3)",
            head = "Hellion-Kapuze (T8.3)",
            armor = "Hellion-Rüstung (T8.3)",
            shoes = "Königliche Schuhe (T8.3)",
            mount = "Kampf-Ochse",
            potion = "Riesentrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 5200000L,
            estimatedMarginPercent = 30.0,
            damageRating = 97,
            defenseRating = 82,
            mobilityRating = 70,
            healingRating = 60,
            strongestWeaponHighlight = "💥 -20% MAX-HP DEBUFF: Reduziert das maximale Leben aller getroffenen Ziele um 20%!",
            aiEvaluationDe = "🪓 UNERSÄTZLICH IN JEDER ZVZ: Der E-Aftershock spaltet den Boden und halbiert die gegnerischen HP-Balken für deinen Raid.",
            combatRotationDe = "1. Q-Adrenaline Boost zünden ➔ 2. E-Aftershock mitten in die gegnerische Hauptgruppe schlagen ➔ 3. Hellion Aura zünden & aufräumen!",
            vorteileDe = listOf("Reduziert das maximale Leben von bis zu 20 Gegnern gleichzeitig", "Extrem hoher Rüstungswert und Selbstheilung", "Perfekt für Gruppenkampf & ZvZ"),
            nachteileDe = listOf("Teure Waffe in der Anschaffung")
        ),

        // S-TIER GOD BUILD 6: CAMLANN BATTLEBOUND TANK
        EquipmentBuild(
            id = "CAMLANN_TANK_GOD",
            title = "🛡️ Camlann Black Hole GOD Tank (T8.3)",
            category = BuildCategory.TANK,
            weapon = "Camlann-Streitkolben (T8.3)",
            head = "Ritter-Helm (T8.3)",
            armor = "Richter-Rüstung (T8.3)",
            shoes = "Jäger-Schuhe (T8.3)",
            mount = "Gepanzerter Bär",
            potion = "Widerstandstrank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 4100000L,
            estimatedMarginPercent = 22.0,
            damageRating = 40,
            defenseRating = 100,
            mobilityRating = 60,
            healingRating = 30,
            strongestWeaponHighlight = "🧲 BLACK HOLE ENGAGE: Saugt den kompletten gegnerischen Raid auf einen einzelnen Pixel zusammen!",
            aiEvaluationDe = "🛡️ ULTRA ENGAGE TANK: Erzeugt ein unentrinnbares Schwarzes Loch. Sobald die Gegner zusammengezogen sind, folgt der sofortige Wipe durch deine Gilde.",
            combatRotationDe = "1. Ritter-Helm zünden (Immunität gegen Crowd Control) ➔ 2. E-Black Hole auf die gegnerische Backline schießen ➔ 3. W-Snare Charge zünden!",
            vorteileDe = listOf("Bester Engage-Mace im Spiel", "Maximale Crowd Control Immunität für dich selbst", "Unzerstörbarer Rüstungswert"),
            nachteileDe = listOf("Benötigt verlässliche Follow-Up DPS Teammates")
        ),

        // S-TIER GOD BUILD 7: LIGHT CROSSBOW EXPLOSIVE PVE
        EquipmentBuild(
            id = "LIGHT_CROSSBOW_PVE_GOD",
            title = "💣 Light Crossbow GOD Solo & HCE Speedclear (T8.3)",
            category = BuildCategory.PVE_FARMING,
            weapon = "Leichte Armbrust / Light Crossbow (T8.3)",
            head = "Kultisten-Kapuze (T8.3)",
            armor = "Druiden-Robe (T8.3)",
            shoes = "Königliche Sandalen (T8.3)",
            mount = "Reitpferd T8",
            potion = "Gifttrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 2800000L,
            estimatedMarginPercent = 40.0,
            damageRating = 100,
            defenseRating = 35,
            mobilityRating = 45,
            healingRating = 10,
            strongestWeaponHighlight = "💣 AUTO-RESET BOMB: Q-Reset Mechanik erlaubt 4x Auto-Bombing in unter 3 Sekunden!",
            aiEvaluationDe = "🌾 SCHNELLSTER PVE CLEAR IN ALBION: Löscht ganze Dungeon-Räume und T8 Bosse in Sekunden durch ununterbrochene Q-Bomb-Resets aus.",
            combatRotationDe = "1. Druiden-Robe aktivieren für +50% Schaden ➔ 2. Q-Bomb ➔ W-Knockback (Reset) ➔ Q-Bomb ➔ E-Explosive Bolt ➔ Q-Bomb!",
            vorteileDe = listOf("Höchster Single-Target & AoE PvE Schaden", "Sehr günstige Reparaturkosten im Verhältnis zum Output", "Perfekt für Solo Dungeons, Static & HCE"),
            nachteileDe = listOf("Wenig Mobilität bei feindlichen PK-Gankern")
        ),

        // S-TIER GOD BUILD 8: LIFECURSE SUPPORT DEBUFF
        EquipmentBuild(
            id = "LIFECURSE_SUPPORT_GOD",
            title = "✨ Lifecurse GOD Purge & Debuff Support (T8.3)",
            category = BuildCategory.SUPPORT,
            weapon = "Fluch des Lebens / Lifecurse Staff (T8.3)",
            head = "Ritter-Helm (T8.3)",
            armor = "Kleriker-Robe (T8.3)",
            shoes = "Söldner-Schuhe (T8.3)",
            mount = "Schneller Panther",
            potion = "Widerstandstrank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3600000L,
            estimatedMarginPercent = 26.0,
            damageRating = 50,
            defenseRating = 85,
            mobilityRating = 70,
            healingRating = 40,
            strongestWeaponHighlight = "🔮 AOE PURGE ZONE: Löscht sämtliche gegnerischen Buffs (Ritter-Helme, Sword-Charges, Heilschilde) augenblicklich!",
            aiEvaluationDe = "✨ ULTIMATIVER DEBUFF SUPPORT: Neutralisiert die stärksten Gegner-Spells im Vorfeld. Macht den gegnerischen Engage völlig nutzlos.",
            combatRotationDe = "1. Warten bis der Gegner engagt ➔ 2. E-Enfeeble Area direkt unter die Gegner legen (Löscht alle Gegner-Buffs & senkt deren Schaden um 50%)!",
            vorteileDe = listOf("Löscht gegnerische Schild- und Schadensbuffs", "Reduziert gegnerischen Schaden um 50%", "Wendet das Blatt in Unterzahl-Kämpfen"),
            nachteileDe = listOf("Erfordert perfektes Reaktions-Timing")
        ),

        // WEAPON SPECIFIC OP BUILDS
        EquipmentBuild(
            id = "CARVING_SWORD_SPECIAL",
            title = "🗡️ Carving Sword GOD (T8.4 Masterwork)",
            category = BuildCategory.SWORDS,
            weapon = "Schnitzschwert / Carving Sword (T8.4)",
            head = "Stalker-Kapuze (T8.4)",
            armor = "Hellion-Rüstung (T8.4)",
            shoes = "Dämonen-Schuhe (T8.4)",
            potion = "Heiltrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 9500000L,
            damageRating = 100,
            defenseRating = 80,
            mobilityRating = 95,
            strongestWeaponHighlight = "🗡️ ARMOR SHRED KING: E-Fearless Strike senkt gegnerische Rüstung um bis zu 45%!",
            aiEvaluationDe = "🗡️ UNFAIRE SCHWERT-DOMINANZ: Kombiniert unendliche Q-Bleeds mit exzellenter Durchschlagskraft.",
            combatRotationDe = "1. 3x Q-Charge ➔ 2. Stalker-Kapuze D ➔ 3. E-Fearless Strike durch das Ziel ➔ 4. R-Hellion Aura aktivieren!"
        ),
        EquipmentBuild(
            id = "BEAR_PAWS_SPECIAL",
            title = "🪓 Bear Paws GOD Execute (T8.4 Masterwork)",
            category = BuildCategory.AXES,
            weapon = "Bärenpranken / Bear Paws (T8.4)",
            head = "Jäger-Kapuze (T8.4)",
            armor = "Söldner-Rüstung (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            potion = "Gifttrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 12000000L,
            damageRating = 100,
            defenseRating = 75,
            mobilityRating = 95,
            strongestWeaponHighlight = "🪓 BLEEDING LEAP: Springt über Hindernisse und verursacht gewaltige Ticks ungereinigter Blutung!",
            aiEvaluationDe = "🪓 TOEDLICHE ÄXTE: Die Blutung geht durch Schilde hindurch und erzwingt das Ersticken des Gegners.",
            combatRotationDe = "1. W-Adrenaline ➔ 2. E-Razor Cut Sprung ➔ 3. Q-Spam + Söldner-Lifesteal!"
        ),
        EquipmentBuild(
            id = "GREAT_HAMMER_SPECIAL",
            title = "🔨 Great Hammer GOD Unkillable (T8.4)",
            category = BuildCategory.MACES_HAMMERS,
            weapon = "Großer Hammer / Great Hammer (T8.4)",
            head = "Wächter-Helm (T8.4)",
            armor = "Richter-Rüstung (T8.4)",
            shoes = "Söldner-Schuhe (T8.4)",
            potion = "Widerstandstrank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 7800000L,
            damageRating = 60,
            defenseRating = 100,
            mobilityRating = 90,
            strongestWeaponHighlight = "🔨 TACKLE CHARGE: 6-Sekunden Crowd-Control Lockout mit unendlich Stun!",
            aiEvaluationDe = "🔨 UNKILLABLE CROWD CONTROL: Der Hammer stuns, stört und unterbricht jeden gegnerischen Cast.",
            combatRotationDe = "1. E-Tackle ➔ 2. W-Iron Will ➔ 3. Richter Rüstung zünden!"
        ),
        EquipmentBuild(
            id = "DAYBREAKER_SPEAR_SPECIAL",
            title = "🔱 Daybreaker Spear GOD Purge (T8.4)",
            category = BuildCategory.DAGGER_SPEAR,
            weapon = "Tagesbrecher / Daybreaker (T8.4)",
            head = "Purpur-Kapuze (T8.4)",
            armor = "Söldner-Rüstung (T8.4)",
            shoes = "Dämonen-Schuhe (T8.4)",
            potion = "Heiltrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 8900000L,
            damageRating = 95,
            defenseRating = 85,
            mobilityRating = 90,
            strongestWeaponHighlight = "🔱 PURGING THRUST: Löscht alle Buffs des Gegners im Vorbeifliegen!",
            aiEvaluationDe = "🔱 UNFAIRER SPEER: Der E-Thrust sticht durch Schilde und reinigt gegnerische Sprint- und Heilbuffs.",
            combatRotationDe = "1. 3x Spear Stacks ➔ 2. E-Thrust durch den Gegner ➔ 3. W-Cripple zum Enteilen!"
        ),
        EquipmentBuild(
            id = "WARBOW_SPECIAL",
            title = "🏹 Warbow GOD Long-Range Snipe (T8.4)",
            category = BuildCategory.BOWS_CROSSBOWS,
            weapon = "Kriegsbogen / Warbow (T8.4)",
            head = "Magier-Mütze (T8.4)",
            armor = "Kleriker-Robe (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            potion = "Gifttrank (T8)",
            food = "Gänse-Omelett (T8.3)",
            estimatedCostSilver = 6500000L,
            damageRating = 98,
            defenseRating = 50,
            mobilityRating = 95,
            strongestWeaponHighlight = "🏹 MAGIC ARROW SNIPE: Bietet maximale Snipe-Reichweite aus dem Nebel!",
            aiEvaluationDe = "🏹 SNIPER GOD: Beseitigt Ziele aus extremer Entfernung, ohne dass diese zurückschlagen können.",
            combatRotationDe = "1. Frost Shot W zum Abstandgewinnen ➔ 2. E-Magic Arrow aus 20 Meter Entfernung zünden!"
        ),
        EquipmentBuild(
            id = "DAWNSONG_SPECIAL",
            title = "🔥 Dawnsong GOD Inferno (T8.4)",
            category = BuildCategory.FIRE_FROST_STAFF,
            weapon = "Morgenlied / Dawnsong (T8.4)",
            head = "Kultisten-Kapuze (T8.4)",
            armor = "Kleriker-Robe (T8.4)",
            shoes = "Königliche Sandalen (T8.4)",
            potion = "Major Poison Potion (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 11000000L,
            damageRating = 100,
            defenseRating = 40,
            mobilityRating = 55,
            strongestWeaponHighlight = "🔥 INFERNAL FLAME: Brennt 3000+ Schaden über Zeit in Rüstungen hinein!",
            aiEvaluationDe = "🔥 FLAMMEN INFERNO: Vernichtet feindliche Frontlines in Sekundenschnelle.",
            combatRotationDe = "1. Druid Robe Buff ➔ 2. E-Dawnsong Beam durch den Gegner-Blob ➔ 3. Q-Firewall zünden!"
        ),
        EquipmentBuild(
            id = "SHADOWCALLER_SPECIAL",
            title = "🔮 Shadowcaller GOD Dot Melter (T8.4)",
            category = BuildCategory.CURSED_ARCANE_STAFF,
            weapon = "Schattenrufer / Shadowcaller (T8.4)",
            head = "Kultisten-Kapuze (T8.4)",
            armor = "Hellion-Rüstung (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            potion = "Major Poison Potion (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 14500000L,
            damageRating = 100,
            defenseRating = 80,
            mobilityRating = 60,
            strongestWeaponHighlight = "🔮 SHADOW AURA: Zerschmilzt Mobs und Spieler mit doppelter Armor-Shred Aura!",
            aiEvaluationDe = "🔮 DER STAEARKSTE CURSE STAFF: Kombiniert gewaltige Fluch-Stapel mit Hellion-Aura Lifesteal.",
            combatRotationDe = "1. 4x Vile Curses ➔ 2. E-Shadow Aura zünden ➔ 3. Hellion Rüstung aktivieren!"
        ),
        EquipmentBuild(
            id = "BLIGHT_STAFF_SPECIAL",
            title = "🌿 Blight Staff GOD Unkillable Nature (T8.4)",
            category = BuildCategory.HOLY_NATURE_STAFF,
            weapon = "Seuchenstab / Blight Staff (T8.4)",
            head = "Söldner-Kapuze (T8.4)",
            armor = "Kleriker-Robe (T8.4)",
            shoes = "Söldner-Schuhe (T8.4)",
            potion = "Energietrank (T8)",
            food = "Gänse-Omelett (T8.3)",
            estimatedCostSilver = 5500000L,
            damageRating = 30,
            defenseRating = 90,
            healingRating = 100,
            strongestWeaponHighlight = "🌿 MOVING HEAL AURA: Läuft tanzend durch feindliches Feuer und heilt alle Teammates auf 100%!",
            aiEvaluationDe = "🌿 UNSTERBLICHER NATUR-HEILER: Ständige Rejuvenation Stacks versorgen das Team mit unendlichem Leben.",
            combatRotationDe = "1. 3x Rejuvenation Stacks spamen ➔ 2. E-Blight Dance zünden und durch die Gruppe bewegen!"
        ),
        EquipmentBuild(
            id = "PROWLER_STAFF_SPECIAL",
            title = "🐺 Prowler Staff GOD Panther Stealth (T8.4)",
            category = BuildCategory.SHAPESHIFTER,
            weapon = "Pirschstab / Prowler Staff (T8.4)",
            head = "Stalker-Kapuze (T8.4)",
            armor = "Feen-Robe (T8.4)",
            shoes = "Dämonen-Schuhe (T8.4)",
            potion = "Unsichtbarkeitstrank (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 18000000L,
            damageRating = 100,
            defenseRating = 75,
            mobilityRating = 100,
            strongestWeaponHighlight = "🐺 PANTHER STEALTH EXECUTE: Verwandelt dich in eine unsichtbare Raubkatze mit Pounce Execute!",
            aiEvaluationDe = "🐺 UNFAIRER SHAPESHIFTER: Kann im Kampf in Panther-Gestalt springen und Feinde im Invisibility-Pounce zerschmettern.",
            combatRotationDe = "1. Human Q/W Spells vorwärmen ➔ 2. E-Transform in Panther ➔ 3. Pounce Execute aus dem Hinterhalt!"
        )
    )
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
