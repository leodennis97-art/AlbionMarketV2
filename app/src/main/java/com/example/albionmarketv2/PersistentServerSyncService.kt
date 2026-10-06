package com.example.albionmarketv2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours

/**
 * 24/7 Persistent Server Synchronization Service.
 * Maintains a continuous background connection to the AlbionDataPro server immediately upon APK installation and device boot.
 * Also runs a 5-minute background check for gold price 5%+ changes and new lucrative trade opportunities.
 */
class PersistentServerSyncService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "albion_persistent_sync_channel"
        const val NOTIFICATION_ID = 3003
        private var isRunning = false

        fun startService(context: Context) {
            try {
                val intent = Intent(context, PersistentServerSyncService::class.java)
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    try {
                        context.startService(intent)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Suppress("unused")
        fun stopService(context: Context) {
            try {
                val intent = Intent(context, PersistentServerSyncService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Suppress("unused")
        fun isConnected(): Boolean = isRunning
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlbionMarketV2::ServerSyncWakeLock")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isRunning = true
        startForegroundNotification()
        startContinuousServerLoop()
        start5MinuteBackgroundCheckLoop()
        startHourlyOtaDownloadLoop()
        start30MinGoldStatusLoop()
    }

    private fun start30MinGoldStatusLoop() {
        serviceScope.launch {
            while (isRunning) {
                delay(60.minutes)
                try {
                    val prefs = AppPreferences(this@PersistentServerSyncService)
                    if (prefs.systemNotificationsEnabled) {
                        val goldPrices = AlbionGoldApi.fetchGoldPrices(prefs.server, count = 1)
                        val currentGold = goldPrices.firstOrNull()?.price ?: 4250
                        val ownedGold = prefs.goldAmount
                        val profit = ownedGold * (currentGold - 4250)
                        NotificationHelper.showGoldProfitNotification(
                            this@PersistentServerSyncService,
                            goldAmount = ownedGold.toInt(),
                            netProfitSilver = profit,
                            roiPercent = (profit.toDouble() / (ownedGold * 4250).coerceAtLeast(1L)) * 100.0,
                            currentGoldPrice = currentGold
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isRunning = true
        startForegroundNotification()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Albion 24/7 Dauerhafte Server-Verbindung",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AlbionDataPro Server-Plugin Verbunden")
            .setContentText("Dauerhafte 24/7 Hintergrund-Verbindung zum Server aktiv")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    private var consecutivePingFailures = 0

    private fun startContinuousServerLoop() {
        serviceScope.launch {
            while (isRunning) {
                var isConnected = false
                try {
                    // Brief WakeLock acquire during sync to prevent CPU sleep during network roundtrip
                    wakeLock?.acquire(5000L)

                    // Strict internet dependency check
                    NetworkDependencyManager.checkInternetOrCrash(this@PersistentServerSyncService)

                    val prefs = AppPreferences(this@PersistentServerSyncService)
                    val activeOrdersCount = prefs.getTradeOrders().count { it.status == OrderStatus.ACTIVE }

                    // Ping server and update device status continuously
                    val stats = ServerSyncManager.pingServer(this@PersistentServerSyncService, activeOrdersCount)
                    if (stats != null) {
                        isConnected = true
                        consecutivePingFailures = 0
                        ServerSyncManager.isServerConnected = true
                    } else {
                        consecutivePingFailures++
                        if (consecutivePingFailures >= 3) {
                            isConnected = false
                            ServerSyncManager.isServerConnected = false
                        } else {
                            isConnected = true // Maintain stable connected state during transient blips
                        }
                    }

                    // Sync price snapshots
                    val snapshots = prefs.getPriceSnapshots(prefs.server)
                    if (snapshots.isNotEmpty()) {
                        ServerSyncManager.syncPriceSnapshots(this@PersistentServerSyncService, snapshots)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (_: Exception) {}
                }
                // Battery-optimized background sync interval: 10 minutes when connected, 2 minutes retry when offline
                delay(if (isConnected) 10.minutes else 2.minutes)
            }
        }
    }

    private var lastKnownGoldPrice: Int = 0
    private var lastNotifiedOppKey: String = ""

    private fun start5MinuteBackgroundCheckLoop() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    val prefs = AppPreferences(this@PersistentServerSyncService)
                    if (!prefs.systemNotificationsEnabled) {
                        delay(15.minutes)
                        continue
                    }

                    // 1. Check Gold Price change >= 5%
                    val goldPrices = AlbionGoldApi.fetchGoldPrices(prefs.server, count = 1)
                    val currentGold = goldPrices.firstOrNull()?.price ?: 0
                    if (currentGold > 0) {
                        if (lastKnownGoldPrice > 0) {
                            val priceDiffPercent = abs(currentGold - lastKnownGoldPrice).toDouble() / lastKnownGoldPrice * 100.0
                            if (priceDiffPercent >= 5.0) {
                                val changeDirection = if (currentGold > lastKnownGoldPrice) "gestiegen 📈" else "gefallen 📉"
                                NotificationHelper.showTradeNotification(
                                    this@PersistentServerSyncService,
                                    title = "🪙 Goldpreis $changeDirection um ${String.format(Locale.GERMANY, "%.1f", priceDiffPercent)}%",
                                    message = "Neuer Goldpreis: $currentGold Silber (Vorher: $lastKnownGoldPrice Silber)"
                                )
                            }
                        }
                        lastKnownGoldPrice = currentGold
                    }

                    // 2. Check for new lucrative trade opportunities
                    try {
                        val cloudSnapshots = ServerSyncManager.fetchCloudPrices(this@PersistentServerSyncService)
                        if (cloudSnapshots.isNotEmpty()) {
                            prefs.savePriceSnapshots(prefs.server, cloudSnapshots)
                        }
                    } catch (_: Exception) {}

                    val snapshots = prefs.getPriceSnapshots(prefs.server)
                    val priceMap = if (snapshots.isNotEmpty()) {
                        snapshots.map { s ->
                            MarketPrice(
                                itemId = s.itemId,
                                city = s.city,
                                quality = 1,
                                sellPriceMin = s.sellPriceMin,
                                sellPriceMinDate = "",
                                buyPriceMax = s.buyPriceMax,
                                buyPriceMaxDate = ""
                            )
                        }.groupBy { it.itemId }
                    } else {
                        AlbionMarketApi.getFallbackMarketPrices()
                    }

                    val opps = TradeCalculator.calculateOpportunities(
                        resources = AlbionResourceRepository.resources,
                        pricesByItem = priceMap,
                        silverBudget = prefs.silverBudget,
                        carryCapacityKg = prefs.carryCapacityKg,
                        marketTaxPercent = if (prefs.hasPremium) 4.0 else 8.0,
                        targetMarginPercent = prefs.targetMarginPercent,
                        avoidDangerousZones = prefs.avoidDangerousZones,
                        currentGoldPrice = currentGold.coerceAtLeast(4250),
                        hideBrecilien = prefs.hideBrecilien,
                        hideBlackMarket = prefs.hideBlackMarket
                    )

                    val bubbleCategory = prefs.bubbleCategory
                    val bubbleTier = prefs.bubbleTier
                    val filteredOpps = opps.filter { opp ->
                        val matchesCat = if (bubbleCategory != "ALL") {
                            try {
                                opp.resource.category.name.equals(bubbleCategory, ignoreCase = true) || 
                                opp.resource.category.name == bubbleCategory ||
                                opp.resource.category.displayName.contains(bubbleCategory, ignoreCase = true)
                            } catch (_: Exception) {
                                true
                            }
                        } else true

                        val matchesTier = if (bubbleTier > 0) {
                            opp.resource.tier == bubbleTier
                        } else true

                        matchesCat && matchesTier
                    }

                    val topOpp = filteredOpps.firstOrNull()
                    if (topOpp != null) {
                        val oppKey = "${topOpp.resource.fullId}_${topOpp.buyCity}_${topOpp.sellCity}_${topOpp.buyPrice}"
                        if (oppKey != lastNotifiedOppKey && topOpp.totalNetProfit >= 10_000) {
                            lastNotifiedOppKey = oppKey
                            NotificationHelper.showTradeNotification(
                                this@PersistentServerSyncService,
                                title = "🔥 Neue Handelschance: ${topOpp.resource.nameDe}",
                                message = "Kaufen in ${topOpp.buyCity} -> Verkaufen in ${topOpp.sellCity} (+${NumberFormat.getNumberInstance(Locale.GERMANY).format(topOpp.totalNetProfit)} Silber Gewinn / +${String.format(Locale.GERMANY, "%.1f", topOpp.roiPercent)}% Marge)"
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(15.minutes) // 15-minute background pulse
            }
        }
    }

    private fun startHourlyOtaDownloadLoop() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    // Periodic check of download stats from Render Cloud
                    NetworkDependencyManager.checkInternetOrCrash(this@PersistentServerSyncService)
                    
                    val stats = ServerSyncManager.fetchDownloadStats(this@PersistentServerSyncService)
                    if (stats.hourly24h.isNotEmpty()) {
                        println("24h Downloads Hourly Sync: Total=${stats.totalDownloads}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30.minutes)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            val restartIntent = Intent(this, BootReceiver::class.java).apply {
                action = "com.example.albionmarketv2.ACTION_RESTART_SYNC_SERVICE"
            }
            sendBroadcast(restartIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
