package com.example.albionmarketv2

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import java.util.Date

data class AdminUser(
    val username: String,
    val password: String = "••••••••",
    val isAdmin: Boolean,
    val isLicensed: Boolean,
    val licenseExpiresAt: String
)

data class AdminLicense(
    val key: String,
    val tier: String,
    val price: String,
    val created: String,
    val note: String
)

data class AdminDevice(
    val hwId: String,
    val deviceName: String,
    val appVersion: String,
    val username: String,
    val lastSeen: String,
    val licenseExpiresAt: String,
    val isBanned: Boolean
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

            if (conn.responseCode in 200..299) {
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

            if (conn.responseCode in 200..299) {
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
                    licenseExpiresAt = obj.optString("licenseExpiresAt", "")
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
        val now = Date()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val bannedUntil = obj.optString("bannedUntil", "")
            val isBanned = !bannedUntil.isNullOrBlank()
            list.add(
                AdminDevice(
                    hwId = obj.optString("hwId", ""),
                    deviceName = obj.optString("deviceName", "Android Device"),
                    appVersion = obj.optString("appVersion", "1.3.8"),
                    username = obj.optString("username", "Unbekannt"),
                    lastSeen = obj.optString("lastSeen", ""),
                    licenseExpiresAt = obj.optString("licenseExpiresAt", ""),
                    isBanned = isBanned
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

    suspend fun banDevice(context: Context, hwId: String): Boolean {
        val base = getBaseUrl(context)
        val json = JSONObject().apply { put("hwId", hwId) }
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
                        text = { Text("🔑 Lizenzen", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("👥 Nutzer", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📱 Geräte", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                    }
                } else {
                    when (selectedTab) {
                        0 -> AdminLicensesTab(licenses, onRefresh = { refreshAll() })
                        1 -> AdminUsersTab(users, onRefresh = { refreshAll() })
                        2 -> AdminDevicesTab(devices, onRefresh = { refreshAll() })
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
                    tiers.take(3).forEach { (code, label) ->
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
                    tiers.drop(3).forEach { (code, label) ->
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
                                Toast.makeText(context, "🟢 Lizenz erstellt!", Toast.LENGTH_SHORT).show()
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
                    modifier = Modifier.fillMaxWidth()
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
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👤 Neuen Benutzer erstellen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))

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

        Text("👥 Alle Benutzer (${sortedUsers.size}) - Alphabetisch sortiert", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(sortedUsers) { usr ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${usr.username} ${if (usr.isAdmin) "👑 (Admin - Keine Lizenz erforderlich)" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (usr.isAdmin) Color(0xFFFFD700) else Color.White
                            )
                            Text("Passwort: ${usr.password}", fontSize = 10.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                            Text(if (usr.isAdmin) "Lizenz: Unbegrenzt (Admin)" else "Lizenz aktiv: ${if (usr.isLicensed) "Ja" else "Nein"}", fontSize = 10.sp, color = if (usr.isAdmin) Color(0xFF10B981) else Color(0xFF94A3B8))
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
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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

@Composable
fun AdminDevicesTab(
    devices: List<AdminDevice>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sortedDevices = remember(devices) { devices.sortedWith(compareBy({ it.username.lowercase() }, { it.deviceName.lowercase() })) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Text("📱 Registrierte Geräte (${sortedDevices.size}) - nach Benutzer & Gerät sortiert", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(sortedDevices) { dev ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (dev.isBanned) Color(0xFF7F1D1D) else Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(dev.deviceName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            Text(if (dev.isBanned) "🔴 GEBANNT" else "🟢 Aktiv", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (dev.isBanned) Color(0xFFEF4444) else Color(0xFF10B981))
                        }
                        Text("HWID: ${dev.hwId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                        Text("Benutzer: ${dev.username} • App v${dev.appVersion}", fontSize = 10.sp, color = Color(0xFF38BDF8))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            if (dev.isBanned) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val ok = AdminControlManager.unbanDevice(context, dev.hwId)
                                            if (ok) {
                                                Toast.makeText(context, "Entbannt", Toast.LENGTH_SHORT).show()
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
                                        coroutineScope.launch {
                                            val ok = AdminControlManager.banDevice(context, dev.hwId)
                                            if (ok) {
                                                Toast.makeText(context, "Banned & Gekickt", Toast.LENGTH_SHORT).show()
                                                onRefresh()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(1.dp)
                                ) {
                                    Text("Bannen / Kicken", fontSize = 10.sp, color = Color.White)
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
