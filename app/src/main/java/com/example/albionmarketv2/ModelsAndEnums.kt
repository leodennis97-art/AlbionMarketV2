package com.example.albionmarketv2

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AlbionServer(val displayName: String, val apiDomain: String) {
    WEST("Americas (West)", "west.albion-online-data.com"),
    EAST("Asia (East)", "east.albion-online-data.com"),
    EUROPE("Europe (EU)", "europe.albion-online-data.com");

    val serverId: String
        get() = name.lowercase()

    companion object {
        fun fromName(name: String?): AlbionServer {
            return entries.find { it.name.equals(name, true) } ?: EUROPE
        }
        fun generateLiveEvents(): List<LiveEventItem> {
            return listOf(
                LiveEventItem(
                    id = "event_1",
                    title = "⚔️ Avalonian Invasion (T7 Blackzone)",
                    category = "Invasion",
                    remainingMinutes = 15,
                    zoneName = "Eldertree Slope (T7)",
                    zoneSafety = ZoneSafety.RED,
                    description = "Avalonian troops have invaded the zone. High rewards upon closing portals.",
                    rewardSummary = "Runen, Artefakte & Avalonian Energy"
                ),
                LiveEventItem(
                    id = "event_2",
                    title = "🕷️ Crystal Spider Boss Spawn",
                    category = "Boss",
                    remainingMinutes = 8,
                    zoneName = "Sting Fen (T6 Redzone)",
                    zoneSafety = ZoneSafety.RED,
                    description = "Crystal Spider has spawned in the Red Zone. High combat fame and loot.",
                    rewardSummary = "Kristalle, Seltene Ausrüstung & Silber"
                ),
                LiveEventItem(
                    id = "event_3",
                    title = "🌀 Legendary Mists Chest Active",
                    category = "Mists",
                    remainingMinutes = 24,
                    zoneName = "Die Nebel (The Mists)",
                    zoneSafety = ZoneSafety.RED,
                    description = "A radiant legendary chest has appeared in the Mists.",
                    rewardSummary = "Verzauberte T8 Ausrüstung & Folianten"
                ),
                LiveEventItem(
                    id = "event_4",
                    title = "🔥 2v2 Hellgate Portal Opened",
                    category = "PvP",
                    remainingMinutes = 11,
                    zoneName = "T5 Blackzone Portal",
                    zoneSafety = ZoneSafety.RED,
                    description = "Hellgate is active for group PvP combat and elite chest drop.",
                    rewardSummary = "PvP Ruhm, Folianten & Combat Loot"
                )
            )
        }
    }
}

enum class OrderStatus {
    PENDING, COMPLETED, CANCELLED, ACTIVE, DISCARDED
}

enum class ZoneSafety(val displayName: String) {
    BLUE("Sicher (Blau)"),
    YELLOW("Geprüft (Gelb)"),
    RED("Gefährlich (Rot)"),
    BLACK("Full-Loot (Schwarz)"),
    ROADS("Roads of Avalon"),
    MISTS("The Mists"),
    SAFE_BLUE("Sichere Blaue Zone"),
    SAFE_YELLOW("Sichere Gelbe Zone"),
    DANGEROUS_RED("Gefährliche Rote Zone"),
    DANGEROUS_BLACK("Full-Loot Schwarze Zone")
}

enum class MonsterSortMode {
    NAME, PROFIT, DIFFICULTY, MOST_LUCRATIVE
}

enum class PlayerCategory {
    ALL, SOLO, DUO, SMALL_GROUP, ZVZ, GROUP, RAID
}

enum class MonsterType {
    BOSS, GATHERING, GANG, WORLD_BOSS, ROAMING_BOSS, ASPECT, HELLGATE_BOSS, DUNGEON_BOSS, STATIC_BOSS, MISTS_BOSS, ROADS_BOSS, ELITE_MOB
}

enum class BuildCategory {
    SOLO_PVE, GROUP_PVE, CORRUPTED_DUNGEON, HELLGATE, MISTS, ZVZ, GATHERING, MELEE_DPS, DAMAGE_DPS
}

enum class TimerCategory {
    FARM, LABOURER, ALCHEMY, CUSTOM, CROP, ANIMAL
}

