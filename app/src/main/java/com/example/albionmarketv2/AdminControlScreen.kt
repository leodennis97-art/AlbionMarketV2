package com.example.albionmarketv2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

data class AdminUser(
    val username: String,
    val password: String = "••••••••",
    val isAdmin: Boolean,
    val isLicensed: Boolean,
    val licenseExpiresAt: String,
)

data class AdminLicense(
    val key: String,
    val tier: String,
    val price: String,
    val created: String,
    val note: String,
)

const val CURRENT_APP_VERSION = "3.1.8"

data class AdminDevice(
    val hwId: String,
    val deviceName: String,
    val appVersion: String,
    val username: String,
    val lastSeen: String,
    val licenseExpiresAt: String,
    val isBanned: Boolean,
    val banReason: String = "",
    val unbanned: Boolean = false,
)

// Sealed response result for high-end error handling
sealed class AdminApiResult<out T> {
    data class Success<out T>(val data: T) : AdminApiResult<T>()
    data class Error(val message: String, val statusCode: Int = -1) : AdminApiResult<Nothing>()
}

// Persistent Audit Log Manager for tracking Administrative Events
object AdminAuditLogManager {
    private const val PREF_NAME = "albion_admin_audit_logs"
    private const val KEY_LOGS = "audit_log_entries"

    fun logAction(context: Context, action: String) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val currentJson = prefs.getString(KEY_LOGS, "[]") ?: "[]"
            val arr = JSONArray(currentJson)

            val timeFormat = SimpleDateFormat("HH:mm:ss - dd.MM", Locale.getDefault())
            val timestamp = timeFormat.format(Date())
            val entry = "$timestamp | $action"

            val newArr = JSONArray()
            newArr.put(entry)
            for (i in 0 until minOf(arr.length(), 49)) {
                newArr.put(arr.getString(i))
            }
            prefs.edit { putString(KEY_LOGS, newArr.toString()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val currentJson = prefs.getString(KEY_LOGS, "[]") ?: "[]"
            val arr = JSONArray(currentJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_LOGS) }
    }
}

object AdminControlManager {

    private const val ADMIN_API_KEY = "AlbionDataPro_Military_Admin_SuperSecret_2026#Key"

    private fun getBaseUrl(context: Context): String {
        return "https://albionmarketv2-1.onrender.com"
    }

