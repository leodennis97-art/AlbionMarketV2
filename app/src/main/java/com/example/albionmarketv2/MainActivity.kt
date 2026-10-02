package com.example.albionmarketv2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.albionmarketv2.ui.theme.AlbionMarketV2Theme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = BillingManager(this)

        // Initialize Server Config & Start 24/7 Persistent Server Sync Service
        ServerConfigManager.initServerConfig(this)
        try {
            PersistentServerSyncService.startService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Setup Notification Channel & Request Notifications Permission
        NotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Automatic All Files Access / External Storage Permission Handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }

        // Request Ignore Battery Optimization
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        if ((powerManager != null) && (!powerManager.isIgnoringBatteryOptimizations(packageName))) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Request Overlay / Floating Bubble Permission if missing
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            } catch (_: Exception) {}
        }

        setContent {
            AlbionMarketV2Theme {
                val context = LocalContext.current
                val prefs = remember { AppPreferences(context) }

                var isCompromised by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (DeviceSecurityManager.isDeviceCompromised(context)) {
                        isCompromised = true
                    }
                }

                if (isCompromised) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚨 SICHERHEITS-ALARM\n\nGeräteintegrität verletzt (Root / Tampering erkannt).\nAus Sicherheitsgründen gesperrt.",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    return@AlbionMarketV2Theme
                }

                val initialAuthValid = remember {
                    ((prefs.savedUsername.isNotBlank() && prefs.savedPassword.isNotBlank()) || prefs.isUserLoggedIn) && LicenseManager.isLicenseValid(context)
                }
                var isUserLoggedInState by remember { mutableStateOf(initialAuthValid) }
                var isUnlockedForSession by remember { mutableStateOf(initialAuthValid) }
                var showWelcomeDialog by remember { mutableStateOf(false) }

                // Auto login / restore session on launch if saved credentials exist
                LaunchedEffect(Unit) {
                    if ((prefs.savedUsername.isNotBlank() && prefs.savedPassword.isNotBlank()) || prefs.isUserLoggedIn) {
                        if (LicenseManager.isLicenseValid(context)) {
                            prefs.isUserLoggedIn = true
                            isUserLoggedInState = true
                            isUnlockedForSession = true
                        }
                    }
                }

                // Live Popup Alert Handling for Admin Messages & Screen Alarm
                val currentPopupAlert = ServerSyncManager.activePopupAlert
                if (currentPopupAlert != null) {
                    LaunchedEffect(currentPopupAlert.id) {
                        if (currentPopupAlert.playAlarmSound) {
                            try {
                                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                                ringtone?.play()
                            } catch (_: Exception) {}
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { ServerSyncManager.activePopupAlert = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currentPopupAlert.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(currentPopupAlert.message, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                Text("Gesendet: ${currentPopupAlert.timestamp}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { ServerSyncManager.activePopupAlert = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Gelesen / Schließen", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Real-Time Background Server Authentication & Data Sync (Immediate + every 2s)
                LaunchedEffect(Unit) {
                    while (isActive) {
                        withContext(Dispatchers.IO) {
                            ServerSyncManager.pingServer(context)
                            ServerSyncManager.testAndConnectToServer(context)
                        }
                        delay(2000.milliseconds)
                    }
                }

                // Automatic Floating Overlay Bubble Management (Only when user is authenticated & logged in)
                LaunchedEffect(isUserLoggedInState) {
                    if (isUserLoggedInState && LicenseManager.isLicenseValid(context)) {
                        if (Settings.canDrawOverlays(context) && !FloatingBubbleService.isServiceRunning()) {
                            try {
                                FloatingBubbleService.startService(context)
                            } catch (_: Exception) {}
                        }
                    } else {
                        if (FloatingBubbleService.isServiceRunning()) {
                            try {
                                FloatingBubbleService.stopService(context)
                            } catch (_: Exception) {}
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Show Lockscreen Login when NOT logged in or license invalid
                    if (!isUserLoggedInState || !LicenseManager.isLicenseValid(context)) {
                        var usernameInput by remember { mutableStateOf(prefs.savedUsername) }
                        var savePasswordLocally by remember { mutableStateOf(prefs.savedPassword.isNotBlank()) }
                        var passwordInput by remember { mutableStateOf(if (savePasswordLocally) prefs.savedPassword else "") }
                        var isServerConnected by remember { mutableStateOf<Boolean?>(null) }
                        var isAuthenticating by remember { mutableStateOf(false) }
                        var licenseKeyInput by remember { mutableStateOf("") }
                        var showLoginUpdatesDialog by remember { mutableStateOf(false) }
                        var showRegisterDialog by remember { mutableStateOf(false) }
                        var isCheckingUpdate by remember { mutableStateOf(false) }
                        var updateCheckResult by remember { mutableStateOf<String?>(null) }
                        val coroutineScope = rememberCoroutineScope()

                        if (showLoginUpdatesDialog) {
                            AlertDialog(
                                onDismissRequest = { showLoginUpdatesDialog = false },
                                title = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🚀 Neueste Updates & News", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable { showLoginUpdatesDialog = false }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Willkommen bei AlbionDataPro v1.3.9!", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text("• v1.3.9: Blitzschneller 24/7 Cloud-Sync, Sicherheitssperre & automatisches OTA-Update.", fontSize = 12.sp, color = Color.White)
                                        Text("• v1.3.4: Live 24/7 Server-Sync, Anti-Cheat Schutz, Echtzeit-Uhrzeit & Gold-Bot Signale.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("Gib deine Zugangsdaten ein und verifiziere dich, um das Spiel zu betreten.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showLoginUpdatesDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Verstanden", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF1E293B)
                            )
                        }

                        LaunchedEffect(Unit) {
                            // Autonomous initial connection attempt immediately on launch
                            withContext(Dispatchers.IO) {
                                ServerSyncManager.testAndConnectToServer(context)
                            }
                            var failCount = 0
                            while (isActive) {
                                val stats = withContext(Dispatchers.IO) {
                                    var s = ServerSyncManager.pingServer(context)
                                    if (s == null) {
                                        ServerSyncManager.testAndConnectToServer(context)
                                        s = ServerSyncManager.pingServer(context)
                                    }
                                    s
                                }
                                withContext(Dispatchers.Main) {
                                    if (stats != null) {
                                        failCount = 0
                                        isServerConnected = true
                                    } else {
                                        failCount++
                                        if (failCount >= 3) { // Require 3 consecutive failures before dropping status
                                            isServerConnected = false
                                        }
                                    }
                                }
                                delay(1500.milliseconds)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "🔒 Login",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )

                                    val statusColor = when (isServerConnected) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> Color(0xFFF59E0B)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, statusColor),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = statusColor,
                                                modifier = Modifier.size(8.dp),
                                            ) {}
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (isServerConnected) {
                                                    true -> "🟢 Server-Verbindung aktiv (Cloud)"
                                                    false -> "🔴 Keine Verbindung zur Cloud"
                                                    null -> "🟡 Suche Cloud-Server (Verbinde...)"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                            )
                                        }
                                    }

                                    // Live Version Check & Update Section on Lockscreen
                                    val currentAppVer = remember { OtaUpdateManager.getInstalledVersionName(context) }
                                    val targetVer = ServerSyncManager.latestTargetVersion
                                    val isUpdateAvailableOnLockscreen = !targetVer.isNullOrBlank() && OtaUpdateManager.compareVersionStrings(targetVer, currentAppVer) > 0

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isUpdateAvailableOnLockscreen) Color(0xFF065F46) else Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, if (isUpdateAvailableOnLockscreen) Color(0xFF10B981) else Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📱 App-Version: v$currentAppVer",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = if (targetVer.isNullOrBlank()) {
                                                            "⚡ Server-Version: Wird geprüft..."
                                                        } else if (isUpdateAvailableOnLockscreen) {
                                                            "🚀 Neue Version verfügbar: v$targetVer"
                                                        } else {
                                                            "⚡ Server-Version: v$targetVer (Aktuell)"
                                                        },
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isUpdateAvailableOnLockscreen) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isUpdateAvailableOnLockscreen) Color(0xFF34D399) else Color(0xFF94A3B8)
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        isCheckingUpdate = true
                                                        ServerSyncManager.dismissedOtaVersion = null
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            val stats = ServerSyncManager.pingServer(context)
                                                            withContext(Dispatchers.Main) {
                                                                isCheckingUpdate = false
                                                                val current = OtaUpdateManager.getInstalledVersionName(context)
                                                                val target = ServerSyncManager.latestTargetVersion
                                                                if (!target.isNullOrBlank() && OtaUpdateManager.compareVersionStrings(target, current) > 0) {
                                                                    updateCheckResult = "🚀 Neue Version v$target verfügbar!"
                                                                    Toast.makeText(context, "🚀 Neue Version v$target verfügbar!", Toast.LENGTH_SHORT).show()
                                                                } else if (stats != null) {
                                                                    updateCheckResult = "✅ App ist auf dem neuesten Stand (v$current)."
                                                                    Toast.makeText(context, "✅ App ist auf dem neuesten Stand (v$current)", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    updateCheckResult = "❌ Keine Verbindung zum Server."
                                                                    Toast.makeText(context, "❌ Server nicht erreichbar.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = !isCheckingUpdate,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    if (isCheckingUpdate) {
                                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                                    } else {
                                                        Text("🔄 Updates suchen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }

                                            if (isUpdateAvailableOnLockscreen) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            Toast.makeText(context, "📥 Lade Update v$targetVer herunter...", Toast.LENGTH_SHORT).show()
                                                            val success = OtaUpdateManager.downloadAndInstallUpdate(context)
                                                            if (!success) {
                                                                Toast.makeText(context, "❌ Download fehlgeschlagen. Bitte Server-Verbindung prüfen.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("⚡ Jetzt v$targetVer installieren & aktualisieren", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                                }
                                            } else if (updateCheckResult != null) {
                                                Text(
                                                    text = updateCheckResult!!,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (updateCheckResult!!.contains("✅")) Color(0xFF34D399) else Color(0xFFF87171)
                                                )
                                            }
                                        }
                                    }

                                    if (isAuthenticating) {
                                        CircularProgressIndicator(color = Color(0xFF10B981), strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                                    } else {
                                        var passwordVisible by remember { mutableStateOf(false) }

                                        OutlinedTextField(
                                            value = usernameInput,
                                            onValueChange = { usernameInput = it },
                                            label = { Text("Benutzername", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = { Text("Passwort", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(
                                                        imageVector = if (passwordVisible) Icons.Default.Warning else Icons.Default.Lock,
                                                        contentDescription = if (passwordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                                        tint = Color(0xFF94A3B8)
                                                    )
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { savePasswordLocally = !savePasswordLocally }
                                        ) {
                                            Checkbox(
                                                checked = savePasswordLocally,
                                                onCheckedChange = { savePasswordLocally = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF10B981),
                                                    uncheckedColor = Color(0xFF94A3B8),
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "💾 Passwort lokal auf diesem Gerät speichern",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE2E8F0),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Actions: Login & Registrieren Buttons nebeneinander
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val (canLogin, lockoutSec) = LoginSecurityManager.canAttemptLogin(context)
                                                    if (!canLogin) {
                                                        Toast.makeText(context, "⏳ Zu viele Fehlversuche! Bitte warte $lockoutSec Sekunden.", Toast.LENGTH_LONG).show()
                                                        return@Button
                                                    }

                                                    val uValid = LoginSecurityManager.validateUsername(usernameInput)
                                                    if (!uValid.first) {
                                                        Toast.makeText(context, uValid.second, Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }

                                                    val pValid = LoginSecurityManager.validatePassword(passwordInput)
                                                    if (!pValid.first) {
                                                        Toast.makeText(context, pValid.second, Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }

                                                    isAuthenticating = true
                                                    lifecycleScope.launch {
                                                        val success = ServerSyncManager.loginWithServer(context, usernameInput, passwordInput)
                                                        isAuthenticating = false
                                                        if (success) {
                                                            LoginSecurityManager.resetFailedAttempts(context)
                                                            prefs.isUserLoggedIn = true
                                                            prefs.savedUsername = usernameInput.trim()
                                                            if (savePasswordLocally) {
                                                                prefs.savedPassword = passwordInput.trim()
                                                            } else {
                                                                prefs.savedPassword = ""
                                                            }
                                                            if (usernameInput.trim().equals("dnnx", ignoreCase = true)) {
                                                                prefs.isAdmin = true
                                                            }
                                                            isUserLoggedInState = true
                                                            isUnlockedForSession = true
                                                            Toast.makeText(context, "🟢 Cloud-Login & Lizenz erfolgreich verifiziert!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val lockout = LoginSecurityManager.recordFailedAttempt(context)
                                                            prefs.isUserLoggedIn = false
                                                            isUserLoggedInState = false
                                                            isUnlockedForSession = false
                                                            if (lockout > 0) {
                                                                Toast.makeText(context, "🔴 Login fehlgeschlagen! Für $lockout Sekunden gesperrt.", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                val errStr = ServerSyncManager.lastLoginErrorMessage ?: "🔴 Login fehlgeschlagen! Kein Konto, ungültige Lizenz oder keine Cloud-Verbindung."
                                                                Toast.makeText(context, errStr, Toast.LENGTH_LONG).show()
                                                            }
                                                            if (ServerSyncManager.isOtaUpdateAvailable || ServerSyncManager.latestTargetVersion != null) {
                                                                Toast.makeText(context, "🚀 Installiere neuste Version automatisch...", Toast.LENGTH_SHORT).show()
                                                                lifecycleScope.launch(Dispatchers.IO) {
                                                                    OtaUpdateManager.downloadAndInstallUpdate(context, force = true)
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text("🔑 Login", fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            OutlinedButton(
                                                onClick = { showRegisterDialog = true },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF0EA5E9))
                                            ) {
                                                Text("👤 Registrieren", fontWeight = FontWeight.Bold, color = Color(0xFF0EA5E9))
                                            }
                                        }

                                        // Lizenzschlüssel Bereich
                                        OutlinedTextField(
                                            value = licenseKeyInput,
                                            onValueChange = { licenseKeyInput = it },
                                            label = { Text("🔑 Lizenzschlüssel eingeben", color = Color(0xFF8B5CF6), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF8B5CF6),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )

                                        Button(
                                            onClick = {
                                                if (licenseKeyInput.isBlank()) {
                                                    Toast.makeText(context, "❌ Bitte einen Lizenzschlüssel eingeben", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val activated = LicenseManager.activateLicense(context, licenseKeyInput)
                                                if (activated) {
                                                    prefs.isUserLoggedIn = true
                                                    if (licenseKeyInput.uppercase(Locale.ROOT).contains("DNNX")) {
                                                        prefs.isAdmin = true
                                                        prefs.savedUsername = "dnnx"
                                                    }
                                                    isUserLoggedInState = true
                                                    isUnlockedForSession = true
                                                    Toast.makeText(context, "🟢 Lizenzschlüssel verifiziert! Erfolgreich freigeschaltet.", Toast.LENGTH_SHORT).show()
                                                    licenseKeyInput = ""
                                                } else {
                                                    Toast.makeText(context, "❌ Ungültiger Lizenzschlüssel!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = "🔑 Lizenz freischalten",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                            )
                                        }

                                        if (showRegisterDialog) {
                                            var regUsername by remember { mutableStateOf("") }
                                            var regPassword by remember { mutableStateOf("") }
                                            var regPasswordVisible by remember { mutableStateOf(false) }
                                            var isRegLoading by remember { mutableStateOf(false) }

                                            AlertDialog(
                                                onDismissRequest = { showRegisterDialog = false },
                                                title = { Text("👤 Neuen Account registrieren", fontWeight = FontWeight.Bold, color = Color.White) },
                                                text = {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("Gib deinen gewünschten Benutzernamen und ein Passwort ein. Nach der Registrierung erhältst du deine Lizenz.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                                        OutlinedTextField(
                                                            value = regUsername,
                                                            onValueChange = { regUsername = it },
                                                            label = { Text("Benutzername (min. 3 Zeichen)", fontSize = 11.sp) },
                                                            singleLine = true,
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White
                                                            ),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        OutlinedTextField(
                                                            value = regPassword,
                                                            onValueChange = { regPassword = it },
                                                            label = { Text("Passwort (min. 8 Zeichen)", fontSize = 11.sp) },
                                                            singleLine = true,
                                                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                            trailingIcon = {
                                                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                                                    Icon(
                                                                        imageVector = if (regPasswordVisible) Icons.Default.Warning else Icons.Default.Lock,
                                                                        contentDescription = if (regPasswordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                                                        tint = Color(0xFF94A3B8)
                                                                    )
                                                                }
                                                            },
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White
                                                            ),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        PasswordStrengthMeter(password = regPassword)
                                                    }
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            val uValid = LoginSecurityManager.validateUsername(regUsername)
                                                            if (!uValid.first) {
                                                                Toast.makeText(context, uValid.second, Toast.LENGTH_SHORT).show()
                                                                return@Button
                                                            }
                                                            val pValid = LoginSecurityManager.validatePassword(regPassword, isRegistration = true)
                                                            if (!pValid.first) {
                                                                Toast.makeText(context, pValid.second, Toast.LENGTH_SHORT).show()
                                                                return@Button
                                                            }
                                                            isRegLoading = true
                                                            lifecycleScope.launch {
                                                                val (ok, msg) = ServerSyncManager.registerUser(context, regUsername.trim(), regPassword.trim())
                                                                if (ok) {
                                                                    usernameInput = regUsername.trim()
                                                                    passwordInput = regPassword.trim()
                                                                    val loginSuccess = ServerSyncManager.loginWithServer(context, regUsername.trim(), regPassword.trim())
                                                                    isRegLoading = false
                                                                    showRegisterDialog = false
                                                                    if (loginSuccess) {
                                                                        LoginSecurityManager.resetFailedAttempts(context)
                                                                        prefs.isUserLoggedIn = true
                                                                        prefs.savedUsername = regUsername.trim()
                                                                        if (savePasswordLocally) {
                                                                            prefs.savedPassword = regPassword.trim()
                                                                        }
                                                                        isUserLoggedInState = true
                                                                        isUnlockedForSession = true
                                                                        Toast.makeText(context, "🟢 Account erstellt & erfolgreich eingeloggt!", Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(context, "🟢 Account erfolgreich erstellt! Bitte einloggen.", Toast.LENGTH_LONG).show()
                                                                    }
                                                                } else {
                                                                    isRegLoading = false
                                                                    Toast.makeText(context, "❌ $msg", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(if (isRegLoading) "Registriert..." else "Kostenlos registrieren", fontWeight = FontWeight.Bold)
                                                    }
                                                },
                                                dismissButton = {
                                                    OutlinedButton(
                                                        onClick = { showRegisterDialog = false },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Abbrechen", color = Color.White)
                                                    }
                                                },
                                                containerColor = Color(0xFF1E293B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        TelegramLicensePurchaseSection(context = context)
                                    }
                                }
                            }
                        }
                    } else if (!isUnlockedForSession) {
                        LicenseActivationScreen(
                            billingManager = billingManager,
                        ) {
                            isUserLoggedInState = true
                            isUnlockedForSession = true
                            showWelcomeDialog = true
                            lifecycleScope.launch {
                                try {
                                    ServerSyncManager.pingServer(this@MainActivity)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        UnlockedLockscreenContent(
                            context = context,
                            prefs = prefs,
                            onLogout = {
                                prefs.isUserLoggedIn = false
                                isUserLoggedInState = false
                                isUnlockedForSession = false
                                try {
                                    FloatingBubbleService.stopService(this@MainActivity)
                                } catch (_: Exception) {}
                                Toast.makeText(this@MainActivity, "👋 Erfolgreich abgemeldet!", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (ServerSyncManager.isOtaUpdateAvailable) {
                            AlertDialog(
                                onDismissRequest = { ServerSyncManager.dismissOtaUpdate() },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🚀 Neue Version verfügbar!", fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Eine neue Version der App ist verfügbar (${ServerSyncManager.latestTargetVersion ?: "neu"}).")
                                        Text("Möchtest du die aktuellsten Änderungen jetzt herunterladen und installieren?")
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            ServerSyncManager.dismissOtaUpdate()
                                            lifecycleScope.launch {
                                                Toast.makeText(this@MainActivity, "📥 Lade Update herunter...", Toast.LENGTH_SHORT).show()
                                                val success = OtaUpdateManager.downloadAndInstallUpdate(this@MainActivity)
                                                if (!success) {
                                                    Toast.makeText(this@MainActivity, "❌ Download fehlgeschlagen. Bitte Server-Verbindung prüfen.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("⚡ Jetzt aktualisieren & installieren", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { ServerSyncManager.dismissOtaUpdate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Später", color = Color.White)
                                    }
                                },
                            )
                        }

                        if (showWelcomeDialog) {
                            AlertDialog(
                                onDismissRequest = { showWelcomeDialog = false },
                                title = { Text(text = "🎉 Erfolgreich angemeldet!", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "🔑 Konto / Lizenz-Schlüssel:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = LicenseManager.getActivatedCode(context), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(text = "⏱️ Verbleibende Lizenzdauer:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = LicenseManager.getExpirationDateString(context), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(text = "🚀 Neueste Enterprise-Updates v1.3.5:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "• 45.000 Global Market Preismatrix Sync", fontSize = 11.sp)
                                        Text(text = "• Echtzeit WebSocket & GZIP Komprimierung", fontSize = 11.sp)
                                        Text(text = "• Anti-Debugging & Anti-Cheat Schutz aktiv", fontSize = 11.sp)
                                        Text(text = "• Bildschirmsperre Ladebildschirm Integration", fontSize = 11.sp)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showWelcomeDialog = false },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(text = "Loslegen", fontWeight = FontWeight.Bold)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billingManager.isInitialized) {
            billingManager.queryActivePurchases()
        }
        if (Settings.canDrawOverlays(this)) {
            try {
                if (!FloatingBubbleService.isServiceRunning()) {
                    FloatingBubbleService.startService(this)
                }
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun UnlockedLockscreenContent(
    context: Context,
    prefs: AppPreferences,
    onLogout: () -> Unit
) {
    var isBubbleRunning by remember { mutableStateOf(FloatingBubbleService.isServiceRunning()) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isBubbleRunning = FloatingBubbleService.isServiceRunning()
            hasOverlayPermission = Settings.canDrawOverlays(context)
            delay(1000.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "🔒 Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🟢 Freigeschaltet & Cloud Verbunden",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val username = prefs.savedUsername.ifBlank { if (prefs.isAdmin) "dnnx" else "User" }
                        Text(
                            text = "👤 Angemeldet als: $username",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "🔑 Lizenz: ${LicenseManager.getActivatedCode(context)}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "⏱️ Gültig bis: ${LicenseManager.getExpirationDateString(context)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ Floating Bubble Overlay Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        val bubbleStatusColor = if (isBubbleRunning) Color(0xFF10B981) else Color(0xFFEF4444)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = bubbleStatusColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, bubbleStatusColor)
                        ) {
                            Text(
                                text = if (isBubbleRunning) "🟢 Floating Bubble ist AKTIV" else "🔴 Floating Bubble ist DEAKTIVIERT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bubbleStatusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri()
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⚠️ Overlay-Berechtigung erteilen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isBubbleRunning) {
                                            FloatingBubbleService.stopService(context)
                                            isBubbleRunning = false
                                            Toast.makeText(context, "🛑 Floating Bubble gestoppt", Toast.LENGTH_SHORT).show()
                                        } else {
                                            FloatingBubbleService.startService(context)
                                            isBubbleRunning = true
                                            Toast.makeText(context, "⚡ Floating Bubble gestartet!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBubbleRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isBubbleRunning) "🛑 Overlay Beenden" else "⚡ Overlay Starten",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        (context as? Activity)?.moveTaskToBack(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📲 App Minimieren", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 Hinweis zur Steuerung:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Alle Albion Market Funktionen (Markt-Scanner, Handwerk, Marge, Goldmarkt, Insel, Builds & Admin) befinden sich direkt im Floating Bubble Overlay über dem Spiel.",
                            fontSize = 10.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔒 Abmelden / Sperren", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
