package com.example.albionmarketv2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class OpportunitySort {
    HIGHEST_MARGIN,
    NEWEST,
    FEWEST_STOCK
}

data class ResourceUiState(
    val selectedCategory: ResourceCategory = ResourceCategory.ALL,
    val selectedTier: Int = 0, // 0 means all tiers
    val selectedEnchantment: Int = 0, // 0 to 4
    val searchQuery: String = "",
    val filteredResources: List<AlbionResource> = AlbionResourceRepository.resources,
    val selectedResource: AlbionResource? = null,
    val sortOption: OpportunitySort = OpportunitySort.HIGHEST_MARGIN,
    val selectedTierFilter: Int = 0, // 0 = All, 1..8 = Tiers 1-8
    val selectedOpportunityEnchantmentFilter: Int = -1, // -1 = All, 0..4 = Enchantment levels
    val opportunitySearchQuery: String = "",

    // Navigation Tabs:
    // 0 = Aufträge & Historie, 1 = Rechner & Marge (Top 50), 2 = Katalog, 3 = Order-Statistik,
    // 4 = KI Ausrüstung & Build-Sets, 5 = Events & Loot, 6 = Goldmarkt, 7 = Weltkarte
    val activeTab: Int = 1,

    // Market & Calculator
    val server: AlbionServer = AlbionServer.EUROPE,
    val silverBudget: Long = 1_000_000L,
    val carryCapacityKg: Double = 2_000.0,
    val goldAmount: Long = 0L,
    val selectedMount: String = "Transportochs T5 (+1800 kg)",
    val selectedBag: String = "Tasche T5 (+220 kg)",
    val selectedBoots: String = "Transport-Schuhe T4 (+80 kg)",
    val targetMarginPercent: Double = 5.0,
    val maxZonesFilter: Int = 99, // Max zu durchlaufende Gebiete / Zonen
    val hideBrecilien: Boolean = false,
    val hideBlackMarket: Boolean = false,
    val favoriteItemIds: Set<String> = emptySet(),
    val craftingMasteryLevel: Int = 50,
    val selectedOpportunityCategory: ResourceCategory = ResourceCategory.ALL,
    val aiAnalyzerEnabled: Boolean = true,
    val aiMinMarginPercent: Double = 5.0,
    val aiAnalysisResult: AiAnalysisResult? = null,
    val hasPremium: Boolean = true,
    val avoidDangerousZones: Boolean = false, // Caerleon / Rot-Zone Filter
    val filterHighPriorityOnly: Boolean = false, // Filter Prio 90-100%

    // System Notifications Config
    val systemNotificationsEnabled: Boolean = true,
    val goldNotificationsEnabled: Boolean = true,
    val appLanguage: String = "DE",
    val callMeBotPhone: String = "",
    val callMeBotApiKey: String = "",
    val callMeBotAutoSend: Boolean = false,
    val showSettingsDialog: Boolean = false,

    // Total scanned items count across cycles
    val totalScannedItemsCount: Long = 0L,

    // Active & Completed Orders (Max 10 active orders allowed)
    val tradeOrders: List<TradeOrder> = emptyList(),
    val orderErrorMsg: String? = null,

    // Gold Market & Portfolio
    val goldPurchases: List<GoldPurchase> = emptyList(),
    val goldSales: List<GoldSale> = emptyList(),
    val goldPrices: List<GoldPrice> = emptyList(),
    val serverGoldPrices: Map<AlbionServer, Int> = emptyMap(),
    val currentGoldPrice: Int = 8250,
    val isLoadingGold: Boolean = false,
    val lastGoldFetchTime: String? = null,

    // Market Statistics & Timeframes
    val priceSnapshots: List<PriceSnapshot> = emptyList(),
    val selectedTimeFrame: TimeFrame = TimeFrame.DAY,

    // KI Ausrüstung & Build-Sets
    val equipmentBuilds: List<EquipmentBuild> = EquipmentBuildRepository.builds,
    val selectedBuildCategory: BuildCategory = BuildCategory.MELEE_DPS,

    val marketPrices: Map<String, List<MarketPrice>> = emptyMap(),
    val tradeOpportunities: List<TradeOpportunity> = emptyList(),
    val selectedCityFilter: String = "ALLE",
    val liveEventsList: List<LiveEventItem> = AlbionWorldData.generateLiveEvents().filter { !it.isExpired },
    val isLoadingPrices: Boolean = false,
    val priceFetchError: String? = null,
    val lastFetchTime: String? = null,

    // Server Live Download Analytics
    val totalServerDownloads: Int = 0,
    val hourlyDownloads24h: List<HourlyDownloadStat> = emptyList(),
) {
    val marketTaxPercent: Double
        get() = if (hasPremium) 4.0 else 8.0

    val activeOrders: List<TradeOrder>
        get() = tradeOrders.filter { it.status == OrderStatus.ACTIVE }

    val completedOrders: List<TradeOrder>
        get() = tradeOrders.filter { it.status == OrderStatus.COMPLETED }

    val totalCompletedProfit: Long
        get() = completedOrders.sumOf { it.realizedNetProfit }

    val totalCompletedSpent: Long
        get() = completedOrders.sumOf { it.actualSilverSpent ?: it.targetInvestment }

    val totalCompletedEarned: Long
        get() = completedOrders.sumOf { it.actualSilverEarned ?: (it.targetInvestment + it.targetNetProfit) }

    val totalGoldBought: Int
        get() = goldPurchases.sumOf { it.amountGold }

    val totalGoldSold: Int
        get() = goldSales.sumOf { it.amountGold }

    val totalGoldOwned: Int
        get() = (totalGoldBought - totalGoldSold).coerceAtLeast(0)

    val totalGoldCostSilver: Long
        get() = goldPurchases.sumOf { it.totalCostSilver }

    val totalGoldEarnedSilver: Long
        get() = goldSales.sumOf { it.totalEarnedSilver }

    val avgGoldBuyPrice: Int
        get() = if (totalGoldBought > 0) (totalGoldCostSilver / totalGoldBought).toInt() else currentGoldPrice

    val remainingGoldCostSilver: Long
        get() = totalGoldOwned.toLong() * avgGoldBuyPrice

    val totalGoldCurrentValueSilver: Long
        get() = totalGoldOwned.toLong() * currentGoldPrice

    @Suppress("unused")
    val realizedGoldProfitSilver: Long
        get() = totalGoldEarnedSilver - (totalGoldSold.toLong() * avgGoldBuyPrice)

    @Suppress("unused")
    val unrealizedGoldProfitSilver: Long
        get() = totalGoldCurrentValueSilver - remainingGoldCostSilver

    val totalGoldNetProfitSilver: Long
        get() = (totalGoldCurrentValueSilver + totalGoldEarnedSilver) - totalGoldCostSilver

    val goldBuyPrice: Int
        get() = currentGoldPrice

    val goldSellPrice: Int
        get() = (currentGoldPrice * (1.0 - (marketTaxPercent / 100.0))).toInt()
}

