package com.example.albionmarketv2

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.Toast

class NotificationAcceptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (action == "ACTION_ACCEPT_TRADE_ORDER") {
            val resourceNameDe = intent.getStringExtra("resourceNameDe") ?: "Trade"
            val buyCity = intent.getStringExtra("buyCity") ?: ""
            val sellCity = intent.getStringExtra("sellCity") ?: ""
            val netProfit = intent.getLongExtra("targetNetProfit", 0L)
            val dateStr = DateFormat.format("dd.MM.yyyy HH:mm", System.currentTimeMillis()).toString()
            
            ProfitHistoryManager.addProfitEntry(context, "$resourceNameDe ($buyCity ➔ $sellCity)", netProfit, dateStr)
            Toast.makeText(context, "✅ Handelsauftrag '$resourceNameDe' angenommen!", Toast.LENGTH_SHORT).show()
        } else if (action == "ACTION_DISCARD_TRADE_ORDER") {
            val notificationId = intent.getIntExtra("notificationId", -1)
            if (notificationId != -1) {
                notificationManager.cancel(notificationId)
            }
            Toast.makeText(context, "❌ Handelsauftrag verworfen", Toast.LENGTH_SHORT).show()
        }
    }
}
