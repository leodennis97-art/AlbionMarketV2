package com.example.albionmarketv2

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.clickable(enabled = enabled) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } catch (_: Exception) {}
        onClick()
    }
}

@Composable
fun ShimmerLoadingCard(modifier: Modifier = Modifier, height: Dp = 90.dp) {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFF1E293B).copy(alpha = 0.6f),
        Color(0xFF334155).copy(alpha = 0.9f),
        Color(0xFF1E293B).copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
    )
}

@Composable
fun RainbowBlinkingText(
    textPrefix: String = "",
    rainbowName: String = "DataPro - Market Companion (Unofficial)",
    textSuffix: String = "",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowBlink")
    val hueAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainbowHue"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    val rainbowColor = Color.hsv(hueAnim, 0.85f, 1.0f)

    Text(
        text = buildAnnotatedString {
            append(textPrefix)
            append(rainbowName)
            append(textSuffix)
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = rainbowColor,
        modifier = modifier.graphicsLayer {
            alpha = blinkAlpha
        }
    )
}

@Composable
fun NeonGlowCard(
    modifier: Modifier = Modifier,
    glowColors: List<Color> = listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6)),
    containerColor: Color = Color(0xFF0F172A),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlow")
    val phaseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlowPhase"
    )

    val borderBrush = Brush.linearGradient(
        colors = glowColors + glowColors,
        start = Offset(phaseAnim - 500f, 0f),
        end = Offset(phaseAnim, 500f)
    )

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderBrush),
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier
) {
    if (password.isEmpty()) return

    val (score, label, color) = remember(password) {
        when {
            password.length >= 10 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } ->
                Triple(1.0f, "Legendär (Super-Sicher)", Color(0xFF10B981))
            password.length >= 8 && password.any { it.isDigit() } ->
                Triple(0.75f, "Stark", Color(0xFF38BDF8))
            password.length >= 6 ->
                Triple(0.5f, "Mittel", Color(0xFFF59E0B))
            else ->
                Triple(0.25f, "Schwach", Color(0xFFEF4444))
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Passwort-Stärke:", fontSize = 9.sp, color = Color(0xFF94A3B8))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(score)
                    .background(color, CircleShape)
            )
            if (score < 1.0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1.0f - score)
                        .background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isOnline: Boolean = true,
    pingMs: Long = -1L,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isOnline) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF7F1D1D).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PulsingStatusDot(isOnline = isOnline)
            Text(
                text = if (pingMs >= 0) "$label • ${pingMs}ms" else label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
            )
        }
    }
}

object ZoneThemeColors {
    val RedZoneColor = Color(0xFFEF4444)      // Rot für Rote Zonen (Caerleon / Rotzone)
    val BrecilienColor = Color(0xFF38BDF8)    // Blau für Brecilien
    val BlackMarketColor = Color(0xFF64748B)  // Grau für Schmuggellager / Schwarzmarkt

    fun isBlackMarket(city: String): Boolean {
        val c = city.lowercase(Locale.ROOT)
        return c.contains("schmuggellager") || c.contains("schwarzmarkt") || c.contains("black market") || c.contains("blackmarket")
    }

    fun isBrecilien(city: String): Boolean {
        return TradeCalculator.isBrecilien(city)
    }

    fun isRedZone(city: String): Boolean {
        val c = city.lowercase(Locale.ROOT)
        val dangerous = listOf("caerleon", "arthur's rest", "merlyn's rest", "morgana's rest", "cairn drain")
        return dangerous.any { c.contains(it) } || c.contains("rot") || c.contains("red")
    }

    fun getCityZoneColor(city: String): Color {
        return when {
            isBlackMarket(city) -> BlackMarketColor // Schmuggellager -> Grau
            isBrecilien(city) -> BrecilienColor // Brecilien -> Blau
            isRedZone(city) -> RedZoneColor // Rote Zonen -> Rot
            city.equals("Martlock", ignoreCase = true) -> Color(0xFF3B82F6)
            city.equals("Lymhurst", ignoreCase = true) -> Color(0xFF10B981)
            city.equals("Bridgewatch", ignoreCase = true) -> Color(0xFFF59E0B)
            city.equals("Fort Sterling", ignoreCase = true) -> Color(0xFFE2E8F0)
            city.equals("Thetford", ignoreCase = true) -> Color(0xFFA855F7)
            else -> Color(0xFF38BDF8)
        }
    }

    fun getCityZoneBadgeText(city: String): String {
        return when {
            isBlackMarket(city) -> "🏴‍☠️ Schmuggellager"
            isBrecilien(city) -> "✨ Brecilien"
            isRedZone(city) -> "🔴 Rote Zone"
            else -> city
        }
    }

    fun getOpportunityBorderColor(buyCity: String, sellCity: String): Color {
        return when {
            isBlackMarket(buyCity) || isBlackMarket(sellCity) -> BlackMarketColor // Grau
            isRedZone(buyCity) || isRedZone(sellCity) -> RedZoneColor // Rot
            isBrecilien(buyCity) || isBrecilien(sellCity) -> BrecilienColor // Blau
            else -> Color(0xFF10B981)
        }
    }

    fun getOpportunityContainerBg(buyCity: String, sellCity: String): Color {
        return when {
            isBlackMarket(buyCity) || isBlackMarket(sellCity) -> Color(0xFF1E293B) // Dunkelgrau für Schmuggellager
            isRedZone(buyCity) || isRedZone(sellCity) -> Color(0xFF3F1D1D) // Dunkelrot für Rote Zonen
            isBrecilien(buyCity) || isBrecilien(sellCity) -> Color(0xFF0F2B48) // Dunkelblau für Brecilien
            else -> Color(0xFF1E3A4C)
        }
    }
}

@Composable
fun GlowingLanguageSelectorButton(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "LanguageGlow")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val currentLang = remember(currentLanguageCode) {
        LanguageManager.AppLanguage.entries.find { it.code.equals(currentLanguageCode, ignoreCase = true) }
            ?: LanguageManager.AppLanguage.DE
    }

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = alphaGlow)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${currentLang.flag} ${currentLang.code}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Sprache wählen",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showDialog) {
        Popup(
            onDismissRequest = { showDialog = false },
            alignment = Alignment.TopEnd,
            properties = PopupProperties(focusable = true)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .padding(top = 28.dp, end = 4.dp)
                    .width(250.dp)
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
                        Text(
                            text = "🌐 Sprache / Language",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        IconButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        LanguageManager.AppLanguage.entries.forEach { lang ->
                            val isSelected = lang.code.equals(currentLanguageCode, ignoreCase = true)
                            Surface(
                                onClick = {
                                    onLanguageSelected(lang.code)
                                    showDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(text = lang.flag, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Ausgewählt",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
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