@Suppress("unused")
class AlbionResourceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val notifiedKeys = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(
        ResourceUiState(
            server = prefs.server,
            silverBudget = prefs.silverBudget,
            carryCapacityKg = prefs.carryCapacityKg,
            goldAmount = prefs.goldAmount,
            selectedMount = prefs.selectedMount,
            selectedBag = prefs.selectedBag,
            selectedBoots = prefs.selectedBoots,
            targetMarginPercent = prefs.targetMarginPercent,
            hasPremium = prefs.hasPremium,
            avoidDangerousZones = prefs.avoidDangerousZones,
            maxZonesFilter = prefs.maxCityDistance,
            hideBrecilien = prefs.hideBrecilien,
            hideBlackMarket = prefs.hideBlackMarket,
            favoriteItemIds = prefs.favoriteItemIds,
            craftingMasteryLevel = prefs.craftingMasteryLevel,
            systemNotificationsEnabled = prefs.systemNotificationsEnabled,
            goldNotificationsEnabled = prefs.goldNotificationsEnabled,
            appLanguage = prefs.appLanguage,
            callMeBotPhone = prefs.callMeBotPhone,
            callMeBotApiKey = prefs.callMeBotApiKey,
            callMeBotAutoSend = prefs.callMeBotAutoSend,
            tradeOrders = prefs.getTradeOrders(),
            goldPurchases = prefs.getGoldPurchases(),
            goldSales = prefs.getGoldSales(),
            priceSnapshots = prefs.getPriceSnapshots(),
            marketPrices = emptyMap(),
        )
    )
    val uiState: StateFlow<ResourceUiState> = _uiState.asStateFlow()

    init {
        performInitialFullDataSync()
        startUnifiedMasterUpdateCycle()

        if (prefs.systemNotificationsEnabled) {
            NotificationHelper.showTradeNotification(
                getApplication(),
                title = "Albion Markt Scanner gestartet",
                message = "Automatisches Hintergrund-Scanning aktiv – Benachrichtigungen für gewinnbringende Handelschancen sind aktiviert!"
            )
        }
    }

    private fun performInitialFullDataSync() {
        viewModelScope.launch {
            // Einmaliges vollständiges Laden aller Daten beim App-Start
            _uiState.value = _uiState.value.copy(isLoadingPrices = true)
            val fallback = withContext(Dispatchers.IO) { AlbionMarketApi.getFallbackMarketPrices() }
            _uiState.value = _uiState.value.copy(marketPrices = fallback)
            fetchMarketPricesInternal()
            fetchGoldPricesInternal()
            val freshEvents = AlbionWorldData.generateLiveEvents()
            val initialOrders = prefs.getTradeOrders()
            _uiState.value = _uiState.value.copy(
                liveEventsList = freshEvents,
                tradeOrders = initialOrders
            )
            ServerSyncManager.pingServer(getApplication(), initialOrders.count { it.status == OrderStatus.ACTIVE })
            _uiState.value = _uiState.value.copy(isLoadingPrices = false)
        }
    }

    private fun startUnifiedMasterUpdateCycle() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
            var cycleTick = 0L

            while (true) {
                val nowStr = sdf.format(Date())

                // 1. Marktdaten & Server-Sync Zyklus (alle 2 Minuten bei 30s Haupttakt)
                if ((cycleTick % 4L) == 0L) {
                    _uiState.value = _uiState.value.copy(lastFetchTime = nowStr)
                    fetchMarketPricesInternal()
                    reloadOrdersFromPrefs()
                    val serverStats = ServerSyncManager.pingServer(getApplication(), _uiState.value.activeOrders.size)
                    serverStats?.let { stats ->
                        _uiState.value = _uiState.value.copy(
                            totalServerDownloads = stats.totalDownloads,
                            hourlyDownloads24h = stats.hourly24h
                        )
                    }
                }

                // 2. Goldmarkt-Kurs Zyklus (alle 5 Minuten)
                if ((cycleTick % 10L) == 0L && cycleTick > 0L) {
                    fetchGoldPricesInternal()
                }

                // 3. Live Events Zyklus (alle 15 Minuten)
                if ((cycleTick % 30L) == 0L && cycleTick > 0L) {
                    val freshEvents = AlbionWorldData.generateLiveEvents()
                    _uiState.value = _uiState.value.copy(liveEventsList = freshEvents)
                }

                cycleTick++
                delay(30.seconds) // Optimierter 30-Sekunden-Haupttakt zur Reduzierung von CPU-Auslastung und Überhitzung
            }
        }
    }

    fun reloadOrdersFromPrefs() {
        val updatedOrders = prefs.getTradeOrders()
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun onTabSelected(tabIndex: Int) {
        reloadOrdersFromPrefs()
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun onCategorySelected(category: ResourceCategory) {
        val current = _uiState.value
        updateState(current.copy(selectedCategory = category))
    }

    fun onTierSelected(tier: Int) {
        val current = _uiState.value
        val newTier = if (current.selectedTier == tier) 0 else tier
        updateState(current.copy(selectedTier = newTier))
    }

    fun onEnchantmentSelected(enchantment: Int) {
        val current = _uiState.value
        updateState(current.copy(selectedEnchantment = enchantment))
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        updateState(current.copy(searchQuery = query))
    }

    fun forceRefreshMarketData() {
        viewModelScope.launch {
            fetchMarketPricesInternal()
            fetchGoldPricesInternal()
        }
    }

    fun onResourceClicked(resource: AlbionResource) {
        _uiState.value = _uiState.value.copy(selectedResource = resource)
    }

    fun onDismissDetails() {
        _uiState.value = _uiState.value.copy(selectedResource = null)
    }

    fun onServerChanged(server: AlbionServer) {
        prefs.server = server
        _uiState.value = _uiState.value.copy(server = server)
        viewModelScope.launch {
            fetchMarketPricesInternal()
            fetchGoldPricesInternal()
        }
    }

    fun onSilverBudgetChanged(budget: Long) {
        prefs.silverBudget = budget
        val current = _uiState.value
        val updated = current.copy(silverBudget = budget)
        _uiState.value = updated
        recalculateOpportunities(updated)
    }

    fun onGoldPriceChanged(price: Int) {
        val current = _uiState.value
        val updated = current.copy(currentGoldPrice = price)
        _uiState.value = updated
        recalculateOpportunities(updated)
    }

    fun onEquipmentLoadoutChanged(mount: String, bag: String, boots: String) {
        prefs.selectedMount = mount
        prefs.selectedBag = bag
        prefs.selectedBoots = boots

        val totalCapacity = TradeCalculator.calculateTotalCapacityKg(mount, bag, boots)
        prefs.carryCapacityKg = totalCapacity

        val current = _uiState.value
        val updated = current.copy(
            selectedMount = mount,
            selectedBag = bag,
            selectedBoots = boots,
            carryCapacityKg = totalCapacity
        )
        recalculateOpportunities(updated)
    }

    fun onCarryCapacityChanged(capacity: Double) {
        prefs.carryCapacityKg = capacity
        val current = _uiState.value
        val updated = current.copy(carryCapacityKg = capacity)
        _uiState.value = updated
        recalculateOpportunities(updated)
    }

    fun onGoldAmountChanged(amount: Long) {
        prefs.goldAmount = amount
        val current = _uiState.value
        val updated = current.copy(goldAmount = amount)
        _uiState.value = updated
    }

    fun onTargetMarginChanged(marginPercent: Double) {
        prefs.targetMarginPercent = marginPercent
        val current = _uiState.value
        val updated = current.copy(targetMarginPercent = marginPercent)
        recalculateOpportunities(updated)
    }

    fun onMaxZonesFilterChanged(maxZones: Int) {
        val clamped = maxZones.coerceIn(0, 99)
        prefs.maxCityDistance = clamped
        val current = _uiState.value
        val updated = current.copy(maxZonesFilter = clamped)
        recalculateOpportunities(updated)
    }

    fun onHideBrecilienToggled(hide: Boolean) {
        prefs.hideBrecilien = hide
        prefs.bubbleHideBrecilien = hide
        val current = _uiState.value
        val updated = current.copy(hideBrecilien = hide)
        updateState(updated)
        recalculateOpportunities(updated)
    }

    fun onHideBlackMarketToggled(hide: Boolean) {
        prefs.hideBlackMarket = hide
        prefs.bubbleHideBlackMarket = hide
        val current = _uiState.value
        val updated = current.copy(hideBlackMarket = hide)
        updateState(updated)
        recalculateOpportunities(updated)
    }

    fun onToggleFavorite(itemId: String) {
        val currentSet = _uiState.value.favoriteItemIds
        val newSet = if (currentSet.contains(itemId)) currentSet - itemId else currentSet + itemId
        prefs.favoriteItemIds = newSet
        _uiState.value = _uiState.value.copy(favoriteItemIds = newSet)
    }

    fun onCraftingMasteryChanged(level: Int) {
        val clamped = level.coerceIn(0, 100)
        prefs.craftingMasteryLevel = clamped
        _uiState.value = _uiState.value.copy(craftingMasteryLevel = clamped)
    }

    fun onPremiumToggled(hasPremium: Boolean) {
        prefs.hasPremium = hasPremium
        val current = _uiState.value
        val updated = current.copy(hasPremium = hasPremium)
        updateState(updated)
        recalculateOpportunities(updated)
    }

    fun onAvoidDangerousZonesToggled(avoid: Boolean) {
        prefs.avoidDangerousZones = avoid
        prefs.bubbleAvoidDangerousZones = avoid
        val current = _uiState.value
        val updated = current.copy(avoidDangerousZones = avoid)
        updateState(updated)
        recalculateOpportunities(updated)
    }

    fun onFilterHighPriorityToggled(enabled: Boolean) {
        val current = _uiState.value
        val updated = current.copy(filterHighPriorityOnly = enabled)
        recalculateOpportunities(updated)
    }

    fun onOpportunitySortChanged(sort: OpportunitySort) {
        val current = _uiState.value
        val updated = current.copy(sortOption = sort)
        recalculateOpportunities(updated)
    }

    fun onTierFilterChanged(tier: Int) {
        val current = _uiState.value
        val updated = current.copy(selectedTierFilter = tier)
        recalculateOpportunities(updated)
    }

    fun onOpportunityEnchantmentFilterChanged(enchantment: Int) {
        val current = _uiState.value
        val updated = current.copy(selectedOpportunityEnchantmentFilter = enchantment)
        recalculateOpportunities(updated)
    }

    fun onOpportunitySearchQueryChanged(query: String) {
        val current = _uiState.value
        val updated = current.copy(opportunitySearchQuery = query)
        recalculateOpportunities(updated)
    }

    fun onTimeFrameSelected(timeFrame: TimeFrame) {
        _uiState.value = _uiState.value.copy(selectedTimeFrame = timeFrame)
    }

    fun onBuildCategorySelected(category: BuildCategory) {
        _uiState.value = _uiState.value.copy(selectedBuildCategory = category)
    }

    fun onOpenSettings() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun onDismissSettings() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun onToggleSystemNotifications(enabled: Boolean) {
        prefs.systemNotificationsEnabled = enabled
        _uiState.value = _uiState.value.copy(systemNotificationsEnabled = enabled)
    }

    fun onToggleGoldNotifications(enabled: Boolean) {
        prefs.goldNotificationsEnabled = enabled
        _uiState.value = _uiState.value.copy(goldNotificationsEnabled = enabled)
    }

    fun onLanguageChanged(langCode: String) {
        prefs.appLanguage = langCode
        _uiState.value = _uiState.value.copy(appLanguage = langCode)
    }

    fun onSaveCallMeBotSettings(phone: String, apiKey: String, autoSend: Boolean) {
        prefs.callMeBotPhone = phone
        prefs.callMeBotApiKey = apiKey
        prefs.callMeBotAutoSend = autoSend
        _uiState.value = _uiState.value.copy(
            callMeBotPhone = phone,
            callMeBotApiKey = apiKey,
            callMeBotAutoSend = autoSend
        )
    }

    // Trade Order Actions (Limit bis zu 10 gleichzeitig)
    fun acceptTradeOpportunity(opp: TradeOpportunity) {
        reloadOrdersFromPrefs()
        if (_uiState.value.activeOrders.size >= 10) {
            _uiState.value = _uiState.value.copy(
                orderErrorMsg = "Maximal 10 aktive Aufträge gleichzeitig erlaubt! Bitte schließe zuerst einen bestehenden Auftrag ab."
            )
            return
        }

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val newOrder = TradeOrder(
            id = UUID.randomUUID().toString(),
            resourceId = opp.resource.fullId,
            resourceNameDe = opp.resource.nameDe,
            resourceNameEn = opp.resource.nameEn,
            tier = opp.resource.tier,
            buyCity = opp.buyCity,
            buyPrice = opp.buyPrice,
            sellCity = opp.sellCity,
            sellPrice = opp.sellPrice,
            plannedUnits = opp.tradeUnits,
            targetNetProfit = opp.totalNetProfit,
            targetInvestment = opp.totalInvestment,
            acceptedDate = dateStr,
            status = OrderStatus.ACTIVE,
            recommendedBuyOrderPrice = if (opp.recommendedBuyOrderPrice > 0) opp.recommendedBuyOrderPrice else (opp.buyPrice * 0.88).toInt().coerceAtLeast(1),
            recommendedSellOrderPrice = if (opp.recommendedSellOrderPrice > 0) opp.recommendedSellOrderPrice else (opp.sellPrice * 1.08).toInt().coerceAtLeast(1)
        )

        val updatedOrders = _uiState.value.tradeOrders + newOrder
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(
            tradeOrders = updatedOrders,
            orderErrorMsg = null
        )
    }

    fun acceptCustomOrder(
        resourceId: String,
        resourceNameDe: String,
        tier: Int,
        buyCity: String,
        buyPrice: Int,
        sellCity: String,
        sellPrice: Int,
        plannedUnits: Int,
        targetProfit: Long
    ) {
        val currentOrders = _uiState.value.tradeOrders
        val activeOrders = currentOrders.filter { it.status == OrderStatus.ACTIVE }
        if (activeOrders.size >= 10) {
            _uiState.value = _uiState.value.copy(
                orderErrorMsg = "Maximal 10 aktive Aufträge gleichzeitig erlaubt! Schließe einen bestehenden Auftrag ab, um einen neuen anzunehmen."
            )
            return
        }

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val investment = buyPrice.toLong() * plannedUnits
        val newOrder = TradeOrder(
            id = UUID.randomUUID().toString(),
            resourceId = resourceId,
            resourceNameDe = resourceNameDe,
            resourceNameEn = resourceNameDe,
            tier = tier,
            buyCity = buyCity,
            buyPrice = buyPrice,
            sellCity = sellCity,
            sellPrice = sellPrice,
            plannedUnits = plannedUnits,
            targetNetProfit = targetProfit,
            targetInvestment = investment,
            acceptedDate = dateStr,
            status = OrderStatus.ACTIVE,
            recommendedBuyOrderPrice = (buyPrice * 0.88).toInt().coerceAtLeast(1),
            recommendedSellOrderPrice = (sellPrice * 1.08).toInt().coerceAtLeast(1)
        )

        val updatedOrders = _uiState.value.tradeOrders + newOrder
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(
            tradeOrders = updatedOrders,
            orderErrorMsg = null
        )
    }

    fun clearOrderError() {
        _uiState.value = _uiState.value.copy(orderErrorMsg = null)
    }

    fun completeTradeOrder(orderId: String, actualSpent: Long, actualEarned: Long) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val updatedOrders = _uiState.value.tradeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = OrderStatus.COMPLETED,
                    actualSilverSpent = actualSpent,
                    actualSilverEarned = actualEarned,
                    completedDate = dateStr
                )
            } else order
        }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun completeTradeOrderWithDetails(
        orderId: String,
        actualUnits: Int,
        actualBuyPrice: Int,
        actualSellPrice: Int
    ) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val spent = actualUnits.toLong() * actualBuyPrice.toLong()

        val taxPerUnit = (actualSellPrice * (_uiState.value.marketTaxPercent / 100.0)).toLong()
        val setupFeePerUnit = (actualSellPrice * 0.025).toLong()
        val netSellPrice = actualSellPrice.toLong() - taxPerUnit - setupFeePerUnit
        val earned = actualUnits.toLong() * netSellPrice

        val updatedOrders = _uiState.value.tradeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = OrderStatus.COMPLETED,
                    actualUnits = actualUnits,
                    actualBuyPrice = actualBuyPrice,
                    actualSellPrice = actualSellPrice,
                    actualSilverSpent = spent,
                    actualSilverEarned = earned,
                    completedDate = dateStr
                )
            } else order
        }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun bookAndCompleteOpportunity(
        opp: TradeOpportunity,
        actualUnits: Int,
        actualBuyPrice: Int,
        actualSellPrice: Int
    ) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val spent = actualUnits.toLong() * actualBuyPrice.toLong()

        val taxPerUnit = (actualSellPrice * (_uiState.value.marketTaxPercent / 100.0)).toLong()
        val setupFeePerUnit = (actualSellPrice * 0.025).toLong()
        val netSellPrice = actualSellPrice.toLong() - taxPerUnit - setupFeePerUnit
        val earned = actualUnits.toLong() * netSellPrice

        val completedOrder = TradeOrder(
            id = UUID.randomUUID().toString(),
            resourceId = opp.resource.fullId,
            resourceNameDe = opp.resource.nameDe,
            resourceNameEn = opp.resource.nameEn,
            tier = opp.resource.tier,
            buyCity = opp.buyCity,
            buyPrice = opp.buyPrice,
            sellCity = opp.sellCity,
            sellPrice = opp.sellPrice,
            plannedUnits = opp.tradeUnits,
            targetNetProfit = opp.totalNetProfit,
            targetInvestment = opp.totalInvestment,
            acceptedDate = dateStr,
            status = OrderStatus.COMPLETED,
            actualUnits = actualUnits,
            actualBuyPrice = actualBuyPrice,
            actualSellPrice = actualSellPrice,
            actualSilverSpent = spent,
            actualSilverEarned = earned,
            completedDate = dateStr
        )

        val updatedOrders = _uiState.value.tradeOrders + completedOrder
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders, orderErrorMsg = null)
    }

    fun updateCompletedOrder(orderId: String, newSpent: Long, newEarned: Long) {
        val updatedOrders = _uiState.value.tradeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(
                    actualSilverSpent = newSpent,
                    actualSilverEarned = newEarned
                )
            } else order
        }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun updateCompletedOrderWithDetails(
        orderId: String,
        newUnits: Int,
        newBuyPrice: Int,
        newSellPrice: Int
    ) {
        val spent = newUnits.toLong() * newBuyPrice.toLong()

        val taxPerUnit = (newSellPrice * (_uiState.value.marketTaxPercent / 100.0)).toLong()
        val setupFeePerUnit = (newSellPrice * 0.025).toLong()
        val netSellPrice = newSellPrice.toLong() - taxPerUnit - setupFeePerUnit
        val earned = newUnits.toLong() * netSellPrice

        val updatedOrders = _uiState.value.tradeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(
                    actualUnits = newUnits,
                    actualBuyPrice = newBuyPrice,
                    actualSellPrice = newSellPrice,
                    actualSilverSpent = spent,
                    actualSilverEarned = earned
                )
            } else order
        }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun acceptIslandBuildingOrder(bldg: IslandBuilding) {
        val currentOrders = _uiState.value.tradeOrders
        val active = currentOrders.filter { it.status == OrderStatus.ACTIVE }
        if (active.size >= 10) {
            _uiState.value = _uiState.value.copy(orderErrorMsg = "⚠️ Maximal 10 aktive Aufträge gleichzeitig erlaubt!")
            return
        }
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val totalCost = bldg.upgrades.sumOf { it.silverCost }
        val newOrder = TradeOrder(
            id = UUID.randomUUID().toString(),
            resourceId = bldg.id,
            resourceNameDe = bldg.nameDe,
            resourceNameEn = bldg.nameEn,
            tier = bldg.maxTier,
            buyCity = bldg.cityBonusCity.split("&").first().trim(),
            buyPrice = (totalCost / bldg.maxTier.coerceAtLeast(1)).toInt().coerceAtLeast(1000),
            sellCity = bldg.cityBonusCity.split("&").first().trim(),
            sellPrice = ((totalCost + bldg.estimatedDailyIncomeSilver) / bldg.maxTier.coerceAtLeast(1)).toInt().coerceAtLeast(2000),
            plannedUnits = bldg.maxTier,
            targetNetProfit = bldg.estimatedDailyIncomeSilver,
            targetInvestment = totalCost,
            acceptedDate = dateStr,
            status = OrderStatus.ACTIVE
        )
        val updatedOrders = currentOrders + newOrder
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun deleteCompletedOrder(orderId: String) {
        val updatedOrders = _uiState.value.tradeOrders.filter { it.id != orderId }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    fun discardTradeOrder(orderId: String) {
        val updatedOrders = _uiState.value.tradeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(status = OrderStatus.DISCARDED)
            } else order
        }
        prefs.saveTradeOrders(updatedOrders)
        _uiState.value = _uiState.value.copy(tradeOrders = updatedOrders)
    }

    // Gold Purchase Actions & Live Income Notification (Nur 1 mal in der Stunde bei automatischem Polling, bei Bedarf force)
    fun sendLiveGoldNotification(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        val lastTime = prefs.lastGoldNotificationTimeMs
        val oneHourMillis = 3600 * 1000L

        if (!force && ((currentTime - lastTime) < oneHourMillis)) {
            return // Rate-limited: max once per hour
        }

        prefs.lastGoldNotificationTimeMs = currentTime

        val state = _uiState.value
        val goldAmount = state.totalGoldOwned
        val netProfit = state.totalGoldNetProfitSilver
        val roi = if (state.totalGoldCostSilver > 0) (netProfit.toDouble() / state.totalGoldCostSilver) * 100.0 else 0.0

        NotificationHelper.showGoldProfitNotification(
            context = getApplication(),
            goldAmount = goldAmount,
            netProfitSilver = netProfit,
            roiPercent = roi,
            currentGoldPrice = state.currentGoldPrice
        )
    }

    fun addGoldPurchase(amountGold: Int, buyPricePerGold: Int, purchaseDate: String) {
        val dateStr = purchaseDate.ifBlank {
            SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
        }

        val newPurchase = GoldPurchase(
            id = UUID.randomUUID().toString(),
            amountGold = amountGold,
            buyPricePerGold = buyPricePerGold,
            purchaseDate = dateStr
        )

        val updatedList = _uiState.value.goldPurchases + newPurchase
        prefs.saveGoldPurchases(updatedList)
        _uiState.value = _uiState.value.copy(goldPurchases = updatedList)
        sendLiveGoldNotification()
    }

    fun deleteGoldPurchase(id: String) {
        val updatedList = _uiState.value.goldPurchases.filter { it.id != id }
        prefs.saveGoldPurchases(updatedList)
        _uiState.value = _uiState.value.copy(goldPurchases = updatedList)
    }

    fun updateGoldPurchase(id: String, newAmount: Int, newPrice: Int) {
        val updatedList = _uiState.value.goldPurchases.map { 
            if (it.id == id) it.copy(amountGold = newAmount, buyPricePerGold = newPrice) else it 
        }
        prefs.saveGoldPurchases(updatedList)
        _uiState.value = _uiState.value.copy(goldPurchases = updatedList)
    }

    fun addGoldSale(amountGold: Int, sellPricePerGold: Int, saleDate: String) {
        val dateStr = saleDate.ifBlank {
            SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
        }

        val newSale = GoldSale(
            id = UUID.randomUUID().toString(),
            amountGold = amountGold,
            sellPricePerGold = sellPricePerGold,
            saleDate = dateStr
        )

        val updatedList = _uiState.value.goldSales + newSale
        prefs.saveGoldSales(updatedList)
        _uiState.value = _uiState.value.copy(goldSales = updatedList)
        sendLiveGoldNotification()
    }

    fun deleteGoldSale(id: String) {
        val updatedList = _uiState.value.goldSales.filter { it.id != id }
        prefs.saveGoldSales(updatedList)
        _uiState.value = _uiState.value.copy(goldSales = updatedList)
    }

    fun updateGoldSale(id: String, newAmount: Int, newPrice: Int) {
        val updatedList = _uiState.value.goldSales.map { 
            if (it.id == id) it.copy(amountGold = newAmount, sellPricePerGold = newPrice) else it 
        }
        prefs.saveGoldSales(updatedList)
        _uiState.value = _uiState.value.copy(goldSales = updatedList)
    }

    fun fetchGoldPrices() {
        viewModelScope.launch {
            fetchGoldPricesInternal()
        }
    }

    private suspend fun fetchGoldPricesInternal() {
        _uiState.value = _uiState.value.copy(isLoadingGold = true)
        try {
            val allServerMap = AlbionGoldApi.fetchAllServersLatestGoldPrices()
            val currentServerPrices = AlbionGoldApi.fetchGoldPrices(_uiState.value.server, count = 24)
            val latestActivePrice = allServerMap[_uiState.value.server]
                ?: currentServerPrices.firstOrNull()?.price
                ?: _uiState.value.currentGoldPrice

            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())

            _uiState.value = _uiState.value.copy(
                serverGoldPrices = allServerMap,
                goldPrices = currentServerPrices,
                currentGoldPrice = latestActivePrice,
                isLoadingGold = false,
                lastGoldFetchTime = timeStr
            )
            if (_uiState.value.goldPurchases.isNotEmpty() && _uiState.value.systemNotificationsEnabled && _uiState.value.goldNotificationsEnabled) {
                sendLiveGoldNotification()
            }
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(isLoadingGold = false)
        }
    }

    fun onOpportunityCategorySelected(category: ResourceCategory) {
        val current = _uiState.value
        val updated = current.copy(selectedOpportunityCategory = category)
        recalculateOpportunities(updated)
    }

    fun onToggleAiAnalyzer(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(aiAnalyzerEnabled = enabled)
    }

    fun triggerAiMarketAnalysis() {
        val current = _uiState.value
        val result = AiMarketAnalyzer.analyzeMarketPrices(current.marketPrices, current.aiMinMarginPercent, avoidDangerousZones = current.avoidDangerousZones, silverBudget = current.silverBudget, carryCapacityKg = current.carryCapacityKg, hideBrecilien = current.hideBrecilien, hideBlackMarket = current.hideBlackMarket)
        _uiState.value = current.copy(aiAnalysisResult = result)
    }

    fun onCityFilterSelected(city: String) {
        val current = _uiState.value
        val updated = current.copy(selectedCityFilter = city)
        recalculateOpportunities(updated)
    }

    fun forceReloadAllResources() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Wipe cache and re-fetch from API
                val dynamicItems = try { AlbionMarketApi.fetchDynamicItemsFromAlbionBuilds() } catch (_: Exception) { emptyList() }
                if (dynamicItems.isNotEmpty()) {
                    AlbionResourceRepository.addDynamicResources(dynamicItems)
                }
                fetchMarketPricesInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetPriceHistoryAndRedownload() {
        viewModelScope.launch {
            prefs.savePriceSnapshots(server = _uiState.value.server, snapshots = emptyList())
            notifiedKeys.clear()
            _uiState.value = _uiState.value.copy(
                priceSnapshots = emptyList(),
                totalScannedItemsCount = 0L,
                marketPrices = emptyMap(),
                tradeOpportunities = emptyList()
            )
            fetchMarketPricesInternal()
            fetchGoldPricesInternal()
        }
    }

    private suspend fun fetchMarketPricesInternal() {
        _uiState.value = _uiState.value.copy(isLoadingPrices = true, priceFetchError = null)

        withContext(Dispatchers.IO) {
            try {
                val dynamicItems = try { AlbionMarketApi.fetchDynamicItemsFromAlbionBuilds() } catch (_: Exception) { emptyList() }
                if (dynamicItems.isNotEmpty()) {
                    AlbionResourceRepository.addDynamicResources(dynamicItems)
                }

                val currentResources = AlbionResourceRepository.resources
                val updatedFiltered = AlbionResourceRepository.filterResources(
                    category = _uiState.value.selectedCategory,
                    tier = _uiState.value.selectedTier,
                    enchantment = _uiState.value.selectedEnchantment,
                    query = _uiState.value.searchQuery
                )

                // Build item IDs across all enchantments (.0, .1, .2, .3, .4) for complete market coverage
                val itemIds = currentResources.flatMap { res ->
                    if (res.tier >= 4) {
                        listOf(res.id, "${res.id}@1", "${res.id}@2", "${res.id}@3", "${res.id}@4")
                    } else {
                        listOf(res.id)
                    }
                }.distinct()

                val fetchedPrices = AlbionMarketApi.fetchPrices(_uiState.value.server, itemIds)
                val cloudSnapshots = try { ServerSyncManager.fetchCloudPrices(getApplication()) } catch (_: Exception) { emptyList() }
                val cloudPrices = cloudSnapshots.asSequence().filter { it.sellPriceMin > 0 }.map {
                    MarketPrice(
                        itemId = it.itemId,
                        city = it.city,
                        quality = 1,
                        sellPriceMin = it.sellPriceMin,
                        sellPriceMinDate = "",
                        buyPriceMax = it.buyPriceMax,
                        buyPriceMaxDate = ""
                    )
                }.toList()

                // Overwrite older local prices with newer cloud and API market data
                val combinedPrices = (cloudPrices + fetchedPrices).distinctBy { "${it.itemId}_${it.city}_${it.sellPriceMin}" }

                val priceMap = if (combinedPrices.isNotEmpty()) {
                    combinedPrices.groupBy { it.itemId }
                } else {
                    _uiState.value.marketPrices.ifEmpty {
                        val cached = prefs.getPriceSnapshots(server = _uiState.value.server)
                        cached.asSequence().map {
                            MarketPrice(
                                itemId = it.itemId,
                                city = it.city,
                                quality = 1,
                                sellPriceMin = it.sellPriceMin,
                                sellPriceMinDate = "",
                                buyPriceMax = it.buyPriceMax,
                                buyPriceMaxDate = ""
                            )
                        }.groupBy { it.itemId }.ifEmpty {
                            AlbionMarketApi.getFallbackMarketPrices()
                        }
                    }
                }

                val scannedThisCycle = if (fetchedPrices.isNotEmpty()) fetchedPrices.size.toLong() else itemIds.size.toLong()
                val newTotalScanned = _uiState.value.totalScannedItemsCount + scannedThisCycle

                val currentTimeStr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())

                val newSnapshots = fetchedPrices.filter { it.sellPriceMin > 0 && !AlbionMarketApi.isUnrealisticPrice(it.itemId, it.sellPriceMin) }.map {
                    PriceSnapshot(
                        itemId = it.itemId,
                        city = it.city,
                        sellPriceMin = it.sellPriceMin,
                        buyPriceMax = it.buyPriceMax,
                        sellPriceMinAmount = it.sellPriceMinAmount
                    )
                }
                val accumulatedSnapshots = _uiState.value.priceSnapshots + newSnapshots
                prefs.savePriceSnapshots(server = _uiState.value.server, snapshots = accumulatedSnapshots)
                ServerSyncManager.syncPriceSnapshots(getApplication(), newSnapshots)

                val stateWithPrices = _uiState.value.copy(
                    filteredResources = updatedFiltered,
                    totalScannedItemsCount = newTotalScanned,
                    marketPrices = priceMap,
                    priceSnapshots = accumulatedSnapshots,
                    isLoadingPrices = false,
                    lastFetchTime = currentTimeStr
                )

                if (_uiState.value.aiAnalyzerEnabled) {
                    val aiResult = AiMarketAnalyzer.analyzeMarketPrices(priceMap, _uiState.value.aiMinMarginPercent, avoidDangerousZones = stateWithPrices.avoidDangerousZones, silverBudget = stateWithPrices.silverBudget, carryCapacityKg = stateWithPrices.carryCapacityKg, hideBrecilien = stateWithPrices.hideBrecilien, hideBlackMarket = stateWithPrices.hideBlackMarket)
                    _uiState.value = stateWithPrices.copy(aiAnalysisResult = aiResult)
                } else {
                    _uiState.value = stateWithPrices
                }

                recalculateOpportunities(stateWithPrices)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPrices = false,
                    priceFetchError = e.localizedMessage ?: "Fehler beim automatischen Laden der Marktpreise."
                )
            }
        }
    }

    private fun recalculateOpportunities(state: ResourceUiState) {
        viewModelScope.launch(Dispatchers.Default) {
            val standpunkt = if (state.selectedCityFilter != "ALLE") state.selectedCityFilter else null
            val effectiveMargin = state.targetMarginPercent

            var newCalculated = TradeCalculator.calculateOpportunities(
                resources = AlbionResourceRepository.resources,
                pricesByItem = state.marketPrices,
                silverBudget = state.silverBudget,
                carryCapacityKg = state.carryCapacityKg,
                marketTaxPercent = state.marketTaxPercent,
                targetMarginPercent = effectiveMargin,
                avoidDangerousZones = state.avoidDangerousZones,
                currentGoldPrice = state.currentGoldPrice,
                standpunktCity = standpunkt,
                maxCityDistance = state.maxZonesFilter,
                hideBrecilien = state.hideBrecilien,
                hideBlackMarket = state.hideBlackMarket
            ).filter { it.roiPercent >= effectiveMargin }

            if (state.avoidDangerousZones) {
                newCalculated = newCalculated.filter { !TradeCalculator.isDangerousCity(it.buyCity) && !TradeCalculator.isDangerousCity(it.sellCity) }
            }
            if (state.hideBlackMarket) {
                newCalculated = newCalculated.filter { !TradeCalculator.isBlackMarket(it.buyCity) && !TradeCalculator.isBlackMarket(it.sellCity) }
            }
            if (state.hideBrecilien) {
                newCalculated = newCalculated.filter { !TradeCalculator.isBrecilien(it.buyCity) && !TradeCalculator.isBrecilien(it.sellCity) }
            }

            if (state.filterHighPriorityOnly) {
                val highPrio = newCalculated.filter { it.priorityScore in 90..100 }
                if (highPrio.isNotEmpty()) {
                    newCalculated = highPrio
                }
            }

            if (state.maxZonesFilter > 0) {
                newCalculated = newCalculated.filter { it.zonesWalkedCount <= state.maxZonesFilter }
            }

            // Opportunity Category Filter (Waffen, Offhand, Helme, Rüstung, Stiefel, Umhänge, Taschen, Reittiere, Essen, Tränke, Rohstoffe, Veredelt, Artefakte)
            if (state.selectedOpportunityCategory != ResourceCategory.ALL) {
                newCalculated = newCalculated.filter { it.resource.category == state.selectedOpportunityCategory }
            }

            // Tier filter (T1-T8)
            if (state.selectedTierFilter in 1..8) {
                newCalculated = newCalculated.filter { it.resource.tier == state.selectedTierFilter }
            }

            // Enchantment filter (-1 = All, 0..4 = Enchantment levels)
            if (state.selectedOpportunityEnchantmentFilter >= 0) {
                newCalculated = newCalculated.filter { it.resource.enchantment == state.selectedOpportunityEnchantmentFilter }
            }

            // Search filter
            if (state.opportunitySearchQuery.isNotBlank()) {
                val q = state.opportunitySearchQuery.trim().lowercase()
                newCalculated = newCalculated.filter { 
                    it.resource.nameDe.lowercase().contains(q) || 
                    it.resource.id.lowercase().contains(q) || 
                    it.buyCity.lowercase().contains(q) || 
                    it.sellCity.lowercase().contains(q) 
                }
            }

            // Sorting
            newCalculated = when (state.sortOption) {
                OpportunitySort.NEWEST -> newCalculated.sortedByDescending { it.updatedTimestamp }
                OpportunitySort.FEWEST_STOCK -> newCalculated.sortedBy { if (it.stockAvailable > 0) it.stockAvailable else Int.MAX_VALUE }
                OpportunitySort.HIGHEST_MARGIN -> newCalculated.sortedWith(compareByDescending<TradeOpportunity> { it.roiPercent }.thenByDescending { it.totalNetProfit })
            }.distinctBy { "${it.resource.fullId}_${it.buyCity}_${it.sellCity}" }.take(30)

            _uiState.value = state.copy(
                targetMarginPercent = effectiveMargin,
                tradeOpportunities = newCalculated
            )
            SharedTradeStore.latestOpportunities = newCalculated

            val currentState = _uiState.value
            if (currentState.systemNotificationsEnabled && newCalculated.isNotEmpty()) {
                newCalculated.firstOrNull()?.let { topOpp ->
                    val sysKey = "SYS_${topOpp.resource.fullId}_${topOpp.buyCity}_${topOpp.sellCity}_${topOpp.totalNetProfit}"
                    if (!notifiedKeys.contains(sysKey)) {
                        notifiedKeys.add(sysKey)
                        NotificationHelper.showOpportunityNotification(
                            getApplication(),
                            opp = topOpp
                        )
                    }
                }
            }

            if (currentState.callMeBotAutoSend && currentState.callMeBotPhone.isNotBlank() && currentState.callMeBotApiKey.isNotBlank() && newCalculated.isNotEmpty()) {
                newCalculated.firstOrNull()?.let { topOpp ->
                    val waKey = "WA_${topOpp.resource.fullId}_${topOpp.buyCity}_${topOpp.sellCity}_${topOpp.totalNetProfit}"
                    if (!notifiedKeys.contains(waKey)) {
                        notifiedKeys.add(waKey)
                        val msg = WhatsAppMessageFormatter.formatOpportunityMessage(
                            opp = topOpp,
                            serverName = currentState.server.displayName,
                            silverBudget = currentState.silverBudget,
                            carryCapacityKg = currentState.carryCapacityKg
                        )
                        viewModelScope.launch {
                            CallMeBotApi.sendWhatsAppMessage(
                                phoneNumber = currentState.callMeBotPhone,
                                apiKey = currentState.callMeBotApiKey,
                                message = msg
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateState(newState: ResourceUiState) {
        val resources = AlbionResourceRepository.filterResources(
            category = newState.selectedCategory,
            tier = newState.selectedTier,
            enchantment = newState.selectedEnchantment,
            query = newState.searchQuery
        )
        val updatedList = if (newState.selectedEnchantment > 0) {
            resources.map { it.copy(enchantment = newState.selectedEnchantment) }
        } else {
            resources
        }
        val stateWithFiltered = newState.copy(filteredResources = updatedList)
        recalculateOpportunities(stateWithFiltered)
    }

    fun saveAllScannedItems() {
        viewModelScope.launch {
            try {
                val currentPrices = _uiState.value.marketPrices
                val currentSnapshots = currentPrices.flatMap { (itemId, prices) ->
                    prices.map { p ->
                        PriceSnapshot(
                            itemId = itemId,
                            city = p.city,
                            sellPriceMin = p.sellPriceMin,
                            buyPriceMax = p.buyPriceMax,
                            timestampMs = System.currentTimeMillis(),
                            sellPriceMinAmount = p.sellPriceMinAmount
                        )
                    }
                }
                if (currentSnapshots.isNotEmpty()) {
                    val existing = prefs.getPriceSnapshots()
                    prefs.savePriceSnapshots(server = _uiState.value.server, snapshots = existing + currentSnapshots)
                    _uiState.value = _uiState.value.copy(
                        priceSnapshots = prefs.getPriceSnapshots(),
                        totalScannedItemsCount = (existing + currentSnapshots).size.toLong()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("unused")
    private fun startContinuousSavingLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    saveAllScannedItems()
                    delay(30.seconds)
                } catch (_: Exception) {
                    delay(30.seconds)
                }
            }
        }
    }
}
