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
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlin.time.Duration.Companion.seconds

class FloatingBubbleService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val CHANNEL_ID = "albion_bubble_service_channel"
        const val NOTIFICATION_ID = 2002
        private var isRunning = false

        fun startService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startForegroundService(intent)
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

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var activeOrder by mutableStateOf<TradeOrder?>(null)
    private var topOpportunities by mutableStateOf<List<TradeOpportunity>>(emptyList())
    private var isLoadingOpps by mutableStateOf(value = false)

    // Memory caching to avoid parsing SharedPreferences JSON on every frame
    private var cachedPriceMap: Map<String, List<MarketPrice>>? = null

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        startForegroundNotification()
        setupOverlayView()
        registerScreenStateReceiver()
        updateBubbleVisibility()
        startActiveOrderObserver()
    }

    private fun startForegroundNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Albion Overlay Bubble Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Albion Overlay aktiv")
            .setContentText("Zeigt Marktchancen & Handelsaufträge an")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setFocusableMode(enableFocus: Boolean) {
        if ((!::layoutParams.isInitialized) || (composeView == null)) return
        try {
            if (enableFocus) {
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
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
        val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

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
                            this@FloatingBubbleService.layoutParams.x += dx.toInt()
                            this@FloatingBubbleService.layoutParams.y += dy.toInt()
                            windowManager.updateViewLayout(this@apply, this@FloatingBubbleService.layoutParams)
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

        windowManager.addView(composeView, layoutParams)
    }

    private var screenReceiver: BroadcastReceiver? = null

    private fun updateBubbleVisibility() {
        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            val isKeyguardLocked = (keyguardManager?.isKeyguardLocked == true) || (keyguardManager?.isDeviceLocked == true)
            val prefs = AppPreferences(this)
            val isAuthValid = prefs.isUserLoggedIn && LicenseManager.isLicenseValid(this)

            val shouldShow = !isKeyguardLocked && isAuthValid

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
                updateBubbleVisibility()
                loadActiveOrder()
                val intervalMins = AppPreferences(this@FloatingBubbleService).bubbleIntervalMinutes.coerceAtLeast(1)
                val ticksNeeded = intervalMins * 20 // 20 ticks of 3s = 60s (1 min)
                if ((loopCount % ticksNeeded) == 0) {
                    loadTopOpportunities(forceRefresh = true)
                }
                loopCount++
                delay(1.seconds)
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
                activeOrder = ord
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
                    topOpportunities = opps
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

    private fun calculateTopMarginOpportunities(context: Context, forceRefresh: Boolean = false): List<TradeOpportunity> {
        val prefs = AppPreferences(context)
        if ((cachedPriceMap == null) || forceRefresh) {
            val snapshots = prefs.getPriceSnapshots(prefs.server)
            cachedPriceMap = if (snapshots.isNotEmpty()) {
                snapshots.asSequence().map { s ->
                    MarketPrice(
                        itemId = s.itemId,
                        city = s.city,
                        quality = 1,
                        sellPriceMin = s.sellPriceMin,
                        sellPriceMinDate = "",
                        buyPriceMax = s.buyPriceMax,
                        buyPriceMaxDate = "",
                    )
                }.groupBy { it.itemId }
            } else {
                AlbionMarketApi.getFallbackMarketPrices()
            }
        }

        val priceMap = cachedPriceMap ?: emptyMap()
        val resources = AlbionResourceRepository.resources
        val bubbleCity = prefs.bubbleStandpunktCity
        val standpunkt = if (bubbleCity != "ALLE") bubbleCity else null
        val bubbleCategory = prefs.bubbleCategory
        val bubbleTier = prefs.bubbleTier

        val filteredResources = resources.filter { res ->
            val matchesCategory = if (bubbleCategory != "ALL") {
                try { res.category == ResourceCategory.valueOf(bubbleCategory) } catch (_: Exception) { true }
            } else true

            val matchesTier = if (bubbleTier > 0) {
                res.tier == bubbleTier
            } else true

            matchesCategory && matchesTier
        }

        val opportunities = TradeCalculator.calculateOpportunities(
            resources = filteredResources,
            pricesByItem = priceMap,
            silverBudget = prefs.silverBudget,
            carryCapacityKg = prefs.carryCapacityKg,
            marketTaxPercent = if (prefs.hasPremium) 4.0 else 8.0,
            targetMarginPercent = prefs.bubbleMinMarginPercent,
            avoidDangerousZones = prefs.bubbleAvoidDangerousZones,
            currentGoldPrice = 4250,
            standpunktCity = standpunkt,
            maxCityDistance = prefs.bubbleMaxZones,
            hideBrecilien = prefs.bubbleHideBrecilien,
            maxStockCount = prefs.bubbleMaxStock,
            hideBlackMarket = prefs.bubbleHideBlackMarket,
        ).filter { it.roiPercent >= prefs.bubbleMinMarginPercent }

        return opportunities.asSequence().sortedWith(
            compareByDescending<TradeOpportunity> { it.roiPercent }
                .thenByDescending { it.totalNetProfit },
        ).take(3).toList()
    }

    private fun acceptOpportunity(opp: TradeOpportunity) {
        try {
            val prefs = AppPreferences(this)
            val currentOrders = prefs.getTradeOrders()
            val activeOrders = currentOrders.filter { it.status == OrderStatus.ACTIVE }
            if (activeOrders.size >= 3) {
                Toast.makeText(this, "Maximal 3 aktive Aufträge gleichzeitig erlaubt!", Toast.LENGTH_SHORT).show()
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
            )

            val updatedOrders = currentOrders + newOrder
            prefs.saveTradeOrders(updatedOrders)

            Toast.makeText(
                this,
                if (lang == "DE") "Auftrag angenommen: ${opp.resource.nameDe} (T${opp.resource.tier})" else "Order accepted: ${opp.resource.nameEn} (T${opp.resource.tier})",
                Toast.LENGTH_SHORT,
            ).show()

            activeOrder = newOrder
            loadData(forceRefreshPrices = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

enum class BubbleTab(val titleDe: String, val titleEn: String, val emoji: String) {
    ACTIVE_ORDER("Aufträge", "Orders", "📦"),
    COMPLETED_ORDERS("Historie", "History", "📜"),
    TOP_MARGIN("Handel & Marge", "Trade Margin", "🔥"),
    CATALOG("Katalog", "Catalog", "📖"),
    CRAFTING("Handwerks-Guide", "Crafting Guide", "⚒️"),
    ISLAND("Insel-Guide", "Island Guide", "🏝️"),
    EVENTS("Event & Boss Loot", "Event & Boss Loot", "⚔️"),
    GOLD_MARKET("Goldmarkt", "Gold Market", "🪙")
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
    val cardWidth = if (isLandscape) (if (isMaximized) 460.dp else 380.dp) else (if (isMaximized) 390.dp else 330.dp)
    val maxBubbleHeight = if (isLandscape) (if (isMaximized) 260.dp else 190.dp) else (if (isMaximized) 520.dp else 320.dp)
    val maxBubbleHeightTab = if (isLandscape) (if (isMaximized) 240.dp else 170.dp) else (if (isMaximized) 490.dp else 300.dp)

    var isExpanded by remember { mutableStateOf(value = false) }
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
        if (!isExpanded) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0E2532),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clickable { isExpanded = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.aot_logo),
                        contentDescription = "AlbionDataPro Overlay",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2532)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.width(cardWidth),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Title Bar (Drag gesture listener attached ONLY to Title Bar to avoid scroll lag!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    currentOnDrag(dragAmount.x, dragAmount.y)
                                }
                            },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.aot_logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBookingMode) {
                                    LanguageManager.getString("accept_order", lang)
                                } else {
                                    if (lang == "DE") selectedTab.titleDe else selectedTab.titleEn
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isMaximized = !isMaximized },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Text(
                                    text = if (isMaximized) "🗗" else "🗖",
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
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
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Aktualisieren",
                                    tint = Color.LightGray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer(rotationZ = animatedRotation),
                                )
                            }
                            IconButton(
                                onClick = {
                                    isExpanded = false
                                    isBookingMode = false
                                    onFocusModeChanged(false)
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!isBookingMode) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(BubbleTab.entries) { tab ->
                                val isSelected = selectedTab == tab
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF1E3A4C) else Color(0xFF0A1922),
                                    modifier = Modifier.clickable { selectedTab = tab },
                                ) {
                                    Text(
                                        text = "${tab.emoji} ${if (lang == "DE") tab.titleDe else tab.titleEn}",
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E3A4C),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        val resOpt = AlbionResourceRepository.resources.find { it.id == activeOrder.resourceId }
                                        resOpt?.let { res ->
                                            AsyncImage(
                                                model = res.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                            )
                                        }
                                        Text(
                                            text = "$catEmoji ${activeOrder.resourceNameDe} (T${activeOrder.tier})",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "${LanguageManager.getString("quantity", lang)}: $unitsStr",
                                            color = Color(0xFF81D4FA),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column {
                                            Text(
                                                text = "${LanguageManager.getString("buy_in", lang)}: $buyCityTrans ($buyPriceStr S.)",
                                                color = Color(0xFF81C784),
                                                fontSize = 11.sp,
                                            )
                                            Text(
                                                text = "${LanguageManager.getString("sell_in", lang)}: $sellCityTrans ($sellPriceStr S.)",
                                                color = Color(0xFF81C784),
                                                fontSize = 11.sp,
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+$profitStr S.",
                                                color = Color(0xFFFFB74D),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                            )
                                            Text(
                                                text = activeOrder.acceptedDate,
                                                color = Color.LightGray,
                                                fontSize = 9.sp,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Button(
                                            onClick = {
                                                isBookingMode = true
                                                onFocusModeChanged(true)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (lang == "DE") "Buchen" else "Book", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
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
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (lang == "DE") "Stornieren" else "Cancel", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 11.sp)
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
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${activeOrder.resourceNameDe} (T${activeOrder.tier})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = unitsInput,
                                        onValueChange = {
                                            unitsInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, it, buyPriceInput, sellPriceInput)
                                        },
                                        label = { Text(if (lang == "DE") "Menge" else "Units", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = buyPriceInput,
                                        onValueChange = {
                                            buyPriceInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, unitsInput, it, sellPriceInput)
                                        },
                                        label = { Text(if (lang == "DE") "Kaufpreis" else "Buy Price", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = sellPriceInput,
                                        onValueChange = {
                                            sellPriceInput = it
                                            prefsObj.saveDraftOrderInput(activeOrder.id, unitsInput, buyPriceInput, it)
                                        },
                                        label = { Text(if (lang == "DE") "Verkaufspreis" else "Sell Price", fontSize = 10.sp, color = Color.LightGray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

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
                                            Text(if (lang == "DE") "Abbrechen" else "Cancel", fontSize = 10.sp, color = Color.White)
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
                                                    Toast.makeText(context, if (lang == "DE") "Erfolgreich gebucht!" else "Successfully booked!", Toast.LENGTH_SHORT).show()

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
                                            Text(if (lang == "DE") "Speichern" else "Save", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == BubbleTab.TOP_MARGIN) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxBubbleHeight)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val prefsForCity = remember { AppPreferences(context) }
                            var currentBubbleCity by remember { mutableStateOf(prefsForCity.bubbleStandpunktCity) }
                            val bubbleCities = listOf("ALLE", "Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Caerleon", "Brecilien")

                            Text(
                                text = "🏙️ Standpunkt: ${if (currentBubbleCity == "ALLE") "Alle" else LanguageManager.getCityTranslation(currentBubbleCity, lang)}",
                                color = Color(0xFF81D4FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(bubbleCities) { c ->
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
                                        Text(
                                            text = if (c == "ALLE") "Alle" else LanguageManager.getCityTranslation(c, lang),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📦 " + (if (lang == "DE") "Kategorie-Filter:" else "Category Filter:"),
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            var currentBubbleCategory by remember { mutableStateOf(prefsForCity.bubbleCategory) }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(ResourceCategory.entries) { cat ->
                                    val isSelected = currentBubbleCategory == cat.name
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E3A4C),
                                        modifier = Modifier.clickable {
                                            currentBubbleCategory = cat.name
                                            prefsForCity.bubbleCategory = cat.name
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = cat.displayName,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⭐ " + (if (lang == "DE") "Tier-Filter:" else "Tier Filter:"),
                                color = Color(0xFFFDD835),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            var currentBubbleTier by remember { mutableIntStateOf(prefsForCity.bubbleTier) }
                            val bubbleTiers = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(bubbleTiers) { tier ->
                                    val isSelected = currentBubbleTier == tier
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                        modifier = Modifier.clickable {
                                            currentBubbleTier = tier
                                            prefsForCity.bubbleTier = tier
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = if (tier == 0) "Alle Tiers" else "T$tier",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            // Max Zonen Distance Filter Chips
                            Text(
                                text = "🗺️ " + (if (lang == "DE") "Max. Zonen-Distanz:" else "Max Zone Distance:"),
                                color = Color(0xFF81D4FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            var currentBubbleMaxZones by remember { mutableIntStateOf(prefsForCity.bubbleMaxZones) }
                            val zoneOptions = listOf(1, 2, 3, 5, 99)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(zoneOptions) { z ->
                                    val isSelected = currentBubbleMaxZones == z
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                        modifier = Modifier.clickable {
                                            currentBubbleMaxZones = z
                                            prefsForCity.bubbleMaxZones = z
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = if (z == 99) "Alle Zonen" else "$z ${if (z == 1) "Zone" else "Zonen"}",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Switches for Red / Dangerous Zones & Brecilien
                            var currentBubbleAvoidDangerous by remember { mutableStateOf(prefsForCity.bubbleAvoidDangerousZones) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text("🔴 Rote / PvP Zonen ausblenden", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = currentBubbleAvoidDangerous,
                                    onCheckedChange = {
                                        currentBubbleAvoidDangerous = it
                                        prefsForCity.bubbleAvoidDangerousZones = it
                                        onRefresh()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444))
                                )
                            }

                            var currentBubbleHideBrecilien by remember { mutableStateOf(prefsForCity.bubbleHideBrecilien) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text("✨ Brecilien ausblenden", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = currentBubbleHideBrecilien,
                                    onCheckedChange = {
                                        currentBubbleHideBrecilien = it
                                        prefsForCity.bubbleHideBrecilien = it
                                        onRefresh()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                                )
                            }

                            var currentBubbleHideBlackMarket by remember { mutableStateOf(prefsForCity.bubbleHideBlackMarket) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text("🏴‍☠️ Schmuggler (Schwarzmarkt) ausblenden", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = currentBubbleHideBlackMarket,
                                    onCheckedChange = {
                                        currentBubbleHideBlackMarket = it
                                        prefsForCity.bubbleHideBlackMarket = it
                                        onRefresh()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB74D))
                                )
                            }

                            // KI Preisvergleich Intervall (Minuten) Selector
                            Text(
                                text = "⏱️ " + (if (lang == "DE") "KI-Preisvergleich Intervall:" else "AI Price Check Interval:"),
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            var currentBubbleInterval by remember { mutableIntStateOf(prefsForCity.bubbleIntervalMinutes) }
                            val intervalOptions = listOf(1, 3, 5, 10, 15)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                items(intervalOptions) { mins ->
                                    val isSelected = currentBubbleInterval == mins
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E3A4C),
                                        modifier = Modifier.clickable {
                                            currentBubbleInterval = mins
                                            prefsForCity.bubbleIntervalMinutes = mins
                                            onRefresh()
                                        }
                                    ) {
                                        Text(
                                            text = "$mins Min",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Dynamischer KI-Preisvergleich Button direkt unter dem Intervall
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ KI-Preisvergleich jetzt ausführen",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "🔥 " + LanguageManager.getString("top_opportunities", lang),
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            if (isLoadingOpps && topOpportunities.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF81D4FA), modifier = Modifier.size(24.dp))
                                }
                            } else if (topOpportunities.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E3A4C),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                        Text(if (lang == "DE") "Keine Handelschancen gefunden." else "No trade opportunities found.", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                topOpportunities.forEachIndexed { index, opp ->
                                    key("${opp.resource.fullId}_${opp.buyCity}_${opp.sellCity}") {
                                        val buyTrans = LanguageManager.getCityTranslation(opp.buyCity, lang)
                                        val sellTrans = LanguageManager.getCityTranslation(opp.sellCity, lang)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF1E3A4C),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                                .padding(bottom = 6.dp)
                                                .clickable {
                                                    onAcceptOpportunity(opp)
                                                    selectedTab = BubbleTab.ACTIVE_ORDER
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
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
                                                            fontSize = 11.sp,
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

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // MIDDLE ROW: Buy City & Price -> Sell City & Price
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "🛒 Kaufen in: $buyTrans",
                                                            color = Color.LightGray,
                                                            fontSize = 9.sp
                                                        )
                                                        Text(
                                                            text = "${fmt.format(opp.buyPrice)} Silber",
                                                            color = Color(0xFF81C784),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    }

                                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "🏷️ Verkaufen in: $sellTrans",
                                                            color = Color.LightGray,
                                                            fontSize = 9.sp
                                                        )
                                                        Text(
                                                            text = "${fmt.format(opp.sellPrice)} Silber",
                                                            color = Color(0xFFFFB74D),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // BOTTOM ROW: Total Profit + Zone Distance + Found Time
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "📊 Umsatz: ${fmt.format(opp.totalNetRevenue)} Silber (${fmt.format(opp.tradeUnits)} Stk.)",
                                                        color = Color(0xFF10B981),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                                            Text(
                                                                text = "🗺️ ${opp.zonesWalkedCount} Zonen",
                                                                color = Color(0xFF81D4FA),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }

                                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                                            Text(
                                                                text = "⏰ ${opp.foundTimeStr}",
                                                                color = Color(0xFF38BDF8),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
fun BubbleCatalogTab(
    uiState: ResourceUiState,
    onFocusModeChanged: (Boolean) -> Unit,
    maxHeight: Dp,
    onResourceClick: (AlbionResource) -> Unit
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
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    ) {
        // Search TextField
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Suchname oder ID (z.B. Holz, T4_WOOD)", fontSize = 11.sp) },
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
            items(ResourceCategory.entries) { cat ->
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
            items(tiersList) { tier ->
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
            Text("Keine Ressourcen im Katalog gefunden.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            filtered.forEach { res ->
                key(res.id) {
                    var selectedEnc by remember(res.id) { mutableIntStateOf(res.enchantment) }
                    val effectiveResource = remember(res, selectedEnc) { res.copy(enchantment = selectedEnc) }
                    val prices = uiState.marketPrices[effectiveResource.fullId] ?: uiState.marketPrices[effectiveResource.id] ?: emptyList()
                    val validPrices = prices.filter { it.sellPriceMin > 0 }
                    val bestBuy = validPrices.minByOrNull { it.sellPriceMin }
                    val bestSell = validPrices.maxByOrNull { it.sellPriceMin }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E3A4C),
                        modifier = Modifier.fillMaxWidth().clickable { onResourceClick(effectiveResource) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
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
                                            fontSize = 11.sp,
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🛒 Kaufort: ${bestBuy.city} (${fmt.format(bestBuy.sellPriceMin)} Silber)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 9.sp)
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
    maxHeight: Dp
) {
    val priceMap = remember(uiState.marketPrices) { uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() } }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val topItems = remember(priceMap) {
        AlbionResourceRepository.resources.asSequence().map { res ->
            val recipe = CraftingRepository.getRecipeFor(res)
            val itemSellPrice = CraftingRepository.getPriceInCity(res.fullId, "Martlock", priceMap)
            val totalCost = recipe.ingredients.sumOf { ing ->
                ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, "Martlock", priceMap).toLong()
            }
            val profit = itemSellPrice.toLong() - totalCost
            Triple(res, totalCost, profit)
        }.filter { it.third > 0 }.sortedByDescending { it.third }.take(15).toList()
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
    ) {
        items(topItems, key = { "${it.first.fullId}_${it.second}_${it.third}" }) { (res, cost, profit) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E3A4C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(6.dp)
                ) {
                    AsyncImage(
                        model = res.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = res.nameDe,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kosten: ${fmt.format(cost)} S. | Gewinn: +${fmt.format(profit)} S.",
                            color = Color(0xFF81C784),
                            fontSize = 9.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.acceptTradeOpportunity(
                                TradeOpportunity(
                                    resource = res,
                                    buyCity = "Martlock",
                                    buyPrice = cost.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt(),
                                    sellCity = "Martlock",
                                    sellPrice = (cost + profit).toInt(),
                                    unitNetProfit = profit.toInt(),
                                    unitWeightKg = 1.0,
                                    maxUnitsBySilver = 10,
                                    maxUnitsByWeight = 10,
                                    tradeUnits = 1,
                                    totalInvestment = cost,
                                    totalGrossRevenue = cost + profit,
                                    totalNetRevenue = cost + profit,
                                    totalNetProfit = profit,
                                    totalWeightKg = 1.0,
                                    roiPercent = if (cost > 0) (profit.toDouble() / cost) * 100.0 else 0.0,
                                    priorityScore = 100
                                )
                            )
                            Toast.makeText(viewModel.getApplication(), "${res.nameDe} als Auftrag hinzugefügt!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen", tint = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleIslandTab(
    context: Context,
    maxHeight: Dp
) {
    val buildings = remember { IslandRepository.buildings }
    var selectedCityFilter by remember { mutableStateOf("ALLE") }
    val cities = listOf("ALLE", "Caerleon", "Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Brecilien")

    val filteredBuildings = remember(selectedCityFilter, buildings) {
        if (selectedCityFilter == "ALLE") buildings else buildings.filter { it.cityBonusCity.contains(selectedCityFilter, ignoreCase = true) }
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
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    ) {
        // City Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cities) { city ->
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
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🌾 Insel-Ernte & Tierzucht Timer", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF10B981)) {
                        Text("${activeTimers.size} Aktiv", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (activeTimers.isEmpty()) {
                    Text("Keine aktiven Ernte-Timer.", color = Color.LightGray, fontSize = 9.sp)
                } else {
                    activeTimers.forEach { timer ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                        ) {
                            Text("• ${timer.nameDe}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(timer.remainingFormatted(), color = if (timer.isReady()) Color(0xFF10B981) else Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        filteredBuildings.forEach { bldg ->
            val (yieldQty, yieldPrice) = getYieldDetails(bldg.nameDe)
            key(bldg.id) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3A4C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bldg.nameDe, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("⚡ ${bldg.abilityDescDe}", color = Color.LightGray, fontSize = 9.sp)
                                Text("📍 Bonus: ${bldg.cityBonusCity}", color = Color(0xFF81D4FA), fontSize = 9.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌾 Ertrag: $yieldQty", color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("💰 Wert in ${if (selectedCityFilter == "ALLE") "Stadt" else selectedCityFilter}: $yieldPrice", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
    maxHeight: Dp
) {
    val liveEvents = remember(uiState.liveEventsList) { uiState.liveEventsList.take(10) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
    ) {
        if (liveEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3A4C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine aktiven Live-Events / Boss-Loots.",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            items(liveEvents, key = { it.id }) { event ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3A4C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${event.remainingMinutes} Min",
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 Zone: ${event.zoneName}",
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
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
    maxHeight: Dp
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    var goldAmountInput by remember { mutableStateOf("") }
    var goldPriceInput by remember { mutableStateOf("") }

    val totalOwnedGold = uiState.totalGoldOwned
    val netProfit = uiState.totalGoldNetProfitSilver
    val roi = if (uiState.totalGoldCostSilver > 0) (netProfit.toDouble() / uiState.totalGoldCostSilver) * 100.0 else 0.0

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E3A4C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "🪙 Live Goldpreis (${uiState.server.displayName})",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Portfolio: ${fmt.format(totalOwnedGold)} Gold | Gewinn/Verlust: ${if (netProfit >= 0) "+" else ""}${fmt.format(netProfit)} S. (${String.format(Locale.GERMANY, "%.1f", roi)}%)",
                        color = if (netProfit >= 0) Color(0xFF81C784) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // 🤖 KI Gold-Handelsbot Empfehlung in Floating Bubble
        val botAnalysis = remember(uiState.goldPrices, uiState.currentGoldPrice) {
            GoldBotCalculator.analyzeGoldMarket(uiState.goldPrices, uiState.currentGoldPrice)
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E3A4C),
            border = BorderStroke(1.dp, Color(0xFFFFD700)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "🤖 KI Gold-Handelsbot (Beste Marge)",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("🛍️ Kauf-Order Wert:", color = Color.LightGray, fontSize = 9.sp)
                        Text("${fmt.format(botAnalysis.recommendedBuyOrderPrice)} S.", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("🏷️ Verkauf-Order Wert:", color = Color.LightGray, fontSize = 9.sp)
                        Text("${fmt.format(botAnalysis.recommendedSellOrderPrice)} S.", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 Erwarteter Reingewinn: +${fmt.format(botAnalysis.expectedNetProfitPerGold)} S./Gold (+${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedRoiPercent)}% Marge)",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // Gold Purchase Entry Form inside Floating Bubble
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E3A4C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("✍️ Goldkauf eintragen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = goldAmountInput,
                        onValueChange = { 
                            goldAmountInput = it 
                        },
                        label = { Text("Menge", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onFocusModeChanged(true)
                                }
                            }
                    )

                    OutlinedTextField(
                        value = goldPriceInput,
                        onValueChange = { 
                            goldPriceInput = it
                        },
                        label = { Text("Preis/Gold", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onFocusModeChanged(true)
                                }
                            }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val amount = goldAmountInput.toIntOrNull() ?: 0
                        val price = goldPriceInput.toIntOrNull() ?: 0
                        if ((amount > 0) && (price > 0)) {
                            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
                            val prefs = AppPreferences(viewModel.getApplication())
                            val currentPurchases = prefs.getGoldPurchases()
                            val newPurchase = GoldPurchase(
                                id = UUID.randomUUID().toString(),
                                amountGold = amount,
                                buyPricePerGold = price,
                                purchaseDate = dateStr
                            )
                            prefs.saveGoldPurchases(currentPurchases + newPurchase)
                            goldAmountInput = ""
                            goldPriceInput = ""
                            onFocusModeChanged(false)
                            Toast.makeText(viewModel.getApplication(), "Goldkauf gespeichert! Portfolio wird beobachtet.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(viewModel.getApplication(), "Gültige Menge & Preis eingeben", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Goldkauf speichern", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
        // Einnahmen-Historie für Gold (Top 3)
        val prefs = remember { AppPreferences(viewModel.getApplication()) }
        val goldPurchases = prefs.getGoldPurchases()
        if (goldPurchases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("📜 Letzte 3 Goldkäufe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            goldPurchases.takeLast(3).reversed().forEach { purchase ->
                val netProfit = purchase.netProfitSilver(uiState.currentGoldPrice)
                val roi = purchase.roiPercent(uiState.currentGoldPrice)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A1922),
                    border = BorderStroke(1.dp, Color(0xFF1E3A4C)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${fmt.format(purchase.amountGold)} Gold", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(
                                text = "${if (netProfit >= 0) "+" else ""}${fmt.format(netProfit)} S. (%.1f%%)".format(roi),
                                color = if (netProfit >= 0) Color(0xFF66BB6A) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text("Kaufpreis: ${fmt.format(purchase.buyPricePerGold)} S./Gold", color = Color.LightGray, fontSize = 9.sp)
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
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = if (lang == "DE") "📜 Auftrags-Historie (${completedOrders.size})" else "📜 Order History (${completedOrders.size})",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
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
                    fontSize = 11.sp,
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
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${order.resourceNameDe} (T${order.tier})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (profit >= 0) "+" else ""}${fmt.format(profit)} S.",
                                color = if (profit >= 0) Color(0xFF66BB6A) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
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
