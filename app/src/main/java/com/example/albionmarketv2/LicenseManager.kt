package com.example.albionmarketv2

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object LicenseManager {

    private const val PREFS_NAME = "albion_hardware_license_prefs"
    private const val KEY_ACTIVATED_CODE = "activated_license_code"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_PLAY_STORE_SUB_ACTIVE = "play_store_sub_active"
    private const val KEY_PURCHASE_TOKEN = "play_store_purchase_token"

    const val MONTHLY_PRICE_EUR = "15,00 € / Monat"
    const val TRIAL_DURATION_HOURS = 0
    const val TRIAL_DURATION_MS = 0L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHardwareId(context: Context): String {
        return DeviceHardwareManager.getHardwareId(context)
    }

    fun getActivatedCode(context: Context): String {
        val appPrefs = AppPreferences(context)
        if (appPrefs.isAdmin || appPrefs.savedUsername.equals("dnnx", ignoreCase = true)) {
            return "👑 Admin (Keine Lizenz erforderlich / Unbegrenzt)"
        }
        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)
        val code = prefs.getString(KEY_ACTIVATED_CODE, "") ?: ""
        if (code.isNotBlank()) return code
        val lifetimeDevices = prefs.getString("admin_lifetime_devices", "") ?: ""
        if (lifetimeDevices.contains(hwId)) return "ALBION-PRO-LIFETIME (Admin dnnx)"
        return "Standard-Lizenz ($hwId)"
    }

    private fun getPersistentFiles(context: Context): List<File> {
        val files = mutableListOf<File>()
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null) files.add(File(downloadDir, ".albion_hw_trial_v2.json"))

            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docDir != null) files.add(File(docDir, ".albion_hw_trial_v2.json"))

            files.add(File("/sdcard/Download/.albion_hw_trial_v2.json"))
            files.add(File("/sdcard/Documents/.albion_hw_trial_v2.json"))

            val extFiles = context.getExternalFilesDir(null)
            if (extFiles != null) {
                val rootStorage = extFiles.parentFile?.parentFile?.parentFile?.parentFile
                if (rootStorage != null) {
                    files.add(File(rootStorage, ".albion_hw_trial_v2.json"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return files.distinct()
    }

    private fun readDiskTrialTimestamp(context: Context, hwId: String): Long {
        for (file in getPersistentFiles(context)) {
            try {
                if (file.exists() && file.canRead()) {
                    val content = file.readText()
                    val json = JSONObject(content)
                    if (json.has(hwId)) {
                        val ts = json.getLong(hwId)
                        if (ts > 0L) return ts
                    }
                }
            } catch (e: Exception) {
                // Continue checking next location
            }
        }
        return 0L
    }

    private fun writeDiskTrialTimestamp(context: Context, hwId: String, timestampMs: Long) {
        for (file in getPersistentFiles(context)) {
            try {
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }

                val json = if (file.exists() && file.canRead()) {
                    try { JSONObject(file.readText()) } catch (e: Exception) { JSONObject() }
                } else JSONObject()

                json.put(hwId, timestampMs)
                json.put("${hwId}_scanned_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).format(Date()))

                file.writeText(json.toString())
            } catch (e: Exception) {
                // Ignore individual file write failures
            }
        }
    }

    fun getTrialStartTimestampMs(context: Context): Long {
        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)
        val key = "trial_start_$hwId"

        val spTimestamp = prefs.getLong(key, 0L)
        val diskTimestamp = readDiskTrialTimestamp(context, hwId)

        var finalTimestamp = 0L
        if (diskTimestamp > 0L && (spTimestamp <= 0L || diskTimestamp < spTimestamp)) {
            finalTimestamp = diskTimestamp
        } else if (spTimestamp > 0L) {
            finalTimestamp = spTimestamp
        } else {
            finalTimestamp = System.currentTimeMillis()
        }

        if (spTimestamp != finalTimestamp) {
            prefs.edit().putLong(key, finalTimestamp).apply()
        }
        writeDiskTrialTimestamp(context, hwId, finalTimestamp)

        return finalTimestamp
    }

    fun isTrialActive(context: Context): Boolean {
        return false
    }

    fun getRemainingTrialTimeFormatted(context: Context): String {
        return "Kein Testzeitraum aktiv (Lizenz erforderlich)"
    }

    fun isLicenseValid(context: Context): Boolean {
        val appPrefs = AppPreferences(context)
        if (appPrefs.isAdmin || appPrefs.savedUsername.equals("dnnx", ignoreCase = true)) {
            return true
        }

        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)

        // 1. If server banned, license is invalid
        if (isServerBanned(context)) {
            return false
        }

        // 2. Admin or Lifetime license check
        val lifetimeDevices = prefs.getString("admin_lifetime_devices", "") ?: ""
        val code = prefs.getString(KEY_ACTIVATED_CODE, "") ?: ""
        if (lifetimeDevices.contains(hwId) || code.startsWith("LOGIN-DNNX-ADMIN") || code.contains("LIFETIME")) {
            return true
        }

        // 3. Play Store active subscription check
        if (isPlayStoreSubActive(context)) {
            return true
        }

        // 4. Server-assigned or activated license expiration check
        val exp = prefs.getLong("license_exp_$hwId", 0L)
        if (exp > 0L) {
            return System.currentTimeMillis() < exp
        }

        // 5. Free Trial period check
        return isTrialActive(context)
    }

    fun setPlayStoreSubscriptionActive(context: Context, purchaseToken: String) {
        getPrefs(context).edit()
            .putBoolean(KEY_PLAY_STORE_SUB_ACTIVE, true)
            .putString(KEY_PURCHASE_TOKEN, purchaseToken)
            .apply()
    }

    fun isPlayStoreSubActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PLAY_STORE_SUB_ACTIVE, false)
    }

    private fun parseIsoDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val cleanStr = dateStr.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(cleanStr)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    fun updateLicenseFromServer(context: Context, isBanned: Boolean, bannedUntilStr: String?, isLicenseActive: Boolean, licenseExpiresAtStr: String?) {
        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)

        prefs.edit()
            .putBoolean("server_banned_$hwId", isBanned)
            .putString("server_banned_until_$hwId", bannedUntilStr ?: "")
            .apply()

        if (licenseExpiresAtStr != null && licenseExpiresAtStr.isNotBlank()) {
            val dateMs = parseIsoDateToMillis(licenseExpiresAtStr)
            if (dateMs > 0L) {
                prefs.edit().putLong("license_exp_$hwId", dateMs).apply()
            }
        }
    }

    fun isServerBanned(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)
        return prefs.getBoolean("server_banned_$hwId", false)
    }

    fun getExpirationDateString(context: Context): String {
        val appPrefs = AppPreferences(context)
        if (appPrefs.isAdmin || appPrefs.savedUsername.equals("dnnx", ignoreCase = true)) {
            return "👑 Admin-Konto (Keine Lizenz erforderlich / Unbegrenzt)"
        }

        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)

        if (isServerBanned(context)) {
            val bannedUntil = prefs.getString("server_banned_until_$hwId", "")
            return "🚫 Gerät vom Server gebannt${if (!bannedUntil.isNullOrEmpty()) " bis: $bannedUntil" else ""}"
        }

        val lifetimeDevices = prefs.getString("admin_lifetime_devices", "") ?: ""
        if (lifetimeDevices.contains(hwId)) {
            return "👑 Admin / Lifetime Lizenz (Lebenslang - Aktiv auf bis zu 3 Geräten)"
        }
        if (isPlayStoreSubActive(context)) {
            return "Google Play Monat-Abo (Aktiv - 15,00 €/Monat)"
        }
        val exp = prefs.getLong("license_exp_$hwId", 0L)
        if (System.currentTimeMillis() < exp) {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
            return "Server-Lizenz aktiv bis: ${sdf.format(Date(exp))}"
        }
        if (isTrialActive(context)) {
            return "Kostenloser Testzeitraum (${getRemainingTrialTimeFormatted(context)})"
        }
        return "Testzeitraum & Lizenz abgelaufen (Hardware ID: $hwId)"
    }

    fun getSavedUserEmail(context: Context): String {
        return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun saveUserEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_USER_EMAIL, email.trim()).apply()
    }

    fun activateLicense(context: Context, code: String): Boolean {
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        if (cleanCode.isBlank()) return false
        val prefs = getPrefs(context)
        val hwId = getHardwareId(context)
        val appPrefs = AppPreferences(context)

        if (cleanCode.startsWith("LOGIN-DNNX-ADMIN") || cleanCode == "DNNX") {
            prefs.edit().putString(KEY_ACTIVATED_CODE, "LOGIN-DNNX-ADMIN").apply()
            appPrefs.isUserLoggedIn = true
            appPrefs.savedUsername = "dnnx"
            appPrefs.isAdmin = true
            return true
        }

        if (cleanCode.startsWith("ALBION-3M-")) {
            val exp = System.currentTimeMillis() + (90L * 24L * 3600L * 1000L)
            prefs.edit().putLong("license_exp_$hwId", exp).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        if (cleanCode.startsWith("ALBION-6M-")) {
            val exp = System.currentTimeMillis() + (180L * 24L * 3600L * 1000L)
            prefs.edit().putLong("license_exp_$hwId", exp).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        if (cleanCode.startsWith("ALBION-12M-")) {
            val exp = System.currentTimeMillis() + (365L * 24L * 3600L * 1000L)
            prefs.edit().putLong("license_exp_$hwId", exp).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        if (cleanCode.startsWith("ALBION-LIFETIME") || cleanCode == "ALBION-PRO-LIFETIME") {
            val devicesStr = prefs.getString("admin_lifetime_devices", "") ?: ""
            val list = devicesStr.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (!list.contains(hwId) && list.size >= 3) {
                return false // Max 3 devices reached for Admin / Lifetime license
            }
            list.add(hwId)
            prefs.edit().putString("admin_lifetime_devices", list.joinToString(",")).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        if (cleanCode == "ALBION-TEST-2026") {
            val exp = System.currentTimeMillis() + TRIAL_DURATION_MS
            prefs.edit().putLong("license_exp_$hwId", exp).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        if (cleanCode.startsWith("ALBION-1M-")) {
            val exp = System.currentTimeMillis() + (30L * 24L * 3600L * 1000L)
            prefs.edit().putLong("license_exp_$hwId", exp).putString(KEY_ACTIVATED_CODE, cleanCode).apply()
            appPrefs.isUserLoggedIn = true
            return true
        }
        return false
    }

    fun loginWithCredentials(context: Context, user: String, pass: String): Boolean {
        val cleanUser = user.trim()
        val cleanPass = pass.trim()
        if (cleanUser.isBlank() || cleanPass.isBlank()) return false

        val appPrefs = AppPreferences(context)

        // Admin check (dnnx requires NO license)
        if (cleanUser.equals("dnnx", ignoreCase = true) && (cleanPass == "Dean3153..." || cleanPass.startsWith("Dean3153"))) {
            val prefs = getPrefs(context)
            prefs.edit().putString(KEY_ACTIVATED_CODE, "LOGIN-DNNX-ADMIN").apply()
            appPrefs.isUserLoggedIn = true
            appPrefs.savedUsername = cleanUser
            appPrefs.savedPassword = cleanPass
            appPrefs.isAdmin = true
            return true
        }

        // Check if license is active and not expired for standard users
        if (!isLicenseValid(context)) {
            return false
        }

        // Standard pre-defined accounts or saved device account check
        val knownUsers = mapOf(
            "dnnx" to "Dean3153...",
            "user" to "user123",
            "lol" to "lol",
            "testuser2026" to "Password123!"
        )

        val expectedPass = knownUsers[cleanUser.lowercase(Locale.ROOT)]
        val matchesKnown = expectedPass != null && expectedPass.equals(cleanPass, ignoreCase = true)
        val matchesSaved = appPrefs.savedUsername.isNotBlank() &&
                           appPrefs.savedUsername.equals(cleanUser, ignoreCase = true) &&
                           appPrefs.savedPassword.isNotBlank() &&
                           appPrefs.savedPassword.equals(cleanPass, ignoreCase = true)

        if (matchesKnown || matchesSaved) {
            val prefs = getPrefs(context)
            prefs.edit().putString(KEY_ACTIVATED_CODE, "USER-$cleanUser").apply()
            appPrefs.isUserLoggedIn = true
            appPrefs.savedUsername = cleanUser
            appPrefs.savedPassword = cleanPass
            return true
        }

        // Username / password combination does NOT exist! Access denied!
        return false
    }



    fun generateLicenseCode(email: String): String {
        val hash = (email.trim().lowercase(Locale.ROOT).hashCode() and 0xFFFF).toString(16).uppercase(Locale.ROOT).padStart(4, '0')
        val randomPart = UUID.randomUUID().toString().substring(0, 4).uppercase(Locale.ROOT)
        return "ALBION-1M-$hash-$randomPart"
    }
}
