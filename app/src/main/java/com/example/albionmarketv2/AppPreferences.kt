package com.example.albionmarketv2

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class AppPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)

    var server: AlbionServer
        get() {
            val name = prefs.getString("server_name", AlbionServer.EUROPE.name) ?: AlbionServer.EUROPE.name
            return try { AlbionServer.valueOf(name) } catch (e: Exception) { AlbionServer.EUROPE }
        }
        set(value) = prefs.edit().putString("server_name", value.name).apply()

    var silverBudget: Long
        get() = prefs.getLong("silver_budget", 1_000_000L)
        set(value) = prefs.edit().putLong("silver_budget", value).apply()

    var carryCapacityKg: Double
        get() = prefs.getFloat("carry_capacity_kg", 2000.0f).toDouble()
        set(value) = prefs.edit().putFloat("carry_capacity_kg", value.toFloat()).apply()

    var goldAmount: Long
        get() = prefs.getLong("gold_amount", 0L)
        set(value) = prefs.edit().putLong("gold_amount", value).apply()

    var selectedMount: String
        get() = prefs.getString("selected_mount", "Transportochs T5 (+1800 kg)") ?: "Transportochs T5 (+1800 kg)"
        set(value) = prefs.edit().putString("selected_mount", value).apply()

    var selectedBag: String
        get() = prefs.getString("selected_bag", "Tasche T5 (+220 kg)") ?: "Tasche T5 (+220 kg)"
        set(value) = prefs.edit().putString("selected_bag", value).apply()

    var selectedBoots: String
        get() = prefs.getString("selected_boots", "Transport-Schuhe T4 (+80 kg)") ?: "Transport-Schuhe T4 (+80 kg)"
        set(value) = prefs.edit().putString("selected_boots", value).apply()

    var targetMarginPercent: Double
        get() = prefs.getFloat("target_margin_percent", 5.0f).toDouble()
        set(value) = prefs.edit().putFloat("target_margin_percent", value.toFloat()).apply()

    var hasPremium: Boolean
        get() = prefs.getBoolean("has_premium", true)
        set(value) = prefs.edit().putBoolean("has_premium", value).apply()

    var aiBotName: String
        get() = prefs.getString("ai_bot_name", "AlbionBot") ?: "AlbionBot"
        set(value) = prefs.edit().putString("ai_bot_name", value).apply()

    var bubbleWidthPortrait: Int
        get() = prefs.getInt("bubble_width_portrait", 240)
        set(value) = prefs.edit().putInt("bubble_width_portrait", value.coerceIn(180, 500)).apply()

    var bubbleHeightPortrait: Int
        get() = prefs.getInt("bubble_height_portrait", 220)
        set(value) = prefs.edit().putInt("bubble_height_portrait", value.coerceIn(120, 800)).apply()

    var bubbleWidthLandscape: Int
        get() = prefs.getInt("bubble_width_landscape", 340)
        set(value) = prefs.edit().putInt("bubble_width_landscape", value.coerceIn(220, 700)).apply()

    var bubbleHeightLandscape: Int
        get() = prefs.getInt("bubble_height_landscape", 140)
        set(value) = prefs.edit().putInt("bubble_height_landscape", value.coerceIn(100, 500)).apply()

    var maxCityDistance: Int
        get() = prefs.getInt("max_city_distance", 99)
        set(value) = prefs.edit().putInt("max_city_distance", value).apply()

    var minStockCount: Int
        get() = prefs.getInt("min_stock_count", 0)
        set(value) = prefs.edit().putInt("min_stock_count", value).apply()

    var includeBrecilien: Boolean
        get() = prefs.getBoolean("include_brecilien", false)
        set(value) = prefs.edit().putBoolean("include_brecilien", value).apply()

    var minPriorityScore: Int
        get() = prefs.getInt("min_priority_score", 0)
        set(value) = prefs.edit().putInt("min_priority_score", value.coerceIn(0, 100)).apply()

    var overlayWidthDp: Int
        get() = prefs.getInt("overlay_width_dp", 340)
        set(value) = prefs.edit().putInt("overlay_width_dp", value.coerceIn(260, 480)).apply()

    var systemNotificationsEnabled: Boolean
        get() = prefs.getBoolean("system_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("system_notifications_enabled", value).apply()

    var goldNotificationsEnabled: Boolean
        get() = prefs.getBoolean("gold_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("gold_notifications_enabled", value).apply()

    var appLanguage: String
        get() {
            val saved = prefs.getString("app_language", null)
            if (saved != null) return saved
            val sysLang = Locale.getDefault().language.uppercase()
            return when (sysLang) {
                "DE" -> "DE"
                "ES" -> "ES"
                "FR" -> "FR"
                "PT" -> "PT"
                "RU" -> "RU"
                "ZH" -> "ZH"
                "JA" -> "JA"
                "KO" -> "KO"
                "TR" -> "TR"
                "ID" -> "ID"
                "PL" -> "PL"
                else -> "EN"
            }
        }
        set(value) = prefs.edit().putString("app_language", value).apply()

    var dismissedOtaVersion: String
        get() = prefs.getString("dismissed_ota_version", "") ?: ""
        set(value) = prefs.edit().putString("dismissed_ota_version", value).apply()

    var callMeBotPhone: String
        get() = prefs.getString("callmebot_phone", "") ?: ""
        set(value) = prefs.edit().putString("callmebot_phone", value).apply()

    var callMeBotApiKey: String
        get() = prefs.getString("callmebot_api_key", "") ?: ""
        set(value) = prefs.edit().putString("callmebot_api_key", value).apply()

    var callMeBotAutoSend: Boolean
        get() = prefs.getBoolean("callmebot_auto_send", false)
        set(value) = prefs.edit().putBoolean("callmebot_auto_send", value).apply()

    var lastGoldNotificationTimeMs: Long
        get() = prefs.getLong("last_gold_notif_time_ms", 0L)
        set(value) = prefs.edit().putLong("last_gold_notif_time_ms", value).apply()

    var bubbleStandpunktCity: String
        get() = prefs.getString("bubble_standpunkt_city", "ALLE") ?: "ALLE"
        set(value) = prefs.edit().putString("bubble_standpunkt_city", value).apply()

    var realtimeLiveSyncEnabled: Boolean
        get() = prefs.getBoolean("realtime_live_sync_enabled", true)
        set(value) = prefs.edit().putBoolean("realtime_live_sync_enabled", value).apply()

    var bubbleCategory: String
        get() = prefs.getString("bubble_category", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("bubble_category", value).apply()

    var bubbleTier: Int
        get() = prefs.getInt("bubble_tier", 0)
        set(value) = prefs.edit().putInt("bubble_tier", value).apply()

    var bubbleEnchantment: Int
        get() = prefs.getInt("bubble_enchantment", -1) // -1 = ALLE
        set(value) = prefs.edit().putInt("bubble_enchantment", value).apply()

    var bubbleSubCategory: String
        get() = prefs.getString("bubble_subcategory", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("bubble_subcategory", value).apply()

    var avoidDangerousZones: Boolean
        get() = prefs.getBoolean("avoid_dangerous_zones", prefs.getBoolean("bubble_avoid_dangerous", false))
        set(value) = prefs.edit().putBoolean("avoid_dangerous_zones", value).putBoolean("bubble_avoid_dangerous", value).apply()

    var hideBrecilien: Boolean
        get() = prefs.getBoolean("hide_brecilien", prefs.getBoolean("bubble_hide_brecilien", false))
        set(value) = prefs.edit().putBoolean("hide_brecilien", value).putBoolean("bubble_hide_brecilien", value).apply()

    var hideBlackMarket: Boolean
        get() = prefs.getBoolean("hide_black_market", prefs.getBoolean("bubble_hide_black_market", false))
        set(value) = prefs.edit().putBoolean("hide_black_market", value).putBoolean("bubble_hide_black_market", value).apply()

    var bubbleMinMarginPercent: Double
        get() = prefs.getFloat("bubble_min_margin", 5.0f).toDouble()
        set(value) = prefs.edit().putFloat("bubble_min_margin", value.toFloat()).apply()

    var bubbleMaxZones: Int
        get() = prefs.getInt("bubble_max_zones", 99)
        set(value) = prefs.edit().putInt("bubble_max_zones", value).apply()

    var bubbleMaxStock: Int
        get() = prefs.getInt("bubble_max_stock", 999999)
        set(value) = prefs.edit().putInt("bubble_max_stock", value).apply()

    var bubbleAvoidDangerousZones: Boolean
        get() = prefs.getBoolean("bubble_avoid_dangerous", prefs.getBoolean("avoid_dangerous_zones", false))
        set(value) = prefs.edit().putBoolean("bubble_avoid_dangerous", value).putBoolean("avoid_dangerous_zones", value).apply()

    var bubbleHideBrecilien: Boolean
        get() = prefs.getBoolean("bubble_hide_brecilien", prefs.getBoolean("hide_brecilien", false))
        set(value) = prefs.edit().putBoolean("bubble_hide_brecilien", value).putBoolean("hide_brecilien", value).apply()

    var bubbleHideBlackMarket: Boolean
        get() = prefs.getBoolean("bubble_hide_black_market", prefs.getBoolean("hide_black_market", false))
        set(value) = prefs.edit().putBoolean("bubble_hide_black_market", value).putBoolean("hide_black_market", value).apply()

    var bubbleIntervalMinutes: Int
        get() = prefs.getInt("bubble_interval_minutes", 3)
        set(value) = prefs.edit().putInt("bubble_interval_minutes", value).apply()

    var bubbleLastTab: String
        get() = prefs.getString("bubble_last_tab", "TOP_MARGIN") ?: "TOP_MARGIN"
        set(value) = prefs.edit().putString("bubble_last_tab", value).apply()

    var bubbleSearchQuery: String
        get() = prefs.getString("bubble_search_query", "") ?: ""
        set(value) = prefs.edit().putString("bubble_search_query", value).apply()

    // Draft Order Input Persistence for Bubble Overlay
    data class DraftOrderInput(
        val units: String,
        val buyPrice: String,
        val sellPrice: String
    )

    fun getDraftOrderInput(orderId: String): DraftOrderInput? {
        val units = prefs.getString("draft_units_$orderId", null) ?: return null
        val buy = prefs.getString("draft_buy_$orderId", "") ?: ""
        val sell = prefs.getString("draft_sell_$orderId", "") ?: ""
        return DraftOrderInput(units, buy, sell)
    }

    fun saveDraftOrderInput(orderId: String, units: String, buyPrice: String, sellPrice: String) {
        prefs.edit()
            .putString("draft_units_$orderId", units)
            .putString("draft_buy_$orderId", buyPrice)
            .putString("draft_sell_$orderId", sellPrice)
            .apply()
    }

    fun clearDraftOrderInput(orderId: String) {
        prefs.edit()
            .remove("draft_units_$orderId")
            .remove("draft_buy_$orderId")
            .remove("draft_sell_$orderId")
            .apply()
    }

    // Price Snapshots Persistence (Market History & External Folder Backup per Server)
    fun getPriceSnapshots(server: AlbionServer = AlbionServer.EUROPE): List<PriceSnapshot> {
        val jsonStr = prefs.getString("price_snapshots_${server.name}_json", "[]") ?: "[]"
        val list = mutableListOf<PriceSnapshot>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val itemId = obj.optString("itemId", "")
                val city = obj.optString("city", "")
                if (itemId.isBlank() || city.isBlank()) continue
                list.add(
                    PriceSnapshot(
                        itemId = itemId,
                        city = city,
                        sellPriceMin = obj.optInt("sellPriceMin", 0),
                        buyPriceMax = obj.optInt("buyPriceMax", 0),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                        sellPriceMinAmount = obj.optInt("sellPriceMinAmount", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            // Fallback to legacy global snapshots
            val legacyStr = prefs.getString("price_snapshots_json", "[]") ?: "[]"
            try {
                val arr = JSONArray(legacyStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val itemId = obj.optString("itemId", "")
                    val city = obj.optString("city", "")
                    if (itemId.isBlank() || city.isBlank()) continue
                    list.add(
                        PriceSnapshot(
                            itemId = itemId,
                            city = city,
                            sellPriceMin = obj.optInt("sellPriceMin", 0),
                            buyPriceMax = obj.optInt("buyPriceMax", 0),
                            timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                            sellPriceMinAmount = obj.optInt("sellPriceMinAmount", 0)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }

    fun savePriceSnapshots(server: AlbionServer = AlbionServer.EUROPE, snapshots: List<PriceSnapshot>) {
        // Keep only the latest snapshot for each (itemId, city) to prevent storing duplicate/old prices
        val deduplicated = snapshots
            .groupBy { "${it.itemId}_${it.city}" }
            .mapValues { (_, list) -> list.maxByOrNull { it.timestampMs } ?: list.last() }
            .values
            .toList()
            .takeLast(1500)

        val arr = JSONArray()
        for (s in deduplicated) {
            val obj = JSONObject()
            obj.put("itemId", s.itemId)
            obj.put("city", s.city)
            obj.put("sellPriceMin", s.sellPriceMin)
            obj.put("buyPriceMax", s.buyPriceMax)
            obj.put("timestampMs", s.timestampMs)
            obj.put("sellPriceMinAmount", s.sellPriceMinAmount)
            arr.put(obj)
        }
        prefs.edit().putString("price_snapshots_${server.name}_json", arr.toString()).apply()

        // Backup to external device folder Documents/AlbionDataPro
        ExternalStorageBackupManager.backupSnapshots(context, deduplicated)
    }

    // Trade Orders Persistence
    fun getTradeOrders(): List<TradeOrder> {
        val jsonStr = prefs.getString("trade_orders_json", "[]") ?: "[]"
        val list = mutableListOf<TradeOrder>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                val resourceId = obj.optString("resourceId", "")
                if (id.isBlank() || resourceId.isBlank()) continue

                val statusStr = obj.optString("status", OrderStatus.ACTIVE.name)
                val statusEnum = try { OrderStatus.valueOf(statusStr) } catch (_: Exception) { OrderStatus.ACTIVE }

                list.add(
                    TradeOrder(
                        id = id,
                        resourceId = resourceId,
                        resourceNameDe = obj.optString("resourceNameDe", resourceId),
                        resourceNameEn = obj.optString("resourceNameEn", ""),
                        tier = obj.optInt("tier", 4),
                        enchantment = obj.optInt("enchantment", 0),
                        buyCity = obj.optString("buyCity", "Caerleon"),
                        buyPrice = obj.optInt("buyPrice", 0),
                        sellCity = obj.optString("sellCity", "Caerleon"),
                        sellPrice = obj.optInt("sellPrice", 0),
                        plannedUnits = obj.optInt("plannedUnits", 1),
                        targetNetProfit = obj.optLong("targetNetProfit", 0L),
                        targetInvestment = obj.optLong("targetInvestment", 0L),
                        acceptedDate = obj.optString("acceptedDate", ""),
                        status = statusEnum,
                        actualSilverSpent = if (obj.has("actualSilverSpent")) obj.optLong("actualSilverSpent") else null,
                        actualSilverEarned = if (obj.has("actualSilverEarned")) obj.optLong("actualSilverEarned") else null,
                        actualBuyPrice = if (obj.has("actualBuyPrice")) obj.optInt("actualBuyPrice") else null,
                        actualSellPrice = if (obj.has("actualSellPrice")) obj.optInt("actualSellPrice") else null,
                        actualUnits = if (obj.has("actualUnits")) obj.optInt("actualUnits") else null,
                        completedDate = if (obj.has("completedDate")) obj.optString("completedDate") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveTradeOrders(orders: List<TradeOrder>) {
        val contextRef = context
        CoroutineScope(Dispatchers.IO).launch {
            val arr = JSONArray()
            for (o in orders) {
                val obj = JSONObject()
                obj.put("id", o.id)
                obj.put("resourceId", o.resourceId)
                obj.put("resourceNameDe", o.resourceNameDe)
                obj.put("resourceNameEn", o.resourceNameEn)
                obj.put("tier", o.tier)
                obj.put("enchantment", o.enchantment)
                obj.put("buyCity", o.buyCity)
                obj.put("buyPrice", o.buyPrice)
                obj.put("sellCity", o.sellCity)
                obj.put("sellPrice", o.sellPrice)
                obj.put("plannedUnits", o.plannedUnits)
                obj.put("targetNetProfit", o.targetNetProfit)
                obj.put("targetInvestment", o.targetInvestment)
                obj.put("acceptedDate", o.acceptedDate)
                obj.put("status", o.status.name)
                o.actualSilverSpent?.let { obj.put("actualSilverSpent", it) }
                o.actualSilverEarned?.let { obj.put("actualSilverEarned", it) }
                o.actualBuyPrice?.let { obj.put("actualBuyPrice", it) }
                o.actualSellPrice?.let { obj.put("actualSellPrice", it) }
                o.actualUnits?.let { obj.put("actualUnits", it) }
                o.completedDate?.let { obj.put("completedDate", it) }
                arr.put(obj)
            }
            val jsonStr = arr.toString()
            prefs.edit().putString("trade_orders_json", jsonStr).apply()
            ExternalStorageBackupManager.backupTradeOrders(contextRef, jsonStr)
        }
    }

    var rawTradeOrdersJson: String
        get() = prefs.getString("trade_orders_json", "[]") ?: "[]"
        set(value) {
            prefs.edit().putString("trade_orders_json", value).apply()
        }

    // Gold Purchases Persistence
    fun getGoldPurchases(): List<GoldPurchase> {
        val jsonStr = prefs.getString("gold_purchases_json", "[]") ?: "[]"
        val list = mutableListOf<GoldPurchase>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (id.isBlank()) continue
                list.add(
                    GoldPurchase(
                        id = id,
                        amountGold = obj.optInt("amountGold", 0),
                        buyPricePerGold = obj.optInt("buyPricePerGold", 0),
                        purchaseDate = obj.optString("purchaseDate", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveGoldPurchases(purchases: List<GoldPurchase>) {
        val contextRef = context
        CoroutineScope(Dispatchers.IO).launch {
            val arr = JSONArray()
            for (g in purchases) {
                val obj = JSONObject()
                obj.put("id", g.id)
                obj.put("amountGold", g.amountGold)
                obj.put("buyPricePerGold", g.buyPricePerGold)
                obj.put("purchaseDate", g.purchaseDate)
                arr.put(obj)
            }
            val jsonStr = arr.toString()
            prefs.edit().putString("gold_purchases_json", jsonStr).apply()
            ExternalStorageBackupManager.backupGoldPurchases(contextRef, jsonStr)
        }
    }

    fun getGoldSales(): List<GoldSale> {
        val jsonStr = prefs.getString("gold_sales_json", "[]") ?: "[]"
        val list = mutableListOf<GoldSale>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GoldSale(
                        id = obj.getString("id"),
                        amountGold = obj.getInt("amountGold"),
                        sellPricePerGold = obj.getInt("sellPricePerGold"),
                        saleDate = obj.getString("saleDate")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveGoldSales(sales: List<GoldSale>) {
        val contextRef = context
        CoroutineScope(Dispatchers.IO).launch {
            val arr = JSONArray()
            for (g in sales) {
                val obj = JSONObject()
                obj.put("id", g.id)
                obj.put("amountGold", g.amountGold)
                obj.put("sellPricePerGold", g.sellPricePerGold)
                obj.put("saleDate", g.saleDate)
                arr.put(obj)
            }
            val jsonStr = arr.toString()
            prefs.edit().putString("gold_sales_json", jsonStr).apply()
            ExternalStorageBackupManager.backupGoldSales(contextRef, jsonStr)
        }
    }

    var favoriteItemIds: Set<String>
        get() = prefs.getStringSet("favorite_item_ids", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("favorite_item_ids", value).apply()

    var bubbleOpacity: Float
        get() = prefs.getFloat("bubble_opacity", 0.95f)
        set(value) = prefs.edit().putFloat("bubble_opacity", value).apply()

    var bubbleScale: Float
        get() = prefs.getFloat("bubble_scale", 1.0f)
        set(value) = prefs.edit().putFloat("bubble_scale", value).apply()

    var bubbleCompactMode: Boolean
        get() = prefs.getBoolean("bubble_compact_mode", true)
        set(value) = prefs.edit().putBoolean("bubble_compact_mode", value).apply()

    var hideAppOnBubbleActivate: Boolean
        get() = prefs.getBoolean("hide_app_on_bubble_activate", true)
        set(value) = prefs.edit().putBoolean("hide_app_on_bubble_activate", value).apply()

    var craftingMasteryLevel: Int
        get() = prefs.getInt("crafting_mastery_level", 50)
        set(value) = prefs.edit().putInt("crafting_mastery_level", value.coerceIn(0, 100)).apply()

    var isUserLoggedIn: Boolean
        get() = prefs.getBoolean("is_user_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_user_logged_in", value).apply()

    var isAdmin: Boolean
        get() = prefs.getBoolean("is_admin_user", false) && savedUsername.lowercase() == "dnnx"
        set(value) = prefs.edit().putBoolean("is_admin_user", value).apply()

    var savedUsername: String
        get() {
            val encrypted = prefs.getString("saved_username_encrypted", "") ?: ""
            if (encrypted.isNotBlank()) {
                val decrypted = CryptoSecurityUtils.decryptAES(encrypted)
                if (decrypted.isNotBlank()) return decrypted
            }
            return prefs.getString("user_email", "") ?: ""
        }
        set(value) {
            val encrypted = if (value.isNotBlank()) CryptoSecurityUtils.encryptAES(value.trim()) else ""
            prefs.edit()
                .putString("saved_username_encrypted", encrypted)
                .remove("user_email")
                .apply()
        }

    var savedPassword: String
        get() {
            val encrypted = prefs.getString("saved_password_encrypted", "") ?: ""
            return if (encrypted.isNotBlank()) CryptoSecurityUtils.decryptAES(encrypted) else ""
        }
        set(value) {
            val encrypted = if (value.isNotBlank()) CryptoSecurityUtils.encryptAES(value.trim()) else ""
            prefs.edit().putString("saved_password_encrypted", encrypted).apply()
        }
}
