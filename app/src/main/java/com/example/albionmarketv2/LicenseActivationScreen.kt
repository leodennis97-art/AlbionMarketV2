package com.example.albionmarketv2

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import java.net.URLEncoder
import kotlinx.coroutines.launch

@Composable
fun LicenseActivationScreen(
    @Suppress("UNUSED_PARAMETER") billingManager: BillingManager,
    onLicenseActivated: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }
    var usernameInput by remember { mutableStateOf(prefs.savedUsername) }
    var passwordInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(value = false) }
    var statusText by remember { mutableStateOf("Bitte Server-Zugangsdaten eingeben") }

    var currentAppLang by remember { mutableStateOf(prefs.appLanguage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF06B6D4)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Header Row with Language Button on Login Screen
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
                        }
                    )
                }

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )

                Text(
                    text = LanguageManager.translateUI("🔒 Server Login & Authentifizierung", currentAppLang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Live Server Connection Status Indicator
                val isConn = ServerSyncManager.isServerConnected
                val statusBg = if (isConn) Color(0xFF065F46) else Color(0xFF991B1B)
                val statusFg = if (isConn) Color(0xFF34D399) else Color(0xFFF87171)
                val statusLabel = if (isConn) LanguageManager.translateUI("🟢 Cloud verbunden (Render Pro)", currentAppLang) else LanguageManager.translateUI("🔴 Keine Verbindung zur Render Cloud", currentAppLang)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        textAlign = TextAlign.Center
                    )
                }

                // Update Button with exact same width/dimensions as connection status box
                Button(
                    onClick = {
                        coroutineScope.launch {
                            statusText = "Suche nach Updates..."
                            val updated = OtaUpdateManager.downloadAndInstallUpdate(context, force = true)
                            if (!updated) {
                                statusText = "Kein Update verfügbar / aktuell."
                                Toast.makeText(context, "Kein Update verfügbar. App ist auf dem neuesten Stand.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Update",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageManager.translateUI("🔄 Auf Update prüfen / Installieren", currentAppLang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = LanguageManager.translateUI(statusText, currentAppLang),
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )

                if (isAuthenticating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(LanguageManager.translateUI("Benutzername", currentAppLang), color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(LanguageManager.translateUI("Passwort", currentAppLang), color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = if (passwordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    var showCaptchaDialog by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = {
                            val (canLogin, lockoutSec) = LoginSecurityManager.canAttemptLogin(context)
                            if (!canLogin) {
                                statusText = "⏳ Zu viele Fehlversuche! Bitte warte $lockoutSec Sekunden."
                                Toast.makeText(context, "⏳ Zu viele Fehlversuche! Bitte warte $lockoutSec Sekunden.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            val uValid = LoginSecurityManager.validateUsername(usernameInput)
                            if (!uValid.first) {
                                statusText = uValid.second
                                Toast.makeText(context, uValid.second, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val pValid = LoginSecurityManager.validatePassword(passwordInput)
                            if (!pValid.first) {
                                statusText = pValid.second
                                Toast.makeText(context, pValid.second, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            showCaptchaDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Einloggen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    if (showCaptchaDialog) {
                        var captchaInputDialog by remember { mutableStateOf("") }
                        var num1 by remember { mutableIntStateOf((3..12).random()) }
                        var num2 by remember { mutableIntStateOf((2..9).random()) }

                        AlertDialog(
                            onDismissRequest = { showCaptchaDialog = false },
                            title = { Text("🤖 Mensch-Bestätigung", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Was ist $num1 + $num2 ?")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = captchaInputDialog,
                                        onValueChange = { captchaInputDialog = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (captchaInputDialog.trim().toIntOrNull() == num1 + num2) {
                                        showCaptchaDialog = false
                                        isAuthenticating = true
                                        statusText = "Verbinde mit Server..."
                                        coroutineScope.launch {
                                            val success = ServerSyncManager.loginWithServer(context, usernameInput, passwordInput)
                                            isAuthenticating = false
                                            if (success) {
                                                LoginSecurityManager.resetFailedAttempts(context)
                                                prefs.isUserLoggedIn = true
                                                prefs.savedUsername = usernameInput.trim()
                                                prefs.savedPassword = passwordInput.trim()
                                                if (usernameInput.trim().equals("dnnx", ignoreCase = true)) {
                                                    prefs.isAdmin = true
                                                }
                                                statusText = "Anmeldung erfolgreich!"
                                                onLicenseActivated()
                                            } else {
                                                val lockout = LoginSecurityManager.recordFailedAttempt(context)
                                                if (lockout > 0) {
                                                    statusText = "Login fehlgeschlagen! Für $lockout Sekunden gesperrt."
                                                } else {
                                                    statusText = ServerSyncManager.lastLoginErrorMessage ?: "Anmeldung fehlgeschlagen. Bitte Daten prüfen."
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "❌ Falsche Antwort! Neue Aufgabe wird erstellt.", Toast.LENGTH_SHORT).show()
                                        num1 = (3..12).random()
                                        num2 = (2..9).random()
                                        captchaInputDialog = ""
                                    }
                                }) {
                                    Text("Prüfen & Einloggen")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showCaptchaDialog = false }) {
                                    Text("Abbrechen")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    WebsiteLicensePurchaseSection(context = context)
                }
            }
        }
    }
}

@Composable
fun WebsiteLicensePurchaseSection(context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(color = Color(0xFF334155))

        Text(
            text = "🛒 Lizenz erwerben",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, "https://www.paypal.com/ncp/payment/GB4DKRADU46SL".toUri())
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "PayPal konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "💳 Hier Lizenz erwerben (PayPal)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, "https://albionmarketv2-1.onrender.com".toUri())
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Webseite konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "🌐 Offizielle Webseite besuchen:\nalbionmarketv2-1.onrender.com",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(4.dp))

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Elegantes, modernes Feature-Highlights Panel (Sowohl Vertikal als auch Querformat perfekt optimiert)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                RainbowBlinkingText(rainbowName = "AlbionDataPro", fontSize = if (isLandscape) 14.sp else 16.sp)
            }

            Text(
                text = "Die mächtigste Markt- & Trading-Suite für Albion Online – Entwickelt für maximale Silber-Profite, absolute Sicherheit und Echtzeit-Performance.",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )

            HorizontalDivider(color = Color(0xFF1E293B))

            val features = listOf(
                Triple(
                    "🛡️ 100% Anti-Cheat & Garantierter Bann-Schutz",
                    "Maximaler Schutz & absolut sicherer Gebrauch.",
                    "Vollständig konform mit den Albion Online AGB durch rein passives, read-only Auslesen der Marktnetzwerkdaten. Kein Eingriff in den Spielclient, kein Risiko."
                ),
                Triple(
                    "💰 Garantierter Silbergewinn & Arbitrage-Scout",
                    "Automatisierte Gewinngarantie für Trader.",
                    "Präzise Berechnung aller Handelsrouten zwischen Royal-Städten, Caerleon und dem Schwarzmarkt. Ermittelt automatisch die lukrativsten Gewinne unter Berücksichtigung von Transport-Traglast und Marktsteuern."
                ),
                Triple(
                    "📊 Live-Marktmatrix für alle Hauptkategorien",
                    "Vollständige Abdeckung aller Items.",
                    "Echtzeit-Analyse aller Albion-Hauptkategorien: Rohstoffe, veredelte Ressourcen, Ausrüstung (Waffen/Rüstungen), Tränke, Nahrung, Reittiere sowie seltene Artefakte."
                ),
                Triple(
                    "🪙 Real-Time Goldmarkt & KI-Prognose",
                    "Live Gold-Sync & Portfolio-Analyse.",
                    "Live-Synchronisierung des Albion Goldpreises, KI-gestützte Trend-Trendprognosen (3 Tage & 7 Tage), automatisierte Kauf-/Verkauf-Orders sowie fortlaufendes Reingewinn-Tracking."
                ),
                Triple(
                    "🫧 Ingame Floating Bubble Overlay",
                    "Nahtloses Gaming ohne Alt-Tab.",
                    "Schwebendes Ingame-Overlay direkt über dem Spiel. Erlaubt dir blitzschnelle Preisvergleiche, Katalog-Suchen und Auftragsverwaltung mitten im laufenden Gameplay."
                ),
                Triple(
                    "⚡ Localhost Realtime Sync & Mass OTA",
                    "Hochgeschwindigkeits-Netzwerk-Engine.",
                    "3-Sekunden-Echtzeit-Synchronisierung mit dem Localhost-Server (Port 4000), automatische Geräteregistrierung sowie sofortige Push-Updates für die app-debug.apk."
                )
            )

            if (isLandscape) {
                // Adaptive 2-Spalten Layout im Querformat
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in features.indices step 2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val f1 = features[i]
                            val f2 = if (i + 1 < features.size) features[i + 1] else null

                            FeatureItemCard(
                                title = f1.first,
                                subtitle = f1.second,
                                description = f1.third,
                                modifier = Modifier.weight(1f)
                            )

                            if (f2 != null) {
                                FeatureItemCard(
                                    title = f2.first,
                                    subtitle = f2.second,
                                    description = f2.third,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Vertikales 1-Spalten Layout im Hochformat
                features.forEach { (title, subtitle, description) ->
                    FeatureItemCard(
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItemCard(
    title: String,
    subtitle: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF38BDF8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Geprüft ✓",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
        }

        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFD700)
        )

        Text(
            text = description,
            fontSize = 9.5.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 13.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
    }
}