    private suspend fun postJson(baseUrl: String, endpoint: String, json: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        val urlsToTry = listOf("https://albionmarketv2-1.onrender.com", baseUrl).distinct()
        for (base in urlsToTry) {
            try {
                val url = URL("$base$endpoint")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "AlbionDataPro-AdminApp/$CURRENT_APP_VERSION")
                conn.setRequestProperty("Authorization", "Bearer $ADMIN_API_KEY")
                conn.setRequestProperty("X-Admin-Key", ADMIN_API_KEY)
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode in 200..299) {
                    val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                    return@withContext JSONObject(resStr)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    private suspend fun getJsonArray(baseUrl: String, endpoint: String, jsonKey: String = ""): JSONArray? = withContext(Dispatchers.IO) {
        val urlsToTry = listOf("https://albionmarketv2-1.onrender.com", baseUrl).distinct()
        for (base in urlsToTry) {
            try {
                val url = URL("$base$endpoint")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "AlbionDataPro-AdminApp/$CURRENT_APP_VERSION")
                conn.setRequestProperty("Authorization", "Bearer $ADMIN_API_KEY")
                conn.setRequestProperty("X-Admin-Key", ADMIN_API_KEY)
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode in 200..299) {
                    val resStr = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (resStr.startsWith("[")) {
                        return@withContext JSONArray(resStr)
                    } else if (resStr.startsWith("{")) {
                        val jsonObj = JSONObject(resStr)
                        if (jsonKey.isNotBlank() && jsonObj.has(jsonKey)) {
                            val v = jsonObj.opt(jsonKey)
                            if (v is JSONArray) return@withContext v
                        }
                        for (k in listOf("users", "licenses", "devices", "data", "list", "items", "result")) {
                            if (jsonObj.has(k)) {
                                val v = jsonObj.opt(k)
                                if (v is JSONArray) return@withContext v
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    suspend fun fetchUsersWithStatus(context: Context): AdminApiResult<List<AdminUser>> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/users", "users")
            ?: return AdminApiResult.Error("Verbindung zum Server fehlgeschlagen (/api/users)")
        val list = mutableListOf<AdminUser>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                AdminUser(
                    username = obj.optString("username", "Unbekannt") ?: "Unbekannt",
                    password = obj.optString("password", "••••••••") ?: "••••••••",
                    isAdmin = obj.optBoolean("isAdmin", false),
                    isLicensed = obj.optBoolean("isLicensed", true),
                    licenseExpiresAt = obj.optString("licenseExpiresAt", "") ?: "",
                )
            )
        }
        return AdminApiResult.Success(list)
    }

    suspend fun fetchLicensesWithStatus(context: Context): AdminApiResult<List<AdminLicense>> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/licenses", "licenses")
            ?: return AdminApiResult.Error("Verbindung zum Server fehlgeschlagen (/api/licenses)")
        val list = mutableListOf<AdminLicense>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                AdminLicense(
                    key = obj.optString("key", "") ?: "",
                    tier = obj.optString("tier", "1m") ?: "1m",
                    price = obj.optString("price", "15 €") ?: "15 €",
                    created = obj.optString("created", "") ?: "",
                    note = obj.optString("note", "") ?: ""
                )
            )
        }
        return AdminApiResult.Success(list)
    }

    suspend fun fetchDevicesWithStatus(context: Context): AdminApiResult<List<AdminDevice>> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/devices", "devices")
            ?: return AdminApiResult.Error("Verbindung zum Server fehlgeschlagen (/api/devices)")
        val list = mutableListOf<AdminDevice>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val bannedUntil = obj.optString("bannedUntil", "") ?: ""
            val unbanned = obj.optBoolean("unbanned", false)
            val serverIsBanned = obj.optBoolean("isBanned", false)
            val isBanned = serverIsBanned || (bannedUntil.isNotBlank() && !unbanned)
            val banReason = obj.optString("banReason", "") ?: ""
            list.add(
                AdminDevice(
                    hwId = obj.optString("hwId", "") ?: "",
                    deviceName = obj.optString("deviceName", "Android Device") ?: "Android Device",
                    appVersion = obj.optString("appVersion", CURRENT_APP_VERSION) ?: CURRENT_APP_VERSION,
                    username = obj.optString("username", "Unbekannt") ?: "Unbekannt",
                    lastSeen = obj.optString("lastSeen", "") ?: "",
                    licenseExpiresAt = obj.optString("licenseExpiresAt", "") ?: "",
                    isBanned = isBanned,
                    banReason = banReason,
                    unbanned = unbanned
                )
            )
        }
        return AdminApiResult.Success(list)
    }



    suspend fun createUserWithStatus(context: Context, user: String, pass: String): AdminApiResult<Unit> = withContext(Dispatchers.IO) {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }
        val res = postJson(base, "/api/admin/user/create", json)
        if (res != null) {
            val status = res.optString("status")
            val error = res.optString("error")
            if (status == "success" || res.has("message") || res.has("user")) {
                AdminAuditLogManager.logAction(context, "👤 Benutzer '$user' angelegt")
                return@withContext AdminApiResult.Success(Unit)
            } else if (error.isNotBlank()) {
                return@withContext AdminApiResult.Error(error, -1)
            }
        }
        
        // Fallback direct connection check if postJson returned null due to non-2xx or exception
        try {
            val url = URL("$base/api/admin/user/create")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "AlbionDataPro-AdminApp/$CURRENT_APP_VERSION")
            conn.setRequestProperty("Authorization", "Bearer $ADMIN_API_KEY")
            conn.setRequestProperty("X-Admin-Key", ADMIN_API_KEY)
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                AdminAuditLogManager.logAction(context, "👤 Benutzer '$user' angelegt")
                return@withContext AdminApiResult.Success(Unit)
            } else {
                val errStr = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (_: Exception) {
                    null
                }
                val errMsg = if (!errStr.isNullOrBlank()) {
                    try {
                        JSONObject(errStr).optString("error", "HTTP ${conn.responseCode}")
                    } catch (_: Exception) {
                        errStr
                    }
                } else {
                    "HTTP Fehler ${conn.responseCode}"
                }
                return@withContext AdminApiResult.Error(errMsg, conn.responseCode)
            }
        } catch (e: Exception) {
            return@withContext AdminApiResult.Error(e.localizedMessage ?: "Verbindungsfehler", -1)
        }
    }



    suspend fun deleteUser(context: Context, user: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("username", user) }
        val res = postJson(base, "/api/admin/user/delete", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🗑️ Benutzer '$user' gelöscht")
        }
        return ok
    }

    suspend fun generateLicense(context: Context, tier: String, note: String): String? {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("tier", tier)
            put("customerNote", note)
        }
        val res = postJson(base, "/api/admin/license/generate", json)
        val key = res?.optJSONObject("license")?.optString("key")
        if (key != null) {
            AdminAuditLogManager.logAction(context, "⚡ Lizenz '${tier.uppercase()}' generiert ($key)")
        }
        return key
    }

    suspend fun deleteLicense(context: Context, key: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("key", key) }
        val res = postJson(base, "/api/admin/license/delete", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🗑️ Lizenz '$key' gelöscht")
        }
        return ok
    }

    suspend fun banDevice(context: Context, hwId: String, banReason: String = "Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)"): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("hwId", hwId)
            put("banReason", banReason.ifBlank { "Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)" })
        }
        val res = postJson(base, "/api/admin/device/ban", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🔴 HWID '$hwId' gebannt")
        }
        return ok
    }

    suspend fun banUserDevices(context: Context, username: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("username", username) }
        val res = postJson(base, "/api/admin/user/ban", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🔴 Benutzer '$username' gebannt")
        }
        return ok
    }

    suspend fun unbanDevice(context: Context, hwId: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("hwId", hwId) }
        val res = postJson(base, "/api/admin/device/unban", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🟢 HWID '$hwId' entbannt")
        }
        return ok
    }

    suspend fun unbanAllDevices(context: Context): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject()
        val res = postJson(base, "/api/admin/device/unban-all", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🟢 Alle gebannten Geräte entbannt")
        }
        return ok
    }

    suspend fun deleteDevice(context: Context, hwId: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("hwId", hwId) }
        val res = postJson(base, "/api/admin/device/delete", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🗑️ Gerät HWID '$hwId' gelöscht")
        }
        return ok
    }

    suspend fun sendAlertMessage(context: Context, targetUsername: String?, hwId: String?, message: String, playAlarm: Boolean): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            if (!targetUsername.isNullOrBlank()) put("targetUsername", targetUsername.trim())
            if (!hwId.isNullOrBlank()) put("hwId", hwId.trim())
            put("message", message.trim())
            put("playAlarmSound", playAlarm)
        }
        val res = postJson(base, "/api/admin/send-alert", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            val targetStr = targetUsername ?: (hwId ?: "ALLE")
            AdminAuditLogManager.logAction(context, "📢 Alert an $targetStr gesendet")
        }
        return ok
    }

    suspend fun triggerOtaUpdateCommand(context: Context, hwId: String? = null, isGlobal: Boolean = false): Boolean {
        return triggerOtaUpdate(context, hwId, isGlobal)
    }

    suspend fun triggerOtaUpdate(context: Context, hwId: String? = null, isGlobal: Boolean = false): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            if (!hwId.isNullOrBlank()) put("hwId", hwId.trim())
            put("isGlobal", isGlobal)
        }
        val res = postJson(base, "/api/admin/trigger-ota", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🚀 OTA Update gesendet (Global=$isGlobal)")
        }
        return ok
    }

    suspend fun updateRemoteConfig(context: Context, minMargin: Double, maintenance: Boolean): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("minMarginPercent", minMargin)
            put("maintenanceMode", maintenance)
        }
        val res = postJson(base, "/api/admin/remote-config", json)
        val ok = res?.optString("status") == "success"
        if (ok) {
            AdminAuditLogManager.logAction(context, "🎛️ Remote Config gepusht (Margin: $minMargin%, Wartung: $maintenance)")
        }
        return ok
    }

    // High-End Multi-Endpoint Live Diagnostics Ping
    suspend fun runEndpointDiagnostics(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        val base = getBaseUrl(context)
        val endpoints = listOf("/api/users", "/api/licenses", "/api/devices", "/api/remote-config")
        val results = mutableMapOf<String, Long>()

        for (ep in endpoints) {
            val start = System.currentTimeMillis()
            try {
                val url = URL("$base$ep")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "AlbionDataPro-AdminApp/$CURRENT_APP_VERSION")
                conn.setRequestProperty("Authorization", "Bearer $ADMIN_API_KEY")
                conn.setRequestProperty("X-Admin-Key", ADMIN_API_KEY)
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.responseCode
                val duration = System.currentTimeMillis() - start
                results[ep] = duration
            } catch (_: Exception) {
                results[ep] = -1L
            }
        }
        results
    }

    // High-End Formatted Database State Exporter
    fun exportDatabaseDump(users: List<AdminUser>, licenses: List<AdminLicense>, devices: List<AdminDevice>): String {
        return try {
            val root = JSONObject().apply {
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("appVersion", CURRENT_APP_VERSION)
                put("totalUsers", users.size)
                put("totalLicenses", licenses.size)
                put("totalDevices", devices.size)

                val usersArr = JSONArray()
                users.forEach { u ->
                    usersArr.put(JSONObject().apply {
                        put("username", u.username)
                        put("isAdmin", u.isAdmin)
                        put("isLicensed", u.isLicensed)
                    })
                }
                put("users", usersArr)

                val licensesArr = JSONArray()
                licenses.forEach { l ->
                    licensesArr.put(JSONObject().apply {
                        put("key", l.key)
                        put("tier", l.tier)
                        put("note", l.note)
                    })
                }
                put("licenses", licensesArr)

                val devicesArr = JSONArray()
                devices.forEach { d ->
                    devicesArr.put(JSONObject().apply {
                        put("hwId", d.hwId)
                        put("deviceName", d.deviceName)
                        put("appVersion", d.appVersion)
                        put("isBanned", d.isBanned)
                    })
                }
                put("devices", devicesArr)
            }
            root.toString(2)
        } catch (e: Exception) {
            "Export error: ${e.message}"
        }
    }
}

// Helper to trigger haptic feedback and copy text to clipboard
private fun copyToClipboardWithHaptics(context: Context, view: View, label: String, text: String, successMessage: String) {
    try {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    } catch (_: Exception) {}
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard != null) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }
    }
}

// Helper to generate a strong random password for new users
private fun generateRandomPassword(length: Int = 10): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$"
    return (1..length)
        .asSequence()
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}

@Composable
fun PulsingStatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val color = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alphaAnim))
    )
}

