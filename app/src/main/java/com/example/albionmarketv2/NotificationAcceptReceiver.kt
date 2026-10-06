package com.example.albionmarketv2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NotificationAcceptReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            "ACTION_ACCEPT_TRADE_ORDER" -> {
                val prefs = AppPreferences(context)
                val currentOrders = prefs.getTradeOrders()
                val activeOrders = currentOrders.filter { it.status == OrderStatus.ACTIVE }

                if (activeOrders.size >= 10) {
                    Toast.makeText(context, "⚠️ Maximal 10 aktive Aufträge gleichzeitig erlaubt!", Toast.LENGTH_LONG).show()
                    return
                }

                val resourceId = intent.getStringExtra("resourceId") ?: "T4_ITEM"
                val resourceNameDe = intent.getStringExtra("resourceNameDe") ?: "Handelsitem"
                val resourceNameEn = intent.getStringExtra("resourceNameEn") ?: "Trade Item"
                val tier = intent.getIntExtra("tier", 4)
                val enchantment = intent.getIntExtra("enchantment", 0)
                val buyCity = intent.getStringExtra("buyCity") ?: "Bridgewatch"
                val buyPrice = intent.getIntExtra("buyPrice", 1000)
                val sellCity = intent.getStringExtra("sellCity") ?: "Caerleon"
                val sellPrice = intent.getIntExtra("sellPrice", 2000)
                val plannedUnits = intent.getIntExtra("plannedUnits", 10)
                val targetNetProfit = intent.getLongExtra("targetNetProfit", 5000L)
                val targetInvestment = intent.getLongExtra("targetInvestment", 10000L)

                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())

                val newOrder = TradeOrder(
                    id = UUID.randomUUID().toString(),
                    resourceId = resourceId,
                    resourceNameDe = resourceNameDe,
                    resourceNameEn = resourceNameEn,
                    tier = tier,
                    enchantment = enchantment,
                    buyCity = buyCity,
                    buyPrice = buyPrice,
                    sellCity = sellCity,
                    sellPrice = sellPrice,
                    plannedUnits = plannedUnits,
                    targetNetProfit = targetNetProfit,
                    targetInvestment = targetInvestment,
                    acceptedDate = dateStr,
                    status = OrderStatus.ACTIVE,
                    recommendedBuyOrderPrice = (buyPrice * 0.88).toInt().coerceAtLeast(1),
                    recommendedSellOrderPrice = (sellPrice * 1.08).toInt().coerceAtLeast(1)
                )

                val updatedList = currentOrders + newOrder
                prefs.saveTradeOrders(updatedList)

                Toast.makeText(context, "✅ Auftrag '$resourceNameDe' aus Benachrichtigung gebucht! (${activeOrders.size + 1}/3)", Toast.LENGTH_LONG).show()
            }

            "ACTION_DISCARD_TRADE_ORDER" -> {
                val notifId = intent.getIntExtra("notificationId", 0)
                val name = intent.getStringExtra("resourceNameDe") ?: "Handelschance"
                if (notifId != 0) {
                    try {
                        NotificationManagerCompat.from(context).cancel(notifId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Toast.makeText(context, "❌ Trade '$name' aus Benachrichtigung storniert.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
