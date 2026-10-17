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
        val idAt = resourceId.replace("_", "@")
        val idUnderscore = resourceId.replace("@", "_")
        val prices = priceMap[resourceId] ?: priceMap[idAt] ?: priceMap[idUnderscore] ?: emptyList()

        val cityPrices = prices.filter { TradeCalculator.citiesMatch(it.city, cityName) && it.sellPriceMin > 0 }
        val lowestCityPrice = cityPrices.minByOrNull { it.sellPriceMin }?.sellPriceMin
        if (lowestCityPrice != null && lowestCityPrice > 0) return lowestCityPrice

        val cityBuyOrder = prices.find { TradeCalculator.citiesMatch(it.city, cityName) }?.buyPriceMax
        if (cityBuyOrder != null && cityBuyOrder > 0) return cityBuyOrder

        val globalLowestSell = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }?.sellPriceMin
        if (globalLowestSell != null && globalLowestSell > 0) return globalLowestSell

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
    val tier: Int = 8,
    val categoryName: String = "Gebäude",
    val topProductDe: String = "",
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
            estimatedRoiPercent = 28.5,
            tier = 8,
            categoryName = "Kräuter",
            topProductDe = "🌿 T8 Elfenkraut & Drachenteufel (~48.000 S./Beet)"
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
            estimatedRoiPercent = 24.0,
            tier = 8,
            categoryName = "Feldfrüchte",
            topProductDe = "🌾 T8 Kürbisse & Mais (~42.000 S./Feld)"
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
            estimatedRoiPercent = 35.0,
            tier = 8,
            categoryName = "Tiere",
            topProductDe = "🐄 T8 Rinder & T8 Schreckenswolf-Welpen (~85.000 S./Gehege)"
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
            estimatedRoiPercent = 42.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "📚 T8 Gefüllte Tagebücher (~125.000 S./Haus)"
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
            estimatedRoiPercent = 38.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "⚔️ T8.4 Meisterhaft Königsmacher (~450.000 S. Profit/Craft)"
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
            estimatedRoiPercent = 40.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "🔮 T8.4 Schattenrufer & Eisstab (~480.000 S. Profit/Craft)"
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
            estimatedRoiPercent = 34.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "🏹 T8.4 Totengeber & Belagerungsarmbrust (~390.000 S. Profit/Craft)"
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
            estimatedRoiPercent = 31.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "🎒 T8.4 Meister-Taschen & T8 Spitzhacken (~210.000 S. Profit/Craft)"
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
            estimatedRoiPercent = 37.5,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "⛏️ T8.4 Adamantiumbarren (~280.000 S. Profit/Batch)"
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
            estimatedRoiPercent = 36.0,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "🐆 T8.4 Makelloses Leder (~260.000 S. Profit/Batch)"
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
            estimatedRoiPercent = 33.5,
            tier = 8,
            categoryName = "Gebäude",
            topProductDe = "🌾 T8.4 Geisterhanf Stoff (~240.000 S. Profit/Batch)"
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
            // Dummy-Einträge entfernt, Start ohne automatische Timer
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
    EXTREME_LIFESTEAL("🩸", "Extremer Lebensraub"),
    EXTREME_ONE_HIT("💥", "Extremer One-Hit Schaden"),
    EXTREME_DEFENSE("🛡️", "Extreme Verteidigung"),
    EXTREME_HEALING("💚", "Extreme Heilung"),
    UNSTOPPABLE_PVE("🌾", "Unzerstörbarer Solo-PvE")
}