@Composable
fun AdminSimpleView(
    viewModel: AlbionResourceViewModel,
    users: List<AdminUser>,
    licenses: List<AdminLicense>,
    devices: List<AdminDevice>,
    showMergeBot: Boolean = true,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onRefresh: () -> Unit
) {
    val view = LocalView.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val categories = remember(showMergeBot) {
        val list = mutableListOf(
            "👤 Benutzer" to Color(0xFF38BDF8),
            "💎 Lizenzen" to Color(0xFF8B5CF6),
            "📱 Geräte" to Color(0xFF10B981),
            "🎛️ Server" to Color(0xFF0EA5E9),
            "📊 Statistik" to Color(0xFFF59E0B)
        )
        if (showMergeBot) {
            list.add("🤖 Merge-Bot" to Color(0xFFEC4899))
        }
        list
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Fixed Category Tab Bar at the top
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
        ) {
            itemsIndexed(categories) { index, (title, color) ->
                val isSelected = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) color else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, color),
                    modifier = Modifier.clickable {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        selectedTab = index
                    }
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Remaining screen space display area for the selected category with scrolling enabled everywhere
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, categories[selectedTab.coerceIn(0, categories.lastIndex)].second),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        Text("Kategorie: Benutzer & Konten", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                        Text("Subkategorie: Verwaltung & Registrierung", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        AdminUsersTab(users = users, devices = devices, onFocusModeChanged = onFocusModeChanged, onRefresh = onRefresh)
                    }
                    1 -> {
                        Text("Kategorie: Lizenzen & Tarife", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF8B5CF6))
                        Text("Subkategorie: Generator & Schlüssel", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        AdminLicensesTab(licenses = licenses, onFocusModeChanged = onFocusModeChanged, onRefresh = onRefresh)
                    }
                    2 -> {
                        Text("Kategorie: Geräte & Sicherheit", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                        Text("Subkategorie: Verbundene Hardware & Massen-Aktionen", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        AdminDevicesTab(devices = devices, initialFilterOverride = 0, onFocusModeChanged = onFocusModeChanged, onRefresh = onRefresh)
                    }
                    3 -> {
                        Text("Kategorie: Server & Konfiguration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0EA5E9))
                        Text("Subkategorie: Netzwerk, Feature Flags & OTA", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        AdminRemoteConfigTab(viewModel = viewModel, onFocusModeChanged = onFocusModeChanged)
                    }
                    4 -> {
                        Text("Kategorie: Statistik & Diagnostik", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF59E0B))
                        Text("Subkategorie: 24h Datenfluss & Audit Radar", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        AdminAnalyticsTab(licenses = licenses)
                        Spacer(modifier = Modifier.height(6.dp))
                        AdminAuditAndDiagnosticsTab(users = users, licenses = licenses, devices = devices, onRefresh = onRefresh)
                    }
                    5 -> {
                        if (showMergeBot) {
                            Text("Kategorie: Merge-Bot & KI Arbitrage", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEC4899))
                            Text("Subkategorie: 100% Statistische Verzauberung & Top Trade Order Vorhersagen", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            AdminMergeBotTab(viewModel = viewModel, onFocusModeChanged = onFocusModeChanged)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AdminControlDialog(
    viewModel: AlbionResourceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var licenses by remember { mutableStateOf<List<AdminLicense>>(emptyList()) }
    var devices by remember { mutableStateOf<List<AdminDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastPingMs by remember { mutableLongStateOf(0L) }

    // Rotation animation for refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "RefreshSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    // Fast parallel data refresh via Coroutines async with latency measurement
    fun refreshAll() {
        isLoading = true
        errorMessage = null
        val startTime = System.currentTimeMillis()
        coroutineScope.launch {
            try {
                coroutineScope {
                    val usersDef = async { AdminControlManager.fetchUsersWithStatus(context) }
                    val licensesDef = async { AdminControlManager.fetchLicensesWithStatus(context) }
                    val devicesDef = async { AdminControlManager.fetchDevicesWithStatus(context) }

                    val usersRes = usersDef.await()
                    val licensesRes = licensesDef.await()
                    val devicesRes = devicesDef.await()

                    lastPingMs = System.currentTimeMillis() - startTime

                    if (usersRes is AdminApiResult.Success) users = usersRes.data
                    if (licensesRes is AdminApiResult.Success) licenses = licensesRes.data
                    if (devicesRes is AdminApiResult.Success) devices = devicesRes.data

                    val errors = listOfNotNull(
                        (usersRes as? AdminApiResult.Error)?.message,
                        (licensesRes as? AdminApiResult.Error)?.message,
                        (devicesRes as? AdminApiResult.Error)?.message
                    )

                    if (errors.isNotEmpty()) {
                        errorMessage = errors.first()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                errorMessage = "Netzwerkfehler: ${e.localizedMessage ?: "Verbindung fehlgeschlagen"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    // Dashboard Statistics Metrics
    val totalAdmins = remember(users) { users.count { it.isAdmin } }
    val totalBannedDevices = remember(devices) { devices.count { it.isBanned } }
    val totalOutdatedDevices = remember(devices) { devices.count { OtaUpdateManager.compareVersionStrings(it.appVersion ?: CURRENT_APP_VERSION, CURRENT_APP_VERSION) < 0 } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AlbionDataPro Admin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PulsingStatusDot(isOnline = errorMessage == null)
                        }
                        Text(
                            text = if (errorMessage == null) "Live Network • v$CURRENT_APP_VERSION ${if (lastPingMs > 0) "($lastPingMs ms)" else ""}" else "⚠️ Connection Issue",
                            fontSize = 10.sp,
                            color = if (errorMessage == null) Color(0xFF38BDF8) else Color(0xFFEF4444)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // High-End Error State Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMessage ?: "Verbindung fehlgeschlagen",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            OutlinedButton(
                                onClick = { refreshAll() },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color.White),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Erneut", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // High-End Interactive KPI Statistics Header Card
                AdminKpiHeader(
                    totalUsers = users.size,
                    totalAdmins = totalAdmins,
                    totalLicenses = licenses.size,
                    totalDevices = devices.size,
                    bannedDevices = totalBannedDevices,
                    outdatedDevices = totalOutdatedDevices,
                    activeTab = 0
                ) { _, _ ->
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShimmerLoadingCard(height = 90.dp)
                        ShimmerLoadingCard(height = 70.dp)
                        ShimmerLoadingCard(height = 70.dp)
                        ShimmerLoadingCard(height = 70.dp)
                    }
                } else {
                    AdminKpiHeader(
                        totalUsers = users.size,
                        totalAdmins = users.count { it.isAdmin },
                        totalLicenses = licenses.size,
                        totalDevices = devices.size,
                        bannedDevices = devices.count { it.isBanned },
                        outdatedDevices = devices.count { it.appVersion.trim() < CURRENT_APP_VERSION },
                        activeTab = 0,
                        onSelectTabAndFilter = { _, _ -> }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminSimpleView(
                        viewModel = viewModel,
                        users = users,
                        licenses = licenses,
                        devices = devices,
                        onRefresh = { refreshAll() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                    refreshAll()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (isLoading) Modifier.rotate(rotationAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLoading) "Lädt..." else "Aktualisieren", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AdminKpiHeader(
    totalUsers: Int,
    totalAdmins: Int,
    totalLicenses: Int,
    totalDevices: Int,
    bannedDevices: Int,
    outdatedDevices: Int,
    activeTab: Int,
    onSelectTabAndFilter: (tab: Int, filter: Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        item {
            KpiStatCard(
                title = "Einnahmen / ARR",
                value = "~${totalLicenses * 15} €",
                subtitle = "Geschätzt/Monat",
                color = Color(0xFFF59E0B),
                icon = Icons.Default.CheckCircle,
                isSelected = false,
                onClick = { }
            )
        }
        item {
            KpiStatCard(
                title = "Benutzer",
                value = totalUsers.toString(),
                subtitle = "👑 $totalAdmins Admins",
                color = Color(0xFF38BDF8),
                icon = Icons.Default.Person,
                isSelected = activeTab == 1,
                onClick = { onSelectTabAndFilter(1, 0) }
            )
        }
        item {
            KpiStatCard(
                title = "Lizenzen",
                value = totalLicenses.toString(),
                subtitle = "Generiert",
                color = Color(0xFF10B981),
                icon = Icons.Default.Lock,
                isSelected = activeTab == 0,
                onClick = { onSelectTabAndFilter(0, 0) }
            )
        }
        item {
            KpiStatCard(
                title = "Geräte",
                value = totalDevices.toString(),
                subtitle = if (bannedDevices > 0) "🔴 $bannedDevices Gebannt" else "🟢 Alle Aktiv",
                color = if (bannedDevices > 0) Color(0xFFEF4444) else Color(0xFF8B5CF6),
                icon = Icons.Default.Phone,
                isSelected = activeTab == 2,
                onClick = { onSelectTabAndFilter(2, if (bannedDevices > 0) 3 else 0) }
            )
        }
        item {
            KpiStatCard(
                title = "Versionen",
                value = "v$CURRENT_APP_VERSION",
                subtitle = if (outdatedDevices > 0) "⚠️ $outdatedDevices Veraltet" else "🟢 Alle Aktuell",
                color = if (outdatedDevices > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                icon = Icons.Default.Lock,
                isSelected = activeTab == 2 && outdatedDevices > 0,
                onClick = { onSelectTabAndFilter(2, if (outdatedDevices > 0) 2 else 0) }
            )
        }
        item {
            KpiStatCard(
                title = "Telemetrie",
                value = "Online",
                subtitle = "API & Cloud Sync",
                color = Color(0xFF4ADE80),
                icon = Icons.Default.Refresh,
                isSelected = false,
                onClick = { }
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else color.copy(alpha = 0.3f)),
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(subtitle, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
fun AdminSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Suchen...",
    onFocusModeChanged: (Boolean) -> Unit = {}
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 11.sp, color = Color(0xFF64748B)) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Löschen", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F172A),
            unfocusedContainerColor = Color(0xFF0F172A),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
    )
}

// Confirmation Dialog for Destructive Actions
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = itemName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🔴 unwiderruflich Löschen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Abbrechen", color = Color.White, fontSize = 11.sp)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AdminLicensesTab(
    licenses: List<AdminLicense>,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTier by remember { mutableStateOf("1m") }
    var customerNoteInput by remember { mutableStateOf("") }
    var generatedKeyResult by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var tierFilter by remember { mutableStateOf("ALL") }

    var deleteTargetLicense by remember { mutableStateOf<AdminLicense?>(null) }

    val tiers = listOf(
        "1m" to "1 Monat (15€)",
        "3m" to "3 Monate (30€)",
        "6m" to "6 Monate (50€)",
        "12m" to "12 Monate (100€)",
        "lifetime" to "Lifetime (250€)"
    )

    // Calculate Estimated Total Revenue Value of generated keys
    val totalRevenueValue = remember(licenses) {
        licenses.sumOf {
            when (it.tier.lowercase()) {
                "1m" -> 15
                "3m" -> 30
                "6m" -> 50
                "12m" -> 100
                "lifetime" -> 250
                else -> 15
            }
        }
    }

    val filteredLicenses = remember(licenses, searchQuery, tierFilter) {
        val list = if (tierFilter == "ALL") licenses else licenses.filter { it.tier.equals(tierFilter, ignoreCase = true) }
        if (searchQuery.isBlank()) list
        else list.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
                    it.tier.contains(searchQuery, ignoreCase = true) ||
                    it.note.contains(searchQuery, ignoreCase = true)
        }
    }

    if (deleteTargetLicense != null) {
        val targetKey = deleteTargetLicense?.key
        val targetTier = deleteTargetLicense?.tier
        ConfirmDeleteDialog(
            title = "Lizenz löschen",
            message = "Möchtest du diese generierte Lizenz wirklich löschen?",
            itemName = "${targetKey} (${targetTier?.uppercase()})",
            onDismiss = { deleteTargetLicense = null },
            onConfirm = {
                val key = targetKey ?: return@ConfirmDeleteDialog
                deleteTargetLicense = null
                coroutineScope.launch {
                    try {
                        val deleted = AdminControlManager.deleteLicense(context, key)
                        withContext(Dispatchers.Main) {
                            if (deleted) {
                                Toast.makeText(context, "🟢 Lizenz gelöscht", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Fehler: ${e.localizedMessage ?: "Unbekannt"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕ Neue Lizenz generieren", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            "💎 Volumen: $totalRevenueValue €",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    tiers.take(3).forEach { (code, _) ->
                        Button(
                            onClick = {
                                try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                selectedTier = code
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTier == code) Color(0xFF10B981) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Text(code.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    tiers.drop(3).forEach { (code, _) ->
                        Button(
                            onClick = {
                                try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                selectedTier = code
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTier == code) Color(0xFF10B981) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Text(code.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = customerNoteInput,
                    onValueChange = { customerNoteInput = it },
                    label = { Text("Kunden-Notiz (Optional)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Button(
                    onClick = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        coroutineScope.launch {
                            val newKey = AdminControlManager.generateLicense(context, selectedTier, customerNoteInput.trim())
                            if (newKey != null) {
                                generatedKeyResult = newKey
                                customerNoteInput = ""
                                copyToClipboardWithHaptics(context, view, "Albion License Key", newKey, "🟢 Lizenz erstellt & in Zwischenablage kopiert!")
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Erstellen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("⚡ Lizenzschlüssel erstellen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                generatedKeyResult?.let { key ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                copyToClipboardWithHaptics(context, view, "Albion License Key", key, "📋 Kopiert: $key")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = key,
                                color = Color(0xFF10B981),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Kopieren", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋 Generierte Schlüssel (${filteredLicenses.size}/${licenses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

            if (licenses.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val allKeysStr = licenses.joinToString("\n") { "${it.key} (${it.tier.uppercase()}${if (it.note.isNotBlank()) " - " + it.note else ""})" }
                        copyToClipboardWithHaptics(context, view, "Albion Licenses All", allKeysStr, "📋 Alle ${licenses.size} Lizenzen kopiert!")
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("📋 Alle Kopieren", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tier Filter Bar
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                DeviceFilterChipButton("Alle (${licenses.size})", selected = tierFilter == "ALL", onClick = { tierFilter = "ALL" })
            }
            item {
                DeviceFilterChipButton("1M", selected = tierFilter == "1m", onClick = { tierFilter = "1m" })
            }
            item {
                DeviceFilterChipButton("3M", selected = tierFilter == "3m", onClick = { tierFilter = "3m" })
            }
            item {
                DeviceFilterChipButton("6M", selected = tierFilter == "6m", onClick = { tierFilter = "6m" })
            }
            item {
                DeviceFilterChipButton("12M", selected = tierFilter == "12m", onClick = { tierFilter = "12m" })
            }
            item {
                DeviceFilterChipButton("Lifetime", selected = tierFilter == "lifetime", onClick = { tierFilter = "lifetime" })
            }
        }

        AdminSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "🔍 Schlüssel, Paket oder Notiz suchen...",
            onFocusModeChanged = onFocusModeChanged
        )

        if (filteredLicenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isNotBlank()) "Keine Lizenzen für '$searchQuery' gefunden." else "Keine Lizenzschlüssel vorhanden.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                filteredLicenses.forEach { lic ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                copyToClipboardWithHaptics(context, view, "Albion License Key", lic.key, "📋 Lizenzschlüssel ${lic.key} kopiert!")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lic.key, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF10B981))
                                Text("Paket: ${lic.tier.uppercase()} (${lic.price}) ${if (lic.created.isNotBlank()) "• Erstellt: " + lic.created else ""}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                if (lic.note.isNotBlank()) Text("Notiz: ${lic.note}", fontSize = 10.sp, color = Color(0xFF38BDF8))
                            }
                            IconButton(
                                onClick = {
                                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                    deleteTargetLicense = lic
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(
    users: List<AdminUser>,
    devices: List<AdminDevice>,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var userTypeFilter by remember { mutableStateOf("ALL") } // ALL, ADMINS, LICENSED, UNLICENSED

    var alertTargetUser by remember { mutableStateOf<String?>(null) }
    var alertTargetHwId by remember { mutableStateOf<String?>(null) }
    var showSendAlertDialog by remember { mutableStateOf(false) }

    var deleteTargetUser by remember { mutableStateOf<AdminUser?>(null) }
    var revealedPasswordsMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    if (deleteTargetUser != null) {
        val targetUsername = deleteTargetUser?.username
        ConfirmDeleteDialog(
            title = "Benutzer löschen",
            message = "Möchtest du diesen Benutzer und seine Zugänge löschen?",
            itemName = "Benutzer: $targetUsername",
            onDismiss = { deleteTargetUser = null },
            onConfirm = {
                val username = targetUsername ?: return@ConfirmDeleteDialog
                deleteTargetUser = null
                coroutineScope.launch {
                    try {
                        val deleted = AdminControlManager.deleteUser(context, username)
                        withContext(Dispatchers.Main) {
                            if (deleted) {
                                Toast.makeText(context, "🟢 Benutzer gelöscht", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Fehler: ${e.localizedMessage ?: "Unbekannt"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    if (showSendAlertDialog) {
        SendAlertDialog(
            targetUser = alertTargetUser,
            targetHwId = alertTargetHwId,
            onDismiss = { showSendAlertDialog = false },
            onFocusModeChanged = onFocusModeChanged,
            onSend = { message, playAlarm ->
                coroutineScope.launch {
                    val ok = AdminControlManager.sendAlertMessage(
                        context,
                        targetUsername = alertTargetUser,
                        hwId = alertTargetHwId,
                        message = message,
                        playAlarm = playAlarm
                    )
                    if (ok) {
                        Toast.makeText(context, "📢 Nachricht übertragen!", Toast.LENGTH_LONG).show()
                        showSendAlertDialog = false
                    } else {
                        Toast.makeText(context, "❌ Übertragung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    val filteredUsers = remember(users, searchQuery, userTypeFilter) {
        val typeList = when (userTypeFilter) {
            "ADMINS" -> users.filter { it.isAdmin }
            "LICENSED" -> users.filter { it.isLicensed }
            "UNLICENSED" -> users.filter { !it.isLicensed }
            else -> users
        }
        val sorted = typeList.sortedWith(
            compareByDescending<AdminUser> { it.isAdmin || it.isLicensed }
                .thenBy { it.username.lowercase() }
        )
        if (searchQuery.isBlank()) sorted
        else sorted.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                    devices.any { dev -> dev.username.equals(it.username, ignoreCase = true) && dev.hwId.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().imePadding()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("👤 Neuen Benutzer erstellen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                    Button(
                        onClick = {
                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                            alertTargetUser = null
                            alertTargetHwId = null
                            showSendAlertDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📢 Broadcast (ALLE)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Benutzername", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Passwort", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )

                    OutlinedButton(
                        onClick = {
                            newPassword = generateRandomPassword()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("🎲 Zufall", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }

                PasswordStrengthMeter(password = newPassword)

                Button(
                    onClick = {
                        if (newUsername.isBlank() || newPassword.isBlank()) return@Button
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        coroutineScope.launch {
                            val res = AdminControlManager.createUserWithStatus(context, newUsername.trim(), newPassword.trim())
                            if (res is AdminApiResult.Success) {
                                newUsername = ""
                                newPassword = ""
                                Toast.makeText(context, "🟢 Benutzer angelegt!", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } else if (res is AdminApiResult.Error) {
                                Toast.makeText(context, "❌ ${res.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Anlegen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // User Type Filters
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            DeviceFilterChipButton("Alle (${users.size})", selected = userTypeFilter == "ALL", onClick = { userTypeFilter = "ALL" }, modifier = Modifier.weight(1f))
            DeviceFilterChipButton("👑 Admins", selected = userTypeFilter == "ADMINS", onClick = { userTypeFilter = "ADMINS" }, modifier = Modifier.weight(1f))
            DeviceFilterChipButton("🟢 Aktiv", selected = userTypeFilter == "LICENSED", onClick = { userTypeFilter = "LICENSED" }, modifier = Modifier.weight(1f))
        }

        AdminSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "🔍 Benutzername oder HWID suchen...",
            onFocusModeChanged = onFocusModeChanged
        )

        Text("👥 Registrierte Benutzer (${filteredUsers.size}/${users.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Keine Benutzer gefunden.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredUsers.forEach { usr ->
                    val userDevices = devices.filter { it.username.trim().equals(usr.username.trim(), ignoreCase = true) }
                    val isPasswordRevealed = revealedPasswordsMap[usr.username] == true

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (usr.isAdmin) Color(0xFFFFD700) else Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = usr.username,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        if (usr.isAdmin) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, Color(0xFFFFD700))
                                            ) {
                                                Text(
                                                    "👑 ADMIN",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFFFD700),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            copyToClipboardWithHaptics(context, view, "Username Password", "${usr.username}:${usr.password}", "📋 Zugangsdaten kopiert!")
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Passwort: ${if (isPasswordRevealed) usr.password else "••••••••"}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF38BDF8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isPasswordRevealed) Icons.Default.Info else Icons.Default.Lock,
                                            contentDescription = "Anzeigen/Verbergen",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    revealedPasswordsMap = revealedPasswordsMap.toMutableMap().apply {
                                                        put(usr.username, !isPasswordRevealed)
                                                    }
                                                }
                                        )
                                    }

                                    Text(
                                        text = getRemainingDaysForUser(usr.licenseExpiresAt, usr.isAdmin),
                                        fontSize = 10.sp,
                                        color = if (usr.isAdmin) Color(0xFF10B981) else Color(0xFF38BDF8)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                            alertTargetUser = usr.username
                                            alertTargetHwId = null
                                            showSendAlertDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("📢 Alert", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    if (!usr.isAdmin) {
                                        Button(
                                            onClick = {
                                                try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                                coroutineScope.launch {
                                                    val ok = AdminControlManager.banUserDevices(context, usr.username)
                                                    if (ok) {
                                                        Toast.makeText(context, "🔴 Benutzer '${usr.username}' gebannt", Toast.LENGTH_SHORT).show()
                                                        onRefresh()
                                                    } else {
                                                        Toast.makeText(context, "❌ Fehler beim Bannen", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("🔴 Bannen", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                                deleteTargetUser = usr
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Löschen", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E293B))

                            // Device List for this user
                            Text("📱 Verknüpfte Geräte (${userDevices.size}/2 max):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            if (userDevices.isEmpty()) {
                                Text("Kein Gerät mit diesem Konto verbunden.", fontSize = 10.sp, color = Color(0xFF64748B))
                            } else {
                                userDevices.forEach { dev ->
                                    val isCurrent = (dev.appVersion.trim() >= CURRENT_APP_VERSION)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (dev.isBanned) Color(0xFF7F1D1D) else Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text("${dev.deviceName} (${if (dev.isBanned) "🔴 GEBANNT" else "🟢 Aktiv"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isCurrent) Color(0xFF065F46) else Color(0xFF78350F)
                                                        ) {
                                                            Text(
                                                                if (isCurrent) "🟢 v${dev.appVersion}" else "⚠️ v${dev.appVersion}",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCurrent) Color(0xFF34D399) else Color(0xFFFBBF24),
                                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        "HWID: ${dev.hwId}",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF94A3B8),
                                                        modifier = Modifier.clickable {
                                                            copyToClipboardWithHaptics(context, view, "HWID", dev.hwId, "📋 HWID ${dev.hwId} kopiert!")
                                                        }
                                                    )
                                                    if (dev.isBanned) {
                                                        Text("⛔ Grund: ${dev.banReason.ifBlank { "Verstoß gegen Nutzungsbedingungen" }}", fontSize = 9.sp, color = Color(0xFFFCA5A5))
                                                    } else if (dev.unbanned) {
                                                        Text("🟢 Entbannt (Auto-Bann geschützt & HWID frei)", fontSize = 9.sp, color = Color(0xFF86EFAC))
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (dev.isBanned) {
                                                        Button(
                                                            onClick = {
                                                                try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                                                coroutineScope.launch {
                                                                    val ok = AdminControlManager.unbanDevice(context, dev.hwId)
                                                                    if (ok) {
                                                                        Toast.makeText(context, "🟢 Entbannt & HWID freigegeben!", Toast.LENGTH_SHORT).show()
                                                                        onRefresh()
                                                                    }
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                            shape = RoundedCornerShape(4.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) {
                                                            Text("Entbannen", fontSize = 9.sp, color = Color.White)
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                                            alertTargetUser = null
                                                            alertTargetHwId = dev.hwId
                                                            showSendAlertDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("📩 Alert", fontSize = 9.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendAlertDialog(
    targetUser: String?,
    targetHwId: String?,
    onDismiss: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onSend: (message: String, playAlarm: Boolean) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    var playAlarmSound by remember { mutableStateOf(true) }

    val presetMessages = listOf(
        "⚠️ Wichtiger Server-Wartungshinweis: Bitte App neustarten.",
        "🚀 Neues Update v$CURRENT_APP_VERSION verfügbar! Bitte jetzt aktualisieren.",
        "🚨 Sicherheits-Überprüfung gestartet.",
        "🌐 Besuche unsere Webseite: https://albionmarketv2-1.onrender.com"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (!targetUser.isNullOrBlank()) "📢 Nachricht an '$targetUser'"
                else if (!targetHwId.isNullOrBlank()) "📢 Nachricht an HWID '$targetHwId'"
                else "📢 Broadcast an ALLE Geräte",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Bildschirm-Nachricht", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Text("Schnellauswahl Vorlagen:", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetMessages.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { messageInput = preset }
                        ) {
                            Text(
                                preset,
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playAlarmSound = !playAlarmSound }
                ) {
                    Checkbox(
                        checked = playAlarmSound,
                        onCheckedChange = { playAlarmSound = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🚨 Lauten Alarm-Ton beim Empfang abspielen", fontSize = 11.sp, color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSend(messageInput, playAlarmSound)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Senden", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun BanDeviceDialog(
    deviceName: String,
    hwId: String,
    onDismiss: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onConfirmBan: (reason: String) -> Unit
) {
    var reasonText by remember { mutableStateOf("Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)") }

    val presetReasons = listOf(
        "Verstoß gegen Nutzungsbedingungen",
        "Manipulation / Anti-Cheat Auslösung",
        "Unbefugter Zugriff / Multi-Account",
        "Inaktives / Nicht autorisiertes Gerät"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🔴 Gerät bannen & kicken", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gerät: $deviceName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("HWID: $hwId", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))

                Spacer(modifier = Modifier.height(4.dp))
                Text("Bann-Grund eingeben oder auswählen:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Bann-Grund") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEF4444), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Text("Schnellauswahl:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetReasons.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reasonText = preset }
                        ) {
                            Text(
                                "• $preset",
                                fontSize = 10.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmBan(reasonText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🔴 Jetzt Bannen", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Abbrechen", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
private fun DeviceFilterChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF0284C7) else Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color.White)
    }
}

@Composable
fun AdminAnalyticsTab(
    licenses: List<AdminLicense> = emptyList()
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<ServerDownloadStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val res = ServerSyncManager.fetchDownloadStats(context)
            withContext(Dispatchers.Main) {
                stats = res
                isLoading = false
            }
        }
    }

    val tierCounts = remember(licenses) {
        licenses.groupingBy { it.tier.lowercase() }.eachCount()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📊 Live 24h Data Download Chart", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                Text("Quelle: https://europe.albiononline2d.com/en/item & Albion Data API", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text("Gesamte geladene Daten: ${stats?.totalDownloads ?: 45280} Items", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
            }
        }

        // License Tier Breakdown
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💎 Lizenz-Paket Verteilung (${licenses.size} Gesamt)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF8B5CF6))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("1m", "3m", "6m", "12m", "lifetime").forEach { tier ->
                        val count = tierCounts[tier] ?: 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tier.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Text(count.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Text("📈 24h Stündlicher Datenfluss:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)

        if (isLoading) {
            ShimmerLoadingCard(height = 160.dp)
        } else {
            val hourly = stats?.hourly24h ?: emptyList()
            val maxVal = (hourly.maxOfOrNull { it.downloads } ?: 1000).coerceAtLeast(100)

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    hourly.takeLast(12).forEach { stat ->
                        val ratio = (stat.downloads.toFloat() / maxVal.toFloat()).coerceIn(0.1f, 1.0f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stat.downloads.toString(), fontSize = 8.sp, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratio)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF10B981))
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(stat.hour, fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMergeBotTab(
    viewModel: AlbionResourceViewModel,
    onFocusModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedBudgetMio by remember { mutableLongStateOf(20L) }
    var safeRoutesOnly by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf("ALLE") }

    val uiState by viewModel.uiState.collectAsState()

    val predictions = remember(uiState.marketPrices, selectedBudgetMio, safeRoutesOnly, selectedCategoryFilter) {
        AdvancedTradingBot.calculateAdminMergeAndTradeOpportunities(
            pricesMap = uiState.marketPrices,
            silverBudget = selectedBudgetMio * 1_000_000L,
            selectedCategoryName = selectedCategoryFilter,
            avoidDangerousZones = safeRoutesOnly,
            enableMergeBot = true,
            topN = 10
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Budget & Parameter Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🤖 Merge-Bot & KI Arbitrage Radar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFEC4899)
                    )
                    Button(
                        onClick = {
                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                            viewModel.forceRefreshMarketData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preise Scannen", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Text("Silber-Budget:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(5L to "5M", 10L to "10M", 20L to "20M", 50L to "50M").forEach { (budget, label) ->
                        DeviceFilterChipButton(
                            text = label,
                            selected = selectedBudgetMio == budget,
                            onClick = { selectedBudgetMio = budget },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { safeRoutesOnly = !safeRoutesOnly }
                ) {
                    Switch(
                        checked = safeRoutesOnly,
                        onCheckedChange = { safeRoutesOnly = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nur sichere Routen (keine Rot/Schwarz-Zonen)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        Text(
            "⚡ Top Vorhersagen (${predictions.size} Chancen)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.White
        )

        if (predictions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Keine aktiven Merge-Chancen für die gewählten Kriterien gefunden.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("Tipp: Drücke 'Preise Scannen' oder passe das Silber-Budget an.", fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }
        } else {
            predictions.forEach { pred ->
                val cardBorderColor = if (pred.isMergeOpportunity) Color(0xFFEC4899) else Color(0xFF38BDF8)
                val badgeText = if (pred.isMergeOpportunity) "⚡ MERGE-BOT CHANCE" else "📊 ARBITRAGE CHANCE"
                val badgeColor = if (pred.isMergeOpportunity) Color(0xFFEC4899) else Color(0xFF38BDF8)

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, badgeColor)
                            ) {
                                Text(
                                    badgeText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    "+${String.format(Locale.US, "%.1f", pred.maxMarginPercent)}% Marge",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = pred.resourceNameDe,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Kauf: ${pred.buyCity}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Order: ${String.format(Locale.GERMANY, "%,d", pred.predictedBuyOrderPrice)} Silber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Text("➔", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Verkauf: ${pred.sellCity}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Order: ${String.format(Locale.GERMANY, "%,d", pred.predictedSellOrderPrice)} Silber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Erwarteter Gesamtgewinn:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    "+${String.format(Locale.GERMANY, "%,d", pred.totalExpectedProfit)} Silber",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Text(
                            text = pred.strategyRecommendationDe,
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            fontStyle = FontStyle.Italic
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pred.zoneSafetyText,
                                    fontSize = 9.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "🛡️ ${pred.statisticalGuaranteeLabel}",
                                    fontSize = 8.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                    val opp = pred.toTradeOpportunity(selectedBudgetMio * 1_000_000L)
                                    viewModel.acceptTradeOpportunity(opp)
                                    Toast.makeText(context, "✅ KI-Auftrag '${pred.resourceNameDe}' zu aktiven Aufträgen hinzugefügt!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("⚡ Auftrag annehmen", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRemoteConfigTab(
    viewModel: AlbionResourceViewModel,
    onFocusModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var minMarginInput by remember { mutableStateOf("12.0") }
    var maintenanceMode by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(ServerConfigManager.getCustomServerUrls(context).firstOrNull() ?: "https://albionmarketv2-1.onrender.com") }
    var syncIntervalSeconds by remember { mutableStateOf("10") }
    var maxDevicesPerUser by remember { mutableStateOf("3") }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Kategorie 1: Netzwerk & Verbindungseinstellungen
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kategorie: Netzwerk & Verbindung", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("Server-URL / Ngrok Bridge Endpoint", fontSize = 9.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = syncIntervalSeconds,
                        onValueChange = { syncIntervalSeconds = it },
                        label = { Text("Sync Intervall (Sek.)", fontSize = 9.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )

                    OutlinedTextField(
                        value = maxDevicesPerUser,
                        onValueChange = { maxDevicesPerUser = it },
                        label = { Text("Max. Geräte / Nutzer", fontSize = 9.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                    )
                }

                Button(
                    onClick = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        ServerConfigManager.updateServerUrl(context, serverUrlInput)
                        Toast.makeText(context, "Server-URL & Sync gespeichert!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Server-URL & Sync speichern", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Kategorie 2: Live Config & Feature Flags
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kategorie: Live Config & Feature Flags", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))

                OutlinedTextField(
                    value = minMarginInput,
                    onValueChange = { minMarginInput = it },
                    label = { Text("Mindest-Gewinnmarge % (Global)", fontSize = 9.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocusModeChanged(true) }
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(
                        checked = maintenanceMode,
                        onCheckedChange = { maintenanceMode = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wartungsmodus aktivieren", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        val margin = minMarginInput.replace(',', '.').toDoubleOrNull() ?: 12.0
                        coroutineScope.launch {
                            val ok = AdminControlManager.updateRemoteConfig(context, margin, maintenanceMode)
                            if (ok) {
                                Toast.makeText(context, "Remote-Config gepusht!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Fehler beim Veröffentlichen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Config an alle Geräte pushen", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        coroutineScope.launch {
                            Toast.makeText(context, "Ressourcen werden neu geladen...", Toast.LENGTH_SHORT).show()
                            viewModel.forceReloadAllResources()
                            Toast.makeText(context, "Alle Ressourcen aktualisiert!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Alle Ressourcen neu laden", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Kategorie 3: OTA Updates & Massen-Verwaltung
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kategorie: OTA Updates & Massen-Verwaltung", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF8B5CF6))
                Text("Befiehlt allen verbundenen Geräten, AlbionDataPro.apk im Hintergrund herunterzuladen.", fontSize = 9.sp, color = Color(0xFF94A3B8))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                            coroutineScope.launch {
                                val ok = AdminControlManager.triggerOtaUpdateCommand(context, isGlobal = true)
                                if (ok) {
                                    Toast.makeText(context, "Globaler OTA-Update Befehl gesendet!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Fehler beim Senden", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Globales OTA Update", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                            coroutineScope.launch {
                                val ok = AdminControlManager.unbanAllDevices(context)
                                if (ok) {
                                    Toast.makeText(context, "Alle Geräte entbannt!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Fehler beim Entbannen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Alle Entbannen", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// New High-End Tab for Audit Event Log & Endpoint Live Diagnostics Radar
@Composable
fun AdminAuditAndDiagnosticsTab(
    users: List<AdminUser>,
    licenses: List<AdminLicense>,
    devices: List<AdminDevice>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var diagnosticsMap by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isRunningDiag by remember { mutableStateOf(false) }

    fun refreshLogs() {
        logs = AdminAuditLogManager.getLogs(context)
    }

    fun runDiagnostics() {
        isRunningDiag = true
        coroutineScope.launch {
            diagnosticsMap = AdminControlManager.runEndpointDiagnostics(context)
            isRunningDiag = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
        runDiagnostics()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        // Multi-Endpoint Diagnostic Radar Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📡 Multi-Node Diagnostics Radar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = { runDiagnostics() }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Diagnose", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                if (isRunningDiag) {
                    Text("⏳ Teste Endpunkte in Echtzeit...", fontSize = 10.sp, color = Color(0xFF94A3B8))
                } else if (diagnosticsMap.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        diagnosticsMap.forEach { (ep, ms) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ep, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCBD5E1))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (ms in 0..500) Color(0xFF065F46) else Color(0xFF7F1D1D)
                                ) {
                                    Text(
                                        text = if (ms >= 0) "$ms ms 🟢" else "Offline 🔴",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ms in 0..500) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Batch Action Center
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⚡ Speed-Dial / Massen-Aktionen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val ok = AdminControlManager.triggerOtaUpdate(context, isGlobal = true)
                                if (ok) {
                                    Toast.makeText(context, "🚀 Aggressiver Massen-OTA Push an alle Geräte gesendet!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ OTA Push fehlgeschlagen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        Text("🚀 Massen-OTA Push", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val ok = AdminControlManager.sendAlertMessage(context, targetUsername = null, hwId = null, message = "🚨 SICHERHEITS-ALARM: Wichtige Systemnachricht vom Administrator!", playAlarm = true)
                                if (ok) {
                                    Toast.makeText(context, "📢 Massen-Alarm an alle Geräte gesendet!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Alarm-Senden fehlgeschlagen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        Text("📢 Massen-Alarm", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val dump = AdminControlManager.exportDatabaseDump(users, licenses, devices)
                            copyToClipboardWithHaptics(context, view, "Admin DB Backup JSON", dump, "📋 DB-Backup JSON in Zwischenablage kopiert!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        Text("📋 DB Export", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val ok = AdminControlManager.unbanAllDevices(context)
                                if (ok) {
                                    Toast.makeText(context, "🟢 Alle Geräte entbannt!", Toast.LENGTH_SHORT).show()
                                    onRefresh()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        Text("🟢 Massen-Entbannung", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            AdminAuditLogManager.clearLogs(context)
                            refreshLogs()
                            Toast.makeText(context, "🧹 Log-Historie geleert", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        Text("🧹 Log Leeren", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Event Audit Log List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📜 Admin Activity Log (${logs.size} Einträge)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            IconButton(onClick = { refreshLogs() }, modifier = Modifier.size(20.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Aktualisieren", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
            }
        }

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Noch keine Admin-Aktionen in dieser Sitzung protokolliert.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                logs.forEach { log ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDevicesTab(
    devices: List<AdminDevice>,
    initialFilterOverride: Int = 0,
    onFocusModeChanged: (Boolean) -> Unit = {},
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var showBanDialog by remember { mutableStateOf(false) }
    var banTargetHwId by remember { mutableStateOf("") }
    var banTargetDeviceName by remember { mutableStateOf("") }

    var deleteTargetDevice by remember { mutableStateOf<AdminDevice?>(null) }

    var selectedVersionFilter by remember { mutableIntStateOf(initialFilterOverride) } // 0 = Alle, 1 = Aktuell, 2 = Veraltet, 3 = Gebannt
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(initialFilterOverride) {
        if (initialFilterOverride != 0) {
            selectedVersionFilter = initialFilterOverride
        }
    }

    val currentVersionCount = remember(devices) {
        devices.count { it.appVersion.trim() >= CURRENT_APP_VERSION }
    }
    val outdatedVersionCount = remember(devices) {
        devices.count { it.appVersion.trim() < CURRENT_APP_VERSION }
    }
    val bannedCount = remember(devices) {
        devices.count { it.isBanned }
    }

    val filteredDevices = remember(devices, selectedVersionFilter, searchQuery) {
        val list = when (selectedVersionFilter) {
            1 -> devices.filter { it.appVersion.trim() >= CURRENT_APP_VERSION }
            2 -> devices.filter { it.appVersion.trim() < CURRENT_APP_VERSION }
            3 -> devices.filter { it.isBanned }
            else -> devices
        }
        val searched = if (searchQuery.isBlank()) list
        else list.filter {
            it.deviceName.contains(searchQuery, ignoreCase = true) ||
                    it.hwId.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true) ||
                    it.appVersion.contains(searchQuery, ignoreCase = true)
        }
        searched.sortedWith(compareBy({ it.username.lowercase() }, { it.deviceName.lowercase() }))
    }

    if (deleteTargetDevice != null) {
        ConfirmDeleteDialog(
            title = "Gerät löschen",
            message = "Möchtest du dieses registrierte Gerät aus der Datenbank entfernen?",
            itemName = "Gerät: ${deleteTargetDevice?.deviceName} (HWID: ${deleteTargetDevice?.hwId})",
            onDismiss = { deleteTargetDevice = null },
            onConfirm = {
                val hwId = deleteTargetDevice?.hwId ?: return@ConfirmDeleteDialog
                deleteTargetDevice = null
                coroutineScope.launch {
                    try {
                        val deleted = AdminControlManager.deleteDevice(context, hwId)
                        if (deleted) {
                            Toast.makeText(context, "🟢 Gerät gelöscht", Toast.LENGTH_SHORT).show()
                            onRefresh()
                        } else {
                            Toast.makeText(context, "❌ Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "❌ Fehler: ${e.localizedMessage ?: "Unbekannt"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showBanDialog) {
        BanDeviceDialog(
            deviceName = banTargetDeviceName,
            hwId = banTargetHwId,
            onDismiss = { showBanDialog = false },
            onFocusModeChanged = onFocusModeChanged,
            onConfirmBan = { reason ->
                showBanDialog = false
                coroutineScope.launch {
                    try {
                        val ok = AdminControlManager.banDevice(context, banTargetHwId, reason)
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                Toast.makeText(context, "🔴 Banned & Gekickt", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Bannen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Fehler: ${e.localizedMessage ?: "Unbekannt"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        // KI Security Guard Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "🤖 KI-Security Guard & HWID Freigabe (Aktiv)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
                Text(
                    "✓ 24/7 KI-Anomalieerkennung & Anti-Cheat Schutz aktiv\n✓ Manuell entbannte Geräte werden NIEMALS erneut automatisch gebannt\n✓ HWID wird beim Entbannen sofort wieder freigegeben",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // OTA Command Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "📲 OTA-Update Befehls-Zentrale",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    "Eine neue Version wird NUR installiert, wenn du diesen Befehl als Admin auslöst.",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                Button(
                    onClick = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                        coroutineScope.launch {
                            val ok = AdminControlManager.triggerOtaUpdate(context, isGlobal = true)
                            if (ok) {
                                Toast.makeText(context, "🚀 Update-Befehl an ALLE Geräte gesendet! Neue APK wird jetzt verteilt.", Toast.LENGTH_LONG).show()
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Senden fehlgeschlagen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp)
                ) {
                    Text("🚀 OTA Update jetzt auf allen Geräten installieren", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        AdminSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "🔍 Gerätename, Benutzer oder HWID suchen...",
            onFocusModeChanged = onFocusModeChanged
        )

        // Filter Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DeviceFilterChipButton(
                text = "Alle (${devices.size})",
                selected = selectedVersionFilter == 0,
                onClick = {
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                    selectedVersionFilter = 0
                },
                modifier = Modifier.weight(1f)
            )
            DeviceFilterChipButton(
                text = "🟢 Aktuell ($currentVersionCount)",
                selected = selectedVersionFilter == 1,
                onClick = {
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                    selectedVersionFilter = 1
                },
                modifier = Modifier.weight(1f)
            )
            DeviceFilterChipButton(
                text = "⚠️ Veraltet ($outdatedVersionCount)",
                selected = selectedVersionFilter == 2,
                onClick = {
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                    selectedVersionFilter = 2
                },
                modifier = Modifier.weight(1f)
            )
            DeviceFilterChipButton(
                text = "🔴 Gebannt ($bannedCount)",
                selected = selectedVersionFilter == 3,
                onClick = {
                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                    selectedVersionFilter = 3
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (filteredDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        searchQuery.isNotBlank() -> "Keine Geräte für '$searchQuery' gefunden."
                        selectedVersionFilter == 1 -> "Keine Geräte mit der aktuellen Version (v$CURRENT_APP_VERSION) gefunden."
                        selectedVersionFilter == 2 -> "Keine veralteten Geräte vorhanden! Alle auf v$CURRENT_APP_VERSION. 🎉"
                        selectedVersionFilter == 3 -> "Keine gebannten Geräte vorhanden."
                        else -> "Keine Geräte registriert."
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                filteredDevices.forEach { dev ->
                    val isUpToDate = OtaUpdateManager.compareVersionStrings(dev.appVersion ?: CURRENT_APP_VERSION, CURRENT_APP_VERSION) >= 0

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (dev.isBanned) Color(0xFF7F1D1D) else Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (dev.isBanned) Color(0xFFEF4444) else Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(dev.deviceName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isUpToDate) Color(0xFF065F46) else Color(0xFF78350F)
                                    ) {
                                        Text(
                                            text = if (isUpToDate) "🟢 v${dev.appVersion} (Aktuell)" else "⚠️ v${dev.appVersion} (Veraltet)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUpToDate) Color(0xFF34D399) else Color(0xFFFBBF24),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(if (dev.isBanned) "🔴 GEBANNT" else "🟢 Aktiv", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (dev.isBanned) Color(0xFFEF4444) else Color(0xFF10B981))
                                }
                            }

                            Text(
                                "HWID: ${dev.hwId}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.clickable {
                                    copyToClipboardWithHaptics(context, view, "HWID", dev.hwId, "📋 HWID ${dev.hwId} kopiert!")
                                }
                            )
                            Text("Benutzer: ${dev.username} • App v${dev.appVersion}", fontSize = 10.sp, color = Color(0xFF38BDF8))

                            if (dev.isBanned) {
                                Text("⛔ Grund: ${dev.banReason.ifBlank { "Verstoß gegen Nutzungsbedingungen" }}", fontSize = 10.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.SemiBold)
                            } else if (dev.unbanned) {
                                Text("🟢 Entbannt (Auto-Bann geschützt & HWID freigegeben)", fontSize = 9.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                        coroutineScope.launch {
                                            val ok = AdminControlManager.triggerOtaUpdate(context, hwId = dev.hwId)
                                            if (ok) {
                                                Toast.makeText(context, "📲 Update-Befehl an ${dev.deviceName} gesendet!", Toast.LENGTH_SHORT).show()
                                                onRefresh()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(1.dp)
                                ) {
                                    Text("📲 Update befehlen", fontSize = 10.sp, color = Color.White)
                                }

                                if (dev.isBanned) {
                                    Button(
                                        onClick = {
                                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                            coroutineScope.launch {
                                                try {
                                                    val ok = AdminControlManager.unbanDevice(context, dev.hwId)
                                                    withContext(Dispatchers.Main) {
                                                        if (ok) {
                                                            Toast.makeText(context, "🟢 Entbannt & HWID freigegeben!", Toast.LENGTH_SHORT).show()
                                                            onRefresh()
                                                        } else {
                                                            Toast.makeText(context, "❌ Entbannen fehlgeschlagen", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "❌ Fehler: ${e.localizedMessage ?: "Unbekannt"}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(1.dp)
                                    ) {
                                        Text("Entbannen", fontSize = 10.sp, color = Color.White)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                            banTargetHwId = dev.hwId
                                            banTargetDeviceName = dev.deviceName
                                            showBanDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(1.dp)
                                    ) {
                                        Text("Bannen", fontSize = 10.sp, color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = {
                                        try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                        deleteTargetDevice = dev
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(1.dp)
                                ) {
                                    Text("Löschen", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getRemainingDaysForUser(expiresAtStr: String?, isAdmin: Boolean): String {
    if (isAdmin) return "👑 Unbegrenzt (Admin)"
    if (expiresAtStr.isNullOrBlank()) return "Keine Lizenz-Info"
    if (expiresAtStr.isBlank()) return "Keine Lizenz-Info"
    val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd", "dd.MM.yyyy")
    var dateMs = 0L
    for (f in formats) {
        try {
            val sdf = SimpleDateFormat(f, Locale.US)
            if (f.contains("Z")) sdf.timeZone = TimeZone.getTimeZone("UTC")
            val d = sdf.parse(expiresAtStr)
            if (d != null) {
                dateMs = d.time
                break
            }
        } catch (_: Exception) {}
    }
    if (dateMs <= 0L) return "Ablauf: $expiresAtStr"
    val diff = dateMs - System.currentTimeMillis()
    if (diff <= 0L) return "❌ Lizenz abgelaufen"
    val days = diff / (24L * 3600L * 1000L)
    return "⏱️ $days Tage verbleibend"
}
