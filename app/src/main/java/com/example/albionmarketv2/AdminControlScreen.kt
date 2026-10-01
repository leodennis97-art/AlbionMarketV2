package com.example.albionmarketv2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

const val CURRENT_APP_VERSION = "1.3.9"

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

object AdminControlManager {

    private fun getBaseUrl(context: Context): String {
        return ServerSyncManager.getServerBaseUrls(context).firstOrNull() ?: "https://albionmarketv2-1.onrender.com"
    }

    private suspend fun postJson(baseUrl: String, endpoint: String, json: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in (200..299)) {
                val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext JSONObject(resStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private suspend fun getJsonArray(baseUrl: String, endpoint: String): JSONArray? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            if (conn.responseCode in (200..299)) {
                val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext JSONArray(resStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchUsers(context: Context): List<AdminUser> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/users") ?: return emptyList()
        val list = mutableListOf<AdminUser>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AdminUser(
                    username = obj.optString("username", "Unbekannt"),
                    password = obj.optString("password", "••••••••"),
                    isAdmin = obj.optBoolean("isAdmin", false),
                    isLicensed = obj.optBoolean("isLicensed", true),
                    licenseExpiresAt = obj.optString("licenseExpiresAt", ""),
                )
            )
        }
        return list
    }

    suspend fun fetchLicenses(context: Context): List<AdminLicense> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/licenses") ?: return emptyList()
        val list = mutableListOf<AdminLicense>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AdminLicense(
                    key = obj.optString("key", ""),
                    tier = obj.optString("tier", "1m"),
                    price = obj.optString("price", "15 €"),
                    created = obj.optString("created", ""),
                    note = obj.optString("note", "")
                )
            )
        }
        return list
    }

    suspend fun fetchDevices(context: Context): List<AdminDevice> {
        val base = getBaseUrl(context)
        val arr = getJsonArray(base, "/api/devices") ?: return emptyList()
        val list = mutableListOf<AdminDevice>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val bannedUntil = obj.optString("bannedUntil", "")
            val unbanned = obj.optBoolean("unbanned", false)
            val isBanned = !bannedUntil.isNullOrBlank() && !unbanned
            val banReason = obj.optString("banReason", "")
            list.add(
                AdminDevice(
                    hwId = obj.optString("hwId", ""),
                    deviceName = obj.optString("deviceName", "Android Device"),
                    appVersion = obj.optString("appVersion", CURRENT_APP_VERSION),
                    username = obj.optString("username", "Unbekannt"),
                    lastSeen = obj.optString("lastSeen", ""),
                    licenseExpiresAt = obj.optString("licenseExpiresAt", ""),
                    isBanned = isBanned,
                    banReason = banReason,
                    unbanned = unbanned
                )
            )
        }
        return list
    }

    suspend fun createUser(context: Context, user: String, pass: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }
        val res = postJson(base, "/api/admin/user/create", json)
        return res?.optString("status") == "success"
    }

    suspend fun deleteUser(context: Context, user: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("username", user) }
        val res = postJson(base, "/api/admin/user/delete", json)
        return res?.optString("status") == "success"
    }

    suspend fun generateLicense(context: Context, tier: String, note: String): String? {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("tier", tier)
            put("customerNote", note)
        }
        val res = postJson(base, "/api/admin/license/generate", json)
        return res?.optJSONObject("license")?.optString("key")
    }

    suspend fun deleteLicense(context: Context, key: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("key", key) }
        val res = postJson(base, "/api/admin/license/delete", json)
        return res?.optString("status") == "success"
    }

    suspend fun banDevice(context: Context, hwId: String, banReason: String = "Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)"): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("hwId", hwId)
            put("banReason", banReason.ifBlank { "Verstoß gegen Nutzungsbedingungen / Manipulation (Cheat)" })
        }
        val res = postJson(base, "/api/admin/device/ban", json)
        return res?.optString("status") == "success"
    }

    suspend fun unbanDevice(context: Context, hwId: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("hwId", hwId) }
        val res = postJson(base, "/api/admin/device/unban", json)
        return res?.optString("status") == "success"
    }

    suspend fun deleteDevice(context: Context, hwId: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("hwId", hwId) }
        val res = postJson(base, "/api/admin/device/delete", json)
        return res?.optString("status") == "success"
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
        return res?.optString("status") == "success"
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
        return res?.optString("status") == "success"
    }

    suspend fun updateRemoteConfig(context: Context, minMargin: Double, maintenance: Boolean): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply {
            put("minMarginPercent", minMargin)
            put("maintenanceMode", maintenance)
        }
        val res = postJson(base, "/api/admin/remote-config", json)
        return res?.optString("status") == "success"
    }
}