data class EquipmentBuild(
    val id: String = "",
    val title: String = "",
    val category: BuildCategory = BuildCategory.EXTREME_LIFESTEAL,
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
        // 🩸 1. EXTREMER LEBENSRAUB - BUILD 1: BATTLEAXE VAMPIRE GOD
        EquipmentBuild(
            id = "UNFAIR_LIFESTEAL_BATTLEAXE",
            title = "🩸 Schlachtaxt Unendlicher Lebensraub (T8.4)",
            category = BuildCategory.EXTREME_LIFESTEAL,
            weapon = "Schlachtaxt / Battleaxe (T8.4) + Muysak (T8.4)",
            head = "Söldner-Kapuze / Mercenary Hood (T8.4)",
            armor = "Hellion-Rüstung / Hellion Jacket (T8.4)",
            shoes = "Dämonen-Stiefel / Demon Boots (T8.4)",
            mount = "Panther / Geisterwildschwein",
            potion = "Major Healing Potion / Heiltrank (T8)",
            food = "Schweinebraten / Roast Pork (T8.3 - +13.5% Lifesteal)",
            estimatedCostSilver = 3850000L,
            estimatedMarginPercent = 38.5,
            damageRating = 92,
            defenseRating = 98,
            mobilityRating = 88,
            healingRating = 100,
            strongestWeaponHighlight = "🩸 UNENDLICHER LEBENSRAUB: Regeneriert 1200+ HP pro Sekunde im laufenden Kampf!",
            aiEvaluationDe = "🩸 EXTREMER LEBENSRAUB (SCHLACHTAXT): Regeneriert 1200+ HP pro Sekunde! Die Kombination aus Doppelaxt-Axtwurf (E) mit Hellion-Aura und Schweinebraten heilt dich im Kampf von 10% auf 100% HP auf.",
            combatRotationDe = "1. Q-Bleeds stapeln ➔ 2. Hellion-Aura (Rüstung R) aktivieren ➔ 3. E-Axtwurf zünden für +1200 HP Sofort-Heilung!",
            vorteileDe = listOf("Unsterblich im 1v1 & 1vX Mists", "Extrem hohe Eigenheilung ohne Manaprobleme", "Invertiert Niederlagen bei <20% HP"),
            nachteileDe = listOf("Erfordert Kontakt zum Ziel für Lifesteal")
        ),

        // 🩸 1. EXTREMER LEBENSRAUB - BUILD 2: BLOODLETTER MERCENARY VAMPIRE
        EquipmentBuild(
            id = "UNFAIR_LIFESTEAL_BLOODLETTER",
            title = "🩸 Blutmacher Vampir-Exekutor (T8.4)",
            category = BuildCategory.EXTREME_LIFESTEAL,
            weapon = "Blutstrikefeld / Bloodletter (T8.4) + Nebelrufer (T8.4)",
            head = "Jäger-Kapuze / Hunter Hood (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf / Swiftclaw",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 4120000L,
            estimatedMarginPercent = 35.0,
            damageRating = 95,
            defenseRating = 90,
            mobilityRating = 100,
            healingRating = 95,
            strongestWeaponHighlight = "🩸 BLUTRAUSCH-HEILUNG: Söldner-Blutrausch heilt +200 HP pro Treffer bei schnellen Dolch-Hits!",
            aiEvaluationDe = "🩸 EXTREMER LEBENSRAUB (DOLCHE): Unbesiegbares Vampire-Build! Kombiniert Söldner-Blutrausch (+200 HP pro Treffer) mit schnellen Dolch-Hits (Q+W) und Schweinebraten-Lifesteal.",
            combatRotationDe = "1. Dash an den Gegner ➔ 2. Blutrausch (R) zünden + Q-Spam für +1600 HP Heilung in 2 Sekunden ➔ 3. E-Execute unter 40% HP!",
            vorteileDe = listOf("Höchste Mobilität im Spiel", "Sehr schnelle Heil-Impulse", "Gegner unter 40% HP sterben sofort"),
            nachteileDe = listOf("Empfindlich gegen Stuns während Blutrausch")
        ),

        // 🩸 1. EXTREMER LEBENSRAUB - BUILD 3: GREAT NATURE LIFE REGEN
        EquipmentBuild(
            id = "UNFAIR_LIFESTEAL_NATURE",
            title = "🩸 Naturstab Unsterblicher Lebensregen (T8.4)",
            category = BuildCategory.EXTREME_LIFESTEAL,
            weapon = "Großer Naturstab / Great Nature Staff (T8.4)",
            head = "Kultisten-Kapuze / Cultist Cowl (T8.4)",
            armor = "Hellion-Rüstung / Hellion Jacket (T8.4)",
            shoes = "Dämonen-Stiefel / Demon Boots (T8.4)",
            mount = "Grizzlybär / Ochs",
            potion = "Gigantismus-Trank (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3450000L,
            estimatedMarginPercent = 42.0,
            damageRating = 85,
            defenseRating = 100,
            mobilityRating = 80,
            healingRating = 100,
            strongestWeaponHighlight = "🌿 UNSTERBLICHE NATUR: Jeder gegnerische Treffer wandelt sich in +350 HP Heilung um!",
            aiEvaluationDe = "🩸 EXTREMER LEBENSRAUB (NATURMAGIE): Unsterbliche Heil-Barriere! Der Living Armor E-Spell wandelt JEDEN gegnerischen Treffer in +350 HP Heilung um. Unmöglich im 1v1 zu töten.",
            combatRotationDe = "1. 3x Rejuvenation Stack aufrechterhalten ➔ 2. Living Armor (E) bei gegnerischem Burst zünden ➔ 3. Hellion-Aura zünden!",
            vorteileDe = listOf("Hält selbst ZvZ-Schaden im 1vX aus", "Gegnerische Angriffe heilen dich", "Sehr entspanntes Solo-PvP"),
            nachteileDe = listOf("Etwas geringere Burst-Gefahr für Gegner")
        ),

        // 🩸 1. EXTREMER LEBENSRAUB - BUILD 4: SHADOWCALLER VAMPIRE
        EquipmentBuild(
            id = "UNFAIR_LIFESTEAL_SHADOWCALLER",
            title = "🩸 Schattenrufer Vampir-Zauberer (T8.4)",
            category = BuildCategory.EXTREME_LIFESTEAL,
            weapon = "Schattenrufer / Shadowcaller (T8.4) + Kryptakerze (T8.4)",
            head = "Kapuze der Reinheit / Cowl of Purity (T8.4)",
            armor = "Hellion-Rüstung / Hellion Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf / Panther",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 5200000L,
            estimatedMarginPercent = 36.8,
            damageRating = 98,
            defenseRating = 92,
            mobilityRating = 90,
            healingRating = 98,
            strongestWeaponHighlight = "🔮 AOE SHRED LIFESTEAL: Zerschmettert Rüstungen um -45% und saugt 1800+ HP pro Sekunde ab!",
            aiEvaluationDe = "🩸 EXTREMER LEBENSRAUB (FLUCHMAGIE): Riesiger AoE-Lebensraub! Schattenrufer E-Ring zerschmettert Gegner-Rüstungen um -45% und saugt 1800+ HP pro Sekunde aus kompletten Spieler-Gruppen ab.",
            combatRotationDe = "1. E-Shadowcaller Ring zünden (-45% Rüstung) ➔ 2. Hellion-Aura (R) aktivieren ➔ 3. Q-Cursed Sickle spammen für massiven Vampir-Ertrag!",
            vorteileDe = listOf("Gigantischer Gruppenschaden & Gruppenheilung", "Zerstört gegnerische Rüstungen", "Ideal für Mists & Small Scale"),
            nachteileDe = listOf("Hoher Preis für T8.4 Schattenrufer")
        ),

        // 🩸 1. EXTREMER LEBENSRAUB - BUILD 5: KAMPFAXT LEBENSRAUB JÄGER
        EquipmentBuild(
            id = "UNFAIR_LIFESTEAL_GREATAXE",
            title = "🩸 Kampfaxt Vampir-Wirbelwind (T8.4)",
            category = BuildCategory.EXTREME_LIFESTEAL,
            weapon = "Große Kampfaxt / Greataxe (T8.4)",
            head = "Gelehrten-Kapuze / Scholar Cowl (T8.4)",
            armor = "Hellion-Rüstung / Hellion Jacket (T8.4)",
            shoes = "Dämonen-Stiefel / Demon Boots (T8.4)",
            mount = "Schreckenswolf / Bär",
            potion = "Major Healing Potion (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3600000L,
            estimatedMarginPercent = 34.5,
            damageRating = 94,
            defenseRating = 90,
            mobilityRating = 85,
            healingRating = 96,
            strongestWeaponHighlight = "🪓 WIRBELWIND HEILUNG: E-Spin saugt Leben von mehreren Zielen gleichzeitig ab!",
            aiEvaluationDe = "🩸 EXTREMER LEBENSRAUB (KAMPFAXT): Der rotierende Axt-Wirbelwind (E) in Kombination mit Hellion-Rüstung heilt den Träger pro Sekunden-Hit massiv auf.",
            combatRotationDe = "1. Q-Bleeds auftragen ➔ 2. Hellion-Rüstung (R) aktivieren ➔ 3. E-Wirbelwind durch die Gegnergruppe drehen!",
            vorteileDe = listOf("Hohe Flächenheilung bei mehreren Zielen", "Ununterbrechbarer Axt-Spin", "Sehr stark im Mob-Train"),
            nachteileDe = listOf("Anfällig gegen Kiting auf Distanz")
        ),

        // 💥 2. EXTREMER ONE-HIT SCHADEN - BUILD 1: DAGGER PAIR EXECUTIONER
        EquipmentBuild(
            id = "ONE_HIT_DAGGER_PAIR",
            title = "💥 Dolchpaar 1-Hit Instant Exekution (T8.4)",
            category = BuildCategory.EXTREME_ONE_HIT,
            weapon = "Dolchpaar / Dagger Pair (T8.4 Masterwork)",
            head = "Stalker-Kapuze / Stalker Hood (T8.4)",
            armor = "Assassinen-Rüstung / Assassin Jacket (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Schreckenswolf / Swiftclaw",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Rinder-Spezialeintopf / Beef Stew (T8.3 - +13.5% Schaden)",
            estimatedCostSilver = 4400000L,
            estimatedMarginPercent = 40.0,
            damageRating = 100,
            defenseRating = 75,
            mobilityRating = 98,
            healingRating = 60,
            strongestWeaponHighlight = "💥 3500+ SOFORT-SCHADEN: Vernichtet Gegner mit 1-Hit Exekution in 0.4 Sekunden!",
            aiEvaluationDe = "💥 EXTREMER ONE-HIT SCHADEN: Vernichtet Gegner mit 3500+ Instant Damage auf EINEN SCHLAG! 3x Assassin Charges + Stealth Ambush (+40% Dmg) + Royal Sandals (+20% Dmg) + Stalker Hood (-35% Rüstung) = 1-Hit Tötung!",
            combatRotationDe = "1. 3x Assassin Charges aufladen ➔ 2. Royal Sandals (+20% Dmg) zünden ➔ 3. Ambush Stealth (+40% Dmg) ➔ 4. Stalker Hood (-35% Rüstung) ➔ 5. E-Exekution = SOFORT-TOD!",
            vorteileDe = listOf("Löscht Gegner im Mists in 0.4 Sekunden aus", "Gegner haben keine Reaktionszeit", "Höchster Single-Target Burst im Spiel"),
            nachteileDe = listOf("Gefahr bei Verfehlen des E-Spells")
        ),

        // 💥 2. EXTREMER ONE-HIT SCHADEN - BUILD 2: CLAYMORE SLAUGHTER
        EquipmentBuild(
            id = "ONE_HIT_CLAYMORE_GOD",
            title = "💥 Claymore 1-Hit Todesstoss (T8.4)",
            category = BuildCategory.EXTREME_ONE_HIT,
            weapon = "Zweihänder / Claymore (T8.4 Masterwork)",
            head = "Stalker-Kapuze / Stalker Hood (T8.4)",
            armor = "Assassinen-Rüstung / Assassin Jacket (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Schreckenswolf / Direwolf",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Rinder-Spezialeintopf / Beef Stew (T8.3)",
            estimatedCostSilver = 4600000L,
            estimatedMarginPercent = 37.5,
            damageRating = 100,
            defenseRating = 85,
            mobilityRating = 95,
            healingRating = 70,
            strongestWeaponHighlight = "💥 UNAUFHALTSAMER TODESSTOSS: Durchschlägt Schilde & Rüstungen für 3200+ Sofort-Schaden!",
            aiEvaluationDe = "💥 EXTREMER ONE-HIT CLAYMORE: 3x Heroic Charge + Ambush Stealth + E-Charge (Unaufhaltsam) durchschlägt Schild & Rüstung für 3200+ Sofort-Schaden auf einen Schlag!",
            combatRotationDe = "1. 3x Heroic Charges stacken ➔ 2. Ambush Stealth aktivieren ➔ 3. Stalker-Kapuze zünden ➔ 4. E-Claymore Charge auf das Ziel!",
            vorteileDe = listOf("Der E-Charge kann NICHT unterbrochen werden", "Sehr hoher Sofort-Schaden", "Stark im 1v1 Corrupted & Mists"),
            nachteileDe = listOf("Gegenwehr erfordert Purge-Schutz")
        ),

        // 💥 2. EXTREMER ONE-HIT SCHADEN - BUILD 3: ARMBRUST SNIPER
        EquipmentBuild(
            id = "ONE_HIT_CROSSBOW_SNIPER",
            title = "💥 Armbrust 20m Sniper One-Shot (T8.4)",
            category = BuildCategory.EXTREME_ONE_HIT,
            weapon = "Schwere Armbrust / Siegecross / Boltcasters (T8.4)",
            head = "Stalker-Kapuze / Stalker Hood (T8.4)",
            armor = "Königliche Robe / Royal Robe (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Schreckenswolf / Swiftclaw",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Rinder-Spezialeintopf / Beef Stew (T8.3)",
            estimatedCostSilver = 3900000L,
            estimatedMarginPercent = 41.0,
            damageRating = 100,
            defenseRating = 70,
            mobilityRating = 75,
            healingRating = 50,
            strongestWeaponHighlight = "💥 20-METER SNIPER ONE-SHOT: Schießt Gegner aus maximaler Distanz mit EINEM Treffer weg!",
            aiEvaluationDe = "💥 EXTREMER ONE-SHOT ARMBRUST SNIPER: Der Magic Circle (+50% Dmg) + Royal Sandals (+20% Dmg) schießen Gegner aus 20 Metern Entfernung mit EINEM E-Treffer komplett aus dem Spiel!",
            combatRotationDe = "1. Royal Robe Magic Circle (+50% Dmg) legen ➔ 2. Royal Sandals (+20% Dmg) aktivieren ➔ 3. E-Sniper Shot aus 20m Distanz zünden!",
            vorteileDe = listOf("Extrem hohe Reichweite (20 Meter)", "Keine Annäherung an Gegner erforderlich", "Ideal für Ganking & Open World"),
            nachteileDe = listOf("Geringe Mobilität beim Zaubern")
        ),

        // 💥 2. EXTREMER ONE-HIT SCHADEN - BUILD 4: BEAR PAWS RIP BURST
        EquipmentBuild(
            id = "ONE_HIT_BEAR_PAWS",
            title = "💥 Bärenpfoten 1-Hit Riss-Schaden (T8.4)",
            category = BuildCategory.EXTREME_ONE_HIT,
            weapon = "Bärenpfoten / Bear Paws (T8.4)",
            head = "Hellion-Kapuze / Hellion Hood (T8.4)",
            armor = "Assassinen-Rüstung / Assassin Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Poison Potion (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 5100000L,
            estimatedMarginPercent = 38.0,
            damageRating = 100,
            defenseRating = 80,
            mobilityRating = 96,
            healingRating = 60,
            strongestWeaponHighlight = "💥 E-AOT RISS: Erzeugt W True-Damage Bleeds + E-Sprung für 3000+ Schaden!",
            aiEvaluationDe = "💥 EXTREMER ONE-HIT BEAR PAWS: Der unaufhaltsame E-Sprungschnitt erzeugt echten Schaden (True Damage) und durchdringt Rüstungen vollständig.",
            combatRotationDe = "1. Stealth Ambush aktivieren ➔ 2. W-Adrenaline Boost ➔ 3. E-Sprung ins Ziel für Sofort-Explosion!",
            vorteileDe = listOf("True Damage ignoriert Rüstung komplett", "Sehr hoher Sprung-Burst", "Sehr beliebt in Mists"),
            nachteileDe = listOf("Hoher T8.4 Preis")
        ),

        // 💥 2. EXTREMER ONE-HIT SCHADEN - BUILD 5: SHADOW CLAWS PHANTOM
        EquipmentBuild(
            id = "ONE_HIT_CLAWS_PHANTOM",
            title = "💥 Klauen 1-Hit Schatten-Kombination (T8.4)",
            category = BuildCategory.EXTREME_ONE_HIT,
            weapon = "Klauen / Claws (T8.4)",
            head = "Stalker-Kapuze / Stalker Hood (T8.4)",
            armor = "Assassinen-Rüstung / Assassin Jacket (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Panther",
            potion = "Major Poison Potion (T8)",
            food = "Rinder-Spezialeintopf (T8.3)",
            estimatedCostSilver = 3700000L,
            estimatedMarginPercent = 36.0,
            damageRating = 98,
            defenseRating = 70,
            mobilityRating = 95,
            healingRating = 50,
            strongestWeaponHighlight = "💥 DISSEMBLER STUN: Fixiert Gegner und führt 8x Sofort-Schnitte in 1 Sekunde aus!",
            aiEvaluationDe = "💥 EXTREMER ONE-HIT CLAWS: Stunned Gegner für 3 Sekunden und fügt während der Fixierung 3200+ Gesamtschaden zu.",
            combatRotationDe = "1. Stealth Ambush ➔ 2. Stalker Kapuze ➔ 3. E-Dissemble Fixierung für 1-Hit Exekution!",
            vorteileDe = listOf("Gegner kann während E-Spell nicht ausweichen", "Hohe Stun-Dauer", "Perfekt für Ganking"),
            nachteileDe = listOf("Anfällig gegen Cleanse von Helfern")
        ),

        // 🛡️ 3. EXTREME VERTEIDIGUNG - BUILD 1: JUDICATOR BOLLWERK
        EquipmentBuild(
            id = "EXTREME_DEFENSE_JUDICATOR",
            title = "🛡️ Platten-Ochs Unüberwindbares Bollwerk (T8.4)",
            category = BuildCategory.EXTREME_DEFENSE,
            weapon = "Hüter-Keule / Heavy Mace (T8.4) + Schweres Schild",
            head = "Richter-Helm / Judicator Helmet (T8.4)",
            armor = "Richter-Rüstung / Judicator Armor (T8.4)",
            shoes = "Königliche Stiefel / Royal Boots (T8.4)",
            mount = "Armored Siege Ochs / Grizzly",
            potion = "Gigantismus-Trank (T8)",
            food = "Pork Omelette / Schweine-Omelett (T8.3)",
            estimatedCostSilver = 4200000L,
            estimatedMarginPercent = 32.0,
            damageRating = 60,
            defenseRating = 100,
            mobilityRating = 80,
            healingRating = 85,
            strongestWeaponHighlight = "🛡️ 95% SCHADENS-REDUKTION: Unüberwindbares Schutzschild für das gesamte Team!",
            aiEvaluationDe = "🛡️ EXTREME VERTEIDIGUNG: Maximale Rüstungs- & Widerstandswerte im Spiel! Die Judicator-Kuppel gewährt +60% Rüstung und Schilde gegen unbegrenzten Schaden.",
            combatRotationDe = "1. In die Gegnergruppe springen ➔ 2. Judicator Shield Kuppel (R) aktivieren ➔ 3. Silencing Silence Ring (E) zünden!",
            vorteileDe = listOf("Höchste Rüstungswerte im gesamten Spiel", "Schützt auch Verbündete", "Extrem schwer zu töten"),
            nachteileDe = listOf("Geringerer Eigenschaden")
        ),

        // 🛡️ 3. EXTREME VERTEIDIGUNG - BUILD 2: KNIGHT ESCORT
        EquipmentBuild(
            id = "EXTREME_DEFENSE_KNIGHT",
            title = "🛡️ Ritter-Bollwerk Unbesiegbare Wand (T8.4)",
            category = BuildCategory.EXTREME_DEFENSE,
            weapon = "Eisenholz-Stab / Ironclad Staff (T8.4)",
            head = "Dämmerfeder-Helm / Duskweaver Helmet (T8.4)",
            armor = "Ritter-Rüstung / Knight Armor (T8.4)",
            shoes = "Richter-Stiefel / Judicator Boots (T8.4)",
            mount = "Grizzlybär",
            potion = "Gigantismus-Trank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3900000L,
            estimatedMarginPercent = 34.0,
            damageRating = 65,
            defenseRating = 100,
            mobilityRating = 85,
            healingRating = 80,
            strongestWeaponHighlight = "🛡️ WINDWALL BARRIERE: Stoppt alle feindlichen Angriffe und stößt Chaser zurück!",
            aiEvaluationDe = "🛡️ EXTREME VERTEIDIGUNG: Blockiert enge Passagen mit einer unüberwindbaren Windwall-Barriere. Schützt Transporte vor jedem Hinterhalt.",
            combatRotationDe = "1. Positionieren ➔ 2. Windwall (R) aktivieren ➔ 3. Ironclad Hurricane Spin zünden!",
            vorteileDe = listOf("Blockiert feindliche Truppen komplett", "Sehr hoher CC-Effekt", "Unentbehrlich in Roten Zonen"),
            nachteileDe = listOf("Erfordert gutes Positioning")
        ),

        // 🛡️ 3. EXTREME VERTEIDIGUNG - BUILD 3: GUARDIAN EISENHAUT
        EquipmentBuild(
            id = "EXTREME_DEFENSE_GUARDIAN",
            title = "🛡️ Hüter-Eisenhaut Unzerbrechlich (T8.4)",
            category = BuildCategory.EXTREME_DEFENSE,
            weapon = "Erdrunden-Keule / Earthrune Mace (T8.4)",
            head = "Helm der Tapferkeit / Helmet of Valor (T8.4)",
            armor = "Hüter-Rüstung / Guardian Armor (T8.4)",
            shoes = "Dämonen-Stiefel / Demon Boots (T8.4)",
            mount = "Ochs T8",
            potion = "Gigantismus-Trank (T8)",
            food = "Geflügelpastete (T8.3)",
            estimatedCostSilver = 3600000L,
            estimatedMarginPercent = 33.0,
            damageRating = 70,
            defenseRating = 100,
            mobilityRating = 82,
            healingRating = 75,
            strongestWeaponHighlight = "🛡️ HÜTER-AURA: Schwächt allen feindlichen Schaden im Umkreis um -60% ab!",
            aiEvaluationDe = "🛡️ EXTREME VERTEIDIGUNG: Reduziert feindlichen Schaden um 60% und neutralisiert Buffs von gegnerischen Zielen.",
            combatRotationDe = "1. Guardian Area (R) zünden ➔ 2. Helmet Purge (D) ➔ 3. Ground Slam (E)!",
            vorteileDe = listOf("Entzieht Gegnern ihren Schadens-Buff", "Massive Schadensreduktion", "Sehr langlebig"),
            nachteileDe = listOf("Kompakte Reichweite")
        ),

        // 🛡️ 3. EXTREME VERTEIDIGUNG - BUILD 4: MACES SHIELD TANK
        EquipmentBuild(
            id = "EXTREME_DEFENSE_MACE",
            title = "🛡️ Keulen-Titan Schild-Festung (T8.4)",
            category = BuildCategory.EXTREME_DEFENSE,
            weapon = "Inkubus-Keule / Incubus Mace (T8.4) + Sarkophag-Schild",
            head = "Königlicher Helm (T8.4)",
            armor = "Richter-Rüstung (T8.4)",
            shoes = "Tenacity Boots (T8.4)",
            mount = "Grizzly",
            potion = "Gigantismus-Trank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 4100000L,
            estimatedMarginPercent = 35.0,
            damageRating = 68,
            defenseRating = 100,
            mobilityRating = 80,
            healingRating = 80,
            strongestWeaponHighlight = "🛡️ SCHRUMPF-AURA: Verringert die Größe & den Schaden von Gegnern um -50%!",
            aiEvaluationDe = "🛡️ EXTREME VERTEIDIGUNG: Verkleinert feindliche Angreifer und macht deren Angriffe wirkungslos.",
            combatRotationDe = "1. Shrinking Slam (E) ➔ 2. Judicator Bubble ➔ 3. Stun Snare!",
            vorteileDe = listOf("Reduziert feindliche HP-Maximalwerte", "Sehr hoher Disruption-Wert", "Unerschütterlich"),
            nachteileDe = listOf("Auf Abklingzeiten angewiesen")
        ),

        // 🛡️ 3. EXTREME VERTEIDIGUNG - BUILD 5: ZVZ BARRICADE
        EquipmentBuild(
            id = "EXTREME_DEFENSE_ZVZ",
            title = "🛡️ Dämonen-Barrikade ZvZ Titan (T8.4)",
            category = BuildCategory.EXTREME_DEFENSE,
            weapon = "Großer Hammer / Polehammer (T8.4)",
            head = "Ritter-Helm (T8.4)",
            armor = "Dämonen-Rüstung (T8.4)",
            shoes = "Gelehrten-Sandalen (T8.4)",
            mount = "Siege Ochs",
            potion = "Gigantismus-Trank (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3800000L,
            estimatedMarginPercent = 31.5,
            damageRating = 72,
            defenseRating = 100,
            mobilityRating = 85,
            healingRating = 70,
            strongestWeaponHighlight = "🛡️ REFLEKTIONS-SCHILD: Reflektiert 80% des erlittenen Schadens zurück auf Angreifer!",
            aiEvaluationDe = "🛡️ EXTREME VERTEIDIGUNG: Wirft 80% des gegnerischen Schadens direkt zurück und gewährt Immunität gegen CC-Effekte.",
            combatRotationDe = "1. Reflexions-Aura (R) aktivieren ➔ 2. CC-Immunität (D) zünden ➔ 3. Hammer Slam (E)!",
            vorteileDe = listOf("Tötet Angreifer durch ihren eigenen Schaden", "Vollständige CC-Immunität", "Extrem stabil"),
            nachteileDe = listOf("Erfordert gutes timing bei Schadenswellen")
        ),

        // 💚 4. EXTREME HEILUNG - BUILD 1: FALLEN STAFF GOD
        EquipmentBuild(
            id = "EXTREME_HEALING_FALLEN",
            title = "💚 Gefallener Heiligstab Gotteslicht (T8.4)",
            category = BuildCategory.EXTREME_HEALING,
            weapon = "Gefallener Stab / Fallen Staff (T8.4)",
            head = "Kapuze der Reinheit / Cowl of Purity (T8.4)",
            armor = "Kultisten-Robe / Cultist Robe (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Energy Potion (T8)",
            food = "Pork Omelette / Schweine-Omelett (T8.3)",
            estimatedCostSilver = 4300000L,
            estimatedMarginPercent = 39.0,
            damageRating = 50,
            defenseRating = 85,
            mobilityRating = 85,
            healingRating = 100,
            strongestWeaponHighlight = "💚 GOTTESLICHT HEILUNG: Stellt 2500+ HP für das gesamte Team in 1 Sekunde wieder her!",
            aiEvaluationDe = "💚 EXTREME HEILUNG: Höchster Gruppenheilungs-Burst im gesamten Spiel! Der Fallen E-Sanctuary Kreis rettet komplette Teams aus dem Tode.",
            combatRotationDe = "1. Q-Heal Orbs legen ➔ 2. Cultist Robe Levitation (R) für Mana & HP ➔ 3. Fallen Sanctuary (E) auf verwundete Verbündete!",
            vorteileDe = listOf("Stellt +2500 HP in 1 Sekunde für 10+ Spieler her", "Bereinigt negative CC-Effekte", "Unersetzlich in PvP & ZvZ"),
            nachteileDe = listOf("Benötigt gutes Vorhersehen von Burst-Schaden")
        ),

        // 💚 4. EXTREME HEILUNG - BUILD 2: WILD NATURE BLOOM
        EquipmentBuild(
            id = "EXTREME_HEALING_WILD",
            title = "💚 Wildnis-Naturstab Unendliche Blüte (T8.4)",
            category = BuildCategory.EXTREME_HEALING,
            weapon = "Wildnis-Stab / Wild Staff (T8.4)",
            head = "Druiden-Kapuze / Druid Cowl (T8.4)",
            armor = "Feen-Robe / Feystrike Robe (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Geisterwildschwein",
            potion = "Major Energy Potion (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3950000L,
            estimatedMarginPercent = 37.0,
            damageRating = 55,
            defenseRating = 88,
            mobilityRating = 82,
            healingRating = 100,
            strongestWeaponHighlight = "🌿 LEBENS-FLUSS: Ein fließender Heilungs-Teppich heilt kontinuierlich +800 HP pro Sekunde!",
            aiEvaluationDe = "💚 EXTREME HEILUNG: Bietet permanente, unterbrechungsfreie Gruppenheilung über Zeit (HoT).",
            combatRotationDe = "1. 3x Rejuvenation Stacks verteilen ➔ 2. Druid Cowl Mana-Aura ➔ 3. Wild Bloom (E) Wellen heilen kontinuierlich!",
            vorteileDe = listOf("Permanenter Lifestream für die Gruppe", "Sehr hohe Mana-Effizienz", "Starke Gruppen-Passivheilung"),
            nachteileDe = listOf("Längere Anlaufzeit als Instant-Heal")
        ),

        // 💚 4. EXTREME HEILUNG - BUILD 3: DIVINE BENEDICTION
        EquipmentBuild(
            id = "EXTREME_HEALING_DIVINE",
            title = "💚 Göttlicher Stab Segnungs-Säule (T8.4)",
            category = BuildCategory.EXTREME_HEALING,
            weapon = "Göttlicher Stab / Divine Staff (T8.4)",
            head = "Söldner-Kapuze / Mercenary Hood (T8.4)",
            armor = "Kleriker-Robe / Cleric Robe (T8.4)",
            shoes = "Richter-Stiefel / Boots of Valor (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Energy Potion (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3700000L,
            estimatedMarginPercent = 36.0,
            damageRating = 50,
            defenseRating = 90,
            mobilityRating = 85,
            healingRating = 100,
            strongestWeaponHighlight = "🛡️ GÖTTLICHER SCHUTZSCHILD: Gewährt +1800 HP Schild + Unverwundbarkeit!",
            aiEvaluationDe = "💚 EXTREME HEILUNG: Legt ein undurchdringliches Schutzschild um fokussierte Ziele und heilt zeitgleich im Überfluss.",
            combatRotationDe = "1. Divine Shield (E) auf das Ziel ➔ 2. Cleric Robe Immunität ➔ 3. Q-Single Target Flash Heal!",
            vorteileDe = listOf("Rettet fokussierte Ziele vor 1-Hit Burst", "Gewährt zusätzliche Immunität", "Perfekt für 2v2 & 5v5"),
            nachteileDe = listOf("Ausschließlich Single-Target fokussiert")
        ),

        // 💚 4. EXTREME HEILUNG - BUILD 4: REDEMPTION PRIEST
        EquipmentBuild(
            id = "EXTREME_HEALING_REDEMPTION",
            title = "💚 Erlösungs-Stab Kaskaden-Heilung (T8.4)",
            category = BuildCategory.EXTREME_HEALING,
            weapon = "Erlösungs-Stab / Redemption Staff (T8.4)",
            head = "Gelehrten-Kapuze / Scholar Cowl (T8.4)",
            armor = "Robe der Reinheit / Robe of Purity (T8.4)",
            shoes = "Königliche Sandalen / Royal Sandals (T8.4)",
            mount = "Panther",
            potion = "Major Energy Potion (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 4150000L,
            estimatedMarginPercent = 38.5,
            damageRating = 52,
            defenseRating = 86,
            mobilityRating = 88,
            healingRating = 100,
            strongestWeaponHighlight = "⚡ HEILUNGS-KASKADE: Kaskadierender Heilungs-Ball springt zwischen 4 Zielen hin und her!",
            aiEvaluationDe = "💚 EXTREME HEILUNG: Der E-Ball springt automatisch zu den am schwersten verletzten Verbündeten.",
            combatRotationDe = "1. Cascade Ball (E) werfen ➔ 2. Scholar Cowl Mana-Shield ➔ 3. Flash Heal spamen!",
            vorteileDe = listOf("Automatische Zielauswahl der Verletzten", "Sehr hoher Kaskaden-Heilwert", "Stark in Arena & Hellgates"),
            nachteileDe = listOf("Benötigt mehrere Ziele im Umkreis")
        ),

        // 💚 4. EXTREME HEILUNG - BUILD 5: LIFETOUCH SANCTUARY
        EquipmentBuild(
            id = "EXTREME_HEALING_LIFETOUCH",
            title = "💚 Lebenshauch-Stab Purge-Oase (T8.4)",
            category = BuildCategory.EXTREME_HEALING,
            weapon = "Lebenshauch-Stab / Lifetouch Staff (T8.4) + Muysak",
            head = "Druiden-Kapuze (T8.4)",
            armor = "Kleriker-Robe (T8.4)",
            shoes = "Königliche Schuhe (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Energy Potion (T8)",
            food = "Schweine-Omelett (T8.3)",
            estimatedCostSilver = 3800000L,
            estimatedMarginPercent = 35.5,
            damageRating = 50,
            defenseRating = 88,
            mobilityRating = 90,
            healingRating = 100,
            strongestWeaponHighlight = "✨ CLEANSE HEAL: Bereinigt alle Flüche & Stuns und stellt +1500 HP her!",
            aiEvaluationDe = "💚 EXTREME HEILUNG: Säubert das Ziel von allen Fluch-Stacks, Giften und Stuns und heilt simultan.",
            combatRotationDe = "1. Cleanse Beam (E) auf das Ziel ➔ 2. Cleric Robe R-Immunität ➔ 3. Q-Heal!",
            vorteileDe = listOf("Entfernt 100% aller negativen Effekte", "Sehr kurze Abklingzeit", "Ideal gegen Cursed Staves"),
            nachteileDe = listOf("Kanalisierung kann unterbrochen werden")
        ),

        // 🌾 5. UNZERSTÖRBARER SOLO-PVE - BUILD 1: SHADOWCALLER PVE GOD
        EquipmentBuild(
            id = "UNSTOPPABLE_PVE_SHADOWCALLER",
            title = "🌾 Unzerstörbarer Solo-PvE Schattenrufer God-Build (T8.4)",
            category = BuildCategory.UNSTOPPABLE_PVE,
            weapon = "Schattenrufer / Shadowcaller (T8.4 Masterwork) + Kryptakerze (T8.4)",
            head = "Gelehrten-Kapuze / Scholar Cowl (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Bär / Ochs / Panther",
            potion = "Major Poison Potion / Gifttrank (T8)",
            food = "Schweinebraten / Roast Pork (T8.3 - +13.5% Lifesteal)",
            estimatedCostSilver = 5400000L,
            estimatedMarginPercent = 45.0,
            damageRating = 100,
            defenseRating = 98,
            mobilityRating = 88,
            healingRating = 100,
            strongestWeaponHighlight = "🌾 UNZERSTÖRBAR IM SOLO-PVE: Solost T8 Static Dungeons & Boss-Gegner ohne Gruppenhilfe!",
            aiEvaluationDe = "🌾 UNZERSTÖRBARER SOLO-PVE GOD-BUILD: Dieser Build ist absolut unzerstörbar im Solo-PvE! Zerschmettert T8 Static Dungeons, Weltbosse und Mists-Mobs im Alleingang. Der Schattenrufer E-Ring zerschmettert Mob-Rüstungen um -45%, während Söldner-Blutrausch und Schweinebraten dich ununterbrochen vollheilen.",
            combatRotationDe = "1. Mob-Gruppen zusammenziehen ➔ 2. E-Schattenrufer Ring zünden (-45% Rüstung) ➔ 3. Söldner-Blutrausch (R) zünden ➔ 4. Q-Cursed Sickle spammen = 100% Vollheilung & Mob-Löschung!",
            vorteileDe = listOf("Absolut unzerstörbar in T8 Static Dungeons & World Bosses", "Massiver AoE-Schaden + Lifesteal", "Unendliches Mana durch Scholar Cowl"),
            nachteileDe = listOf("Hohe Anschaffungskosten für T8.4 Schattenrufer")
        ),

        // 🌾 5. UNZERSTÖRBARER SOLO-PVE - BUILD 2: SPIKED GAUNTLETS CLEAR
        EquipmentBuild(
            id = "UNSTOPPABLE_PVE_SPIKED",
            title = "🌾 Dämonen-Manover Solo-PvE Blitz-Farmer (T8.4)",
            category = BuildCategory.UNSTOPPABLE_PVE,
            weapon = "Stachel-Kampfhandschuhe / Spiked Gauntlets (T8.4)",
            head = "Gelehrten-Kapuze / Scholar Cowl (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Poison Potion (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3800000L,
            estimatedMarginPercent = 41.0,
            damageRating = 100,
            defenseRating = 92,
            mobilityRating = 90,
            healingRating = 95,
            strongestWeaponHighlight = "🌾 INSTANT CLEAR: Löscht Mobs in Plattenrüstung mit EINEM E-Schlag vollständig aus!",
            aiEvaluationDe = "🌾 UNZERSTÖRBARER SOLO-PVE: Vernichtet Mob-Camps im Mists & Open World in unter 1 Sekunde. Fügt gegnerischen Mobs in Plattenrüstung +400% Zusatzschaden zu.",
            combatRotationDe = "1. Mobs sammeln ➔ 2. E-Spiked Slam zünden ➔ 3. Camp komplett gecleart!",
            vorteileDe = listOf("Schnellster Mob-Clear im Spiel", "Sehr hoher Platten-Zusatzschaden", "Perfekt für Open World Fame-Farm"),
            nachteileDe = listOf("Etwas schwächer gegen Fliehende PvP-Ziele")
        ),

        // 🌾 5. UNZERSTÖRBARER SOLO-PVE - BUILD 3: NATURE STAFF SOLO
        EquipmentBuild(
            id = "UNSTOPPABLE_PVE_NATURE",
            title = "🌾 Naturstab Unzerstörbarer Boss-Solist (T8.4)",
            category = BuildCategory.UNSTOPPABLE_PVE,
            weapon = "Dornen-Naturstab / Thorn Nature Staff (T8.4) + Muysak",
            head = "Mage Cowl / Magier-Haube (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Geisterwildschwein",
            potion = "Major Poison Potion (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3200000L,
            estimatedMarginPercent = 44.0,
            damageRating = 90,
            defenseRating = 100,
            mobilityRating = 80,
            healingRating = 100,
            strongestWeaponHighlight = "🌾 BOSS-SOLO GOD: Besiegt goldene T8 Dungeons-Bosse im Alleingang ohne HP-Verlust!",
            aiEvaluationDe = "🌾 UNZERSTÖRBARER SOLO-PVE: Der entspannteste Boss-Kill Build in ganz Albion. Dornen-Stacks fügen kontinuierlich AoE-Schaden zu, während Rejuvenation Stacks deine HP dauerhaft auf 100% halten.",
            combatRotationDe = "1. 3x Thorn Stacks aufladen ➔ 2. Thorns spamen ➔ 3. Blutrausch bei Bedarf zünden!",
            vorteileDe = listOf("Besiegt schwerste Boss-Gegner problemlos", "Sehr kostengünstig & stabil", "Keine Komplexität in der Rotation"),
            nachteileDe = listOf("Langsamerer Clear als Spiked Gauntlets")
        ),

        // 🌾 5. UNZERSTÖRBARER SOLO-PVE - BUILD 4: BATTLEAXE SOLO MISTS
        EquipmentBuild(
            id = "UNSTOPPABLE_PVE_BATTLEAXE",
            title = "🌾 Schlachtaxt Solo-Mists & Dungeon Krieger (T8.4)",
            category = BuildCategory.UNSTOPPABLE_PVE,
            weapon = "Schlachtaxt / Battleaxe (T8.4) + Nebelrufer (T8.4)",
            head = "Soldaten-Helm / Soldier Helmet (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Schreckenswolf",
            potion = "Major Healing Potion (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3500000L,
            estimatedMarginPercent = 40.0,
            damageRating = 94,
            defenseRating = 96,
            mobilityRating = 88,
            healingRating = 98,
            strongestWeaponHighlight = "🌾 SOLO MISTS KING: Perfekte Balance aus Mob-Farm Clear und Unbesiegbarkeit gegen Ganker!",
            aiEvaluationDe = "🌾 UNZERSTÖRBARER SOLO-PVE: Äußerst verlässlich in Mists & Corrupted Dungeons. Der Soldaten-Helm gewährt 2 Sekunden Unverwundbarkeit, falls man überrascht wird.",
            combatRotationDe = "1. Q-Axt-Risse auf Mobs auftragen ➔ 2. E-Axtwurf zünden für HP-Refill ➔ 3. Unbegrenzt weiterfarmin!",
            vorteileDe = listOf("Hervorragende Kombination aus PvE & PvP-Schutz", "Invasions-Sicher", "Hoher Einzelschaden"),
            nachteileDe = listOf("Benötigt Axt-Treffer für Heilung")
        ),

        // 🌾 5. UNZERSTÖRBARER SOLO-PVE - BUILD 5: LIGHT CROSSBOW DUNGEON HUNTER
        EquipmentBuild(
            id = "UNSTOPPABLE_PVE_CROSSBOW",
            title = "🌾 Leicht-Armbrust Solo-Dungeon Schnelläufer (T8.4)",
            category = BuildCategory.UNSTOPPABLE_PVE,
            weapon = "Leichte Armbrust / Light Crossbow (T8.4) + Muysak",
            head = "Gelehrten-Kapuze / Scholar Cowl (T8.4)",
            armor = "Söldner-Rüstung / Mercenary Jacket (T8.4)",
            shoes = "Königliche Schuhe / Royal Shoes (T8.4)",
            mount = "Swiftclaw",
            potion = "Major Poison Potion (T8)",
            food = "Schweinebraten / Roast Pork (T8.3)",
            estimatedCostSilver = 3300000L,
            estimatedMarginPercent = 43.0,
            damageRating = 98,
            defenseRating = 90,
            mobilityRating = 85,
            healingRating = 92,
            strongestWeaponHighlight = "🌾 EXPLOSIONS-SPAM: Q-Auto-Reset nach jedem Spell löscht Räume in Sekunden aus!",
            aiEvaluationDe = "🌾 UNZERSTÖRBARER SOLO-PVE: Q-Explosionen resetten automatisch nach jedem W/E-Spell. Ermöglicht pausenloses Bomben von Mob-Räumen.",
            combatRotationDe = "1. Q ➔ 2. W ➔ 3. Q ➔ 4. E ➔ 5. Q = Mob-Raum gesäubert!",
            vorteileDe = listOf("Sehr hoher konstanter AoE-Schaden", "Sehr günstiges Equipment", "Perfekt für Solo Dungeons"),
            nachteileDe = listOf("Kanalisierung der W-Fähigkeit erfordert Stehenbleiben")
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
