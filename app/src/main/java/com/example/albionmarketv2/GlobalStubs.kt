package com.example.albionmarketv2

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = TextStyle.Default
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

// --- Additional Stubs & Extensions ---

@Composable
fun SharedViewModelProvider.get(vararg args: Any?): AlbionResourceViewModel {
    val context = LocalContext.current
    val app = context.applicationContext as? Application ?: Application()
    return AlbionResourceViewModel(app)
}

fun TradeOrder.isPriceStillValid(vararg args: Any?): Boolean = true

val BuildCategory.icon: String get() = "⚔️"
val BuildCategory.displayName: String get() = name

val PlayerCategory.iconEmoji: String get() = "👥"
val PlayerCategory.displayNameDe: String get() = name

val ZoneSafety.displayName: String get() = name

data class MonsterDrop(
    val itemNameDe: String = "Item",
    val itemNameEn: String = "Item",
    val isRare: Boolean = false,
    val dropChancePercent: Int = 10
)

data class BossChest(
    val nameDe: String = "Chest",
    val rarity: String = "Rare",
    val dropChancePercent: Int = 10
)

data class BossLoot(
    val name: String = "",
    val possibleChests: List<BossChest> = emptyList()
)

data class MonsterItem(
    val id: String = "",
    val nameDe: String = "",
    val nameEn: String = "",
    val type: MonsterType = MonsterType.WORLD_BOSS,
    val playerCategory: PlayerCategory = PlayerCategory.SOLO,
    val recommendedPlayerCount: String = "1",
    val estimatedSilverPerHour: String = "100k S/h",
    val imageUrl: String = "",
    val zoneLocationDe: String = "Outlands",
    val zoneLocationEn: String = "Outlands",
    val zoneSafety: ZoneSafety = ZoneSafety.YELLOW,
    val tier: Int = 4,
    val chestDropSummaryDe: String = "Loot",
    val estimatedProfitSilver: Long = 10000L,
    val difficultyDe: String = "Mittel",
    val difficultyEn: String = "Medium",
    val respawnTimeDe: String = "15 Min",
    val respawnTimeEn: String = "15 Min",
    val drops: List<MonsterDrop> = emptyList(),
    val combatTipsDe: String = "Tipp",
    val combatTipsEn: String = "Tip"
)

data class BossAdvice(
    val aiRecommendationDe: String = "Empfehlung",
    val estimatedProfitScore: String = "Hoch",
    val riskLevelDe: String = "Gering"
)

val MonsterSortMode.labelDe: String get() = name
val MonsterSortMode.labelEn: String get() = name

val worldRegions: List<WorldMapRegion> get() = listOf(
    WorldMapRegion("Bridgewatch", ZoneSafety.SAFE_YELLOW, "Steppe", "Steppe", listOf("Ore", "Stone"), listOf("Martlock", "Lymhurst")),
    WorldMapRegion("Lymhurst", ZoneSafety.SAFE_YELLOW, "Forest", "Forest", listOf("Wood", "Fiber"), listOf("Bridgewatch", "Fort Sterling")),
    WorldMapRegion("Fort Sterling", ZoneSafety.SAFE_YELLOW, "Mountain", "Mountain", listOf("Rock", "Ore"), listOf("Lymhurst", "Thetford")),
    WorldMapRegion("Martlock", ZoneSafety.SAFE_YELLOW, "Highland", "Highland", listOf("Hide", "Stone"), listOf("Bridgewatch", "Thetford")),
    WorldMapRegion("Thetford", ZoneSafety.SAFE_YELLOW, "Swamp", "Swamp", listOf("Fiber", "Wood"), listOf("Fort Sterling", "Martlock")),
    WorldMapRegion("Caerleon", ZoneSafety.DANGEROUS_RED, "Black Market", "Black Market", listOf("All"), listOf("Outlands"))
)

val bossLootList: List<BossLoot> get() = emptyList()

data class GoldBotAnalysis(
    val trendForecastDe: String = "Stabil",
    val dailyLow: Int = 3000,
    val monthlyLow: Int = 2800,
    val dailyHigh: Int = 3200,
    val monthlyHigh: Int = 3400,
    val forecast3Days: Int = 3100,
    val forecast7Days: Int = 3200,
    val recommendedBuyOrderPrice: Int = 2900,
    val expectedPriceDropPercent: Double = 3.5,
    val buyOrderProbabilityStr: String = "Hoch",
    val recommendedSellOrderPrice: Int = 3300,
    val expectedPriceRisePercent: Double = 4.0,
    val sellOrderProbabilityStr: String = "Mittel",
    val aiOrderRecommendationTextDe: String = "Kaufen bei Dip",
    val expectedNetProfitPerGold: Long = 200L,
    val expectedRoiPercent: Double = 5.0
)

fun LanguageManager.setLanguage(vararg args: Any?) {}
fun NetworkDependencyManager.setupPermissiveSSLAndHostnameVerifier(vararg args: Any?) {}
fun MarketPrice.isUnrealisticPrice(vararg args: Any?): Boolean = sellPriceMin <= 0 || buyPriceMax <= 0