@Composable
fun AdminControlDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var licenses by remember { mutableStateOf<List<AdminLicense>>(emptyList()) }
    var devices by remember { mutableStateOf<List<AdminDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshAll() {
        isLoading = true
        coroutineScope.launch {
            users = AdminControlManager.fetchUsers(context)
            licenses = AdminControlManager.fetchLicenses(context)
            devices = AdminControlManager.fetchDevices(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️ AlbionDataProAdmin - Zentrale", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
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
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF38BDF8)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("🔑 Lizenzen", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("👥 Nutzer", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📱 Geräte", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("📊 24h/KI", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("🎛️ Config", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                    }
                } else {
                    when (selectedTab) {
                        0 -> AdminLicensesTab(licenses) { refreshAll() }
                        1 -> AdminUsersTab(users, devices) { refreshAll() }
                        2 -> AdminDevicesTab(devices) { refreshAll() }
                        3 -> AdminAnalyticsTab()
                        4 -> AdminRemoteConfigTab()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { refreshAll() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aktualisieren", fontSize = 12.sp, color = Color.White)
                }
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AdminLicensesTab(
    licenses: List<AdminLicense>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTier by remember { mutableStateOf("1m") }
    var customerNoteInput by remember { mutableStateOf("") }
    var generatedKeyResult by remember { mutableStateOf<String?>(null) }

    val tiers = listOf(
        "1m" to "1 Monat (15€)",
        "3m" to "3 Monate (30€)",
        "6m" to "6 Monate (50€)",
        "12m" to "12 Monate (100€)",
        "lifetime" to "Lifetime (250€)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("➕ Neue Lizenz generieren", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    tiers.take(3).forEach { (code, _) ->
                        Button(
                            onClick = { selectedTier = code },
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
                            onClick = { selectedTier = code },
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
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newKey = AdminControlManager.generateLicense(context, selectedTier, customerNoteInput.trim())
                            if (newKey != null) {
                                generatedKeyResult = newKey
                                customerNoteInput = ""

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                if (clipboard != null) {
                                    val clip = ClipData.newPlainText("Albion License Key", newKey)
                                    clipboard.setPrimaryClip(clip)
                                }

                                Toast.makeText(context, "🟢 Lizenz erstellt & automatisch in Zwischenablage kopiert!", Toast.LENGTH_LONG).show()
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = key,
                            color = Color(0xFF10B981),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }

        Text("📋 Generierte Schlüssel (${licenses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(licenses) { lic ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (clipboard != null) {
                                val clip = ClipData.newPlainText("Albion License Key", lic.key)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "📋 Lizenzschlüssel ${lic.key} kopiert!", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lic.key, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF10B981))
                            Text("Paket: ${lic.tier.uppercase()} (${lic.price})", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            if (lic.note.isNotBlank()) Text("Notiz: ${lic.note}", fontSize = 10.sp, color = Color(0xFF38BDF8))
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val deleted = AdminControlManager.deleteLicense(context, lic.key)
                                    if (deleted) {
                                        Toast.makeText(context, "Lizenz gelöscht", Toast.LENGTH_SHORT).show()
                                        onRefresh()
                                    }
                                }
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

@Composable
fun AdminUsersTab(
    users: List<AdminUser>,
    devices: List<AdminDevice>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    var alertTargetUser by remember { mutableStateOf<String?>(null) }
    var alertTargetHwId by remember { mutableStateOf<String?>(null) }
    var alertMessageInput by remember { mutableStateOf("") }
    var alertPlayAlarmSound by remember { mutableStateOf(true) }
    var showSendAlertDialog by remember { mutableStateOf(false) }

    if (showSendAlertDialog) {
        AlertDialog(
            onDismissRequest = { showSendAlertDialog = false },
            title = {
                Text(
                    text = if (alertTargetUser != null) "📢 Nachricht an '$alertTargetUser'" else if (alertTargetHwId != null) "📢 Nachricht an HWID '$alertTargetHwId'" else "📢 Broadcast an ALLE Geräte",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = alertMessageInput,
                        onValueChange = { alertMessageInput = it },
                        label = { Text("Bildschirm-Nachricht", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alertPlayAlarmSound = !alertPlayAlarmSound }
                    ) {
                        Checkbox(
                            checked = alertPlayAlarmSound,
                            onCheckedChange = { alertPlayAlarmSound = it },
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
                        if (alertMessageInput.isBlank()) return@Button
                        coroutineScope.launch {
                            val ok = AdminControlManager.sendAlertMessage(
                                context,
                                targetUsername = alertTargetUser,
                                hwId = alertTargetHwId,
                                message = alertMessageInput,
                                playAlarm = alertPlayAlarmSound
                            )
                            if (ok) {
                                Toast.makeText(context, "📢 Nachricht an Gerät(e) übertragen!", Toast.LENGTH_LONG).show()
                                showSendAlertDialog = false
                                alertMessageInput = ""
                            } else {
                                Toast.makeText(context, "❌ Übertragung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Senden", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSendAlertDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("👤 Neuen Benutzer erstellen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                    Button(
                        onClick = {
                            alertTargetUser = null
                            alertTargetHwId = null
                            showSendAlertDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("📢 Broadcast an ALLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Benutzername", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Passwort", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (newUsername.isBlank() || newPassword.isBlank()) return@Button
                        coroutineScope.launch {
                            val created = AdminControlManager.createUser(context, newUsername.trim(), newPassword.trim())
                            if (created) {
                                newUsername = ""
                                newPassword = ""
                                Toast.makeText(context, "🟢 Benutzer angelegt!", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Anlegen", Toast.LENGTH_SHORT).show()
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

        val sortedUsers = remember(users) { users.sortedBy { it.username.lowercase() } }

        Text("👥 Benutzer & Zugeordnete Geräte (${sortedUsers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(sortedUsers) { usr ->
                val userDevices = remember(devices, usr) {
                    devices.filter { it.username.trim().equals(usr.username.trim(), ignoreCase = true) }
                }

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
                                Text(
                                    text = "${usr.username} ${if (usr.isAdmin) "👑 (Admin)" else ""}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (usr.isAdmin) Color(0xFFFFD700) else Color.White
                                )
                                Text("Passwort: ${usr.password}", fontSize = 10.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                Text(if (usr.isAdmin) "Lizenz: Unbegrenzt (Admin)" else "Lizenz aktiv: ${if (usr.isLicensed) "Ja" else "Nein"}", fontSize = 10.sp, color = if (usr.isAdmin) Color(0xFF10B981) else Color(0xFF94A3B8))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        alertTargetUser = usr.username
                                        alertTargetHwId = null
                                        showSendAlertDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("📢 Nachricht", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                if (!usr.isAdmin) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val deleted = AdminControlManager.deleteUser(context, usr.username)
                                                if (deleted) {
                                                    Toast.makeText(context, "Benutzer gelöscht", Toast.LENGTH_SHORT).show()
                                                    onRefresh()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
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
                        Text("📱 Geräte (${userDevices.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        if (userDevices.isEmpty()) {
                            Text("Kein Gerät mit diesem Konto verbunden.", fontSize = 10.sp, color = Color(0xFF64748B))
                        } else {
                            userDevices.forEach { dev ->
                                val isCurrent = (dev.appVersion.trim() == CURRENT_APP_VERSION) || (dev.appVersion.trim() >= CURRENT_APP_VERSION)
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
                                                Text("HWID: ${dev.hwId}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
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

@Composable
fun BanDeviceDialog(
    deviceName: String,
    hwId: String,
    onDismiss: () -> Unit,
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
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Schnellauswahl:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetReasons.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            onClick = { reasonText = preset },
                            modifier = Modifier.fillMaxWidth()
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
        }
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
            containerColor = if (selected) Color(0xFF0284C7) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color.White)
    }
}

@Composable
fun AdminAnalyticsTab() {
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📊 Live 24h Data Download Chart", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                Text("Quelle: https://europe.albiononline2d.com/en/item & Albion Data API", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text("Gesamte geladene Daten: ${stats?.totalDownloads ?: 45280} Items", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
            }
        }

        Text("📈 24h Stündlicher Datenfluss:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            val hourly = stats?.hourly24h ?: emptyList()
            val maxVal = (hourly.maxOfOrNull { it.downloads } ?: 1000).coerceAtLeast(100)

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
fun AdminRemoteConfigTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var minMarginInput by remember { mutableStateOf("12.0") }
    var maintenanceMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎛️ Remote Live-Config Engine (Feature-Flags)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))

                OutlinedTextField(
                    value = minMarginInput,
                    onValueChange = { minMarginInput = it },
                    label = { Text("Mindest-Gewinnmarge % (Global)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(
                        checked = maintenanceMode,
                        onCheckedChange = { maintenanceMode = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🛠️ Wartungsmodus aktivieren", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val margin = minMarginInput.toDoubleOrNull() ?: 12.0
                        coroutineScope.launch {
                            val ok = AdminControlManager.updateRemoteConfig(context, margin, maintenanceMode)
                            if (ok) {
                                Toast.makeText(context, "🟢 Remote-Config an alle Geräte gepusht!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Veröffentlichen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("⚡ Config live an ALLE Geräte pushen", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🚀 In-App OTA Update Befehl", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF8B5CF6))
                Text("Befiehlt allen verbundenen Geräten, AlbionDataPro.apk sofort im Hintergrund herunterzuladen.", fontSize = 10.sp, color = Color(0xFF94A3B8))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val ok = AdminControlManager.triggerOtaUpdateCommand(context, isGlobal = true)
                            if (ok) {
                                Toast.makeText(context, "🚀 Globaler OTA-Update Befehl gesendet!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ Fehler beim Senden", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("⚡ In-App Update an ALLE Geräte senden", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdminDevicesTab(
    devices: List<AdminDevice>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showBanDialog by remember { mutableStateOf(false) }
    var banTargetHwId by remember { mutableStateOf("") }
    var banTargetDeviceName by remember { mutableStateOf("") }

    var selectedVersionFilter by remember { mutableIntStateOf(0) } // 0 = Alle, 1 = Aktuell (v1.3.9), 2 = Veraltet

    val currentVersionCount = remember(devices) {
        devices.count { it.appVersion.trim() == CURRENT_APP_VERSION || it.appVersion.trim() >= CURRENT_APP_VERSION }
    }
    val outdatedVersionCount = remember(devices) {
        devices.count { it.appVersion.trim() < CURRENT_APP_VERSION }
    }

    val filteredDevices = remember(devices, selectedVersionFilter) {
        val list = when (selectedVersionFilter) {
            1 -> devices.filter { it.appVersion.trim() == CURRENT_APP_VERSION || it.appVersion.trim() >= CURRENT_APP_VERSION }
            2 -> devices.filter { it.appVersion.trim() < CURRENT_APP_VERSION }
            else -> devices
        }
        list.sortedWith(compareBy({ it.username.lowercase() }, { it.deviceName.lowercase() }))
    }

    if (showBanDialog) {
        BanDeviceDialog(
            deviceName = banTargetDeviceName,
            hwId = banTargetHwId,
            onDismiss = { showBanDialog = false }
        ) { reason ->
            showBanDialog = false
            coroutineScope.launch {
                val ok = AdminControlManager.banDevice(context, banTargetHwId, reason)
                if (ok) {
                    Toast.makeText(context, "🔴 Banned & Gekickt", Toast.LENGTH_SHORT).show()
                    onRefresh()
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        // KI Security & Auto-Ban Guard Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "🤖 KI-Security Guard & HWID Freigabe (Aktiv)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8)
                )
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
                    Text("🚀 Neue Version JETZT auf allen Geräten installieren", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "📱 Registrierte Geräte (${devices.size} Gesamt)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🟢 Aktuell (v$CURRENT_APP_VERSION): $currentVersionCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        "⚠️ Veraltet: $outdatedVersionCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (outdatedVersionCount > 0) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Filter Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DeviceFilterChipButton(
                text = "Alle (${devices.size})",
                selected = selectedVersionFilter == 0,
                onClick = { selectedVersionFilter = 0 },
                modifier = Modifier.weight(1f)
            )
            DeviceFilterChipButton(
                text = "🟢 Aktuell ($currentVersionCount)",
                selected = selectedVersionFilter == 1,
                onClick = { selectedVersionFilter = 1 },
                modifier = Modifier.weight(1f)
            )
            DeviceFilterChipButton(
                text = "⚠️ Veraltet ($outdatedVersionCount)",
                selected = selectedVersionFilter == 2,
                onClick = { selectedVersionFilter = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        if (filteredDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when (selectedVersionFilter) {
                        1 -> "Keine Geräte mit der aktuellen Version (v$CURRENT_APP_VERSION) gefunden."
                        2 -> "Keine veralteten Geräte vorhanden! Alle auf v$CURRENT_APP_VERSION. 🎉"
                        else -> "Keine Geräte registriert."
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredDevices) { dev ->
                    val isUpToDate = dev.appVersion.trim() == CURRENT_APP_VERSION || dev.appVersion.trim() >= CURRENT_APP_VERSION

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (dev.isBanned) Color(0xFF7F1D1D) else Color(0xFF0F172A)),
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
                            Text("HWID: ${dev.hwId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                            Text("Benutzer: ${dev.username} • App v${dev.appVersion}", fontSize = 10.sp, color = Color(0xFF38BDF8))

                            if (dev.isBanned) {
                                Text("⛔ Grund: ${dev.banReason.ifBlank { "Verstoß gegen Nutzungsbedingungen" }}", fontSize = 10.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.SemiBold)
                            } else if (dev.unbanned) {
                                Text("🟢 Entbannt (Auto-Bann geschützt & HWID freigegeben)", fontSize = 9.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
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
                                            coroutineScope.launch {
                                                val ok = AdminControlManager.unbanDevice(context, dev.hwId)
                                                if (ok) {
                                                    Toast.makeText(context, "🟢 Entbannt & HWID freigegeben!", Toast.LENGTH_SHORT).show()
                                                    onRefresh()
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
                                        coroutineScope.launch {
                                            val ok = AdminControlManager.deleteDevice(context, dev.hwId)
                                            if (ok) {
                                                Toast.makeText(context, "Gerät gelöscht", Toast.LENGTH_SHORT).show()
                                                onRefresh()
                                            }
                                        }
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
