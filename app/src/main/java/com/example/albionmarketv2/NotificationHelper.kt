package com.example.albionmarketv2

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "albion_trade_alerts"
    private const val CHANNEL_NAME = "Albion Markt Alerts"
    private const val CHANNEL_DESC = "Benachrichtigungen für gewinnbringende Handelschancen"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun isDeviceOrAppLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardLocked = (keyguardManager?.isKeyguardLocked == true) || (keyguardManager?.isDeviceLocked == true)
        val prefs = AppPreferences(context)
        val isAppLocked = !prefs.isUserLoggedIn || !ServerSyncManager.isServerConnected
        return isKeyguardLocked || isAppLocked
    }

    fun showTradeNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt(),
    ) {
        if (isDeviceOrAppLocked(context)) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showOpportunityNotification(
        context: Context,
        opp: TradeOpportunity,
        notificationId: Int = System.currentTimeMillis().toInt(),
    ) {
        if (isDeviceOrAppLocked(context)) return

        createNotificationChannel(context)

        val prefs = AppPreferences(context)
        val botName = prefs.aiBotName.ifBlank { "AlbionBot" }
        val numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY)

        val title = "🤖 $botName"
        val message = "Hey Freund, ich habe etwas gefunden!\n\n🔥 ${opp.resource.nameDe} (T${opp.resource.tier}${opp.resource.enchantmentText})\n📍 Kauf: ${opp.buyCity} (${numberFormat.format(opp.buyPrice)} S.) ➔ Verkauf: ${opp.sellCity} (${numberFormat.format(opp.sellPrice)} S.)\n💰 Reingewinn: +${numberFormat.format(opp.totalNetProfit)} Silber (+${String.format(Locale.GERMANY, "%.1f", opp.roiPercent)}%)"

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingMainIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, NotificationAcceptReceiver::class.java).apply {
            action = "ACTION_ACCEPT_TRADE_ORDER"
            putExtra("resourceId", opp.resource.fullId)
            putExtra("resourceNameDe", opp.resource.nameDe)
            putExtra("resourceNameEn", opp.resource.nameEn)
            putExtra("tier", opp.resource.tier)
            putExtra("buyCity", opp.buyCity)
            putExtra("buyPrice", opp.buyPrice)
            putExtra("sellCity", opp.sellCity)
            putExtra("sellPrice", opp.sellPrice)
            putExtra("plannedUnits", opp.tradeUnits)
            putExtra("targetNetProfit", opp.totalNetProfit)
            putExtra("targetInvestment", opp.totalInvestment)
        }

        val pendingAcceptIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val discardIntent = Intent(context, NotificationAcceptReceiver::class.java).apply {
            action = "ACTION_DISCARD_TRADE_ORDER"
            putExtra("notificationId", notificationId)
            putExtra("resourceNameDe", opp.resource.nameDe)
        }

        val pendingDiscardIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            discardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingMainIntent)
            .addAction(android.R.drawable.ic_input_add, "✅ Auftrag annehmen", pendingAcceptIntent)
            .addAction(android.R.drawable.ic_delete, "❌ Stornieren", pendingDiscardIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showGoldProfitNotification(
        context: Context,
        goldAmount: Int,
        netProfitSilver: Long,
        roiPercent: Double,
        currentGoldPrice: Int,
        notificationId: Int = System.currentTimeMillis().toInt(),
    ) {
        if (isDeviceOrAppLocked(context)) return

        createNotificationChannel(context)

        val numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val title = "🪙 Goldmarkt Live-Einnahme: +${numberFormat.format(netProfitSilver)} Silber"
        val message = "Dein Goldbestand ($goldAmount Gold) erzielte einen Reingewinn von +${numberFormat.format(netProfitSilver)} Silber (%.1f%% ROI) bei ${numberFormat.format(currentGoldPrice)} Silber/Gold!".format(roiPercent)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingMainIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingMainIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
