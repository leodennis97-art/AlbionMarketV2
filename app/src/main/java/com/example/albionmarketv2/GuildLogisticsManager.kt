package com.example.albionmarketv2

import java.text.NumberFormat
import java.util.Locale

data class GuildGearSlotItem(
    val slotNameDe: String,
    val resourceId: String,
    val itemNameDe: String,
    val tier: Int,
    val quantityPerPlayer: Int = 1
)

data class GuildReGearBOMItem(
    val resourceId: String,
    val itemNameDe: String,
    val totalRequiredQuantity: Int,
    val cheapestCityName: String,
    val cheapestPricePerUnit: Int,
    val totalSilverCost: Long,
    val totalWeightKg: Double
)

data class GuildReGearSummary(
    val buildName: String,
    val playerCount: Int,
    val bomItems: List<GuildReGearBOMItem>,
    val grandTotalSilverCost: Long,
    val grandTotalWeightKg: Double,
    val recommendedTransportMountDe: String,
    val formattedSummaryDe: String
) {
    fun getFormattedSummary(lang: String): String = LanguageManager.translateUI(formattedSummaryDe, lang)
    fun getRecommendedMount(lang: String): String = LanguageManager.translateUI(recommendedTransportMountDe, lang)
}

data class GroupProfitSplitResult(
    val totalGrossRevenueSilver: Long,
    val totalExpensesSilver: Long,
    val totalNetProfitSilver: Long,
    val totalPlayers: Int,
    val sharePerPlayerSilver: Long,
    val gankGuardBonusSilver: Long,
    val summaryFormattedDe: String
) {
    fun getSummaryFormatted(lang: String): String = LanguageManager.translateUI(summaryFormattedDe, lang)
}

object GuildLogisticsManager {

    val PRESET_ZVZ_BUILDS = listOf(
        listOf(
            GuildGearSlotItem("Waffe", "T8_MAIN_PERMAFROST", "Permafrost-Prisma (T8)", 8),
            GuildGearSlotItem("Kopf", "T8_HEAD_SCHOLAR", "Gelehrten-Kapuze (T8)", 8),
            GuildGearSlotItem("Brust", "T8_ARMOR_CLOTH_ROYAL", "Königliches Gewand (T8)", 8),
            GuildGearSlotItem("Schuhe", "T8_SHOES_ROYAL", "Königliche Sandalen (T8)", 8),
            GuildGearSlotItem("Umhang", "T8_CAPE_LYMHURST", "Lymhurst-Umhang (T8)", 8),
            GuildGearSlotItem("Trank", "T8_POTION_GIGANT", "Riesentrank (T8)", 8, quantityPerPlayer = 5),
            GuildGearSlotItem("Essen", "T8_FOOD_OMELETTE", "Schweine-Omelett (T8)", 8, quantityPerPlayer = 2)
        ),
        listOf(
            GuildGearSlotItem("Waffe", "T8_MAIN_ARCANELOCUS", "Arkander Locus (T8)", 8),
            GuildGearSlotItem("Kopf", "T8_HEAD_JUDICATOR", "Richter-Helm (T8)", 8),
            GuildGearSlotItem("Brust", "T8_ARMOR_LEATHER_JUDICATOR", "Richter-Rüstung (T8)", 8),
            GuildGearSlotItem("Schuhe", "T8_SHOES_MERCENARY", "Söldner-Schuhe (T8)", 8),
            GuildGearSlotItem("Umhang", "T8_CAPE_FORTSTERLING", "FortSterling-Umhang (T8)", 8),
            GuildGearSlotItem("Trank", "T8_POTION_INVIS", "Unsichtbarkeitstrank (T8)", 8, quantityPerPlayer = 5)
        )
    )

