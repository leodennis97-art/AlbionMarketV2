package com.example.albionmarketv2

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PriceAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val prefs = AppPreferences(context)
            val server = prefs.server
            val targetItems = listOf("T4_BAG", "T4_MOUNT_HORSE") 
            
            val prices = AlbionMarketApi.fetchPrices(server, targetItems)
            
            for (price in prices) {
                if (price.sellPriceMin > 0 && price.sellPriceMin < 5000) {
                    sendNotification("Schnäppchen-Alarm!", "Das Item ${price.itemId} ist gerade für ${price.sellPriceMin} in ${price.city} zu haben!")
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "price_alerts_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Preis-Alarme",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