data class TradeOrder(
    val id: String = System.currentTimeMillis().toString(),
    val resourceId: String = "",
    val resourceNameDe: String = "",
    val resourceNameEn: String = "",
    val tier: Int = 4,
    val tierText: String = "T4",
    val enchantment: Int = 0,
    val buyCity: String = "Caerleon",
    val buyPrice: Int = 0,
    val sellCity: String = "Bridgewatch",
    val sellPrice: Int = 0,
    val plannedUnits: Int = 1,
    val actualUnits: Int = 1,
    val effectiveUnits: Int = 1,
    val effectiveBuyPrice: Int = 0,
    val effectiveSellPrice: Int = 0,
    val targetNetProfit: Long = 0L,
    val realizedNetProfit: Long = 0L,
    val targetInvestment: Long = 0L,
    val actualSilverSpent: Long = 0L,
    val actualSilverEarned: Long = 0L,
    val actualBuyPrice: Int = 0,
    val actualSellPrice: Int = 0,
    val acceptedDate: String = "",
    val completedDate: String = "",
    val status: OrderStatus = OrderStatus.PENDING,
    val isPriceStillValid: Boolean = true,
    val recommendedBuyOrderPrice: Int = 0,
    val recommendedSellOrderPrice: Int = 0
)

data class GoldPurchase(
    val id: String = System.currentTimeMillis().toString(),
    val amountGold: Int = 100,
    val buyPricePerGold: Int = 3000,
    val totalCostSilver: Long = 300000L,
    val purchaseDate: String = ""
)

data class GoldSale(
    val id: String = System.currentTimeMillis().toString(),
    val amountGold: Int = 100,
    val sellPricePerGold: Int = 3200,
    val totalEarnedSilver: Long = 320000L,
    val saleDate: String = ""
)

fun GoldSale.realizedProfit(avgBuy: Int): Long {
    val totalCost = amountGold.toLong() * avgBuy
    return totalEarnedSilver - totalCost
}

fun GoldSale.realizedRoiPercent(avgBuy: Int): Double {
    val totalCost = amountGold.toLong() * avgBuy
    if (totalCost <= 0L) return 0.0
    return (realizedProfit(avgBuy).toDouble() / totalCost) * 100.0
}

fun GoldPurchase.netProfitSilver(currentGoldPrice: Int): Long {
    val marketValue = amountGold.toLong() * currentGoldPrice
    val netValue = (marketValue * 0.94).toLong()
    return netValue - totalCostSilver
}

fun GoldPurchase.roiPercent(currentGoldPrice: Int): Double {
    if (totalCostSilver <= 0L) return 0.0
    return (netProfitSilver(currentGoldPrice).toDouble() / totalCostSilver) * 100.0
}

data class GoldPrice(
    val price: Int = 3000,
    val timestamp: String = "2024-01-01T00:00:00"
)

data class PriceSnapshot(
    val itemId: String = "",
    val city: String = "",
    val quality: Int = 1,
    val sellPriceMin: Int = 0,
    val sellPriceMinAmount: Int = 1,
    val buyPriceMax: Int = 0,
    val buyPriceMaxAmount: Int = 1,
    val timestampMs: Long = System.currentTimeMillis(),
    val serverId: String = "europe"
)

enum class TimeFrame {
    DAILY, WEEKLY, MONTHLY, DAY
}

data class EquipmentBuild(
    val id: String = "",
    val title: String = "",
    val category: BuildCategory = BuildCategory.SOLO_PVE,
    val estimatedCostSilver: Long = 100000L,
    val estimatedMarginPercent: Double = 15.0,
    val strongestWeaponHighlight: String = "",
    val useCaseFunction: String = "",
    val weapon: String = "",
    val head: String = "",
    val armor: String = "",
    val shoes: String = "",
    val mount: String = "",
    val potion: String = "",
    val food: String = "",
    val recommendedSkills: String = "",
    val headSkill: String = "",
    val armorSkill: String = "",
    val shoesSkill: String = "",
    val damageRating: Int = 80,
    val healingRating: Int = 10,
    val defenseRating: Int = 60,
    val mobilityRating: Int = 70,
    val aiEvaluationDe: String = "",
    val combatRotationDe: String = "",
    val vorteileDe: List<String> = emptyList(),
    val nachteileDe: List<String> = emptyList()
)

data class LiveEventItem(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val remainingMinutes: Int = 30,
    val zoneName: String = "",
    val zoneSafety: ZoneSafety = ZoneSafety.YELLOW,
    val description: String = "",
    val rewardSummary: String = ""
) {
    val isExpired: Boolean
        get() = remainingMinutes <= 0
}

data class WorldMapRegion(
    val name: String = "",
    val safety: ZoneSafety = ZoneSafety.YELLOW,
    val type: String = "City",
    val description: String = "",
    val resourcesFound: List<String> = emptyList(),
    val connectsTo: List<String> = emptyList()
)

