package com.example.albionmarketv2

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
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
    var passwordInput by remember { mutableStateOf(prefs.savedPassword) }
    var captchaNum1 by remember { mutableIntStateOf((3..12).random()) }
    var captchaNum2 by remember { mutableIntStateOf((2..9).random()) }
    var captchaInput by remember { mutableStateOf("") }
    var isCaptchaSolved by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(value = false) }
    var statusText by remember { mutableStateOf("Bitte Server-Zugangsdaten & Captcha eingeben") }

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
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "🔒 Server Login & Authentifizierung",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Benutzername", color = Color(0xFF94A3B8)) },
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
                        label = { Text("Passwort", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = captchaInput,
                        onValueChange = {
                            captchaInput = it
                            isCaptchaSolved = false
                        },
                        label = { Text("🤖 Mensch-Bestätigung: Was ist $captchaNum1 + $captchaNum2 ?", color = Color(0xFF38BDF8)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isCaptchaSolved) Color(0xFF10B981) else Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dedicated Button to verify Math Captcha first!
                    Button(
                        onClick = {
                            val expectedAnswer = captchaNum1 + captchaNum2
                            if (captchaInput.trim().toIntOrNull() == expectedAnswer) {
                                isCaptchaSolved = true
                                statusText = "🟢 Mathe-Aufgabe richtig gelöst! Jetzt einloggen..."
                                Toast.makeText(context, "🟢 Mathe-Aufgabe korrekt gelöst!", Toast.LENGTH_SHORT).show()
                            } else {
                                isCaptchaSolved = false
                                statusText = "❌ Falsches Mathe-Captcha! Bitte richtig lösen."
                                Toast.makeText(context, "❌ Falsche Antwort! Neue Aufgabe wird erzeugt.", Toast.LENGTH_SHORT).show()
                                captchaNum1 = (3..12).random()
                                captchaNum2 = (2..9).random()
                                captchaInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCaptchaSolved) Color(0xFF10B981) else Color(0xFF38BDF8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isCaptchaSolved) "✅ Mathe-Aufgabe gelöst" else "🤖 Mathe-Aufgabe prüfen",
                            fontWeight = FontWeight.Bold,
                            color = if (isCaptchaSolved) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (!isCaptchaSolved) {
                                val expectedAnswer = captchaNum1 + captchaNum2
                                if (captchaInput.trim().toIntOrNull() == expectedAnswer) {
                                    isCaptchaSolved = true
                                } else {
                                    statusText = "❌ Bitte zuerst die Mathe-Aufgabe prüfen & richtig lösen!"
                                    Toast.makeText(context, "❌ Bitte zuerst die Mathe-Aufgabe richtig lösen!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }

                            if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                statusText = "Bitte Benutzername und Passwort eingeben"
                                return@Button
                            }

                            isAuthenticating = true
                            statusText = "Verbinde mit Server..."
                            coroutineScope.launch {
                                val success = ServerSyncManager.loginWithServer(context, usernameInput, passwordInput)
                                isAuthenticating = false
                                if (success) {
                                    prefs.isUserLoggedIn = true
                                    prefs.savedUsername = usernameInput.trim()
                                    prefs.savedPassword = passwordInput.trim()
                                    if (usernameInput.trim().equals("dnnx", ignoreCase = true)) {
                                        prefs.isAdmin = true
                                    }
                                    statusText = "Anmeldung erfolgreich!"
                                    onLicenseActivated()
                                } else {
                                    statusText = "Zugangsdaten ungültig oder Konto nicht freigegeben"
                                }
                            }
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

                    Spacer(modifier = Modifier.height(8.dp))

                    TelegramLicensePurchaseSection(context = context)
                }
            }
        }
    }
}

@Composable
fun TelegramLicensePurchaseSection(context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(color = Color(0xFF334155))

        Text(
            text = "🛒 Lizenz erwerben / Telegram Support",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            textAlign = TextAlign.Center
        )

        val plans = listOf(
            Triple("1 Monat", "15 €", "Hi, ich möchte eine 1-Monat Lizenz (15€) für AlbionDataPro erwerben."),
            Triple("3 Monate", "30 €", "Hi, ich möchte eine 3-Monat Lizenz (30€) für AlbionDataPro erwerben."),
            Triple("6 Monate", "50 €", "Hi, ich möchte eine 6-Monat Lizenz (50€) für AlbionDataPro erwerben."),
            Triple("12 Monate", "100 €", "Hi, ich möchte eine 12-Monat Lizenz (100€) für AlbionDataPro erwerben."),
            Triple("👑 Lifetime", "250 €", "Hi, ich möchte eine Lifetime Lizenz (250€) für AlbionDataPro erwerben.")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val (t0, p0, m0) = plans[0]
                Button(
                    onClick = { openTelegramChat(context, m0) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088cc)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text("$t0 • $p0", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                val (t1, p1, m1) = plans[1]
                Button(
                    onClick = { openTelegramChat(context, m1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088cc)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text("$t1 • $p1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val (t2, p2, m2) = plans[2]
                Button(
                    onClick = { openTelegramChat(context, m2) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088cc)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text("$t2 • $p2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                val (t3, p3, m3) = plans[3]
                Button(
                    onClick = { openTelegramChat(context, m3) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088cc)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text("$t3 • $p3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            val (t4, p4, m4) = plans[4]
            Button(
                onClick = { openTelegramChat(context, m4) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Text("$t4 • $p4 (Lebenslang)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "👑 Unsere Exklusiven High-End Features",
                    fontSize = if (isLandscape) 13.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700)
                )

                RainbowBlinkingText(rainbowName = "AlbionDataPro", fontSize = if (isLandscape) 12.sp else 13.sp)
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

fun openTelegramChat(context: Context, message: String) {
    try {
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val url = "https://t.me/DnnxDigitalCreator?text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Telegram konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
    }
}
