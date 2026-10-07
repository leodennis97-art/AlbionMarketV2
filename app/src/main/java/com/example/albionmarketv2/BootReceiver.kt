package com.example.albionmarketv2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that starts the 24/7 Persistent Server Connection immediately when device boots or APK is updated/installed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if ((action == Intent.ACTION_BOOT_COMPLETED) ||
            (action == Intent.ACTION_MY_PACKAGE_REPLACED) ||
            (action == "android.intent.action.QUICKBOOT_POWERON") ||
            (action == Intent.ACTION_POWER_CONNECTED) ||
            (action == "com.example.albionmarketv2.ACTION_RESTART_SYNC_SERVICE")
        ) {
            try {
                PersistentServerSyncService.startService(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
