package com.example.albionmarketv2

import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.example.albionmarketv2.ui.theme.AlbionMarketV2Theme
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class FloatingBubbleService : LifecycleService(), SavedStateRegistryOwner {

    @Suppress("DEPRECATION")
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val CHANNEL_ID = "albion_bubble_service_channel"
        const val NOTIFICATION_ID = 2002
        private var isRunning = false

        fun startService(context: Context) {
            try {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.startForegroundService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }

        fun isServiceRunning(): Boolean = isRunning
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var floatX = 100f
    private var floatY = 300f

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var activeOrder by mutableStateOf<TradeOrder?>(null)
    private var topOpportunities by mutableStateOf<List<TradeOpportunity>>(emptyList())
    private var isLoadingOpps by mutableStateOf(value = false)
    private var aiPriceStatus by mutableStateOf("🤖 KI-Preisschutz: Aktiv • 100% verifiziert")
    private var lastAiPriceCheckTime by mutableStateOf("")
    private var aiVerifiedItemsCount by mutableIntStateOf(0)

    // Memory caching to avoid parsing SharedPreferences JSON on every frame
    private var cachedPriceMap: Map<String, List<MarketPrice>>? = null

    override fun onCreate() {
        try {
            savedStateRegistryController.performRestore(null)
            super.onCreate()
            isRunning = true
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            startForegroundNotification()
            setupOverlayView()
            registerScreenStateReceiver()
            updateBubbleVisibility()
            startActiveOrderObserver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val restartIntent = Intent(applicationContext, FloatingBubbleService::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            applicationContext.startForegroundService(restartIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundNotification() {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Albion Overlay Bubble Service",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Albion Overlay")
                .setContentText("Overlay aktiv")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSound(null)
                .setVibrate(null)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setFocusableMode(enableFocus: Boolean) {
        if ((!::layoutParams.isInitialized) || (composeView == null)) return
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            if (enableFocus) {
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                composeView?.windowToken?.let { token ->
                    imm?.hideSoftInputFromWindow(token, 0)
                }
            }
            if (composeView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(composeView, layoutParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupOverlayView() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        try {
            val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 300
            }
            floatX = layoutParams.x.toFloat()
            floatY = layoutParams.y.toFloat()

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@FloatingBubbleService)
                setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)

                setContent {
                    AlbionMarketV2Theme {
                        BubbleOverlayContent(
                            context = this@FloatingBubbleService,
                            activeOrder = activeOrder,
                            topOpportunities = topOpportunities,
                            isLoadingOpps = isLoadingOpps,
                            onRefresh = { loadData(forceRefreshPrices = true) },
                            onAcceptOpportunity = { opp -> acceptOpportunity(opp) },
                            onDrag = { dx, dy ->
                                try {
                                    floatX += dx
                                    floatY += dy
                                    this@FloatingBubbleService.layoutParams.x = floatX.roundToInt()
                                    this@FloatingBubbleService.layoutParams.y = floatY.roundToInt()
                                    if (composeView?.isAttachedToWindow == true) {
                                        windowManager.updateViewLayout(this@apply, this@FloatingBubbleService.layoutParams)
                                    }
                                } catch (_: Exception) {}
                            },
                            onFocusModeChanged = { needsFocus ->
                                setFocusableMode(needsFocus)
                            },
                        ) {
                            setFocusableMode(enableFocus = false)
                            loadData(forceRefreshPrices = false)
                        }
                    }
                }
            }

            if (Settings.canDrawOverlays(this)) {
                try {
                    windowManager.addView(composeView, layoutParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var screenReceiver: BroadcastReceiver? = null

    private fun updateBubbleVisibility() {
        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            val isKeyguardLocked = (keyguardManager?.isKeyguardLocked == true) || (keyguardManager?.isDeviceLocked == true)
            val shouldShow = !isKeyguardLocked

            serviceScope.launch(Dispatchers.Main) {
                if (composeView?.isAttachedToWindow == true) {
                    composeView?.visibility = if (shouldShow) View.VISIBLE else View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateBubbleVisibility()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun startActiveOrderObserver() {
        serviceScope.launch(Dispatchers.IO) {
            var loopCount = 0
            while (isRunning && isActive) {
                val prefs = AppPreferences(this@FloatingBubbleService)
                val isLoggedIn = prefs.isUserLoggedIn && LicenseManager.isLicenseValid(this@FloatingBubbleService)
                if (!isLoggedIn) {
                    withContext(Dispatchers.Main) {
                        isRunning = false
                        stopSelf()
                    }
                    break
                }
                loadActiveOrder()
                val intervalMins = prefs.bubbleIntervalMinutes.coerceAtLeast(1)
                val ticksNeeded = (intervalMins * 60) / 30 // loop runs every 30 seconds (battery optimized)
                if ((loopCount % ticksNeeded) == 0) {
                    // Use existing cache / old information instead of permanently re-loading all network data
                    loadTopOpportunities(forceRefresh = false)
                }
                loopCount++
                delay(30.seconds)
            }
        }
    }

    private fun loadData(forceRefreshPrices: Boolean = false) {
        serviceScope.launch(Dispatchers.IO) {
            loadActiveOrder()
            loadTopOpportunities(forceRefresh = forceRefreshPrices)
        }
    }

    private fun loadActiveOrder() {
        try {
            val prefs = AppPreferences(this@FloatingBubbleService)
            val orders = prefs.getTradeOrders().filter { it.status == OrderStatus.ACTIVE }
            val ord = orders.lastOrNull()
            serviceScope.launch(Dispatchers.Main) {
                if (activeOrder != ord) {
                    activeOrder = ord
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTopOpportunities(forceRefresh: Boolean = false) {
        isLoadingOpps = true
        serviceScope.launch(Dispatchers.IO) {
            try {
                val opps = calculateTopMarginOpportunities(this@FloatingBubbleService, forceRefresh = forceRefresh)
                withContext(Dispatchers.Main) {
                    if (topOpportunities != opps) {
                        topOpportunities = opps
                    }
                    isLoadingOpps = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoadingOpps = false
                }
            }
        }
    }

    private suspend fun fetchAndVerifyPricesWithAi(context: Context, forceRefresh: Boolean = false): Map<String, List<MarketPrice>> {
        val prefs = AppPreferences(context)

        var fetchedPrices = emptyList<MarketPrice>()
        if (forceRefresh || (cachedPriceMap == null)) {
            val resources = AlbionResourceRepository.resources
            val itemIds = resources.asSequence().map { it.fullId }.distinct().toList()

            val livePrices = try {
                AlbionMarketApi.fetchPrices(prefs.server, itemIds)
            } catch (_: Exception) {
                emptyList()
            }

            val cloudSnapshots = try {
                ServerSyncManager.fetchCloudPrices(context)
            } catch (_: Exception) {
                emptyList()
            }

            val cloudPrices = cloudSnapshots.asSequence().filter { it.sellPriceMin > 0 }.map {
                MarketPrice(
                    itemId = it.itemId,
                    city = it.city,
                    quality = 1,
                    sellPriceMin = it.sellPriceMin,
                    sellPriceMinDate = "",
                    buyPriceMax = it.buyPriceMax,
                    buyPriceMaxDate = "",
                    sellPriceMinAmount = it.sellPriceMinAmount,
                )
            }.toList()

            fetchedPrices = (livePrices + cloudPrices).distinctBy { "${it.itemId}_${it.city}_${it.sellPriceMin}" }
        }

        val localSnapshots = prefs.getPriceSnapshots(prefs.server)
        val localPrices = localSnapshots.map { s ->
            MarketPrice(
                itemId = s.itemId,
                city = s.city,
                quality = 1,
                sellPriceMin = s.sellPriceMin,
                sellPriceMinDate = "",
                buyPriceMax = s.buyPriceMax,
                buyPriceMaxDate = "",
                sellPriceMinAmount = s.sellPriceMinAmount,
            )
        }

        val rawCombined = (fetchedPrices + localPrices).ifEmpty {
            AlbionMarketApi.getFallbackMarketPrices().values.flatten()
        }

        // 🤖 KI-Preisschutz & Anomalie-Filterung (Echtzeit-Validierung)
        val aiVerifiedPrices = rawCombined.filter { p ->
            val isNotZero = p.sellPriceMin > 0
            val isNotUnrealistic = !AlbionMarketApi.isUnrealisticPrice(p.itemId, p.sellPriceMin)
            val isWithinBounds = (p.sellPriceMin >= 5) && (p.sellPriceMin <= 500_000_000)
            val isBuyOrderValid = p.buyPriceMax == 0 || p.buyPriceMax < (p.sellPriceMin * 3)

            isNotZero && isNotUnrealistic && isWithinBounds && isBuyOrderValid
        }

        if (fetchedPrices.isNotEmpty()) {
            val newSnapshots = fetchedPrices.asSequence().filter { it.sellPriceMin > 0 && !AlbionMarketApi.isUnrealisticPrice(it.itemId, it.sellPriceMin) }.map {
                PriceSnapshot(
                    itemId = it.itemId,
                    city = it.city,
                    sellPriceMin = it.sellPriceMin,
                    buyPriceMax = it.buyPriceMax,
                    sellPriceMinAmount = it.sellPriceMinAmount,
                )
            }.toList()
            if (newSnapshots.isNotEmpty()) {
                val accumulated = (localSnapshots + newSnapshots).distinctBy { "${it.itemId}_${it.city}" }
                prefs.savePriceSnapshots(server = prefs.server, snapshots = accumulated)
                try {
                    ServerSyncManager.syncPriceSnapshots(context, newSnapshots)
                } catch (_: Exception) {}
            }
        }

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())
        val groupedMap = aiVerifiedPrices.groupBy { it.itemId }

        withContext(Dispatchers.Main) {
            lastAiPriceCheckTime = timeStr
            aiVerifiedItemsCount = groupedMap.size
            aiPriceStatus = "🤖 KI-Preisschutz: Aktiv • ${groupedMap.size} Items verifiziert ($timeStr)"
        }

        return groupedMap.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() }
    }

    private suspend fun calculateTopMarginOpportunities(context: Context, forceRefresh: Boolean = false): List<TradeOpportunity> {
        val prefs = AppPreferences(context)
        if ((cachedPriceMap == null) || forceRefresh) {
            cachedPriceMap = fetchAndVerifyPricesWithAi(context, forceRefresh = forceRefresh)
        }

        val priceMap = cachedPriceMap ?: emptyMap()
        val resources = AlbionResourceRepository.resources
        val bubbleCity = prefs.bubbleStandpunktCity
        val standpunkt = if (bubbleCity != "ALLE") bubbleCity else null
        val bubbleCategory = prefs.bubbleCategory
        val bubbleTier = prefs.bubbleTier
        val bubbleEnchantment = prefs.bubbleEnchantment

        val searchQ = prefs.bubbleSearchQuery.trim().lowercase()

        val filteredResources = resources.filter { res ->
            val matchesCategory = when (bubbleCategory) {
                "ALL" -> true
                "SAMMLER" -> (res.category == ResourceCategory.RESOURCES || res.category == ResourceCategory.REFINED)
                "GEAR" -> (res.category == ResourceCategory.WEAPONS || res.category == ResourceCategory.ARMOR || res.category == ResourceCategory.HELMETS || res.category == ResourceCategory.SHOES || res.category == ResourceCategory.OFFHAND || res.category == ResourceCategory.BAG || res.category == ResourceCategory.CAPE)
                "GASTRO" -> (res.category == ResourceCategory.FOOD || res.category == ResourceCategory.POTIONS)
                "LUXUS" -> (res.category == ResourceCategory.MOUNTS || res.category == ResourceCategory.ARTIFACTS)
                else -> (res.category.name.equals(bubbleCategory, ignoreCase = true) || res.category.displayName.equals(bubbleCategory, ignoreCase = true))
            }

            val matchesTier = if (bubbleTier > 0) res.tier == bubbleTier else true
            val matchesEnchantment = if (bubbleEnchantment >= 0) res.enchantment == bubbleEnchantment else true
            val matchesSearch = if (searchQ.isNotBlank()) res.nameDe.lowercase().contains(searchQ) || res.nameEn.lowercase().contains(searchQ) || res.id.lowercase().contains(searchQ) else true

            matchesCategory && matchesTier && matchesEnchantment && matchesSearch
        }

        val avoidDangerous = prefs.bubbleAvoidDangerousZones || prefs.avoidDangerousZones
        val hideBrec = prefs.bubbleHideBrecilien || prefs.hideBrecilien
        val hideBm = prefs.bubbleHideBlackMarket || prefs.hideBlackMarket
        val maxZones = prefs.bubbleMaxZones

        val viewModel = try {
            SharedViewModelProvider.get(context.applicationContext as Application)
        } catch (_: Exception) { null }

        val silverBudget = viewModel?.uiState?.value?.silverBudget ?: prefs.silverBudget
        val carryCapacity = viewModel?.uiState?.value?.carryCapacityKg ?: prefs.carryCapacityKg

        var rawOpportunities = TradeCalculator.calculateOpportunities(
            resources = filteredResources,
            pricesByItem = priceMap,
            silverBudget = silverBudget,
            carryCapacityKg = carryCapacity,
            marketTaxPercent = if (prefs.hasPremium) 4.0 else 8.0,
            targetMarginPercent = prefs.bubbleMinMarginPercent,
            avoidDangerousZones = avoidDangerous,
            currentGoldPrice = 4250,
            standpunktCity = standpunkt,
            maxCityDistance = maxZones,
            hideBrecilien = hideBrec,
            hideBlackMarket = hideBm,
        ).filter { it.roiPercent >= prefs.bubbleMinMarginPercent }

        // 1. Standpunkt strict filter: Buy city MUST match Standpunkt
        if (!standpunkt.isNullOrBlank()) {
            rawOpportunities = rawOpportunities.filter { TradeCalculator.citiesMatch(it.buyCity, standpunkt) }
        }

        // 2. Category strict filter
        if (bubbleCategory != "ALL") {
            rawOpportunities = rawOpportunities.filter { opp ->
                when (bubbleCategory) {
                    "SAMMLER" -> opp.resource.category == ResourceCategory.RESOURCES || opp.resource.category == ResourceCategory.REFINED
                    "GEAR" -> opp.resource.category == ResourceCategory.WEAPONS || opp.resource.category == ResourceCategory.ARMOR || opp.resource.category == ResourceCategory.HELMETS || opp.resource.category == ResourceCategory.SHOES || opp.resource.category == ResourceCategory.OFFHAND || opp.resource.category == ResourceCategory.BAG || opp.resource.category == ResourceCategory.CAPE
                    "GASTRO" -> opp.resource.category == ResourceCategory.FOOD || opp.resource.category == ResourceCategory.POTIONS
                    "LUXUS" -> opp.resource.category == ResourceCategory.MOUNTS || opp.resource.category == ResourceCategory.ARTIFACTS
                    else -> opp.resource.category.name.equals(bubbleCategory, ignoreCase = true) || opp.resource.category.displayName.equals(bubbleCategory, ignoreCase = true)
                }
            }
        }

        // 3. Tier strict filter
        if (bubbleTier > 0) {
            rawOpportunities = rawOpportunities.filter { opp ->
                opp.resource.tier == bubbleTier
            }
        }

        // 4. Enchantment strict filter
        if (bubbleEnchantment >= 0) {
            rawOpportunities = rawOpportunities.filter { opp ->
                opp.resource.enchantment == bubbleEnchantment
            }
        }

        // 5. Max Zones Distance strict filter
        if (maxZones < 99) {
            rawOpportunities = rawOpportunities.filter { opp ->
                opp.zonesWalkedCount <= maxZones
            }
        }

        // 6. Dangerous / Red Zones strict filter
        if (avoidDangerous) {
            rawOpportunities = rawOpportunities.filter { opp ->
                !TradeCalculator.isDangerousCity(opp.buyCity) && !TradeCalculator.isDangerousCity(opp.sellCity)
            }
        }

        // 7. Black Market strict filter
        if (hideBm) {
            rawOpportunities = rawOpportunities.filter { opp ->
                !TradeCalculator.isBlackMarket(opp.buyCity) && !TradeCalculator.isBlackMarket(opp.sellCity)
            }
        }

        // 8. Brecilien strict filter
        if (hideBrec) {
            rawOpportunities = rawOpportunities.filter { opp ->
                !TradeCalculator.isBrecilien(opp.buyCity) && !TradeCalculator.isBrecilien(opp.sellCity)
            }
        }

        // Extra KI-Anomalie Filter: Unrealistische Spitzen verworfen
        val aiSanitizedOpportunities = rawOpportunities.filter { opp ->
            (opp.roiPercent in 0.1..500.0) && (opp.unitNetProfit in 1..50_000_000)
        }

        return aiSanitizedOpportunities.asSequence().sortedByDescending { it.updatedTimestamp }.take(50).toList()
    }

    private fun acceptOpportunity(opp: TradeOpportunity) {
        try {
            val prefs = AppPreferences(this)
            val currentOrders = prefs.getTradeOrders()
            val activeOrders = currentOrders.filter { it.status == OrderStatus.ACTIVE }
            if (activeOrders.size >= 10) {
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "Maximal 10 aktive Aufträge gleichzeitig erlaubt!", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val lang = prefs.appLanguage
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
                recommendedSellOrderPrice = if (opp.recommendedSellOrderPrice > 0) opp.recommendedSellOrderPrice else (opp.sellPrice * 1.08).toInt().coerceAtLeast(1),
            )

            val updatedOrders = currentOrders + newOrder
            prefs.saveTradeOrders(updatedOrders)

            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@FloatingBubbleService,
                    if (lang == "DE") "Auftrag angenommen: ${opp.resource.nameDe} (T${opp.resource.tier})" else "Order accepted: ${opp.resource.nameEn} (T${opp.resource.tier})",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            activeOrder = newOrder
            loadData(forceRefreshPrices = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        composeView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
        try {
            serviceScope.cancel()
        } catch (_: Exception) {}
    }
}

enum class BubbleTab(val titleDe: String, val titleEn: String, val emoji: String) {
    ACTIVE_ORDER("Aufträge", "Orders", "📦"),
    COMPLETED_ORDERS("Historie", "History", "📜"),
    TOP_MARGIN("Handel & Marge", "Trade Margin", "🔥"),
    SMUGGLER_RADAR("Schwarzmarkt-Radar", "Black Market Radar", "🏴‍☠️"),
    INVENTORY_ROUTER("Volle-Taschen Route", "Inventory Route", "🎒"),
    CATALOG("Katalog", "Catalog", "📖"),
    CRAFTING("Handwerks-Guide", "Crafting Guide", "⚒️"),
    ISLAND("Insel-Guide", "Island Guide", "🏝️"),
    EVENTS("Event & Boss Loot", "Event & Boss Loot", "⚔️"),
    GOLD_MARKET("Goldmarkt", "Gold Market", "🪙"),
    BUILDS("KI Ausrüstung", "AI Equipment", "⚔️"),
    WORLD_MAP("Weltkarte", "World Map", "🗺️"),
    SETTINGS("Einstellungen", "Settings", "⚙️"),
    ADMIN("Admin", "Admin", "👑")
}

@Composable
fun BubbleOverlayContent(
    context: Context,
    activeOrder: TradeOrder?,
    topOpportunities: List<TradeOpportunity>,
    isLoadingOpps: Boolean,
    onRefresh: () -> Unit,
    onAcceptOpportunity: (TradeOpportunity) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onFocusModeChanged: (Boolean) -> Unit,
    onOrderBooked: () -> Unit,
) {
    val prefs = remember { AppPreferences(context) }
    val lang = prefs.appLanguage
    val currentOnDrag by rememberUpdatedState(onDrag)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isMaximized by remember { mutableStateOf(value = false) }
    var isCompactMode by remember { mutableStateOf(value = true) }
    var bubbleOpacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var bubbleScale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var bubbleWidthPortrait by remember { mutableIntStateOf(prefs.bubbleWidthPortrait) }
    var bubbleHeightPortrait by remember { mutableIntStateOf(prefs.bubbleHeightPortrait) }
    var bubbleWidthLandscape by remember { mutableIntStateOf(prefs.bubbleWidthLandscape) }
    var bubbleHeightLandscape by remember { mutableIntStateOf(prefs.bubbleHeightLandscape) }

    val effOpacity = bubbleOpacity.coerceIn(0.3f, 1.0f)
    val effScale = bubbleScale.coerceIn(0.6f, 1.5f)

    val cardWidth = ((if (isLandscape) bubbleWidthLandscape else bubbleWidthPortrait).toFloat() * effScale).dp
    val maxBubbleHeight = ((if (isLandscape) bubbleHeightLandscape else bubbleHeightPortrait).toFloat() * effScale).dp
    val maxBubbleHeightTab = ((if (isLandscape) (bubbleHeightLandscape - 45).coerceAtLeast(80) else (bubbleHeightPortrait - 45).coerceAtLeast(100)).toFloat() * effScale).dp

    var isExpanded by remember { mutableStateOf(value = false) }
    var isGhostMode by remember { mutableStateOf(value = false) }
    var isBookingMode by remember { mutableStateOf(value = false) }
    var selectedTab by remember { mutableStateOf(if (activeOrder != null) BubbleTab.ACTIVE_ORDER else BubbleTab.TOP_MARGIN) }

    LaunchedEffect(activeOrder) {
        if ((activeOrder != null) && !isBookingMode) {
            selectedTab = BubbleTab.ACTIVE_ORDER
        } else if (activeOrder == null) {
            selectedTab = BubbleTab.TOP_MARGIN
        }
    }

    Box(
        modifier = Modifier.wrapContentSize(),
    ) {
        if (!isExpanded || isGhostMode) {
            val minBubbleSize = ((if (isCompactMode) 46.dp else 60.dp).value * effScale).dp
            val minIconSize = ((if (isCompactMode) 32.dp else 44.dp).value * effScale).dp
            Surface(
                shape = CircleShape,
                color = Color(0xFF0E2532),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(minBubbleSize)
                    .clip(CircleShape)
                    .graphicsLayer(alpha = if (isGhostMode) 0.5f else effOpacity)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isExpanded = true
                            isGhostMode = false
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.x, dragAmount.y)
                        }
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.aot_logo),
                        contentDescription = "AlbionDataPro Overlay",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(minIconSize),
                    )
                    
                    if (isGhostMode && topOpportunities.isNotEmpty()) {
                        val maxProfit = topOpportunities.maxOfOrNull { it.totalNetProfit } ?: 0L
                        if (maxProfit > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = topOpportunities.size.toString(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(if (isCompactMode) 12.dp else 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2532)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .width(cardWidth)
                    .graphicsLayer(alpha = effOpacity),
            ) {
                Column(modifier = Modifier.padding(if (isCompactMode) 4.dp else 6.dp)) {
                    // Title Bar (Drag gesture attached ONLY to Title area so buttons respond instantly!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        currentOnDrag(dragAmount.x, dragAmount.y)
                                    }
                                },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.aot_logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(if (isCompactMode) 18.dp else 24.dp),
                            )
                            Spacer(modifier = Modifier.width(if (isCompactMode) 4.dp else 6.dp))
                            RainbowBlinkingText(
                                rainbowName = "AlbionDataPro",
                                fontSize = if (isCompactMode) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Ghost Mode (Hide) Button
                            IconButton(
                                onClick = { isGhostMode = true },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Text(
                                    text = "👻",
                                    color = Color.White,
                                    fontSize = if (isCompactMode) 10.sp else 12.sp,
                                )
                            }

                            // Zoom In (+) Button
                            IconButton(
                                onClick = {
                                    val newScale = (bubbleScale + 0.1f).coerceIn(0.6f, 1.5f)
                                    val rounded = round(newScale * 10f) / 10f
                                    bubbleScale = rounded
                                    prefs.bubbleScale = rounded
                                },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Text(
                                    text = "+",
                                    color = Color(0xFF38BDF8),
                                    fontSize = if (isCompactMode) 13.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }


                            IconButton(
                                onClick = { isMaximized = !isMaximized },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Text(
                                    text = if (isMaximized) "🗗" else "🗖",
                                    color = Color(0xFFFFD700),
                                    fontSize = if (isCompactMode) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            var refreshRotationAngle by remember { mutableFloatStateOf(0f) }
                            val animatedRotation by animateFloatAsState(
                                targetValue = refreshRotationAngle,
                                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                                label = "RefreshRotation",
                            )

                            IconButton(
                                onClick = {
                                    refreshRotationAngle += 360f
                                    onRefresh()
                                },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Aktualisieren",
                                    tint = Color.LightGray,
                                    modifier = Modifier
                                        .size(if (isCompactMode) 14.dp else 18.dp)
                                        .graphicsLayer(rotationZ = animatedRotation),
                                )
                            }
                            IconButton(
                                onClick = {
                                    val openIntent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    }
                                    context.startActivity(openIntent)
                                },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Text(
                                    text = "📲",
                                    fontSize = if (isCompactMode) 10.sp else 12.sp,
                                )
                            }

                            // Auffälliger kreisförmiger Beenden / Exit Button mit "X" in Title Bar
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(if (isCompactMode) 22.dp else 26.dp)
                                    .clickable {
                                        Toast.makeText(context, "🛑 Floating Bubble beendet", Toast.LENGTH_SHORT).show()
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                FloatingBubbleService.stopService(context)
                                            } catch (_: Exception) {}
                                        }
                                    },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Beenden",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isCompactMode) 13.dp else 16.dp)
                                    )
                                }
                            }

                            // Minimieren zu Bubble Button
                            IconButton(
                                onClick = {
                                    isExpanded = false
                                    isBookingMode = false
                                    onFocusModeChanged(false)
                                },
                                modifier = Modifier.size(if (isCompactMode) 22.dp else 28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Minimieren",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(if (isCompactMode) 14.dp else 18.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactMode) 2.dp else 4.dp))

                    if (!isBookingMode) {
                        val availableTabs = remember(prefs.isAdmin, prefs.hideBlackMarket, prefs.bubbleHideBlackMarket) {
                            val hideBm = prefs.hideBlackMarket || prefs.bubbleHideBlackMarket
                            BubbleTab.entries.filter { tab ->
                                when (tab) {
                                    BubbleTab.ADMIN -> prefs.isAdmin
                                    BubbleTab.SMUGGLER_RADAR -> !hideBm
                                    else -> true
                                }
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactMode) 3.dp else 6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(availableTabs) { tab: BubbleTab ->
                                val isSelected = selectedTab == tab
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF1E3A4C) else Color(0xFF0A1922),
                                    modifier = Modifier.clickable { selectedTab = tab },
                                ) {
                                    Text(
                                        text = "${tab.emoji} ${if (lang == "DE") tab.titleDe else tab.titleEn}",
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactMode) 8.sp else 9.sp,
                                        modifier = Modifier.padding(
                                            vertical = if (isCompactMode) 2.dp else 4.dp,
                                            horizontal = if (isCompactMode) 3.dp else 4.dp
                                        ),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }



                    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

                    if ((selectedTab == BubbleTab.ACTIVE_ORDER) && (activeOrder != null)) {
                        if (!isBookingMode) {
                            val profitStr = fmt.format(activeOrder.targetNetProfit)
                            val buyPriceStr = fmt.format(activeOrder.buyPrice)
                            val sellPriceStr = fmt.format(activeOrder.sellPrice)
                            val unitsStr = fmt.format(activeOrder.plannedUnits)
                            val buyCityTrans = LanguageManager.getCityTranslation(activeOrder.buyCity, lang)
                            val sellCityTrans = LanguageManager.getCityTranslation(activeOrder.sellCity, lang)

                            val catEmoji = AlbionResourceRepository.resources.find { it.id == activeOrder.resourceId }?.category?.displayName?.substringBefore(" ") ?: "📦"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E3A4C),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            ) {
                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    // Row 1: Item Name, Tier/Enchantment & Net Profit
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val resOpt = AlbionResourceRepository.resources.find { it.id == activeOrder.resourceId }
                                            resOpt?.let { res ->
                                                AsyncImage(
                                                    model = res.imageUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                                                )
                                            }
                                            Text(
                                                text = "$catEmoji ${activeOrder.resourceNameDe} (T${activeOrder.tier})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Text(
                                            text = "+$profitStr S.",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Row 2: Route & Quantity Info
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "📍 $buyCityTrans ($buyPriceStr S.) ➔ $sellCityTrans ($sellPriceStr S.)",
                                            color = Color(0xFF81D4FA),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "📦 $unitsStr Stk.",
                                            color = Color(0xFFFFB74D),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                    }

                                    // Row 3: Orders for 100% execution & Max Margin
                                    val buyOrderPrice = if (activeOrder.recommendedBuyOrderPrice > 0) activeOrder.recommendedBuyOrderPrice else (activeOrder.buyPrice * 0.88).toInt().coerceAtLeast(1)
                                    val sellOrderPrice = if (activeOrder.recommendedSellOrderPrice > 0) activeOrder.recommendedSellOrderPrice else (activeOrder.sellPrice * 1.08).toInt().coerceAtLeast(1)
                                    val localPrefs = remember { AppPreferences(context) }
                                    val marketTaxRate = if (localPrefs.hasPremium) 0.04 else 0.08
                                    val netOrderSellPrice = (sellOrderPrice * (1.0 - marketTaxRate - 0.025)).toLong()
                                    val netOrderUnitProfit = netOrderSellPrice - buyOrderPrice
                                    val maxOrderProfitTotal = netOrderUnitProfit * activeOrder.plannedUnits

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                        Text(
                                            text = "🛒 Kauforder in $buyCityTrans: ${fmt.format(buyOrderPrice)} S.",
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        )

                                        Text(
                                            text = "📈 Verkauforder in $sellCityTrans: ${fmt.format(sellOrderPrice)} S.",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        )

                                        Text(
                                            text = "💡 KI Max-Marge: +${fmt.format(maxOrderProfitTotal)} S. Netto (+${fmt.format(netOrderUnitProfit)} S./Stk.)",
                                            color = Color(0xFF4ADE80),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }

                                    // Row 3: Compact Action Buttons (Buchen / Stornieren)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    ) {
                                        Button(
                                            onClick = {
                                                isBookingMode = true
                                                onFocusModeChanged(true)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.weight(1f).height(26.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(if (lang == "DE") "Buchen" else "Book", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 9.5.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val prefs = AppPreferences(context)
                                                    val orders = prefs.getTradeOrders()
                                                    val updatedOrders = orders.filter { it.id != activeOrder.id }
                                                    prefs.saveTradeOrders(updatedOrders)
                                                    prefs.clearDraftOrderInput(activeOrder.id)
                                                    Toast.makeText(context, if (lang == "DE") "Auftrag storniert!" else "Order cancelled!", Toast.LENGTH_SHORT).show()
                                                    onOrderBooked()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.weight(1f).height(26.dp)
                                        ) {
                                            Text(if (lang == "DE") "Stornieren" else "Cancel", fontSize = 9.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        } else {
                            val prefsObj = remember { AppPreferences(context) }
                            val draft = remember(activeOrder.id) { prefsObj.getDraftOrderInput(activeOrder.id) }

                            var unitsInput by remember(activeOrder.id) { mutableStateOf(draft?.units ?: activeOrder.plannedUnits.toString()) }
                            var buyPriceInput by remember(activeOrder.id) { mutableStateOf(draft?.buyPrice ?: activeOrder.buyPrice.toString()) }
                            var sellPriceInput by remember(activeOrder.id) { mutableStateOf(draft?.sellPrice ?: activeOrder.sellPrice.toString()) }

                            val actualUnits = unitsInput.toIntOrNull() ?: activeOrder.plannedUnits
                            val actualBuyPrice = buyPriceInput.toIntOrNull() ?: activeOrder.buyPrice
                            val actualSellPrice = sellPriceInput.toIntOrNull() ?: activeOrder.sellPrice

                            val marketTaxPercent = if (prefsObj.hasPremium) 4.0 else 8.0
                            val taxPerUnit = (actualSellPrice * (marketTaxPercent / 100.0)).toLong()
                            val setupFeePerUnit = (actualSellPrice * 0.025).toLong()
                            val netSellPrice = actualSellPrice.toLong() - taxPerUnit - setupFeePerUnit

                            val computedSpent = actualUnits.toLong() * actualBuyPrice.toLong()
                            val computedEarned = actualUnits.toLong() * netSellPrice

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E3A4C),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = "${activeOrder.resourceNameDe} (T${activeOrder.tier})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = unitsInput,
                                        onValueChange = {
                                            unitsInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, it, buyPriceInput, sellPriceInput)
                                        },
                                        label = { Text(if (lang == "DE") "Menge" else "Units", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    OutlinedTextField(
                                        value = buyPriceInput,
                                        onValueChange = {
                                            buyPriceInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, unitsInput, it, sellPriceInput)
                                        },
                                        label = { Text(if (lang == "DE") "Kaufpreis" else "Buy Price", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    OutlinedTextField(
                                        value = sellPriceInput,
                                        onValueChange = {
                                            sellPriceInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, unitsInput, buyPriceInput, it)
                                        },
                                        label = { Text(if (lang == "DE") "Verkaufspreis" else "Sell Price", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
                                    val computedProfit = computedEarned - computedSpent

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (lang == "DE") "📊 Netto-Einkommen (nach Steuern): ${fmt.format(computedEarned)} S." else "📊 Net Revenue (after tax): ${fmt.format(computedEarned)} S.",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = if (lang == "DE") "Ausgegeben (Kaufpreis): ${fmt.format(computedSpent)} S." else "Spent: ${fmt.format(computedSpent)} S.",
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = if (lang == "DE") "Reingewinn: ${if (computedProfit >= 0) "+" else ""}${fmt.format(computedProfit)} S." else "Profit/Loss: ${if (computedProfit >= 0) "+" else ""}${fmt.format(computedProfit)} S.",
                                                color = if (computedProfit >= 0) Color(0xFF66BB6A) else Color(0xFFFF8A80),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                isBookingMode = false
                                                onFocusModeChanged(false)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (lang == "DE") "❌ Stornieren" else "Cancel", fontSize = 10.sp, color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                try {
                                                    val prefs = AppPreferences(context)
                                                    val orders = prefs.getTradeOrders()
                                                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())

                                                    val updatedOrders = orders.map { o ->
                                                        if (o.id == activeOrder.id) {
                                                            o.copy(
                                                                status = OrderStatus.COMPLETED,
                                                                actualUnits = actualUnits,
                                                                actualBuyPrice = actualBuyPrice,
                                                                actualSellPrice = actualSellPrice,
                                                                actualSilverSpent = computedSpent,
                                                                actualSilverEarned = computedEarned,
                                                                completedDate = dateStr
                                                            )
                                                        } else o
                                                    }

                                                    prefs.saveTradeOrders(updatedOrders)
                                                    prefs.clearDraftOrderInput(activeOrder.id)
                                                    Toast.makeText(context, if (lang == "DE") "✅ Erfolgreich gebucht!" else "Successfully booked!", Toast.LENGTH_SHORT).show()

                                                    isBookingMode = false
                                                    onFocusModeChanged(false)
                                                    onOrderBooked()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text(if (lang == "DE") "✅ Buchen" else "Book", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        } else if (selectedTab == BubbleTab.TOP_MARGIN) {
                        val viewModel = SharedViewModelProvider.get(context.applicationContext as Application)
                        val uiState by viewModel.uiState.collectAsState()
                        var showAdvancedFilters by remember { mutableStateOf(value = false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxBubbleHeight)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val prefsForCity = remember { AppPreferences(context) }
                            var currentBubbleCity by remember { mutableStateOf(prefsForCity.bubbleStandpunktCity) }
                            var currentBubbleCategory by remember { mutableStateOf(prefsForCity.bubbleCategory) }
                            var currentBubbleTier by remember { mutableIntStateOf(prefsForCity.bubbleTier) }
                            var currentBubbleEnchantment by remember { mutableIntStateOf(prefsForCity.bubbleEnchantment) }
                            var searchInputText by remember { mutableStateOf(prefsForCity.bubbleSearchQuery) }
                            val bubbleCities = remember { listOf("ALLE", "Bridgewatch", "Caerleon", "Fort Sterling", "Lymhurst", "Martlock", "Thetford", "Brecilien", "Black Market") }

                            // 📊 SMART DASHBOARD
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📦 Deals", color = Color.Gray, fontSize = 8.sp)
                                        Text(topOpportunities.size.toString(), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val topProfit = topOpportunities.maxOfOrNull { it.totalNetProfit } ?: 0L
                                        val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
                                        Text("💰 Top Profit", color = Color.Gray, fontSize = 8.sp)
                                        Text("+${fmt.format(topProfit)}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("⏰ Scan", color = Color.Gray, fontSize = 8.sp)
                                        Text("Live", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            // 🔍 Real-Time Category & Item Search Input (Always visible)
                            OutlinedTextField(
                                value = searchInputText,
                                onValueChange = { str ->
                                    searchInputText = str
                                    prefsForCity.bubbleSearchQuery = str
                                    onRefresh()
                                },
                                label = { Text("🔍 Suche Item / Kategorie...", fontSize = 8.5.sp, color = Color.LightGray) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                            )

                            // ⚡ Category Presets Bar (Always visible)
                            val presets = listOf(
                                "ALL" to "🌟 Alle",
                                "SAMMLER" to "🌾 Sammler",
                                "GEAR" to "⚔️ Gear",
                                "GASTRO" to "🧪 Gastro",
                                "LUXUS" to "🐎 Luxus"
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(presets) { pair: Pair<String, String> ->
                                    val (presetKey, presetLabel) = pair
                                    val isSelected = currentBubbleCategory == presetKey
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                        modifier = Modifier.clickable {
                                            currentBubbleCategory = presetKey
                                            prefsForCity.bubbleCategory = presetKey
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = presetLabel,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // ⚙️ Profi-Filter Toggle Button
                            Button(
                                onClick = { showAdvancedFilters = !showAdvancedFilters },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(26.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (showAdvancedFilters) "➖ Profi-Filter einklappen" else "➕ Profi-Filter & Budget", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            AnimatedVisibility(visible = showAdvancedFilters) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("⚡ Silber Budget & Tragkraft", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                                var editSilverText by remember(uiState.silverBudget) { mutableStateOf(uiState.silverBudget.toString()) }
                                                OutlinedTextField(
                                                    value = editSilverText,
                                                    onValueChange = { str ->
                                                        val clean = str.filter { it.isDigit() }
                                                        editSilverText = clean
                                                        val valLong = clean.toLongOrNull() ?: 0L
                                                        viewModel.onSilverBudgetChanged(valLong)
                                                        prefsForCity.silverBudget = valLong
                                                        onRefresh()
                                                    },
                                                    label = { Text("💰 Budget", fontSize = 8.sp, color = Color.LightGray) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                                                )

                                                var editCapText by remember(uiState.carryCapacityKg) { mutableStateOf(uiState.carryCapacityKg.toLong().toString()) }
                                                OutlinedTextField(
                                                    value = editCapText,
                                                    onValueChange = { str ->
                                                        val clean = str.filter { it.isDigit() }
                                                        editCapText = clean
                                                        val valDbl = clean.toDoubleOrNull() ?: 0.0
                                                        viewModel.onCarryCapacityChanged(valDbl)
                                                        prefsForCity.carryCapacityKg = valDbl
                                                        onRefresh()
                                                    },
                                                    label = { Text("⚖️ kg", fontSize = 8.sp, color = Color.LightGray) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                                                )
                                            }

                                            // Schnell-Buttons für Silber
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                                                listOf(1_000_000L to "1M", 5_000_000L to "5M", 10_000_000L to "10M", 50_000_000L to "50M").forEach { (amt, label) ->
                                                    Button(
                                                        onClick = {
                                                            viewModel.onSilverBudgetChanged(amt)
                                                            prefsForCity.silverBudget = amt
                                                            onRefresh()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (uiState.silverBudget == amt) Color(0xFF3B82F6) else Color(0xFF334155)),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.weight(1f).height(24.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            // Schnell-Buttons für Tragkraft
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                                                listOf(1000 to "1t", 2000 to "2t", 3000 to "3t", 5000 to "5t").forEach { (cap, label) ->
                                                    Button(
                                                        onClick = {
                                                            viewModel.onCarryCapacityChanged(cap.toDouble())
                                                            prefsForCity.carryCapacityKg = cap.toDouble()
                                                            onRefresh()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (uiState.carryCapacityKg.toInt() == cap) Color(0xFF3B82F6) else Color(0xFF334155)),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.weight(1f).height(24.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text("🏙️ Standpunkt:", color = Color(0xFF81D4FA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                        items(bubbleCities) { c: String ->
                                            val isSelected = currentBubbleCity == c
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) Color(0xFF4CAF50) else Color(0xFF1E3A4C),
                                                modifier = Modifier.clickable {
                                                    currentBubbleCity = c
                                                    prefsForCity.bubbleStandpunktCity = c
                                                    onRefresh()
                                                }
                                            ) {
                                                Text(if (c == "ALLE") "Alle" else LanguageManager.getCityTranslation(c, lang), color = Color.White, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Text("📦 Kategorie-Filter:", color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        items(ResourceCategory.entries.toList()) { cat: ResourceCategory ->
                                            val isSelected = currentBubbleCategory == cat.name
                                            val dealCount = topOpportunities.count { it.resource.category == cat }
                                            val countText = if (dealCount > 0) " ($dealCount)" else ""
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E3A4C),
                                                modifier = Modifier.clickable {
                                                    currentBubbleCategory = cat.name
                                                    prefsForCity.bubbleCategory = cat.name
                                                    onRefresh()
                                                }
                                            ) {
                                                Text("${cat.displayName}$countText", color = Color.White, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Text("⭐ Tier-Filter:", color = Color(0xFFFDD835), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    var currentBubbleTier by remember { mutableIntStateOf(prefsForCity.bubbleTier) }
                                    val bubbleTiers = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        items(bubbleTiers) { tier: Int ->
                                            val isSelected = currentBubbleTier == tier
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                                modifier = Modifier.clickable {
                                                    currentBubbleTier = tier
                                                    prefsForCity.bubbleTier = tier
                                                    onRefresh()
                                                }
                                            ) {
                                                Text(if (tier == 0) "Alle" else "T$tier", color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Text("✨ Verzauberungs-Filter:", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    var currentBubbleEnchantment by remember { mutableIntStateOf(prefsForCity.bubbleEnchantment) }
                                    val bubbleEnchantments = listOf(-1 to "ALLE", 0 to ".0", 1 to ".1", 2 to ".2", 3 to ".3", 4 to ".4")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        items(bubbleEnchantments) { pair: Pair<Int, String> ->
                                            val (enc, label) = pair
                                            val isSelected = currentBubbleEnchantment == enc
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color(0xFF00E676) else Color(0xFF1E3A4C),
                                                modifier = Modifier.clickable {
                                                    currentBubbleEnchantment = enc
                                                    prefsForCity.bubbleEnchantment = enc
                                                    onRefresh()
                                                }
                                            ) {
                                                Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Text("🗺️ Max. Zonen-Distanz:", color = Color(0xFFCE93D8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    var currentBubbleMaxZones by remember { mutableIntStateOf(prefsForCity.bubbleMaxZones) }
                                    val zoneOptions = listOf(1, 2, 3, 5, 99)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        items(zoneOptions) { z ->
                                            val isSelected = currentBubbleMaxZones == z
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color(0xFFAB47BC) else Color(0xFF1E3A4C),
                                                modifier = Modifier.clickable {
                                                    currentBubbleMaxZones = z
                                                    prefsForCity.bubbleMaxZones = z
                                                    onRefresh()
                                                }
                                            ) {
                                                Text(if (z == 99) "Alle Zonen" else "$z Zonen", color = if (isSelected) Color.White else Color(0xFFB0BEC5), fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    var currentBubbleAvoidDangerous by remember { mutableStateOf(prefsForCity.bubbleAvoidDangerousZones || prefsForCity.avoidDangerousZones) }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                        Text("🔴 Rote / PvP Zonen ausblenden", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = currentBubbleAvoidDangerous,
                                            onCheckedChange = {
                                                currentBubbleAvoidDangerous = it
                                                prefsForCity.bubbleAvoidDangerousZones = it
                                                prefsForCity.avoidDangerousZones = it
                                                onRefresh()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444)),
                                        )
                                    }

                                    var currentBubbleHideBrecilien by remember { mutableStateOf(prefsForCity.bubbleHideBrecilien || prefsForCity.hideBrecilien) }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                        Text("✨ Brecilien ausblenden", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = currentBubbleHideBrecilien,
                                            onCheckedChange = {
                                                currentBubbleHideBrecilien = it
                                                prefsForCity.bubbleHideBrecilien = it
                                                prefsForCity.hideBrecilien = it
                                                onRefresh()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8)),
                                        )
                                    }

                                    var currentBubbleHideBlackMarket by remember { mutableStateOf(prefsForCity.bubbleHideBlackMarket || prefsForCity.hideBlackMarket) }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                        Text("🏴‍☠️ Schmuggler ausblenden", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = currentBubbleHideBlackMarket,
                                            onCheckedChange = {
                                                currentBubbleHideBlackMarket = it
                                                prefsForCity.bubbleHideBlackMarket = it
                                                prefsForCity.hideBlackMarket = it
                                                if (it && selectedTab == BubbleTab.SMUGGLER_RADAR) {
                                                    selectedTab = BubbleTab.TOP_MARGIN
                                                }
                                                onRefresh()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB74D)),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            var currentBubbleSort by remember { mutableStateOf("MARGE") }

                            // Sortier-Optionen im Bubble Overlay (Marge | Neueste | Wenigster Bestand)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentBubbleSort == "MARGE") Color(0xFF10B981) else Color(0xFF1E3A4C),
                                    modifier = Modifier.weight(1f).clickable { currentBubbleSort = "MARGE" }
                                ) {
                                    Text("🔥 Marge", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentBubbleSort == "NEWEST") Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                    modifier = Modifier.weight(1f).clickable { currentBubbleSort = "NEWEST" }
                                ) {
                                    Text("⏰ Neueste", color = if (currentBubbleSort == "NEWEST") Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentBubbleSort == "FEWEST_STOCK") Color(0xFFF59E0B) else Color(0xFF1E3A4C),
                                    modifier = Modifier.weight(1f).clickable { currentBubbleSort = "FEWEST_STOCK" }
                                ) {
                                    Text("📦 Bestand", color = if (currentBubbleSort == "FEWEST_STOCK") Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 3.dp))
                                }
                            }

                            val filteredBubbleOpportunities = remember(
                                topOpportunities,
                                currentBubbleCity,
                                currentBubbleCategory,
                                currentBubbleTier,
                                currentBubbleEnchantment,
                                searchInputText,
                                currentBubbleSort
                            ) {
                                val searchQ = searchInputText.trim().lowercase()
                                val filtered = topOpportunities.filter { opp ->
                                    val matchesCity = currentBubbleCity == "ALLE" || TradeCalculator.citiesMatch(opp.buyCity, currentBubbleCity)
                                    val matchesCat = when (currentBubbleCategory) {
                                        "ALL" -> true
                                        "SAMMLER" -> opp.resource.category == ResourceCategory.RESOURCES || opp.resource.category == ResourceCategory.REFINED
                                        "GEAR" -> opp.resource.category == ResourceCategory.WEAPONS || opp.resource.category == ResourceCategory.ARMOR || opp.resource.category == ResourceCategory.HELMETS || opp.resource.category == ResourceCategory.SHOES || opp.resource.category == ResourceCategory.OFFHAND || opp.resource.category == ResourceCategory.BAG || opp.resource.category == ResourceCategory.CAPE
                                        "GASTRO" -> opp.resource.category == ResourceCategory.FOOD || opp.resource.category == ResourceCategory.POTIONS
                                        "LUXUS" -> opp.resource.category == ResourceCategory.MOUNTS || opp.resource.category == ResourceCategory.ARTIFACTS
                                        else -> opp.resource.category.name.equals(currentBubbleCategory, ignoreCase = true) || opp.resource.category.displayName.equals(currentBubbleCategory, ignoreCase = true)
                                    }
                                    val matchesTier = currentBubbleTier == 0 || opp.resource.tier == currentBubbleTier
                                    val matchesEnc = currentBubbleEnchantment < 0 || opp.resource.enchantment == currentBubbleEnchantment
                                    val matchesSearch = searchQ.isBlank() || opp.resource.nameDe.lowercase().contains(searchQ) || opp.resource.nameEn.lowercase().contains(searchQ) || opp.resource.id.lowercase().contains(searchQ)

                                    matchesCity && matchesCat && matchesTier && matchesEnc && matchesSearch
                                }

                                val (freshBotOpps, olderOpps) = filtered.partition { it.ageInSeconds <= 300 || it.priorityScore >= 90 }
                                when (currentBubbleSort) {
                                    "NEWEST" -> freshBotOpps.sortedByDescending { it.updatedTimestamp } + olderOpps.sortedByDescending { it.updatedTimestamp }
                                    "FEWEST_STOCK" -> freshBotOpps.sortedWith(compareBy<TradeOpportunity> { if (it.stockAvailable > 0) it.stockAvailable else Int.MAX_VALUE }.thenByDescending { it.roiPercent }) + olderOpps.sortedWith(compareBy<TradeOpportunity> { if (it.stockAvailable > 0) it.stockAvailable else Int.MAX_VALUE }.thenByDescending { it.roiPercent })
                                    else -> freshBotOpps.sortedWith(compareByDescending<TradeOpportunity> { it.roiPercent }.thenByDescending { it.totalNetProfit }) + olderOpps.sortedWith(compareByDescending<TradeOpportunity> { it.roiPercent }.thenByDescending { it.totalNetProfit })
                                }
                            }

                            val sortedBubbleOpportunities = filteredBubbleOpportunities

                            if (isLoadingOpps && sortedBubbleOpportunities.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF81D4FA), modifier = Modifier.size(24.dp))
                                }
                            } else if (sortedBubbleOpportunities.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E3A4C),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                        Text(if (lang == "DE") "Keine Handelschancen gefunden." else "No trade opportunities found.", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                sortedBubbleOpportunities.forEachIndexed { index, opp ->
                                    key("${opp.resource.fullId}_${opp.buyCity}_${opp.sellCity}") {
                                        val buyTrans = LanguageManager.getCityTranslation(opp.buyCity, lang)
                                        val sellTrans = LanguageManager.getCityTranslation(opp.sellCity, lang)

                                        val buyColor = ZoneThemeColors.getCityZoneColor(opp.buyCity)
                                        val sellColor = ZoneThemeColors.getCityZoneColor(opp.sellCity)
                                        val borderColor = ZoneThemeColors.getOpportunityBorderColor(opp.buyCity, opp.sellCity)
                                        val containerBg = ZoneThemeColors.getOpportunityContainerBg(opp.buyCity, opp.sellCity)

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = containerBg,
                                            border = BorderStroke(1.5.dp, borderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                                .padding(bottom = 6.dp)
                                                .clickable {
                                                    onAcceptOpportunity(opp)
                                                    selectedTab = BubbleTab.ACTIVE_ORDER
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                if (isCompactMode) {
                                                    // ULTRA-KOMPAKTE ZEILE IM COMPACT MODE: Alle wichtigen Infos auf 1-2 Zeilen ohne Scroll-Overhead
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            AsyncImage(
                                                                model = opp.resource.imageUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Surface(shape = RoundedCornerShape(3.dp), color = getTierColor(opp.resource.tier)) {
                                                                Text("${opp.resource.tierText}${opp.resource.enchantmentText}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp))
                                                            }
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = opp.resource.nameDe,
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981)) {
                                                            Text("+${String.format(Locale.GERMANY, "%.0f", opp.roiPercent)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFF10B981),
                                                            modifier = Modifier.clickable {
                                                                onAcceptOpportunity(opp)
                                                                selectedTab = BubbleTab.ACTIVE_ORDER
                                                            }
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Auftrag", tint = Color.Black, modifier = Modifier.size(16.dp).padding(2.dp))
                                                        }
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "$buyTrans (${fmt.format(opp.buyPrice)}) ➜ $sellTrans (${fmt.format(opp.sellPrice)})",
                                                            color = Color(0xFF81C784),
                                                            fontSize = 8.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "+${fmt.format(opp.totalNetRevenue)} S. | ⏱️ ${opp.ageInSeconds}s",
                                                            color = Color(0xFFFFB74D),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                } else {
                                                    // TOP ROW: Image + Rank + Tier Badge + Item Name + ROI % + Add Order Button
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            AsyncImage(
                                                                model = opp.resource.imageUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = getTierColor(opp.resource.tier)
                                                            ) {
                                                                Text(
                                                                    text = "${opp.resource.tierText}${opp.resource.enchantmentText}",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 9.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "${index + 1}. ${opp.resource.nameDe}",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // ROI Badge
                                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981)) {
                                                            Text(
                                                                text = "+${String.format(Locale.GERMANY, "%.1f", opp.roiPercent)}%",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // Accept Order (+) Button
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color(0xFF10B981),
                                                            modifier = Modifier.clickable {
                                                                onAcceptOpportunity(opp)
                                                                selectedTab = BubbleTab.ACTIVE_ORDER
                                                            }
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = "Auftrag annehmen",
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // MIDDLE ROW: Buy City & Price -> Sell City & Price
                                                    val aiBuyOrder = if (opp.recommendedBuyOrderPrice > 0) opp.recommendedBuyOrderPrice else (opp.buyPrice * 0.88).toInt().coerceAtLeast(1)
                                                    val aiSellOrder = if (opp.recommendedSellOrderPrice > 0) opp.recommendedSellOrderPrice else (opp.sellPrice * 1.08).toInt().coerceAtLeast(1)

                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "🛒 Kaufen in: $buyTrans",
                                                                color = buyColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            )
                                                            Text(
                                                                text = "Kaufpreis: ${fmt.format(opp.buyPrice)} S.",
                                                                color = Color(0xFF81C784),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            )
                                                            Text(
                                                                text = "🤖 Kauforder (KI): ${fmt.format(aiBuyOrder)} S. (Dip -${String.format(Locale.GERMANY, "%.1f", opp.expectedPriceDropPercent)}%)",
                                                                color = Color(0xFF38BDF8),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp
                                                            )
                                                        }

                                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "🏷️ Verkaufen in: $sellTrans",
                                                                color = sellColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            )
                                                            Text(
                                                                text = "Verkaufspreis: ${fmt.format(opp.sellPrice)} S.",
                                                                color = Color(0xFFFFB74D),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            )
                                                            Text(
                                                                text = "🤖 Verkauforder (KI): ${fmt.format(aiSellOrder)} S. (Peak +${String.format(Locale.GERMANY, "%.1f", opp.expectedPriceRisePercent)}%)",
                                                                color = Color(0xFFFFD700),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // AI Trading Suggestion with Rainbow Colors
                                                    val aiText = "🤖 KI-Handelsempfehlung: ${opp.tradeUnits}x kaufen in $buyTrans (${fmt.format(opp.buyPrice)} S.) & verkaufen in $sellTrans (${fmt.format(opp.sellPrice)} S.) - Netto-Gewinn: +${fmt.format(opp.totalNetRevenue)} Silber"
                                                    val rainbowColors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFF8B5CF6), Color(0xFFEC4899))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        aiText.forEachIndexed { charIdx, char ->
                                                            Text(
                                                                text = char.toString(),
                                                                color = rainbowColors[charIdx % rainbowColors.size],
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    val guaranteedPrice = if (opp.recommendedSellOrderPrice > 0) opp.recommendedSellOrderPrice else (opp.sellPrice * 0.98).toInt().coerceAtLeast(1)

                                                    // BOTTOM ROW: Total Profit + Guaranteed Sell Price Badge + Bestand Badge + Zone Distance + Found Time
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "📊 ${fmt.format(opp.totalNetProfit)} S. (${fmt.format(opp.tradeUnits)} Stk.)",
                                                            color = Color(0xFF10B981),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                                                Text(
                                                                    text = "🔥 ${fmt.format(guaranteedPrice)} S.",
                                                                    color = Color(0xFFFFD700),
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 8.5.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }

                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) {
                                                                Text(
                                                                    text = "📦 ${fmt.format(opp.stockAvailable)} Stk.",
                                                                    color = Color(0xFFF59E0B),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 8.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }

                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                                                Text(
                                                                    text = "🗺️ ${opp.zonesWalkedCount} Zonen",
                                                                    color = Color(0xFF81D4FA),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 8.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }

                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                                                val secondsAgo = opp.ageInSeconds
                                                                val timeDisplay = when {
                                                                    secondsAgo < 60 -> "Vor ${secondsAgo}s"
                                                                    secondsAgo < 3600 -> "Vor ${secondsAgo}s (${secondsAgo / 60}m)"
                                                                    else -> "Vor ${secondsAgo}s (${secondsAgo / 3600}h)"
                                                                }
                                                                Text(
                                                                    text = "⏱️ $timeDisplay",
                                                                    color = Color(0xFFFBBF24),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 8.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // ALBION 2D STATS LINK & LIQUIDITY BADGE
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = opp.liquidityScore.ifBlank { "📊 24h Markt-Volumen" },
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )

                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF0F172A),
                                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                                            modifier = Modifier.clickable {
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_VIEW, opp.albion2dUrl.toUri()).apply {
                                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    }
                                                                    context.startActivity(intent)
                                                                } catch (_: Exception) {}
                                                            }
                                                        ) {
                                                            Text(
                                                                text = "🌐 2D Stats",
                                                                color = Color(0xFF38BDF8),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val viewModel = SharedViewModelProvider.get(context.applicationContext as Application)
                        val uiState by viewModel.uiState.collectAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxBubbleHeight)
                                .padding(vertical = 4.dp)
                        ) {
                            when (selectedTab) {
                                BubbleTab.SMUGGLER_RADAR -> BubbleSmugglerRadarTab(viewModel = viewModel, uiState = uiState, maxHeight = maxBubbleHeightTab)
                                BubbleTab.INVENTORY_ROUTER -> BubbleInventoryRouterTab(viewModel = viewModel, uiState = uiState, maxHeight = maxBubbleHeightTab)
                                BubbleTab.COMPLETED_ORDERS -> BubbleCompletedOrdersTab(context = context, onRefresh = onRefresh, maxHeight = maxBubbleHeightTab)
                                BubbleTab.CATALOG -> BubbleCatalogTab(
                                    uiState = uiState,
                                    onFocusModeChanged = onFocusModeChanged,
                                    maxHeight = maxBubbleHeightTab
                                ) { res ->
                                    viewModel.onResourceClicked(res)
                                }
                                BubbleTab.CRAFTING -> BubbleCraftingTab(viewModel = viewModel, uiState = uiState, maxHeight = maxBubbleHeightTab)
                                BubbleTab.ISLAND -> BubbleIslandTab(context = context, maxHeight = maxBubbleHeightTab)
                                BubbleTab.EVENTS -> BubbleEventsTab(uiState = uiState, maxHeight = maxBubbleHeightTab)
                                BubbleTab.GOLD_MARKET -> BubbleGoldTab(viewModel = viewModel, uiState = uiState, onFocusModeChanged = onFocusModeChanged, maxHeight = maxBubbleHeightTab)
                                BubbleTab.BUILDS -> BubbleBuildsTab(uiState = uiState, maxHeight = maxBubbleHeightTab)
                                BubbleTab.WORLD_MAP -> BubbleMapTab(viewModel = viewModel, maxHeight = maxBubbleHeightTab)
                                BubbleTab.SETTINGS -> BubbleSettingsTab(context = context, maxHeight = maxBubbleHeightTab, onRefresh = onRefresh)
                                BubbleTab.ADMIN -> {
                                    if (prefs.isAdmin) {
                                        BubbleAdminTab(context = context, maxHeight = maxBubbleHeightTab, onFocusModeChanged = onFocusModeChanged)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleSmugglerRadarTab(
    @Suppress("UNUSED_PARAMETER") viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState,
    maxHeight: Dp,
) {
    val priceMap = remember(uiState.marketPrices) { uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() } }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    
    val smugglerOpps = remember(priceMap, uiState.silverBudget) {
        TradeCalculator.calculateSmugglerOpportunities(AlbionResourceRepository.resources, priceMap, uiState.silverBudget).take(30)
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
        Text("🏴‍☠️ Schwarzmarkt-Radar (Caerleon)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(bottom = 6.dp))
        
        if (smugglerOpps.isEmpty()) {
            Text("Keine Schwarzmarkt-Deals gefunden. Prüfe Budget.", color = Color.Gray, fontSize = 10.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(smugglerOpps) { idx, opp ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    AsyncImage(model = opp.resource.imageUrl, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${idx+1}. ${opp.resource.nameDe} (T${opp.resource.tier}${if (opp.resource.enchantment>0) ".${opp.resource.enchantment}" else ""})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981)) {
                                    Text("+${String.format(Locale.GERMANY, "%.1f", opp.roiPercent)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("🛒 Kaufe in: ${LanguageManager.getCityTranslation(opp.buyCity, "DE")}", color = ZoneThemeColors.getCityZoneColor(opp.buyCity), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${fmt.format(opp.buyPrice)} S.", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("🏴‍☠️ Verkaufe an: Schwarzmarkt", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${fmt.format(opp.sellPrice)} S.", color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("📊 Reingewinn: +${fmt.format(opp.totalNetProfit)} S. (${fmt.format(opp.tradeUnits)} Stk.)", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleInventoryRouterTab(
    @Suppress("UNUSED_PARAMETER") viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState,
    maxHeight: Dp
) {
    val priceMap = remember(uiState.marketPrices) { uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() } }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    
    val routes = remember(priceMap, uiState.silverBudget, uiState.carryCapacityKg) {
        val baseOpps = TradeCalculator.calculateOpportunities(
            resources = AlbionResourceRepository.resources,
            pricesByItem = priceMap,
            silverBudget = uiState.silverBudget,
            carryCapacityKg = uiState.carryCapacityKg,
            marketTaxPercent = 4.0,
            targetMarginPercent = 5.0,
            avoidDangerousZones = false,
            currentGoldPrice = 4250,
            standpunktCity = null,
            maxCityDistance = 99,
            hideBrecilien = false,
            hideBlackMarket = false
        )
        TradeCalculator.calculateInventoryRoutes(baseOpps, uiState.carryCapacityKg, uiState.silverBudget).take(15)
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
        Text("🎒 Smart-Inventory Router (Volle-Taschen Route)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFF59E0B), modifier = Modifier.padding(bottom = 6.dp))
        
        if (routes.isEmpty()) {
            Text("Keine Routen gefunden. Prüfe Budget & Tragkraft.", color = Color.Gray, fontSize = 10.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(routes) { idx, route ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("🗺️ Route #${idx+1}: ${LanguageManager.getCityTranslation(route.buyCity, "DE")} ➜ ${LanguageManager.getCityTranslation(route.sellCity, "DE")}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) {
                                    Text("${route.zonesWalked} Zonen", color = Color(0xFF81D4FA), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📊 Gesamt-Reingewinn: +${fmt.format(route.totalNetProfit)} S.", color = Color(0xFF4ADE80), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            Text("⚖️ Gesamtgewicht: ${String.format(Locale.GERMANY, "%.1f", route.totalWeightKg)} kg | 💰 Invest: ${fmt.format(route.totalInvestment)} S.", color = Color.LightGray, fontSize = 9.sp)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🛒 Einkaufszettel (${route.itemsToBuy.size} Items):", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Column(modifier = Modifier.padding(start = 4.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                route.itemsToBuy.forEach { item ->
                                    Text("• ${item.tradeUnits}x ${item.resource.nameDe} (${fmt.format(item.buyPrice)} S./Stk)", color = Color.White, fontSize = 8.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleCatalogTab(
    uiState: ResourceUiState,
    onFocusModeChanged: (Boolean) -> Unit,
    maxHeight: Dp,
    onResourceClick: (AlbionResource) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(ResourceCategory.ALL) }
    var selectedTier by remember { mutableIntStateOf(0) }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    val filtered = remember(query, selectedCat, selectedTier, uiState.filteredResources) {
        uiState.filteredResources.asSequence().filter { res ->
            val matchesQuery = query.isBlank() || res.nameDe.contains(query, ignoreCase = true) || res.id.contains(query, ignoreCase = true)
            val matchesCat = if (selectedCat != ResourceCategory.ALL) res.category == selectedCat else true
            val matchesTier = if (selectedTier > 0) res.tier == selectedTier else true
            matchesQuery && matchesCat && matchesTier
        }.take(15).toList()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        // Search TextField
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Suchname oder ID (z.B. Holz, T4_WOOD)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocusModeChanged(true)
                    }
                }
        )

        // Category Filter Chips Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            items(ResourceCategory.entries.toList()) { cat: ResourceCategory ->
                val isSelected = selectedCat == cat
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedCat = cat }
                ) {
                    Text(
                        text = cat.displayName,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Tier Filter Chips Row
        val tiersList = listOf(0, 2, 3, 4, 5, 6, 7, 8)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            items(tiersList) { tier: Int ->
                val isSelected = selectedTier == tier
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedTier = tier }
                ) {
                    Text(
                        text = if (tier == 0) "Alle Stufen" else "Tier $tier",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Catalog Product Cards
        if (filtered.isEmpty()) {
            Text("Keine Ressourcen im Katalog gefunden.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            val priceMap = remember(uiState.marketPrices) { uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() } }

            filtered.forEach { res ->
                key(res.id) {
                    var selectedEnc by remember(res.id) { mutableIntStateOf(res.enchantment) }
                    val effectiveResource = remember(res, selectedEnc) { res.copy(enchantment = selectedEnc) }
                    val rawPrices = priceMap[effectiveResource.fullId] ?: priceMap[effectiveResource.id] ?: emptyList()
                    val mult = when (effectiveResource.enchantment) {
                        1 -> 2.2
                        2 -> 4.5
                        3 -> 9.0
                        4 -> 18.0
                        else -> 1.0
                    }
                    val prices = if ((priceMap[effectiveResource.fullId] == null) && (effectiveResource.enchantment > 0)) {
                        rawPrices.map { it.copy(sellPriceMin = (it.sellPriceMin * mult).toInt(), buyPriceMax = (it.buyPriceMax * mult).toInt()) }
                    } else {
                        rawPrices
                    }
                    val validPrices = prices.filter { it.sellPriceMin > 0 && !AlbionMarketApi.isUnrealisticPrice(it.itemId, it.sellPriceMin) }
                    val bestBuy = validPrices.minByOrNull { it.sellPriceMin }
                    val bestSell = validPrices.maxByOrNull { it.sellPriceMin }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E3A4C),
                        modifier = Modifier.fillMaxWidth().clickable { onResourceClick(effectiveResource) }
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = effectiveResource.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = getTierColor(effectiveResource.tier)
                                        ) {
                                            Text(
                                                text = "${effectiveResource.tierText}${effectiveResource.enchantmentText}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = effectiveResource.nameDe,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Enchantment selector chips (.0, .1, .2, .3, .4)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                                        (0..4).forEach { enc ->
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (selectedEnc == enc) Color(0xFF2E7D32) else Color(0xFF0A1922),
                                                modifier = Modifier.clickable { selectedEnc = enc }
                                            ) {
                                                Text(
                                                    text = ".$enc",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (bestBuy != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🛒 Kaufort: ${bestBuy.city} (${fmt.format(bestBuy.sellPriceMin)} Silber)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                                        Text("✓ KI-Geprüft", color = Color(0xFF10B981), fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                }
                                if ((bestSell != null) && (bestSell.city != bestBuy.city)) {
                                    Text("🏷️ Verkaufort: ${bestSell.city} (${fmt.format(bestSell.sellPriceMin)} Silber)", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            } else {
                                Text("Bisher kein Preis", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleCraftingTab(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState,
    maxHeight: Dp,
) {
    val priceMap = remember(uiState.marketPrices) { uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() } }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val fmtDec = remember { DecimalFormat("0.0", DecimalFormatSymbols(Locale.GERMANY)) }
    var hideCaerleonInCrafting by remember { mutableStateOf(value = false) }

    val craftingOpps = remember(priceMap, hideCaerleonInCrafting) {
        CraftingRepository.calculateCraftingOpportunities(priceMap, hasPremium = true, hideCaerleon = hideCaerleonInCrafting).take(25)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            Text("⚒️ Handwerks-Guide", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔴 Caerleon ausblenden", fontSize = 8.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = hideCaerleonInCrafting,
                    onCheckedChange = { hideCaerleonInCrafting = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444)),
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
        itemsIndexed(craftingOpps, key = { index: Int, opp: CraftingOpportunityDetails -> "${opp.resource.fullId}_$index" }) { index: Int, opp: CraftingOpportunityDetails ->
            val rankBadge = when (index) {
                0 -> "🏆 #1 Beste Marge"
                1 -> "🥈 #2 Top Marge"
                2 -> "🥉 #3 Top Marge"
                else -> "#${index + 1}"
            }
            val rankColor = when (index) {
                0 -> Color(0xFFFFD700)
                1 -> Color(0xFFC0C0C0)
                2 -> Color(0xFFCD7F32)
                else -> Color(0xFF38BDF8)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, if (index < 3) rankColor else Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Header Row: Rank Badge + Item Image + Name + ROI Badge + Accept Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = opp.resource.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = rankColor) {
                                Text(rankBadge, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${opp.resource.nameDe} (T${opp.resource.tier})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981)) {
                            Text("+${fmtDec.format(opp.roiPercent)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                viewModel.acceptTradeOpportunity(
                                    TradeOpportunity(
                                        resource = opp.resource,
                                        buyCity = opp.cheapestBuyCity,
                                        buyPrice = opp.totalIngredientCost.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                                        sellCity = opp.highestSellCity,
                                        sellPrice = opp.finishedItemSellPrice,
                                        unitNetProfit = opp.netProfit.toInt(),
                                        unitWeightKg = 1.0,
                                        maxUnitsBySilver = 10,
                                        maxUnitsByWeight = 10,
                                        tradeUnits = 1,
                                        totalInvestment = opp.totalIngredientCost,
                                        totalGrossRevenue = opp.finishedItemSellPrice.toLong(),
                                        totalNetRevenue = opp.finishedItemSellPrice.toLong(),
                                        totalNetProfit = opp.netProfit,
                                        totalWeightKg = 1.0,
                                        roiPercent = opp.roiPercent,
                                        priorityScore = 100,
                                        recommendedBuyOrderPrice = opp.recBuyOrderCost.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                                        recommendedSellOrderPrice = opp.recSellOrderPrice
                                    )
                                )
                                Toast.makeText(viewModel.getApplication(), "✅ Handwerks-Auftrag ${opp.resource.nameDe} angenommen!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Auftrag annehmen", tint = Color(0xFF10B981))
                        }
                    }

                    // Recipe & Ingredients info:
                    Text(
                        text = "🛒 Zutaten einkaufen in ${opp.cheapestBuyCity}: ${fmt.format(opp.totalIngredientCost)} S.",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "  • ${opp.ingredientSummary}",
                        color = Color.LightGray,
                        fontSize = 8.5.sp
                    )

                    Text(
                        text = "🏷️ Endprodukt verkaufen in ${opp.highestSellCity}: ${fmt.format(opp.finishedItemSellPrice)} S.",
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )

                    // 100% Success Chance Buy / Sell Orders & Max Margin Box:
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("🛒 KI Kauforder (Zutaten): ${fmt.format(opp.recBuyOrderCost)} S.", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                            Text("📈 KI Verkauforder (Endprodukt): ${fmt.format(opp.recSellOrderPrice)} S.", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                            Text("💡 KI Max-Marge Reingewinn: +${fmt.format(opp.maxOrderProfit)} S. Netto", color = Color(0xFF4ADE80), fontWeight = FontWeight.ExtraBold, fontSize = 8.5.sp)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun BubbleIslandTab(
    context: Context,
    maxHeight: Dp,
) {
    val buildings = remember { IslandRepository.buildings }
    var selectedCityFilter by remember { mutableStateOf("ALLE") }
    val cities = listOf("ALLE", "Caerleon", "Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Brecilien")

    val filteredBuildings = remember(selectedCityFilter, buildings) {
        val base = if (selectedCityFilter == "ALLE") buildings else buildings.filter { it.cityBonusCity.contains(selectedCityFilter, ignoreCase = true) }
        base.sortedByDescending { it.estimatedRoiPercent }
    }

    var activeTimers by remember { mutableStateOf(IslandTimerManager.getTimers(context)) }

    fun getYieldDetails(bldgName: String): Pair<String, String> {
        return when {
            bldgName.contains("Ackerland", ignoreCase = true) -> Pair("50x Kürbisse / Karotten", "~18.000 Silber")
            bldgName.contains("Kräutergarten", ignoreCase = true) -> Pair("40x Teufelswurz", "~25.000 Silber")
            bldgName.contains("Weide", ignoreCase = true) -> Pair("12x Jungtiere & Milch", "~55.000 Silber")
            bldgName.contains("Alchemie", ignoreCase = true) -> Pair("25x Gigantentränke", "~48.000 Silber")
            bldgName.contains("Koch", ignoreCase = true) -> Pair("30x Geflügelpastete", "~42.000 Silber")
            else -> Pair("20x Veredelte Erträge", "~35.000 Silber")
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        // City Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cities) { city: String ->
                val isSelected = selectedCityFilter == city
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedCityFilter = city }
                ) {
                    Text(
                        text = city,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Active Timers Section in Bubble
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E3A4C),
            border = BorderStroke(1.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🌾 Insel-Ernte & Tierzucht Timer", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF10B981)) {
                        Text("${activeTimers.size} Aktiv", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }

                if (activeTimers.isEmpty()) {
                    Text("Keine aktiven Ernte-Timer.", color = Color.LightGray, fontSize = 9.sp)
                } else {
                    activeTimers.forEach { timer ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("• ${timer.nameDe}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                Text(timer.remainingFormatted() + " | ${timer.expectedHarvestDateFormatted()}", color = if (timer.isReady()) Color(0xFF10B981) else Color(0xFFFFB74D), fontSize = 8.sp)
                            }
                            IconButton(
                                onClick = {
                                    IslandTimerManager.removeTimer(context, timer.id)
                                    activeTimers = IslandTimerManager.getTimers(context)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text("➕ Timer starten:", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                
                // Schnellstart-Buttons für Ernte & Tiere in Bubble
                LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(IslandTimerManager.standardCropOptions) { option: Pair<String, Int> ->
                        val (cropName, hours) = option
                        Button(
                            onClick = {
                                IslandTimerManager.addTimer(context, cropName.substringBefore(" ("), TimerCategory.CROP, hours)
                                activeTimers = IslandTimerManager.getTimers(context)
                                Toast.makeText(context, "$cropName gestartet!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(cropName.substringBefore(" ("), fontSize = 8.sp, color = Color.White)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(IslandTimerManager.standardAnimalOptions) { option: Pair<String, Int> ->
                        val (animalName, hours) = option
                        Button(
                            onClick = {
                                IslandTimerManager.addTimer(context, animalName.substringBefore(" ("), TimerCategory.ANIMAL, hours)
                                activeTimers = IslandTimerManager.getTimers(context)
                                Toast.makeText(context, "$animalName gestartet!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(animalName.substringBefore(" ("), fontSize = 8.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        filteredBuildings.forEachIndexed { index, bldg ->
            val (yieldQty, yieldPrice) = getYieldDetails(bldg.nameDe)
            key(bldg.id) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3A4C),
                    border = if (index < 3) BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(bldg.nameDe, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            if (index < 3) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFD97706)) {
                                    Text(
                                        text = "🔥 Top Deal (#${index + 1} Marge)",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text("⚡ ${bldg.abilityDescDe}", color = Color.LightGray, fontSize = 9.sp)
                        Text("📍 Bonus: ${bldg.cityBonusCity}", color = Color(0xFF81D4FA), fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("🌾 Ertrag: $yieldQty | 💰 Wert: $yieldPrice", color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏗️ Gebäude-Upgrades (T1 bis T8 Ressourcen & Kosten):", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        bldg.upgrades.forEach { step ->
                            val fmtSilver = NumberFormat.getNumberInstance(Locale.GERMANY).format(step.silverCost)
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                Text("• ${step.tierName}: $fmtSilver S.", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                if (step.woodReq.isNotBlank()) {
                                    Text("   Holz/Planken: ${step.woodReq} | Stein/Blöcke: ${step.stoneReq}", color = Color(0xFF94A3B8), fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleEventsTab(
    uiState: ResourceUiState,
    maxHeight: Dp,
) {
    val liveEvents = remember(uiState.liveEventsList) { uiState.liveEventsList.take(6) }
    var selectedPlayerCategory by remember { mutableStateOf(PlayerCategory.ALL) }

    val filteredMonsters = remember(selectedPlayerCategory) {
        AlbionMonsterRepository.getFilteredAndSorted(
            playerCategoryFilter = selectedPlayerCategory,
            sortMode = MonsterSortMode.MOST_LUCRATIVE
        ).take(8)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        // 1. Live Events Always at the First Position (Top)
        if (liveEvents.isNotEmpty()) {
            Text("🎆 Live Events & Aktivitäten (Top)", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp)
            liveEvents.forEach { event ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E3A4C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "${event.remainingMinutes} Min",
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "📍 Zone: ${event.zoneName}",
                            color = Color(0xFFFFB74D),
                            fontSize = 8.sp
                        )
                        Text(
                            text = "🎁 Belohnung: ${event.rewardSummary}",
                            color = Color.LightGray,
                            fontSize = 8.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        // 2. Player Count Category Chips for Floating Bubble
        Text("👥 Spieler-Kategorie wählen:", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 8.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
            items(PlayerCategory.entries.toList()) { cat: PlayerCategory ->
                val isSelected = selectedPlayerCategory == cat
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedPlayerCategory = cat }
                ) {
                    Text(
                        text = "${cat.iconEmoji} ${cat.displayNameDe}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 3. AI Decision & Advice Card
        val aiAdvice = remember(selectedPlayerCategory) {
            AiEventAndBossAdvisor.getAdviceForCategory(selectedPlayerCategory)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0E2532),
            border = BorderStroke(1.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(5.dp)) {
                Text(
                    text = aiAdvice.aiRecommendationDe,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
                Text(
                    text = "💎 ${aiAdvice.estimatedProfitScore} | ${aiAdvice.riskLevelDe}",
                    color = Color(0xFF10B981),
                    fontSize = 8.sp
                )
            }
        }

        // Dungeons, Bosse & Aktivitäten Section
        Spacer(modifier = Modifier.height(2.dp))
        Text("👹 Dungeons, Bosse & Truhen Drops", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 9.sp)

        filteredMonsters.forEach { monster ->
            key(monster.id) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (monster.playerCategory) {
                                    PlayerCategory.SOLO -> Color(0xFF10B981)
                                    PlayerCategory.DUO -> Color(0xFF38BDF8)
                                    PlayerCategory.GROUP -> Color(0xFF8B5CF6)
                                    PlayerCategory.RAID -> Color(0xFFEC4899)
                                    else -> Color(0xFF64748B)
                                }
                            ) {
                                Text(
                                    text = "${monster.playerCategory.iconEmoji} ${monster.recommendedPlayerCount}",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = monster.estimatedSilverPerHour,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(monster.nameDe, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        Text("📍 ${monster.zoneLocationDe}", color = Color(0xFF94A3B8), fontSize = 8.sp)
                        Text(monster.chestDropSummaryDe, color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleGoldTab(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState,
    onFocusModeChanged: (Boolean) -> Unit,
    maxHeight: Dp,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    var goldAmountInput by remember { mutableStateOf("") }
    var goldPriceInput by remember { mutableStateOf("") }

    val totalOwnedGold = uiState.totalGoldOwned
    val netProfit = uiState.totalGoldNetProfitSilver
    val roi = if (uiState.totalGoldCostSilver > 0) (netProfit.toDouble() / uiState.totalGoldCostSilver) * 100.0 else 0.0

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E3A4C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = "🪙 Live Goldpreis (${uiState.server.displayName})",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Einkauf", color = Color.LightGray, fontSize = 10.sp)
                        Text("${fmt.format(uiState.goldBuyPrice)} S.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Verkauf (nach Steuer)", color = Color.LightGray, fontSize = 10.sp)
                        Text("${fmt.format(uiState.goldSellPrice)} S.", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (totalOwnedGold > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Portfolio: ${fmt.format(totalOwnedGold)} Gold | Gewinn/Verlust: ${if (netProfit >= 0) "+" else ""}${fmt.format(netProfit)} S. (${String.format(Locale.GERMANY, "%.1f", roi)}%)",
                        color = if (netProfit >= 0) Color(0xFF81C784) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // 🤖 KI Gold-Handelsbot Empfehlung in Floating Bubble (Kompakt & Platzsparend)
        val botAnalysis = remember(uiState.goldPrices, uiState.currentGoldPrice) {
            GoldBotCalculator.analyzeGoldMarket(uiState.goldPrices, uiState.currentGoldPrice)
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E3A4C),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🤖 KI Gold-Bot (Max-Marge)",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF10B981)) {
                        Text(
                            text = "Aktiv",
                            color = Color.Black,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🛍️ Kauf: ${fmt.format(botAnalysis.recommendedBuyOrderPrice)} S. (-${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedPriceDropPercent)}%)",
                        color = Color(0xFF81C784),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🏷️ Verkauf: ${fmt.format(botAnalysis.recommendedSellOrderPrice)} S. (+${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedPriceRisePercent)}%)",
                        color = Color(0xFFFFB74D),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = botAnalysis.aiOrderRecommendationTextDe,
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Gold Purchase & Sale Entry Form inside Floating Bubble
        var goldSaleAmountInput by remember { mutableStateOf("") }
        var goldSalePriceInput by remember { mutableStateOf("") }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E3A4C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text("✍️ Goldkauf / Verkauf eintragen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                
                // Kauf Form
                Text("Kauf:", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = goldAmountInput,
                        onValueChange = { goldAmountInput = it },
                        label = { Text("Menge", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )
                    OutlinedTextField(
                        value = goldPriceInput,
                        onValueChange = { goldPriceInput = it },
                        label = { Text("Preis/Gold", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = {
                        val amount = goldAmountInput.toIntOrNull() ?: 0
                        val price = goldPriceInput.toIntOrNull() ?: 0
                        if (amount > 0 && price > 0) {
                            viewModel.addGoldPurchase(amount, price, "")
                            goldAmountInput = ""
                            goldPriceInput = ""
                            onFocusModeChanged(false)
                            Toast.makeText(viewModel.getApplication(), "Goldkauf gespeichert!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(viewModel.getApplication(), "Gültige Menge & Preis eingeben", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+ Kauf eintragen", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Verkauf Form
                Text("Verkauf:", color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = goldSaleAmountInput,
                        onValueChange = { goldSaleAmountInput = it },
                        label = { Text("Menge", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )
                    OutlinedTextField(
                        value = goldSalePriceInput,
                        onValueChange = { goldSalePriceInput = it },
                        label = { Text("Verkaufspreis/Gold", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = {
                        val amount = goldSaleAmountInput.toIntOrNull() ?: 0
                        val price = goldSalePriceInput.toIntOrNull() ?: 0
                        if (amount > 0 && price > 0) {
                            viewModel.addGoldSale(amount, price, "")
                            goldSaleAmountInput = ""
                            goldSalePriceInput = ""
                            onFocusModeChanged(false)
                            Toast.makeText(viewModel.getApplication(), "Goldverkauf gespeichert!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(viewModel.getApplication(), "Gültige Menge & Preis eingeben", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+ Verkauf eintragen", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
            }
        }
        
        // Einnahmen-Historie für Gold (Top 3 Käufe & Verkäufe)
        val prefs = remember { AppPreferences(viewModel.getApplication()) }
        val goldPurchases = prefs.getGoldPurchases()
        val goldSales = prefs.getGoldSales()
        
        if (goldPurchases.isNotEmpty() || goldSales.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text("📜 Letzte Gold-Transaktionen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            goldPurchases.takeLast(2).reversed().forEach { purchase ->
                val netProfit = purchase.netProfitSilver(uiState.currentGoldPrice)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0A1922),
                    border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🛒 ${fmt.format(purchase.amountGold)} Gold (Kauf)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(
                                text = "${if (netProfit >= 0) "+" else ""}${fmt.format(netProfit)} S.",
                                color = if (netProfit >= 0) Color(0xFF66BB6A) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        Text("Kaufpreis: ${fmt.format(purchase.buyPricePerGold)} S./Gold", color = Color.LightGray, fontSize = 8.sp)
                    }
                }
            }
            goldSales.takeLast(2).reversed().forEach { sale ->
                sale.realizedProfit(uiState.currentGoldPrice)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0A1922),
                    border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💰 ${fmt.format(sale.amountGold)} Gold (Verkauf)", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(
                                text = "+${fmt.format(sale.totalEarnedSilver)} S.",
                                color = Color(0xFF66BB6A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        Text("Verkaufspreis: ${fmt.format(sale.sellPricePerGold)} S./Gold", color = Color.LightGray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleCompletedOrdersTab(
    context: Context,
    onRefresh: () -> Unit,
    maxHeight: Dp
) {
    val prefs = remember { AppPreferences(context) }
    var completedOrders by remember { mutableStateOf(prefs.getTradeOrders().filter { it.status == OrderStatus.COMPLETED }) }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val totalProfit = completedOrders.sumOf { it.realizedNetProfit }
    val lang = prefs.appLanguage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF10B981).copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Text(
                    text = if (lang == "DE") "📜 Auftrags-Historie (${completedOrders.size})" else "📜 Order History (${completedOrders.size})",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = (if (lang == "DE") "Netto-Profit: " else "Net Profit: ") + "${fmt.format(totalProfit)} Silber",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (completedOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (lang == "DE") "Keine abgeschlossenen Aufträge vorhanden." else "No completed orders found.",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            completedOrders.forEach { order ->
                val profit = order.realizedNetProfit
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A1922),
                    border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${order.resourceNameDe} (T${order.tier})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (profit >= 0) "+" else ""}${fmt.format(profit)} S.",
                                color = if (profit >= 0) Color(0xFF66BB6A) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            IconButton(
                                onClick = {
                                    val all = prefs.getTradeOrders()
                                    val updated = all.filter { it.id != order.id }
                                    prefs.saveTradeOrders(updated)
                                    completedOrders = updated.filter { it.status == OrderStatus.COMPLETED }
                                    onRefresh()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = "Route: ${order.buyCity} ➔ ${order.sellCity} | ${order.completedDate ?: ""}",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Menge: ${order.effectiveUnits} Stk. | Kauf: ${fmt.format(order.effectiveBuyPrice)} S. | Verk.: ${fmt.format(order.effectiveSellPrice)} S.",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleBuildsTab(
    uiState: ResourceUiState,
    maxHeight: Dp
) {
    var selectedCat by remember { mutableStateOf(BuildCategory.DAMAGE_DPS) }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    val filteredBuilds = remember(selectedCat, uiState.equipmentBuilds) {
        uiState.equipmentBuilds
            .asSequence()
            .filter { it.category == selectedCat }
            .sortedByDescending { it.estimatedMarginPercent }
            .toList()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            items(BuildCategory.entries.toList()) { cat: BuildCategory ->
                val isSelected = selectedCat == cat
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedCat = cat }
                ) {
                    Text(
                        text = "${cat.icon} ${cat.displayName}",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (filteredBuilds.isEmpty()) {
            Text("Keine Builds in dieser Kategorie gefunden.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            filteredBuilds.forEachIndexed { index, build ->
                key(build.id) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E3A4C),
                        border = if (index < 3) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (index < 3) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF059669)
                                        ) {
                                            Text(
                                                text = "🔥 Top Deal (#${index + 1} Marge: +${String.format(Locale.GERMANY, "%.1f", build.estimatedMarginPercent)}%)",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(build.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981)) {
                                    Text("~${fmt.format(build.estimatedCostSilver)} S.", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }

                            if (build.strongestWeaponHighlight.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(build.strongestWeaponHighlight, color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text("⚔️ Waffe: ${build.weapon}", color = Color.White, fontSize = 9.sp)
                            Text("🪖 Kopf: ${build.head} | 🥋 Brust: ${build.armor}", color = Color.LightGray, fontSize = 8.sp)
                            Text("🥾 Schuhe: ${build.shoes} | 🐴 Reittier: ${build.mount}", color = Color.LightGray, fontSize = 8.sp)
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("⭐ Schaden ${build.damageRating}/100 | Def ${build.defenseRating}/100 | Tempo ${build.mobilityRating}/100", color = Color(0xFFFDE047), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(build.aiEvaluationDe, color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleMapTab(
    viewModel: AlbionResourceViewModel,
    maxHeight: Dp
) {
    var selectedRegionName by remember { mutableStateOf("Bridgewatch") }
    val regions = AlbionWorldData.worldRegions
    val activeRegion = regions.find { it.name == selectedRegionName } ?: regions.first()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val cityName = activeRegion.name.split(" ")[0]
    val refinedRes = remember(uiState.marketPrices) {
        AlbionResourceRepository.resources.filter { it.category == ResourceCategory.REFINED }
    }
    val matchingRes = remember(refinedRes, activeRegion.name) {
        refinedRes.asSequence().filter { res ->
            when {
                activeRegion.name.contains("Bridgewatch") -> res.id.contains("LEATHER")
                activeRegion.name.contains("Fort Sterling") -> res.id.contains("METALBAR")
                activeRegion.name.contains("Lymhurst") -> res.id.contains("PLANKS")
                activeRegion.name.contains("Martlock") -> res.id.contains("STONEBLOCK")
                activeRegion.name.contains("Thetford") -> res.id.contains("CLOTH")
                else -> true
            }
        }.take(8).toList()
    }

    val avgCost = remember(matchingRes, cityName, uiState.marketPrices) {
        if (matchingRes.isNotEmpty()) {
            matchingRes.asSequence().map { res ->
                val recipe = CraftingRepository.getRecipeFor(res)
                recipe.ingredients.sumOf { ing ->
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, cityName, uiState.marketPrices).toLong()
                }
            }.average().toLong()
        } else 0L
    }

    val avgNetProfit = remember(matchingRes, cityName, uiState.marketPrices) {
        if (matchingRes.isNotEmpty()) {
            matchingRes.asSequence().map { res ->
                val recipe = CraftingRepository.getRecipeFor(res)
                val cost = recipe.ingredients.sumOf { ing ->
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, cityName, uiState.marketPrices).toLong()
                }
                val sell = CraftingRepository.getPriceInCity(res.fullId, cityName, uiState.marketPrices)
                val netEarn = (sell * 0.96).toLong() // 4% market tax
                netEarn - cost
            }.average().toLong()
        } else 0L
    }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(regions) { reg: WorldMapRegion ->
                val isSelected = selectedRegionName == reg.name
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable { selectedRegionName = reg.name }
                ) {
                    Text(
                        text = reg.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E3A4C),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(activeRegion.name, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (activeRegion.safety) {
                            ZoneSafety.SAFE_BLUE -> Color(0xFF1E88E5)
                            ZoneSafety.SAFE_YELLOW -> Color(0xFFFDD835)
                            ZoneSafety.DANGEROUS_RED -> Color(0xFFE53935)
                            ZoneSafety.DANGEROUS_BLACK -> Color(0xFF424242)
                        }
                    ) {
                        Text(activeRegion.safety.displayName, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(activeRegion.description, color = Color.LightGray, fontSize = 9.sp)

                Spacer(modifier = Modifier.height(2.dp))
                val refiningBonusText = when {
                    activeRegion.name.contains("Bridgewatch") -> "🔥 +15% Veredelungs-Bonus auf Leder & Haut"
                    activeRegion.name.contains("Fort Sterling") -> "🔥 +15% Veredelungs-Bonus auf Erz & Metall"
                    activeRegion.name.contains("Lymhurst") -> "🔥 +15% Veredelungs-Bonus auf Holz & Planken"
                    activeRegion.name.contains("Martlock") -> "🔥 +15% Veredelungs-Bonus auf Stein & Blöcke"
                    activeRegion.name.contains("Thetford") -> "🔥 +15% Veredelungs-Bonus auf Faser & Stoff"
                    activeRegion.name.contains("Caerleon") -> "💀 Schwarzmarkt-Konto (Black Market)"
                    else -> "⭐ Standard Veredelung"
                }
                Text(refiningBonusText, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 9.sp)

                Spacer(modifier = Modifier.height(2.dp))
                Text("⛏️ Ressourcen: ${activeRegion.resourcesFound.joinToString(", ")}", color = Color.White, fontSize = 9.sp)
                Text("🛣️ Verbindet: ${activeRegion.connectsTo.joinToString(" ➔ ")}", color = Color.LightGray, fontSize = 8.sp)

                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E2532),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("💰 Veredelungskosten & Gewinn (Netto):", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Ø Rohstoff-Kosten:", color = Color.LightGray, fontSize = 8.sp)
                            Text("${fmt.format(avgCost)} S.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Ø Netto-Gewinn:", color = Color.LightGray, fontSize = 8.sp)
                            Text("${if (avgNetProfit >= 0) "+" else ""}${fmt.format(avgNetProfit)} S.", color = if (avgNetProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val shortName = activeRegion.name.split(" ")[0]
                        viewModel.onCityFilterSelected(shortName)
                        Toast.makeText(context, "Standpunkt $shortName gesetzt!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                ) {
                    Text("Als Standpunkt im Rechner setzen", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BubbleSettingsTab(
    context: Context,
    maxHeight: Dp,
    onRefresh: () -> Unit = {},
) {
    val prefs = remember { AppPreferences(context) }
    val lang = prefs.appLanguage
    var hasPremium by remember { mutableStateOf(prefs.hasPremium) }
    var bubbleOpacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var bubbleScale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var bubbleWidthPortrait by remember { mutableIntStateOf(prefs.bubbleWidthPortrait) }
    var bubbleHeightPortrait by remember { mutableIntStateOf(prefs.bubbleHeightPortrait) }
    var bubbleWidthLandscape by remember { mutableIntStateOf(prefs.bubbleWidthLandscape) }
    var bubbleHeightLandscape by remember { mutableIntStateOf(prefs.bubbleHeightLandscape) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("⚙️ Floating Bubble Einstellungen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))

        // Opacity Selection
        Text(LanguageManager.getString("bubble_opacity", lang), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF81D4FA))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0.65f to "65%", 0.80f to "80%", 0.90f to "90%", 1.0f to "100%").forEach { (valOp, labelOp) ->
                val isSelected = abs(bubbleOpacity - valOp) < 0.04f
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable {
                        bubbleOpacity = valOp
                        prefs.bubbleOpacity = valOp
                    }
                ) {
                    Text(
                        text = labelOp,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scale Selection & Dimension Adjuster
        Text("📐 Bubble Größe & Skalierung", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFFB74D))
        
        // Quick Presets
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0.7f to "Kompakt (70%)", 0.9f to "Normal (90%)", 1.0f to "Standard (100%)", 1.2f to "Groß (120%)").forEach { (valSc, labelSc) ->
                val isSelected = abs(bubbleScale - valSc) < 0.04f
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFFFFB74D) else Color(0xFF1E3A4C),
                    modifier = Modifier.clickable {
                        bubbleScale = valSc
                        prefs.bubbleScale = valSc
                    }
                ) {
                    Text(
                        text = labelSc,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Fine-tuning Height & Width via direct clean Sliders with immediate State updates
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            val currentW = bubbleWidthPortrait
            val currentH = bubbleHeightPortrait

            Text("Aktuelle Fenster-Breite: ${currentW}px", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Slider(
                value = currentW.toFloat(),
                onValueChange = { newValue ->
                    val w = newValue.toInt()
                    bubbleWidthPortrait = w
                    bubbleWidthLandscape = (w * 1.25f).toInt()
                    prefs.bubbleWidthPortrait = w
                    prefs.bubbleWidthLandscape = (w * 1.25f).toInt()
                },
                valueRange = 220f..600f
            )

            Text("Aktuelle Fenster-Höhe: ${currentH}px", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Slider(
                value = currentH.toFloat(),
                onValueChange = { newValue ->
                    val h = newValue.toInt()
                    bubbleHeightPortrait = h
                    bubbleHeightLandscape = h
                    prefs.bubbleHeightPortrait = h
                    prefs.bubbleHeightLandscape = h
                },
                valueRange = 200f..900f
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(
                checked = hasPremium,
                onCheckedChange = {
                    hasPremium = it
                    prefs.hasPremium = it
                    onRefresh()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aktives Albion Premium (4% Steuer)", fontSize = 10.sp, color = Color.White)
        }

        var hideAppOnBubble by remember { mutableStateOf(prefs.hideAppOnBubbleActivate) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(checked = hideAppOnBubble, onCheckedChange = { hideAppOnBubble = it; prefs.hideAppOnBubbleActivate = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("App bei Bubble-Aktivierung verbergen", fontSize = 10.sp, color = Color.White)
        }

        val viewModel = remember { SharedViewModelProvider.get(context.applicationContext as Application) }
        val uiState by viewModel.uiState.collectAsState()

        // 🔄 RESSOURCEN & MARKTDATEN ECHTZEIT-DOWNLOAD
        HorizontalDivider(color = Color(0xFF334155))
        Text("🔄 Marktdaten & Ressourcen Echtzeit-Download", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF10B981))

        var realtimeLiveSync by remember { mutableStateOf(prefs.realtimeLiveSyncEnabled) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = realtimeLiveSync,
                onCheckedChange = {
                    realtimeLiveSync = it
                    prefs.realtimeLiveSyncEnabled = it
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("⚡ Echtzeit-Live-Sync aktiv", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Aktualisiert Marktdaten im Hintergrund in Echtzeit", fontSize = 8.sp, color = Color.LightGray)
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📊 Geladene Marktpreise: ${uiState.marketPrices.size} Items | Scans: ${uiState.totalScannedItemsCount}",
                    fontSize = 9.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "⏱️ Letztes Update: ${uiState.lastFetchTime.orEmpty().ifBlank { "Echtzeit" }}",
                    fontSize = 9.sp,
                    color = Color(0xFF81C784)
                )
            }
        }

        Button(
            onClick = {
                Toast.makeText(context, "🔄 Download aller Ressourcen & Marktpreise gestartet...", Toast.LENGTH_SHORT).show()
                viewModel.forceReloadAllResources()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isLoadingPrices,
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            if (uiState.isLoadingPrices) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LADE RESSOURCEN...", fontWeight = FontWeight.Bold, fontSize = 10.sp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔄 ALLE RESSOURCEN HERUNTERLADEN", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
            }
        }

        Button(
            onClick = {
                Toast.makeText(context, "🗑️ Historie zurückgesetzt & Marktdaten neu geladen!", Toast.LENGTH_SHORT).show()
                viewModel.resetPriceHistoryAndRedownload()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isLoadingPrices,
            modifier = Modifier.fillMaxWidth().height(34.dp)
        ) {
            Text("🗑️ HISTORIE ZURÜCKSETZEN & NEU LADEN", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
        }

        HorizontalDivider(color = Color(0xFF334155))

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                prefs.isUserLoggedIn = false
                Toast.makeText(context, "🔒 Abgemeldet! Öffne Login-Seite...", Toast.LENGTH_SHORT).show()
                try {
                    val loginIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(loginIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Handler(Looper.getMainLooper()).post {
                    try {
                        FloatingBubbleService.stopService(context)
                    } catch (_: Exception) {}
                    try {
                        PersistentServerSyncService.stopService(context)
                    } catch (_: Exception) {}
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🔒 ABMELDEN / SPERREN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                Toast.makeText(context, "🛑 Floating Bubble beendet", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).post {
                    try {
                        FloatingBubbleService.stopService(context)
                    } catch (_: Exception) {}
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("🛑 OVERLAY KOMPLETT BEENDEN", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
fun BubbleAdminTab(
    context: Context,
    maxHeight: Dp,
    @Suppress("UNUSED_PARAMETER") onFocusModeChanged: (Boolean) -> Unit = {},
) {
    val prefs = remember { AppPreferences(context) }
    if (!prefs.isAdmin) return

    val application = context.applicationContext as Application
    val viewModel = remember { AlbionResourceViewModel(application) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var licenses by remember { mutableStateOf<List<AdminLicense>>(emptyList()) }
    var devices by remember { mutableStateOf<List<AdminDevice>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    fun loadAdminData() {
        coroutineScope.launch {
            try {
                coroutineScope {
                    val usersDef = async { AdminControlManager.fetchUsersWithStatus(context) }
                    val licensesDef = async { AdminControlManager.fetchLicensesWithStatus(context) }
                    val devicesDef = async { AdminControlManager.fetchDevicesWithStatus(context) }

                    val usersRes = usersDef.await()
                    val licensesRes = licensesDef.await()
                    val devicesRes = devicesDef.await()

                    (usersRes as? AdminApiResult.Success)?.let { users = it.data }
                    (licensesRes as? AdminApiResult.Success)?.let { licenses = it.data }
                    (devicesRes as? AdminApiResult.Success)?.let { devices = it.data }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(prefs.isAdmin) {
        if (prefs.isAdmin) {
            loadAdminData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("👑 Admin-Zentrale (Overlay)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
        AdminSimpleView(
            viewModel = viewModel,
            users = users,
            licenses = licenses,
            devices = devices,
            showMergeBot = false,
            onFocusModeChanged = onFocusModeChanged,
        ) { loadAdminData() }
    }
}