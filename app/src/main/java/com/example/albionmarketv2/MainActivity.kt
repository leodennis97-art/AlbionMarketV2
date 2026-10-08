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
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = BillingManager(this)

        // Initialize Server Config
        ServerConfigManager.initServerConfig(this)

        // Setup Notification Channel & Request Notifications Permission
        NotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Request Overlay / Floating Bubble Permission if missing
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri(),
                )
                startActivity(intent)
            } catch (_: Exception) {}
        }

        setContent {
            AlbionMarketV2Theme {
                val context = LocalContext.current
                val prefs = remember { AppPreferences(context) }

                var isCompromised by remember { mutableStateOf(value = false) }
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
                        contentAlignment = Alignment.Center,
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

                var isUserLoggedInState by remember { mutableStateOf(value = false) }
                var isUnlockedForSession by remember { mutableStateOf(value = false) }
                var showWelcomeDialog by remember { mutableStateOf(value = false) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (!prefs.isUserLoggedIn || !LicenseManager.isLicenseValid(context)) {
                                isUserLoggedInState = false
                                isUnlockedForSession = false
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Kein automatischer Login beim Start: Jeder Start erfordert manuelle Anmeldung mit Cloud- und Lizenzprüfung
                LaunchedEffect(Unit) {
                    prefs.isUserLoggedIn = false
                    isUserLoggedInState = false
                    isUnlockedForSession = false
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

                // Real-Time Background Server Authentication & Data Sync (Low-Frequency Background Check)
                LaunchedEffect(Unit) {
                    while (isActive) {
                        try {
                            withContext(Dispatchers.IO) {
                                ServerSyncManager.pingServer(context)
                            }
                        } catch (_: Exception) {}
                        delay(30000.milliseconds)
                    }
                }

                // Automatic Background Notification & Server Management (Only when user is authenticated & logged in)
                LaunchedEffect(isUserLoggedInState) {
                    if (isUserLoggedInState && LicenseManager.isLicenseValid(context)) {
                        try {
                            PersistentServerSyncService.startService(context)
                        } catch (_: Exception) {}
                    } else {
                        try {
                            PersistentServerSyncService.stopService(context)
                        } catch (_: Exception) {}

                        if (FloatingBubbleService.isServiceRunning()) {
                            try {
                                FloatingBubbleService.stopService(context)
                            } catch (_: Exception) {}
                        }
                    }
                }

                var currentAppLang by remember { mutableStateOf(prefs.appLanguage) }

                CompositionLocalProvider(LocalAppLanguage provides currentAppLang) {
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
                        var isRegistrationMode by remember { mutableStateOf(false) }
                        var showLoginUpdatesDialog by remember { mutableStateOf(false) }
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
                                        Text("Willkommen bei DataPro - Market Companion (Unofficial) v$CURRENT_APP_VERSION!", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text("• v$CURRENT_APP_VERSION: Blitzschneller 24/7 Cloud-Sync, Sicherheitssperre & automatisches OTA-Update.", fontSize = 12.sp, color = Color.White)
                                        Text("• v3.1.4: Live 24/7 Server-Sync, Anti-Cheat Schutz, Echtzeit-Uhrzeit & Gold-Bot Signale.", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
                            var failCount = 0
                            while (isActive) {
                                try {
                                    val stats = withContext(Dispatchers.IO) {
                                        ServerSyncManager.pingServer(context)
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (stats != null) {
                                            failCount = 0
                                            isServerConnected = true
                                        } else {
                                            failCount++
                                            if (failCount >= 3) {
                                                isServerConnected = false
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                                delay(15000.milliseconds)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0B1120))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    // 1. BRANDING LOGO HEADER
                                    Image(
                                        painter = painterResource(id = R.drawable.adp_logo),
                                        contentDescription = "DataPro Logo",
                                        modifier = Modifier.size(72.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "DataPro - Market Companion",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFF10B981))
                                        ) {
                                            Text(
                                                text = "v$CURRENT_APP_VERSION",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Echtzeit Markt-Analysen & In-Game Overlay Bot",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    var currentAppLang by remember { mutableStateOf(prefs.appLanguage) }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌐 ${LanguageManager.getString("lang_select", currentAppLang)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )

                                        GlowingLanguageSelectorButton(
                                            currentLanguageCode = currentAppLang,
                                            onLanguageSelected = { newLang ->
                                                prefs.appLanguage = newLang
                                                currentAppLang = newLang
                                                AiTranslationEngine.setLanguage(newLang)
                                            }
                                        )
                                    }

                                    // 2. SEGMENTED TAB SWITCHER
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { isRegistrationMode = false },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (!isRegistrationMode) Color(0xFF2563EB) else Color.Transparent
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = "🔑 ${LanguageManager.translateUI("Anmelden", currentAppLang)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isRegistrationMode) Color.White else Color(0xFF94A3B8)
                                                )
                                            }

                                            Button(
                                                onClick = { isRegistrationMode = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isRegistrationMode) Color(0xFF10B981) else Color.Transparent
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = "📝 ${LanguageManager.translateUI("Registrieren", currentAppLang)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRegistrationMode) Color.White else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }

                                    // 3. SERVER STATUS BADGE
                                    val statusColor = when (isServerConnected) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> Color(0xFFF59E0B)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = statusColor,
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (isServerConnected) {
                                                        true -> "Cloud Server Online"
                                                        false -> "Keine Cloud Verbindung"
                                                        null -> "Verbinde mit Cloud..."
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }

                                            Text(
                                                text = "🔄 Server prüfen",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.clickable {
                                                    isServerConnected = null
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val stats = ServerSyncManager.pingServer(context)
                                                        withContext(Dispatchers.Main) {
                                                            isServerConnected = (stats != null)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    // 4. FORM INPUT FIELDS
                                    if (!isAuthenticating) {
                                        OutlinedTextField(
                                            value = usernameInput,
                                            onValueChange = { usernameInput = it },
                                            label = { Text(LanguageManager.translateUI("Benutzername", currentAppLang), color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        var passwordVisible by remember { mutableStateOf(false) }

                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = { Text(LanguageManager.translateUI("Passwort", currentAppLang), color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Text(if (passwordVisible) "👁️" else "🙈", fontSize = 14.sp)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (isRegistrationMode) {
                                            OutlinedTextField(
                                                value = licenseKeyInput,
                                                onValueChange = { licenseKeyInput = it },
                                                label = { Text("Lizenzschlüssel (falls vorhanden) - Optional", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFFF59E0B),
                                                    unfocusedBorderColor = Color(0xFF334155),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0xFF0F172A),
                                                    unfocusedContainerColor = Color(0xFF0F172A)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // REGISTRATION NOTICE CARD
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF38BDF8).copy(alpha = 0.1f),
                                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "💡 Gib deinen Lizenzschlüssel ein oder erstelle ein neues Konto zur Freischaltung.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }

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
                                                text = "💾 Zugangsdaten merken (Auto-Login)",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE2E8F0),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // 5. ACTION BUTTONS
                                        if (isRegistrationMode) {
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

                                                    val pValid = LoginSecurityManager.validatePassword(passwordInput, isRegistration = true)
                                                    if (!pValid.first) {
                                                        Toast.makeText(context, pValid.second, Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }

                                                    isAuthenticating = true
                                                    lifecycleScope.launch {
                                                        val (regSuccess, regMsg) = ServerSyncManager.registerUser(context, usernameInput, passwordInput, licenseKeyInput)
                                                        if (regSuccess) {
                                                            if (licenseKeyInput.isNotBlank()) {
                                                                LicenseManager.activateLicense(context, licenseKeyInput)
                                                            }

                                                            val loginSuccess = ServerSyncManager.loginWithServer(context, usernameInput, passwordInput)
                                                            isAuthenticating = false
                                                            if (loginSuccess) {
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
                                                                Toast.makeText(context, "🟢 Account & Lizenz erfolgreich registriert & freigeschaltet!", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                prefs.savedUsername = usernameInput.trim()
                                                                isRegistrationMode = false
                                                                Toast.makeText(
                                                                    context,
                                                                    "🟢 Account erfolgreich registriert!",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        } else {
                                                            isAuthenticating = false
                                                            Toast.makeText(context, "❌ Registrierungsfehler: $regMsg", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                            ) {
                                                Text("🚀 Account Erstellen", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            }
                                        } else {
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
                                                            Toast.makeText(context, "🟢 Login verifiziert & freigeschaltet!", Toast.LENGTH_SHORT).show()
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

                                                                if (errStr.contains("App aktualisieren") || errStr.contains("veraltet") || ServerSyncManager.isOtaUpdateAvailable) {
                                                                    val targetV = ServerSyncManager.latestTargetVersion ?: "3.1.9"
                                                                    Toast.makeText(context, "🚀 App aktualisieren: Installiere neueste Version v$targetV...", Toast.LENGTH_LONG).show()
                                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                                        OtaUpdateManager.downloadAndInstallUpdate(context, force = true)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                            ) {
                                                Text("🔑 In Konto Anmelden", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🚀 News & Updates",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.clickable { showLoginUpdatesDialog = true }
                                            )

                                            Text(
                                                text = "🌐 Offizielle Website",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.clickable {
                                                    try {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, "https://albionmarketv2-1.onrender.com".toUri())
                                                        context.startActivity(webIntent)
                                                    } catch (_: Exception) {}
                                                }
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(36.dp))
                                            Text("Verifiziere Daten mit Cloud Server...", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
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
                            prefs = prefs
                        ) {
                            try {
                                prefs.isUserLoggedIn = false
                                isUserLoggedInState = false
                                isUnlockedForSession = false
                                try {
                                    FloatingBubbleService.stopService(this@MainActivity)
                                } catch (_: Exception) {}
                                try {
                                    PersistentServerSyncService.stopService(this@MainActivity)
                                } catch (_: Exception) {}
                                Toast.makeText(this@MainActivity, "👋 Erfolgreich abgemeldet!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (ServerSyncManager.isOtaUpdateAvailable) {
                            AlertDialog(
                                onDismissRequest = { ServerSyncManager.dismissOtaUpdate() },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🚀 Neue Version verfügbar (${ServerSyncManager.latestTargetVersion ?: "3.2.1"})!", fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Eine neue Version ist verfügbar. Die installierte App ist älter als die aktuelle Server-Version.")
                                        Text("✨ Was ist neu / Changelog:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                                        Text("• Optimierte Kauf- & Verkaufspreise für exakte Handelschancen\n• Vollständige Entfernung aller Telegram-Elemente & Weiterleitung zur Webseite\n• Live-Online-Nutzerübersicht im Admin-Dashboard\n• Verbesserte, stabile Abmelde- & Sperr-Funktion\n• 24/7 Cloud-Sync & Performance-Updates", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Möchtest du das Update jetzt herunterladen und installieren?")
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

                                        Text(text = "🚀 Neueste Enterprise-Updates v$CURRENT_APP_VERSION:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::billingManager.isInitialized) {
            billingManager.queryActivePurchases()
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
            delay(5000.milliseconds)
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
                var currentAppLang by remember { mutableStateOf(prefs.appLanguage) }

                // Top Header Row with Language Button on Bubble Activation Screen
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌐 ${LanguageManager.getString("lang_select", currentAppLang)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    GlowingLanguageSelectorButton(
                        currentLanguageCode = currentAppLang,
                        onLanguageSelected = { newLang ->
                            prefs.appLanguage = newLang
                            currentAppLang = newLang
                            AiTranslationEngine.setLanguage(newLang)
                            LanguageManager.updateAppLocale(context, newLang)
                        }
                    )
                }
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
                                        text = LanguageManager.translateUI(if (isBubbleRunning) "🛑 Overlay Beenden" else "⚡ Overlay Starten", currentAppLang),
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
                                    Text(LanguageManager.translateUI("📲 App Minimieren", currentAppLang), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
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
                        Text(LanguageManager.translateUI("🔒 Abmelden / Sperren", currentAppLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
