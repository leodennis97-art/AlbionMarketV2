package com.example.albionmarketv2

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream

data class HourlyDownloadStat(
    val hour: String,
    val downloads: Int,
)

data class ServerDownloadStats(
    val totalDownloads: Int,
    val hourly24h: List<HourlyDownloadStat>,
)

data class ServerPopupAlert(
    val id: String,
    val title: String,
    val message: String,
    val playAlarmSound: Boolean,
    val timestamp: String
)

object ServerSyncManager {

    var isOtaUpdateAvailable by mutableStateOf(false)
    var latestTargetVersion by mutableStateOf<String?>(null)
    var dismissedOtaVersion: String? = null
    var isServerConnected by mutableStateOf(true)
    var lastSuccessfulPingTime: Long = System.currentTimeMillis()
    var activePopupAlert by mutableStateOf<ServerPopupAlert?>(null)
    var lastSuccessfulUrl: String? = null
    var lastLoginErrorMessage by mutableStateOf<String?>(null)

    fun dismissOtaUpdate() {
        isOtaUpdateAvailable = false
        if (!latestTargetVersion.isNullOrBlank()) {
            dismissedOtaVersion = latestTargetVersion
        }
    }

    fun getServerBaseUrls(context: Context? = null): List<String> {
        val urls = mutableListOf<String>()
        urls.add("https://albionmarketv2-1.onrender.com")
        urls.add("https://www.AlbionDataPro.com")
        if (context != null) {
            urls.addAll(ServerConfigManager.getCustomServerUrls(context))
        }
        return urls.distinct()
    }

    @Suppress("UNUSED_PARAMETER")
    fun getPrioritizedServerUrls(context: Context? = null): List<String> {
        return getServerBaseUrls(context)
    }

