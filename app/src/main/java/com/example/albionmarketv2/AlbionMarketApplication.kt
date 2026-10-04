package com.example.albionmarketv2

import android.app.Application
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class that starts the 24/7 Persistent Server Plugin connection immediately upon APK installation or app launch,
 * instantly connecting to Render cloud.
 */
class AlbionMarketApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Global Uncaught Exception Handler to prevent abrupt app crashes ("app wurde beendet")
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                throwable.printStackTrace()
                applicationScope.launch {
                    try {
                        ServerSyncManager.sendTelemetryLog(
                            context = this@AlbionMarketApplication,
                            errorTrace = "${throwable.javaClass.name}: ${throwable.message}\n${throwable.stackTraceToString().take(1000)}"
                        )
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // CPU & Thread Priority Optimization for Maximum Performance
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        } catch (_: Exception) {}

        // Bypass SSL certification errors for development endpoints
        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()

        // Initialize server config folder and server_config.json file upon installation/launch
        ServerConfigManager.initServerConfig(this)

        // Restore settings, credentials, and trade history from persistent external storage folder (Documents/AlbionDataPro)
        try {
            ExternalStorageBackupManager.loadBackupAppSettings(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Instantly connect to Render Cloud upon app startup
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