    fun calculateGuildReGearBOM(
        buildName: String,
        loadoutItems: List<GuildGearSlotItem>,
        playerCount: Int,
        priceMap: Map<String, List<MarketPrice>>
    ): GuildReGearSummary {
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
        val bomList = mutableListOf<GuildReGearBOMItem>()
        var grandTotalSilver = 0L
        var grandTotalWeight = 0.0

        for (item in loadoutItems) {
            val totalQty = item.quantityPerPlayer * playerCount.coerceAtLeast(1)
            val resource = AlbionResourceRepository.resources.find { it.fullId == item.resourceId || it.id == item.resourceId }

            val prices = priceMap[item.resourceId] ?: emptyList()
            val cheapestMarket = prices.filter { it.sellPriceMin > 0 }.minByOrNull { it.sellPriceMin }

            val pricePerUnit = cheapestMarket?.sellPriceMin ?: 150_000
            val city = cheapestMarket?.city ?: "Bridgewatch"

            val totalCost = pricePerUnit.toLong() * totalQty
            val unitWeight = resource?.let { TradeCalculator.getItemWeightKg(it) } ?: 1.5
            val totalWeight = unitWeight * totalQty

            grandTotalSilver += totalCost
            grandTotalWeight += totalWeight

            bomList.add(
                GuildReGearBOMItem(
                    resourceId = item.resourceId,
                    itemNameDe = item.itemNameDe,
                    totalRequiredQuantity = totalQty,
                    cheapestCityName = city,
                    cheapestPricePerUnit = pricePerUnit,
                    totalSilverCost = totalCost,
                    totalWeightKg = totalWeight
                )
            )
        }

        val mountRec = when {
            grandTotalWeight > 10_000 -> "🦣 Mammut-Transport Ochse (15.000kg+)"
            grandTotalWeight > 3_000 -> "🐻 Grizzly-Bär / Gepanzerter Ochse T8"
            else -> "🐂 Standard Transport-Ochse T6"
        }

        val summaryText = "🛡️ **Gilden Re-Gear BOM: $buildName ($playerCount Spieler)**\n" +
                "💰 Gesamtkosten: ${fmt.format(grandTotalSilver)} Silber\n" +
                "⚖️ Gesamtgewicht: ${fmt.format(grandTotalWeight.toInt())} kg\n" +
                "🚚 Empfohlenes Reit tier: $mountRec"

        return GuildReGearSummary(
            buildName = buildName,
            playerCount = playerCount,
            bomItems = bomList,
            grandTotalSilverCost = grandTotalSilver,
            grandTotalWeightKg = grandTotalWeight,
            recommendedTransportMountDe = mountRec,
            formattedSummaryDe = summaryText
        )
    }

    fun calculateGroupProfitSplit(
        grossRevenueSilver: Long,
        repairCostSilver: Long,
        foodPotionCostSilver: Long,
        gankGuardBonusPercent: Double = 10.0,
        playerCount: Int = 4,
        gankGuardCount: Int = 1
    ): GroupProfitSplitResult {
        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
        val totalExpenses = repairCostSilver + foodPotionCostSilver
        val netProfit = (grossRevenueSilver - totalExpenses).coerceAtLeast(0L)

        val count = playerCount.coerceAtLeast(1)
        val gankGuardBonus = ((netProfit * (gankGuardBonusPercent / 100.0)) / gankGuardCount.coerceAtLeast(1)).toLong()
        val poolAfterBonus = (netProfit - (gankGuardBonus * gankGuardCount)).coerceAtLeast(0L)

        val sharePerPlayer = poolAfterBonus / count

        val summary = "🤝 **Gruppen-Transport Gewinnaufteilung**\n" +
                "💰 Netto-Gewinn: ${fmt.format(netProfit)} Silber\n" +
                "👤 Auszahlung pro Spieler ($count Personen): ${fmt.format(sharePerPlayer)} Silber\n" +
                "⚔️ Eskorten-Bonus ($gankGuardCount Scout): +${fmt.format(gankGuardBonus)} Silber"

        return GroupProfitSplitResult(
            totalGrossRevenueSilver = grossRevenueSilver,
            totalExpensesSilver = totalExpenses,
            totalNetProfitSilver = netProfit,
            totalPlayers = count,
            sharePerPlayerSilver = sharePerPlayer,
            gankGuardBonusSilver = gankGuardBonus,
            summaryFormattedDe = summary
        )
    }
}