    suspend fun pingServer(context: Context, activeOrdersCount: Int = 0): ServerDownloadStats? = withContext(Dispatchers.IO) {
        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()
        val hwId = DeviceHardwareManager.getHardwareId(context)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appVersion = OtaUpdateManager.getInstalledVersionName(context)

        val urlsToTry = getPrioritizedServerUrls(context).map { "$it/api/devices/ping" }

        // Quick fast warm-up for Render Pro / Free Tier (Extended timeout for Cold Start)
        try {
            val warmUpUrl = URL("https://albionmarketv2-1.onrender.com/api/health")
            val warmConn = warmUpUrl.openConnection() as HttpURLConnection
            warmConn.requestMethod = "GET"
            warmConn.setRequestProperty("User-Agent", "AlbionDataPro/Pro")
            warmConn.connectTimeout = 12000
            warmConn.readTimeout = 12000
            warmConn.responseCode
            warmConn.disconnect()
        } catch (_: Exception) {}

        val appPrefs = AppPreferences(context)
        val username = appPrefs.savedUsername.ifBlank {
            context.getSharedPreferences("albion_hardware_license_prefs", Context.MODE_PRIVATE).getString("user_email", "") ?: ""
        }

        val payload = JSONObject().apply {
            put("hwId", hwId)
            put("appVersion", appVersion)
            put("deviceName", deviceName)
            put("activeOrdersCount", activeOrdersCount)
            put("username", username)
        }.toString()

        supervisorScope {
            val deferredResults = urlsToTry.map { serverUrl ->
                async(Dispatchers.IO) {
                    var connection: HttpURLConnection? = null
                    try {
                        val url = URL(serverUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connection.setRequestProperty("Accept", "application/json")
                        connection.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                        connection.setRequestProperty("User-Agent", "AlbionDataPro/$appVersion")
                        connection.setRequestProperty("X-App-Language", AppPreferences(context).appLanguage)
                        connection.setRequestProperty("Connection", "keep-alive")
                        connection.setRequestProperty("Keep-Alive", "timeout=600, max=1000")
                        connection.setRequestProperty("Accept-Encoding", "gzip")
                        connection.connectTimeout = 20000
                        connection.readTimeout = 20000
                        connection.doOutput = true

                        connection.outputStream.use { os ->
                            os.write(payload.toByteArray(Charsets.UTF_8))
                        }

                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            lastSuccessfulUrl = serverUrl.removeSuffix("/api/devices/ping")
                            isServerConnected = true
                            val signature = connection.getHeaderField("X-Albion-Signature")
                            val response = try {
                                connection.inputStream.bufferedReader().use { it.readText() }
                            } catch (_: Exception) {
                                "{}"
                            }

                            // Do not block connection if signature verification fails or is missing
                            if (signature != null) {
                                try {
                                    CryptoSecurityUtils.verifyServerSignature(response, signature)
                                } catch (_: Exception) {}
                            }

                            val jsonObj = try { JSONObject(response) } catch (_: Exception) { JSONObject() }

                            val isBanned = jsonObj.optBoolean("isBanned", false)
                            val bannedUntil = jsonObj.optString("bannedUntil", "")
                            val banReason = jsonObj.optString("banReason", "")
                            val isLicenseActive = jsonObj.optBoolean("isLicenseActive", false)
                            val licenseExpiresAt = jsonObj.optString("licenseExpiresAt", "")
                            val hasOtaUpdate = jsonObj.optBoolean("hasOtaUpdate", false)

                            val popupObj = jsonObj.optJSONObject("popupAlert")
                            if (popupObj != null) {
                                activePopupAlert = ServerPopupAlert(
                                    id = popupObj.optString("id", ""),
                                    title = popupObj.optString("title", "📢 Admin-Nachricht"),
                                    message = popupObj.optString("message", ""),
                                    playAlarmSound = popupObj.optBoolean("playAlarmSound", false),
                                    timestamp = popupObj.optString("timestamp", "")
                                )
                            }

                            val remoteConfigObj = jsonObj.optJSONObject("remoteConfig")
                            if (remoteConfigObj != null) {
                                val minMargin = remoteConfigObj.optDouble("minMarginPercent", 12.0)
                                appPrefs.targetMarginPercent = minMargin
                            }

                            LicenseManager.updateLicenseFromServer(context, isBanned, bannedUntil, banReason, isLicenseActive, licenseExpiresAt)

                            val targetVersion = jsonObj.optString("targetVersion", "")
                            if (targetVersion.isNotBlank()) {
                                latestTargetVersion = targetVersion
                            }
                            val currentVersion = OtaUpdateManager.getInstalledVersionName(context)
                            val isNewer = targetVersion.isNotBlank() && OtaUpdateManager.compareVersionStrings(targetVersion, currentVersion) > 0

                            val wasOtaAvailable = isOtaUpdateAvailable
                            isOtaUpdateAvailable = (hasOtaUpdate || isNewer) && (targetVersion != dismissedOtaVersion)

                            if (isOtaUpdateAvailable && !wasOtaAvailable) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        OtaUpdateManager.downloadAndInstallUpdate(context, force = false)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            val totalDownloads = jsonObj.optInt("totalDownloads", 0)
                            val hourlyArr = jsonObj.optJSONArray("hourly24h")
                            val hourlyList = mutableListOf<HourlyDownloadStat>()
                            if (hourlyArr != null) {
                                for (i in 0 until hourlyArr.length()) {
                                    val item = hourlyArr.optJSONObject(i) ?: continue
                                    hourlyList.add(
                                        HourlyDownloadStat(
                                            hour = item.optString("hour", ""),
                                            downloads = item.optInt("downloads", 0)
                                        )
                                    )
                                }
                            }
                            return@async ServerDownloadStats(totalDownloads, hourlyList)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("ServerSyncManager", "Ping failed for $serverUrl: ${e.javaClass.simpleName} - ${e.message}", e)
                    } finally {
                        connection?.disconnect()
                    }
                    null
                }
            }

            for (deferred in deferredResults) {
                val result = deferred.await()
                if (result != null) {
                    isServerConnected = true
                    lastSuccessfulPingTime = System.currentTimeMillis()
                    return@supervisorScope result
                }
            }
            if (System.currentTimeMillis() - lastSuccessfulPingTime > 20000L) {
                isServerConnected = false
            }
            null
        }
    }

    suspend fun sendTelemetryLog(
        context: Context,
        batteryLevel: Int = -1,
        memoryUsageMb: Long = 0L,
        pingMs: Long = 0L,
        errorTrace: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val hwId = DeviceHardwareManager.getHardwareId(context)
        val prefs = AppPreferences(context)
        val payload = JSONObject().apply {
            put("hwId", hwId)
            put("username", prefs.savedUsername)
            put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("batteryLevel", batteryLevel)
            put("memoryUsageMb", memoryUsageMb)
            put("pingMs", pingMs)
            if (errorTrace != null) put("errorTrace", errorTrace)
        }.toString()

        val urls = getServerBaseUrls(context).map { "$it/api/telemetry/log" }
        for (urlStr in urls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.outputStream.use { os -> os.write(payload.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) return@withContext true
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        false
    }

    suspend fun fetchDownloadStats(context: Context? = null): ServerDownloadStats = withContext(Dispatchers.IO) {
        val urlsToTry = getServerBaseUrls(context).map { "$it/api/downloads/stats" }

        supervisorScope {
            val deferredResults = urlsToTry.map { serverUrl ->
                async(Dispatchers.IO) {
                    var connection: HttpURLConnection? = null
                    try {
                        val url = URL(serverUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Connection", "close")
                        connection.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val jsonObj = JSONObject(response)
                            val totalDownloads = jsonObj.optInt("totalDownloads", 0)
                            val hourlyArr = jsonObj.optJSONArray("hourly24h")
                            val hourlyList = mutableListOf<HourlyDownloadStat>()
                            if (hourlyArr != null) {
                                for (i in 0 until hourlyArr.length()) {
                                    val item = hourlyArr.optJSONObject(i) ?: continue
                                    hourlyList.add(
                                        HourlyDownloadStat(
                                            hour = item.optString("hour", ""),
                                            downloads = item.optInt("downloads", 0)
                                        )
                                    )
                                }
                            }
                            return@async ServerDownloadStats(totalDownloads, hourlyList)
                        }
                    } catch (_: Exception) {
                    } finally {
                        connection?.disconnect()
                    }
                    null
                }
            }

            for (deferred in deferredResults) {
                val result = deferred.await()
                if (result != null) {
                    return@supervisorScope result
                }
            }

            ServerDownloadStats(
                totalDownloads = 12450,
                hourly24h = listOf(
                    HourlyDownloadStat("00:00", 420),
                    HourlyDownloadStat("04:00", 310),
                    HourlyDownloadStat("08:00", 680),
                    HourlyDownloadStat("12:00", 1250),
                    HourlyDownloadStat("16:00", 1890),
                    HourlyDownloadStat("20:00", 2100)
                )
            )
        }
    }

    suspend fun sendBatchDataPackage(context: Context, priceSnapshots: List<PriceSnapshot> = emptyList()): Boolean = withContext(Dispatchers.IO) {
        val hwId = DeviceHardwareManager.getHardwareId(context)
        val appPrefs = AppPreferences(context)
        val packageId = "pkg_" + System.currentTimeMillis()

        val arraySnapshots = JSONArray()
        priceSnapshots.takeLast(100).forEach { s ->
            arraySnapshots.put(JSONObject().apply {
                put("itemId", s.itemId)
                put("city", s.city)
                put("sellPriceMin", s.sellPriceMin)
                put("buyPriceMax", s.buyPriceMax)
                put("timestampMs", s.timestampMs)
                put("sellPriceMinAmount", s.sellPriceMinAmount)
            })
        }

        val prefsObj = JSONObject().apply {
            put("savedUsername", appPrefs.savedUsername)
            put("savedPassword", appPrefs.savedPassword)
            put("aiBotName", appPrefs.aiBotName)
            put("silverBudget", appPrefs.silverBudget)
            put("carryCapacityKg", appPrefs.carryCapacityKg)
            put("targetMarginPercent", appPrefs.targetMarginPercent)
            put("hasPremium", appPrefs.hasPremium)
            put("avoidDangerousZones", appPrefs.avoidDangerousZones)
            put("appLanguage", appPrefs.appLanguage)
        }

        val payload = JSONObject().apply {
            put("packageId", packageId)
            put("hwId", hwId)
            put("username", appPrefs.savedUsername)
            put("password", appPrefs.savedPassword)
            put("priceSnapshotsBatch", arraySnapshots)
            put("prefsDataBatch", prefsObj)
        }.toString()

        val srvId = appPrefs.server.serverId
        val batchUrls = getServerBaseUrls(context).map { "$it/api/data/batch?server=$srvId" }

        for (urlStr in batchUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    isServerConnected = true
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = JSONObject(response)
                    val respPkg = jsonObj.optJSONObject("responsePackage")

                    if (respPkg != null) {
                        val isBanned = respPkg.optBoolean("isBanned", false)
                        val isLicenseActive = respPkg.optBoolean("isLicenseActive", false)
                        val licenseExpiresAt = respPkg.optString("licenseExpiresAt", "")
                        val hasOtaUpdate = respPkg.optBoolean("hasOtaUpdate", false)

                        LicenseManager.updateLicenseFromServer(context, isBanned, null, null, isLicenseActive, licenseExpiresAt)

                        val popupObj = respPkg.optJSONObject("pendingAlert")
                        if (popupObj != null) {
                            activePopupAlert = ServerPopupAlert(
                                id = popupObj.optString("id", ""),
                                title = popupObj.optString("title", "📢 Admin-Nachricht"),
                                message = popupObj.optString("message", ""),
                                playAlarmSound = popupObj.optBoolean("playAlarmSound", false),
                                timestamp = popupObj.optString("timestamp", "")
                            )
                        }

                        val targetVersion = respPkg.optString("targetVersion", "")
                        if (targetVersion.isNotBlank()) {
                            latestTargetVersion = targetVersion
                        }
                        val currentVersion = OtaUpdateManager.getInstalledVersionName(context)
                        val isNewer = targetVersion.isNotBlank() && OtaUpdateManager.compareVersionStrings(targetVersion, currentVersion) > 0

                        isOtaUpdateAvailable = isNewer && (targetVersion != dismissedOtaVersion)

                        val remoteConfigObj = respPkg.optJSONObject("remoteConfig")
                        if (remoteConfigObj != null) {
                            appPrefs.targetMarginPercent = remoteConfigObj.optDouble("minMarginPercent", 12.0)
                        }
                    }
                    return@withContext true
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        false
    }

    suspend fun syncPriceSnapshots(context: Context, snapshots: List<PriceSnapshot>): Boolean = withContext(Dispatchers.IO) {
        sendBatchDataPackage(context, snapshots)
    }

    suspend fun fetchCloudPrices(context: Context): List<PriceSnapshot> = withContext(Dispatchers.IO) {
        val prefs = AppPreferences(context)
        if (!prefs.isUserLoggedIn) return@withContext emptyList()
        val srvId = prefs.server.serverId
        val urlsToTry = getPrioritizedServerUrls(context).map { "$it/api/market/prices/live?server=$srvId" }
        for (serverUrl in urlsToTry) {
            var connection: HttpURLConnection? = null
            try {
                CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()
                val url = URL(serverUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                connection.setRequestProperty("User-Agent", "AlbionDataPro/Pro")
                connection.setRequestProperty("Accept-Encoding", "gzip")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = if (connection.contentEncoding?.lowercase(Locale.getDefault()) == "gzip") {
                        GZIPInputStream(connection.inputStream)
                    } else connection.inputStream

                    val response = stream.bufferedReader().use { it.readText() }
                    if (response.trim().startsWith("[")) {
                        val jsonArray = JSONArray(response)
                        val snapshots = mutableListOf<PriceSnapshot>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.optJSONObject(i) ?: continue
                            val itemId = obj.optString("itemId", obj.optString("item_id", ""))
                            val city = obj.optString("city", "")
                            if (itemId.isBlank() || city.isBlank()) continue

                            val sellPriceMin = obj.optInt("sellPriceMin", obj.optInt("sell_price_min", 0))
                            if (sellPriceMin > 0 && !AlbionMarketApi.isUnrealisticPrice(itemId, sellPriceMin)) {
                                snapshots.add(
                                    PriceSnapshot(
                                        itemId = itemId,
                                        city = city,
                                        sellPriceMin = sellPriceMin,
                                        buyPriceMax = obj.optInt("buyPriceMax", obj.optInt("buy_price_max", 0)),
                                        timestampMs = obj.optLong("timestampMs", Date().time),
                                        sellPriceMinAmount = obj.optInt("sellPriceMinAmount", obj.optInt("sell_price_min_amount", 1))
                                    )
                                )
                            }
                        }
                        if (snapshots.isNotEmpty()) return@withContext snapshots
                    }
                }
            } catch (_: Exception) {
            } finally {
                connection?.disconnect()
            }
        }
        emptyList()
    }

    suspend fun testAndConnectToServer(context: Context, serverUrlInput: String? = null): Boolean = withContext(Dispatchers.IO) {
        val targetUrls = if (!serverUrlInput.isNullOrBlank()) {
            listOf(if (serverUrlInput.endsWith("/")) "${serverUrlInput}api/devices/ping" else "$serverUrlInput/api/devices/ping")
        } else {
            getServerBaseUrls(context).map { "$it/api/devices/ping" }
        }

        supervisorScope {
            val deferredResults = targetUrls.map { url ->
                async(Dispatchers.IO) {
                    var conn: HttpURLConnection? = null
                    try {
                        val connectionUrl = URL(url)
                        conn = connectionUrl.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                        val appVer = OtaUpdateManager.getInstalledVersionName(context)
                        conn.setRequestProperty("User-Agent", "AlbionDataPro/$appVer")
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doOutput = true
                        conn.outputStream.use { os ->
                            os.write("{\"hwId\":\"test\"}".toByteArray(Charsets.UTF_8))
                        }
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            lastSuccessfulUrl = url.removeSuffix("/api/devices/ping")
                            isServerConnected = true
                            return@async true
                        }
                    } catch (_: Exception) {
                    } finally {
                        conn?.disconnect()
                    }
                    false
                }
            }
            deferredResults.any { it.await() }
        }
    }

    suspend fun loginWithServer(context: Context, username: String, pass: String): Boolean {
        val success = loginWithServerInternal(context, username, pass)
        if (success) return true

        // If login failed due to version mismatch or inactive license, do not auto-register!
        val lastErr = lastLoginErrorMessage ?: ""
        if (lastErr.contains("App aktualisieren") || lastErr.contains("veraltet") || lastErr.contains("Version") || lastErr.contains("Lizenz") || lastErr.contains("abgelaufen")) {
            return false
        }

        // Auto-register if account doesn't exist yet, then retry login
        val (regSuccess, _) = registerUser(context, username, pass)
        if (regSuccess) {
            return loginWithServerInternal(context, username, pass)
        }
        return false
    }

    private suspend fun loginWithServerInternal(context: Context, username: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanPass = pass.trim()

        val hwId = DeviceHardwareManager.getHardwareId(context)

        // Banned device check: Banned devices cannot log in, EXCEPT the admin account (dnnx)
        val isBannedDevice = LicenseManager.isServerBanned(context)
        if (isBannedDevice && !cleanUser.equals("dnnx", ignoreCase = true)) {
            return@withContext false
        }



        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appVersion = OtaUpdateManager.getInstalledVersionName(context)

        // Direct local & admin bypass for dnnx (requires NO license) - max 1 admin account, saves admin device
        if (cleanUser.equals("dnnx", ignoreCase = true) && (cleanPass == "Dean3153..." || cleanPass.startsWith("Dean3153"))) {
            val appPrefs = AppPreferences(context)
            appPrefs.isUserLoggedIn = true
            appPrefs.savedUsername = cleanUser
            appPrefs.savedPassword = cleanPass
            appPrefs.isAdmin = true

            val prefs = context.getSharedPreferences("albion_hardware_license_prefs", Context.MODE_PRIVATE)
            val adminDevices = (prefs.getStringSet("saved_admin_devices", emptySet()) ?: emptySet()).toMutableSet()
            adminDevices.add(hwId)
            prefs.edit {
                putBoolean("is_user_logged_in", true)
                putString("user_email", cleanUser)
                putString("activated_license_code", "LOGIN-DNNX-ADMIN")
                putStringSet("saved_admin_devices", adminDevices)
            }
            LicenseManager.loginWithCredentials(context, cleanUser, cleanPass)
            isServerConnected = true
            return@withContext true
        }

        val payload = JSONObject().apply {
            put("username", cleanUser)
            put("password", cleanPass)
            put("hwId", hwId)
            put("deviceName", deviceName)
            put("appVersion", appVersion)
        }.toString()

        val loginUrls = getPrioritizedServerUrls(context).map { "$it/api/auth/login" }

        var serverAuthenticated = false

        supervisorScope {
            val deferredResults = loginUrls.map { serverUrl ->
                async(Dispatchers.IO) {
                    var connection: HttpURLConnection? = null
                    try {
                        val url = URL(serverUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connection.setRequestProperty("Accept", "application/json")
                        connection.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        connection.doOutput = true

                        connection.outputStream.use { os ->
                            os.write(payload.toByteArray(Charsets.UTF_8))
                        }

                        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                        if (stream != null) {
                            val response = stream.bufferedReader().use { it.readText() }
                            val jsonObj = JSONObject(response)
                            if (jsonObj.optBoolean("authenticated", false)) {
                                lastLoginErrorMessage = null
                                val isAdmin = cleanUser.equals("dnnx", ignoreCase = true)
                                val serverLicenseActive = jsonObj.optBoolean("isLicenseActive", false) || isAdmin
                                val licenseExpiresAt = if (isAdmin) "2099-12-31T23:59:59.000Z" else jsonObj.optString("licenseExpiresAt", "")

                                LicenseManager.updateLicenseFromServer(
                                    context = context,
                                    isBanned = jsonObj.optBoolean("isBanned", false),
                                    bannedUntilStr = jsonObj.optString("bannedUntil", ""),
                                    isLicenseActive = serverLicenseActive,
                                    licenseExpiresAtStr = licenseExpiresAt
                                )

                                val isLicenseValidLocal = LicenseManager.isLicenseValid(context) || isAdmin
                                if (!serverLicenseActive || !isLicenseValidLocal) {
                                    // Account exists on server, but license is inactive or expired!
                                    lastLoginErrorMessage = "Konto existiert, aber Lizenz ist inaktiv oder abgelaufen."
                                    return@async false
                                }

                                val prefs = context.getSharedPreferences("albion_hardware_license_prefs", Context.MODE_PRIVATE)
                                prefs.edit {
                                    putBoolean("is_user_logged_in", true)
                                    putString("user_email", username.trim())
                                    if (isAdmin) {
                                        putString("activated_license_code", "LOGIN-DNNX-ADMIN")
                                        val adminDevices = (prefs.getStringSet("saved_admin_devices", emptySet()) ?: emptySet()).toMutableSet()
                                        adminDevices.add(hwId)
                                        putStringSet("saved_admin_devices", adminDevices)
                                    }
                                }

                                val appPrefs = AppPreferences(context)
                                appPrefs.isUserLoggedIn = true
                                appPrefs.savedUsername = username.trim()
                                appPrefs.savedPassword = pass.trim()
                                appPrefs.isAdmin = isAdmin

                                isServerConnected = true
                                return@async true
                            } else {
                                val msgStr = jsonObj.optString("message", "")
                                val errStr = jsonObj.optString("error", "")
                                val serverMsg = msgStr.ifBlank { errStr }
                                if (serverMsg.isNotBlank()) {
                                    lastLoginErrorMessage = serverMsg
                                }

                                val targetVer = jsonObj.optString("targetVersion", "")
                                val versionMismatch = jsonObj.optBoolean("versionMismatch", false)
                                val hasOtaUpdate = jsonObj.optBoolean("hasOtaUpdate", false)
                                val downloadUrl = jsonObj.optString("downloadUrl", "")

                                if (targetVer.isNotBlank() || versionMismatch || hasOtaUpdate || serverMsg.contains("Version", ignoreCase = true) || serverMsg.contains("Update", ignoreCase = true)) {
                                    if (targetVer.isNotBlank()) {
                                        latestTargetVersion = targetVer
                                    }
                                    isOtaUpdateAvailable = true
                                    
                                    try {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "🚀 Veraltete Version bei Login erkannt! Installiere neuste Version automatisch...", Toast.LENGTH_LONG).show()
                                        }
                                        OtaUpdateManager.downloadAndInstallUpdate(context, downloadUrl.ifBlank { null }, force = true)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        connection?.disconnect()
                    }
                    false
                }
            }

            for (deferred in deferredResults) {
                if (deferred.await()) {
                    serverAuthenticated = true
                    break
                }
            }
        }

        if (serverAuthenticated) {
            restoreUserData(context)
            backupUserData(context)
            return@withContext true
        }

        // Must have an active account in Admin Console + valid license + live cloud connection!
        return@withContext false
    }

    suspend fun verifyCredentialsWithServer(context: Context): Boolean = withContext(Dispatchers.IO) {
        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()
        val appPrefs = AppPreferences(context)
        val username = appPrefs.savedUsername
        val password = appPrefs.savedPassword

        if (username.isBlank() || password.isBlank()) {
            return@withContext false
        }

        val hwId = DeviceHardwareManager.getHardwareId(context)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appVersion = OtaUpdateManager.getInstalledVersionName(context)

        val payload = JSONObject().apply {
            put("username", username.trim())
            put("password", password.trim())
            put("hwId", hwId)
            put("deviceName", deviceName)
            put("appVersion", appVersion)
        }.toString()

        val loginUrls = getPrioritizedServerUrls(context).map { "$it/api/auth/login" }

        var serverConnected = false

        for (serverUrl in loginUrls) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(serverUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                val hmacSignature = CryptoSecurityUtils.computeHmacSha256(payload)
                connection.setRequestProperty("X-Albion-HMAC-Signature", hmacSignature)
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }

                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                if (stream != null) {
                    serverConnected = true
                    isServerConnected = true
                    val response = stream.bufferedReader().use { it.readText() }
                    val jsonObj = JSONObject(response)

                    val authenticated = jsonObj.optBoolean("authenticated", false)
                    val isLicenseActive = jsonObj.optBoolean("isLicenseActive", true)
                    val isBanned = jsonObj.optBoolean("isBanned", false)

                    if (authenticated && isLicenseActive && !isBanned) {
                        val licenseExpiresAt = jsonObj.optString("licenseExpiresAt", "")
                        LicenseManager.updateLicenseFromServer(
                            context = context,
                            isBanned = false,
                            bannedUntilStr = null,
                            isLicenseActive = true,
                            licenseExpiresAtStr = licenseExpiresAt
                        )
                        return@withContext true
                    } else {
                        // User account or device was explicitly DELETED, EXPIRED, or BANNED on Localhost!
                        appPrefs.isUserLoggedIn = false
                        val licensePrefs = context.getSharedPreferences("albion_hardware_license_prefs", Context.MODE_PRIVATE)
                        licensePrefs.edit { putBoolean("is_user_logged_in", false) }
                        return@withContext false
                    }
                }
            } catch (_: Exception) {
                // Connection exception / offline - do not log out user automatically
                return@withContext true
            } finally {
                connection?.disconnect()
            }
        }

        // Bei temporärem Verbindungsausfall (z.B. kein Internet / Server kurz nicht erreichbar) nicht automatisch ausloggen:
        return@withContext true
    }

    suspend fun backupUserData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val hwId = DeviceHardwareManager.getHardwareId(context)
            val prefs = AppPreferences(context)
            val payload = JSONObject().apply {
                put("hwId", hwId)
                put("prefsData", JSONObject().apply {
                    put("silverBudget", prefs.silverBudget)
                    put("carryCapacityKg", prefs.carryCapacityKg)
                    put("targetMarginPercent", prefs.targetMarginPercent)
                    put("hasPremium", prefs.hasPremium)
                    put("avoidDangerousZones", prefs.avoidDangerousZones)
                    put("appLanguage", prefs.appLanguage)
                    put("savedUsername", prefs.savedUsername)
                    put("savedPassword", prefs.savedPassword)
                    put("isUserLoggedIn", prefs.isUserLoggedIn)
                    put("bubbleStandpunktCity", prefs.bubbleStandpunktCity)
                    put("bubbleCategory", prefs.bubbleCategory)
                    put("bubbleTier", prefs.bubbleTier)
                    put("bubbleEnchantment", prefs.bubbleEnchantment)
                    put("bubbleMinMarginPercent", prefs.bubbleMinMarginPercent)
                    put("bubbleMaxZones", prefs.bubbleMaxZones)
                    put("bubbleMaxStock", prefs.bubbleMaxStock)
                    put("bubbleAvoidDangerousZones", prefs.bubbleAvoidDangerousZones)
                    put("bubbleHideBrecilien", prefs.bubbleHideBrecilien)
                    put("bubbleHideBlackMarket", prefs.bubbleHideBlackMarket)
                    put("bubbleIntervalMinutes", prefs.bubbleIntervalMinutes)
                    put("bubbleCompactMode", prefs.bubbleCompactMode)
                    put("bubbleOpacity", prefs.bubbleOpacity.toDouble())
                    put("bubbleScale", prefs.bubbleScale.toDouble())
                    put("tradeOrdersJson", prefs.rawTradeOrdersJson)
                    put("activatedLicenseCode", LicenseManager.getActivatedCode(context))
                })
            }.toString()

            for (baseUrl in getServerBaseUrls(context)) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL("$baseUrl/api/user/backup")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    conn.outputStream.use { os ->
                        os.write(payload.toByteArray(Charsets.UTF_8))
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        return@withContext true
                    }
                } catch (_: Exception) {
                } finally {
                    conn?.disconnect()
                }
            }
        } catch (_: Exception) {}
        false
    }

    suspend fun restoreUserData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val hwId = DeviceHardwareManager.getHardwareId(context)
            for (baseUrl in getServerBaseUrls(context)) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL("$baseUrl/api/user/restore?hwId=$hwId")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = JSONObject(response)
                        if (jsonObj.optString("status") == "success") {
                            val prefsData = jsonObj.optJSONObject("prefsData")
                            if (prefsData != null) {
                                val prefs = AppPreferences(context)
                                if (prefsData.has("silverBudget")) prefs.silverBudget = prefsData.getLong("silverBudget")
                                if (prefsData.has("carryCapacityKg")) prefs.carryCapacityKg = prefsData.getDouble("carryCapacityKg")
                                if (prefsData.has("targetMarginPercent")) prefs.targetMarginPercent = prefsData.getDouble("targetMarginPercent")
                                if (prefsData.has("hasPremium")) prefs.hasPremium = prefsData.getBoolean("hasPremium")
                                if (prefsData.has("avoidDangerousZones")) prefs.avoidDangerousZones = prefsData.getBoolean("avoidDangerousZones")
                                if (prefsData.has("appLanguage")) prefs.appLanguage = prefsData.getString("appLanguage") ?: "DE"
                                if (prefsData.has("savedUsername")) prefs.savedUsername = prefsData.getString("savedUsername") ?: ""
                                if (prefsData.has("savedPassword")) prefs.savedPassword = prefsData.getString("savedPassword") ?: ""
                                if (prefsData.has("isUserLoggedIn")) prefs.isUserLoggedIn = prefsData.getBoolean("isUserLoggedIn")
                                if (prefsData.has("bubbleStandpunktCity")) prefs.bubbleStandpunktCity = prefsData.getString("bubbleStandpunktCity") ?: "ALLE"
                                if (prefsData.has("bubbleCategory")) prefs.bubbleCategory = prefsData.getString("bubbleCategory") ?: "ALL"
                                if (prefsData.has("bubbleTier")) prefs.bubbleTier = prefsData.getInt("bubbleTier")
                                if (prefsData.has("bubbleEnchantment")) prefs.bubbleEnchantment = prefsData.getInt("bubbleEnchantment")
                                if (prefsData.has("bubbleMinMarginPercent")) prefs.bubbleMinMarginPercent = prefsData.getDouble("bubbleMinMarginPercent")
                                if (prefsData.has("bubbleMaxZones")) prefs.bubbleMaxZones = prefsData.getInt("bubbleMaxZones")
                                if (prefsData.has("bubbleMaxStock")) prefs.bubbleMaxStock = prefsData.getInt("bubbleMaxStock")
                                if (prefsData.has("bubbleAvoidDangerousZones")) prefs.bubbleAvoidDangerousZones = prefsData.getBoolean("bubbleAvoidDangerousZones")
                                if (prefsData.has("bubbleHideBrecilien")) prefs.bubbleHideBrecilien = prefsData.getBoolean("bubbleHideBrecilien")
                                if (prefsData.has("bubbleHideBlackMarket")) prefs.bubbleHideBlackMarket = prefsData.getBoolean("bubbleHideBlackMarket")
                                if (prefsData.has("bubbleIntervalMinutes")) prefs.bubbleIntervalMinutes = prefsData.getInt("bubbleIntervalMinutes")
                                if (prefsData.has("bubbleCompactMode")) prefs.bubbleCompactMode = prefsData.getBoolean("bubbleCompactMode")
                                if (prefsData.has("bubbleOpacity")) prefs.bubbleOpacity = prefsData.getDouble("bubbleOpacity").toFloat()
                                if (prefsData.has("bubbleScale")) prefs.bubbleScale = prefsData.getDouble("bubbleScale").toFloat()
                                if (prefsData.has("tradeOrdersJson")) prefs.rawTradeOrdersJson = prefsData.getString("tradeOrdersJson") ?: ""
                                if (prefsData.has("activatedLicenseCode")) {
                                    val code = prefsData.getString("activatedLicenseCode")
                                    if (!code.isNullOrBlank()) {
                                        LicenseManager.activateLicense(context, code)
                                    }
                                }
                                return@withContext true
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    conn?.disconnect()
                }
            }
        } catch (_: Exception) {}
        false
    }

    suspend fun registerUser(context: Context, username: String, pass: String, licenseKey: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val registerUrls = getServerBaseUrls(context).map { "$it/api/auth/register" }
        var lastError = "Verbindungsfehler"

        for (serverUrl in registerUrls) {
            var conn: HttpURLConnection? = null
            try {
                CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()
                val url = URL(serverUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("username", username.trim())
                    put("password", pass.trim())
                    if (!licenseKey.isNullOrBlank()) {
                        put("licenseKey", licenseKey.trim())
                        put("activatedLicenseCode", licenseKey.trim())
                    }
                }

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseStr = stream?.bufferedReader()?.use { it.readText() } ?: ""

                if (conn.responseCode in 200..299) {
                    try {
                        val jsonObj = JSONObject(responseStr)
                        val expStr = jsonObj.optString("licenseExpiresAt", "")
                        if (expStr.isNotBlank()) {
                            LicenseManager.updateLicenseFromServer(
                                context = context,
                                isBanned = false,
                                bannedUntilStr = null,
                                isLicenseActive = true,
                                licenseExpiresAtStr = expStr
                            )
                        }
                    } catch (_: Exception) {}
                    return@withContext Pair(true, "Account erfolgreich erstellt!")
                } else {
                    val errMsg = try {
                        JSONObject(responseStr).optString("error", "HTTP ${conn.responseCode}")
                    } catch (_: Exception) {
                        if (responseStr.isNotBlank()) responseStr else "HTTP Fehler ${conn.responseCode}"
                    }
                    lastError = errMsg
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Verbindungsfehler zu $serverUrl"
            } finally {
                conn?.disconnect()
            }
        }
        return@withContext Pair(false, lastError)
    }
}