data class AlbionWorldData(
    val name: String = "Caerleon",
    val nameDe: String = "Caerleon",
    val nameEn: String = "Caerleon",
    val zoneLocationDe: String = "Royal Continent",
    val zoneLocationEn: String = "Royal Continent",
    val tier: Int = 5,
    val zoneSafety: ZoneSafety = ZoneSafety.YELLOW,
    val chestDropSummaryDe: String = "Standard Loot",
    val estimatedProfitSilver: Long = 50000L,
    val difficultyDe: String = "Mittel",
    val difficultyEn: String = "Medium",
    val respawnTimeDe: String = "15 Min",
    val respawnTimeEn: String = "15 Min",
    val possibleChests: List<Any> = emptyList(),
    val combatTipsDe: String = "Achte auf Angriffe.",
    val combatTipsEn: String = "Watch out for attacks."
) {
    companion object {
        val bossLootList: List<BossLoot> = emptyList()
        val worldRegions: List<WorldMapRegion> = listOf(
            WorldMapRegion("Bridgewatch", ZoneSafety.SAFE_YELLOW, "Steppe", "Steppe", listOf("Ore", "Stone"), listOf("Martlock", "Lymhurst")),
            WorldMapRegion("Lymhurst", ZoneSafety.SAFE_YELLOW, "Forest", "Forest", listOf("Wood", "Fiber"), listOf("Bridgewatch", "Fort Sterling")),
            WorldMapRegion("Fort Sterling", ZoneSafety.SAFE_YELLOW, "Mountain", "Mountain", listOf("Rock", "Ore"), listOf("Lymhurst", "Thetford")),
            WorldMapRegion("Martlock", ZoneSafety.SAFE_YELLOW, "Highland", "Highland", listOf("Hide", "Stone"), listOf("Bridgewatch", "Thetford")),
            WorldMapRegion("Thetford", ZoneSafety.SAFE_YELLOW, "Swamp", "Swamp", listOf("Fiber", "Wood"), listOf("Fort Sterling", "Martlock")),
            WorldMapRegion("Caerleon", ZoneSafety.DANGEROUS_RED, "Black Market", "Black Market", listOf("All"), listOf("Outlands"))
        )
        fun generateLiveEvents(): List<LiveEventItem> = emptyList()
    }
}

data class IslandBuilding(
    val id: String = "",
    val nameDe: String = "Farm",
    val nameEn: String = "Farm",
    val maxTier: Int = 8,
    val cityBonusCity: String = "Lymhurst",
    val cityBonusDescEn: String = "Bonus",
    val cityBonusDescDe: String = "Bonus",
    val estimatedDailyIncomeSilver: Long = 20000L,
    val estimatedRoiPercent: Double = 10.0,
    val abilityDescDe: String = "",
    val upgrades: List<BuildingUpgrade> = emptyList()
)

data class BuildingUpgrade(
    val tierName: String = "T1",
    val tier: Int = 1,
    val silverCost: Long = 1000L,
    val woodReq: Int = 10,
    val stoneReq: Int = 10
)

data class CraftingRecipe(
    val id: String = "",
    val resourceId: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val tier: Int = 4,
    val category: ResourceCategory = ResourceCategory.RESOURCES,
    val ingredients: List<CraftingIngredient> = emptyList()
)

data class CraftingIngredient(
    val resourceId: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val amount: Int = 1
)

data class CraftingOpportunityDetails(
    val resource: AlbionResource? = null,
    val cheapestBuyCity: String = "",
    val totalIngredientCost: Long = 0L,
    val highestSellCity: String = "",
    val finishedItemSellPrice: Long = 0L,
    val netProfit: Long = 0L,
    val roiPercent: Double = 0.0,
    val recBuyOrderCost: Long = 0L,
    val recSellOrderPrice: Long = 0L,
    val maxOrderProfit: Long = 0L,
    val ingredientSummary: String = ""
)

data class IslandTimerItem(
    val id: String = System.currentTimeMillis().toString(),
    val nameDe: String = "",
    val category: TimerCategory = TimerCategory.CROP,
    val durationHours: Int = 22,
    val startTimeMs: Long = System.currentTimeMillis(),
    val expectedHarvestTimeMs: Long = System.currentTimeMillis() + (22L * 3600L * 1000L)
) {
    fun remainingTimeMs(): Long {
        return (expectedHarvestTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }
    fun isReady(): Boolean {
        return remainingTimeMs() <= 0L
    }
    fun remainingFormatted(): String {
        val rem = remainingTimeMs()
        if (rem <= 0L) return "Fertig zur Ernte! 🎉"
        val hours = rem / (3600 * 1000)
        val mins = (rem % (3600 * 1000)) / (60 * 1000)
        return "Verbleibend: ${hours}h ${mins}m"
    }
    fun expectedHarvestDateFormatted(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        return sdf.format(Date(expectedHarvestTimeMs))
        fun generateLiveEvents(): List<LiveEventItem> = emptyList()
    }
}
