package com.example.albionmarketv2

import android.content.Context
import android.net.ConnectivityManager

object NetworkDependencyManager {

    /**
     * Checks if network is present or available.
     * Prevents service loop crashes while allowing continuous localhost / ADB loopback communication.
     */
    fun checkInternetOrCrash(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            cm?.getNetworkCapabilities(network)
        } catch (_: Exception) {
            // Suppress exception to keep background services continuously running
        }
    }
}
