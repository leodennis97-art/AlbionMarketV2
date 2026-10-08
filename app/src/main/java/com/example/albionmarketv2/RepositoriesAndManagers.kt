package com.example.albionmarketv2

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object EquipmentBuildRepository {
    val builds = listOf(
        EquipmentBuild(id = "1", title = "Pve Solo Sword", category = BuildCategory.SOLO_PVE)
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
    val recipes = emptyList<CraftingRecipe>()
    fun calculateCraftingOpportunity(vararg args: Any?): CraftingOpportunityDetails? = null
    fun getRecipeFor(vararg args: Any?): CraftingRecipe = CraftingRecipe()
    fun calculateCraftingOpportunities(vararg args: Any?): List<Any> = emptyList()
    fun getPriceInCity(vararg args: Any?): Int = 1000
    fun getHighestSellMarketDetails(vararg args: Any?): String = "Caerleon"
    fun getCheapestMarketDetails(vararg args: Any?): String = "Martlock"
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
    fun decryptAES(vararg args: Any?): String = ""
    fun encryptAES(vararg args: Any?): String = ""
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
