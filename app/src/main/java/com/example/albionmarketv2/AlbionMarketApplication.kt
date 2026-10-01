package com.example.albionmarketv2

import android.app.Application
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class that starts the 24/7 Persistent Server Plugin connection immediately upon APK installation or app launch,
 * instantly connecting to both tunnel and localhost.
 */
class AlbionMarketApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // CPU & Thread Priority Optimization for Maximum Performance
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        } catch (_: Exception) {}

        // Bypass SSL certification errors for localhost and development endpoints
        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()

        // Initialize server config folder and server_config.json file upon installation/launch
        ServerConfigManager.initServerConfig(this)

        // Automatically start the persistent 24/7 server sync service right at application startup
        PersistentServerSyncService.startService(this)

        // Instantly connect to both tunnel and localhost upon app startup
        applicationScope.launch {
            try {
                ServerSyncManager.testAndConnectToServer(this@AlbionMarketApplication)
                ServerSyncManager.pingServer(this@AlbionMarketApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
