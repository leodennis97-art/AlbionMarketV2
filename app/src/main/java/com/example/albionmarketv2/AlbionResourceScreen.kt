package com.example.albionmarketv2

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class NumberCommaTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        val formatted = try {
            val parsed = originalText.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
            NumberFormat.getNumberInstance(Locale.GERMANY).format(parsed)
        } catch (_: Exception) {
            originalText
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isBlank()) return 0
                val clampedOffset = offset.coerceIn(0, originalText.length)
                var transformedOffset = 0
                var originalCount = 0
                for (i in formatted.indices) {
                    if (originalCount == clampedOffset) break
                    if (formatted[i].isDigit()) originalCount++
                    transformedOffset++
                }
                return transformedOffset.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (formatted.isBlank()) return 0
                val clampedOffset = offset.coerceIn(0, formatted.length)
                var originalOffset = 0
                for (i in 0 until clampedOffset) {
                    if ((i < formatted.length) && formatted[i].isDigit()) originalOffset++
                }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun RainbowAlbionDataProTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTitle")
    val hueOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "HueTitle"
    )

    val rainbowColors = listOf(
        Color.hsv((hueOffset + 0f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 60f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 120f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 180f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 240f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 300f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 360f) % 360f, 0.95f, 1f)
    )

    Text(
        text = "AlbionDataPro",
        style = TextStyle(
            brush = Brush.horizontalGradient(rainbowColors),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbionResourceScreen(
    viewModel: AlbionResourceViewModel = SharedViewModelProvider.get(LocalContext.current.applicationContext as Application),
    onLogout: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    Scaffold(
        topBar = {
            Column {
                // Top Premium Status Bar / Badge
                val remainingDays = LicenseManager.getRemainingPremiumDays(context)
                val isUnlimited = prefs.isAdmin || prefs.savedUsername.equals("dnnx", ignoreCase = true) || prefs.savedUsername.equals("opa", ignoreCase = true)

                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUnlimited) "👑 Premium: Unbegrenzt aktiv (Admin/VIP)" else "💎 Premium: $remainingDays verbleibende Tage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUnlimited || (remainingDays > 0)) Color(0xFF065F46) else Color(0xFF7F1D1D)
                        ) {
                            Text(
                                text = if (isUnlimited || (remainingDays > 0)) "AKTIV" else "ABGELAUFEN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnlimited || (remainingDays > 0)) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = R.drawable.aot_logo,
                                contentDescription = "AOT Logo",
                                fallback = null,
                                error = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                RainbowAlbionDataProTitle()
                                Text(
                                    text = "Echtzeit-Analyse & Trade Alerts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (FloatingBubbleService.isServiceRunning()) {
                                FloatingBubbleService.stopService(context)
                                Toast.makeText(context, "Overlay Bubble gestoppt", Toast.LENGTH_SHORT).show()
                            } else {
                                val prefs = AppPreferences(context)
                                val isLoggedIn = prefs.isUserLoggedIn && LicenseManager.isLicenseValid(context)
                                if (!isLoggedIn) {
                                    Toast.makeText(context, "Bitte zuerst anmelden!", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                if (Settings.canDrawOverlays(context)) {
                                    FloatingBubbleService.startService(context)
                                    Toast.makeText(context, "Overlay Bubble gestartet", Toast.LENGTH_SHORT).show()
                                    if (prefs.hideAppOnBubbleActivate) {
                                        (context as? Activity)?.moveTaskToBack(true)
                                    }
                                } else {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri()
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Berechtigung erforderlich", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Floating Overlay Bubble starten",
                                tint = if (FloatingBubbleService.isServiceRunning()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { viewModel.onOpenSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                // Modern Pill Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = remember {
                        listOf(
                            Triple(0, Icons.AutoMirrored.Filled.List, "tab_orders"),
                            Triple(1, Icons.Default.ShoppingCart, "tab_calculator"),
                            Triple(2, Icons.Default.Search, "tab_catalog"),
                            Triple(3, Icons.Default.Settings, "tab_crafting"),
                            Triple(4, Icons.Default.Info, "tab_island"),
                            Triple(5, Icons.Default.Star, "tab_builds"),
                            Triple(6, Icons.Default.Warning, "tab_events"),
                            Triple(7, Icons.Default.Star, "tab_gold")
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tabs, key = { it.first }) { (index, icon, langKey) ->
                            val isSelected = uiState.activeTab == index
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.clickable { viewModel.onTabSelected(index) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = LanguageManager.getString(langKey, uiState.appLanguage),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Tab Title Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val activeTitle = when (uiState.activeTab) {
                        0 -> LanguageManager.getString("tab_orders", uiState.appLanguage)
                        1 -> LanguageManager.getString("tab_calculator", uiState.appLanguage)
                        2 -> LanguageManager.getString("tab_catalog", uiState.appLanguage)
                        3 -> LanguageManager.getString("tab_crafting", uiState.appLanguage)
                        4 -> LanguageManager.getString("tab_island", uiState.appLanguage)
                        5 -> LanguageManager.getString("tab_builds", uiState.appLanguage)
                        6 -> LanguageManager.getString("tab_events", uiState.appLanguage)
                        7 -> LanguageManager.getString("tab_gold", uiState.appLanguage)
                        else -> LanguageManager.getString("app_title", uiState.appLanguage)
                    }
                    Text(
                        text = activeTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState.activeTab) {
                0 -> OrdersAndStatsTabContent(viewModel = viewModel, uiState = uiState)
                1 -> CalculatorTabContent(viewModel = viewModel, uiState = uiState)
                2 -> CatalogTabContent(viewModel = viewModel, uiState = uiState)
                3 -> CraftingTabContent(viewModel = viewModel, uiState = uiState)
                4 -> IslandTabContent(viewModel = viewModel, uiState = uiState)
                5 -> EquipmentBuildsTabContent(viewModel = viewModel, uiState = uiState)
                6 -> EventsAndMonstersTabContent(uiState = uiState)
                7 -> GoldMarketTabContent(viewModel = viewModel, uiState = uiState)
            }
        }

        // Settings Dialog (Zahnrad mit "MadeByDnnx" Regenbogen & Order-Statistik)
        if (uiState.showSettingsDialog) {
            AppSettingsDialog(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { viewModel.onDismissSettings() }
            ) {
                viewModel.onDismissSettings()
                if (onLogout != null) {
                    onLogout()
                } else {
                    prefs.isUserLoggedIn = false
                    try {
                        val loginIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(loginIntent)
                    } catch (_: Exception) {}
                    Handler(Looper.getMainLooper()).post {
                        try { FloatingBubbleService.stopService(context) } catch (_: Exception) {}
                        try { PersistentServerSyncService.stopService(context) } catch (_: Exception) {}
                    }
                }
            }
        }

        // Details Bottom Sheet
        uiState.selectedResource?.let { resource ->
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { viewModel.onDismissDetails() },
                sheetState = sheetState
            ) {
                ResourceDetailContent(
                    resource = resource,
                    prices = uiState.marketPrices[resource.fullId] ?: uiState.marketPrices[resource.id] ?: emptyList(),
                    onCopyId = { idToCopy ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Albion Item ID", idToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Item ID in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        // Order Limit Error Dialog (Visible from all tabs)
        val errMsg = uiState.orderErrorMsg
        if (errMsg != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearOrderError() },
                title = { Text("⚠️ Auftrags-Limit erreicht", fontWeight = FontWeight.Bold) },
                text = { Text(errMsg, fontSize = 13.sp) },
                confirmButton = {
                    Button(onClick = { viewModel.clearOrderError() }) {
                        Text("Verstanden")
                    }
                }
            )
        }
    }
}



// ----------------------------------------------------
// TAB 0: AUFTRÄGE & HISTORIE (MAX 10 PARALLELE AUFTRÄGE)
// ----------------------------------------------------
@Composable
fun OrdersAndStatsTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    var orderToComplete by remember { mutableStateOf<TradeOrder?>(null) }
    var orderToEdit by remember { mutableStateOf<TradeOrder?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Live Server Download Analytics Card
        if (uiState.hourlyDownloads24h.isNotEmpty() || uiState.totalServerDownloads > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ServerDownloadsChart(
                            totalDownloads = uiState.totalServerDownloads,
                            hourlyStats = uiState.hourlyDownloads24h
                        )
                    }
                }
            }
        }
        // Overall Statistics Summary Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "📊 Gesamtbilanz & Handels-Statistik",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Abgeschlossene Trades", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${uiState.completedOrders.size} Trades", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gesamter Reingewinn", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val profit = uiState.totalCompletedProfit
                            Text(
                                text = "${if (profit >= 0) "+" else ""}${numberFormat.format(profit)} Silber",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (profit >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ausgegeben: ${numberFormat.format(uiState.totalCompletedSpent)} Silber",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Netto-Umsatz: ${numberFormat.format(uiState.totalCompletedEarned)} Silber",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Completed Orders History (Placed before Active Orders with Total Revenue & Edit/Delete support)
        if (uiState.completedOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📜 Historie angenommener Aufträge (${uiState.completedOrders.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Gesamtgewinn (Netto): ${if (uiState.totalCompletedProfit >= 0) "+" else ""}${numberFormat.format(uiState.totalCompletedProfit)} Silber",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (uiState.totalCompletedProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            items(uiState.completedOrders, key = { it.id }) { order ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${order.resourceNameDe} (${order.tierText})", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            val profit = order.realizedNetProfit
                            Text(
                                text = "${if (profit >= 0) "+" else ""}${numberFormat.format(profit)} Silber",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (profit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                            )
                            IconButton(onClick = { viewModel.deleteCompletedOrder(order.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Text("Route: ${order.buyCity} ➔ ${order.sellCity} | Datum: ${order.completedDate ?: ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stückpreis Kauf: ${numberFormat.format(order.effectiveBuyPrice)} Silber | Verkauf: ${numberFormat.format(order.effectiveSellPrice)} Silber | Menge: ${numberFormat.format(order.effectiveUnits)} Stk.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ausgegeben: ${numberFormat.format(order.actualSilverSpent ?: order.targetInvestment)} Silber | Eingenommen: ${numberFormat.format(order.actualSilverEarned ?: (order.targetInvestment + order.targetNetProfit))} Silber",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { orderToEdit = order },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auftrag bearbeiten", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section: Active Orders (Limit 3)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📋 Aktive Aufträge (${uiState.activeOrders.size} / Max 10)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.activeOrders.size >= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${uiState.activeOrders.size}/10 Aktiv",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.activeOrders.size >= 10) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (uiState.activeOrders.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Keine aktiven Aufträge. Klicke bei einer Handelschance auf ein Item, um einen Auftrag anzunehmen.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val sortedActiveOrders = uiState.activeOrders.sortedBy { it.buyCity }
            items(sortedActiveOrders, key = { it.id }) { order ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = getTierColor(order.tier)
                            ) {
                                Text(order.tierText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(order.resourceNameDe, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))

                            if (order.isPriceStillValid(uiState.marketPrices)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Text("+${numberFormat.format(order.targetNetProfit)} Silber", fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val buyCityTranslated = LanguageManager.getCityTranslation(order.buyCity, uiState.appLanguage)
                        val sellCityTranslated = LanguageManager.getCityTranslation(order.sellCity, uiState.appLanguage)

                        Text("📍 Route: $buyCityTranslated ➔ $sellCityTranslated", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("Kaufpreis: ${numberFormat.format(order.buyPrice)} Silber", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("Verkaufspreis: ${numberFormat.format(order.sellPrice)} Silber", fontSize = 12.sp, color = Color(0xFF66BB6A), fontWeight = FontWeight.SemiBold)
                        }

                        val recBuy = if (order.recommendedBuyOrderPrice > 0) order.recommendedBuyOrderPrice else (order.buyPrice * 0.88).toInt().coerceAtLeast(1)
                        val recSell = if (order.recommendedSellOrderPrice > 0) order.recommendedSellOrderPrice else (order.sellPrice * 1.08).toInt().coerceAtLeast(1)
                        val localContext = LocalContext.current
                        val localPrefs = remember { AppPreferences(localContext) }
                        val hasPremium = localPrefs.hasPremium
                        val taxRate = if (hasPremium) 0.04 else 0.08
                        val netRecSellUnit = (recSell * (1.0 - taxRate - 0.025)).toLong()
                        val netRecUnitProfit = netRecSellUnit - recBuy
                        val totalRecNetProfit = netRecUnitProfit * order.plannedUnits

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("🛒 Kauforder (${order.buyCity}): ${numberFormat.format(recBuy)} S.", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                Text("📈 Verkauforder (${order.sellCity}): ${numberFormat.format(recSell)} S.", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                Text("💡 KI Max-Marge: +${numberFormat.format(totalRecNetProfit)} S. Netto (+${numberFormat.format(netRecUnitProfit)} S./Stk.)", fontSize = 11.sp, color = Color(0xFF4ADE80), fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Text("Menge: ${numberFormat.format(order.plannedUnits)} Stk. | Angenommen: ${order.acceptedDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { orderToComplete = order },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buchen", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.discardTradeOrder(order.id) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stornieren", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog to complete order
    orderToComplete?.let { order ->
        var unitBuyInput by remember(order.id) { mutableStateOf(order.buyPrice.toString()) }
        var unitSellInput by remember(order.id) { mutableStateOf(order.sellPrice.toString()) }
        var unitsInput by remember(order.id) { mutableStateOf(order.plannedUnits.toString()) }

        val uBuy = unitBuyInput.toIntOrNull() ?: order.buyPrice
        val uSell = unitSellInput.toIntOrNull() ?: order.sellPrice
        val units = unitsInput.toIntOrNull() ?: order.plannedUnits

        val taxPerUnit = (uSell * (uiState.marketTaxPercent / 100.0)).toLong()
        val setupFeePerUnit = (uSell * 0.025).toLong()
        val netSellPrice = uSell.toLong() - taxPerUnit - setupFeePerUnit

        val computedSpent = units.toLong() * uBuy.toLong()
        val computedEarned = units.toLong() * netSellPrice
        val computedProfit = computedEarned - computedSpent

        AlertDialog(
            onDismissRequest = { orderToComplete = null },
            title = { Text("Auftrag buchen", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tatsächliche Stückpreise & Stückzahl eintragen:")

                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Gekaufte Stückzahl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitBuyInput,
                        onValueChange = { unitBuyInput = it },
                        label = { Text("Kauf-Stückpreis (Silber)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitSellInput,
                        onValueChange = { unitSellInput = it },
                        label = { Text("Verkauf-Stückpreis (Silber)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📊 Umsatz (Netto): ${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedEarned)} Silber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ausgegeben: ${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedSpent)} Silber",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gewinn/Verlust: ${if (computedProfit >= 0) "+" else ""}${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedProfit)} Silber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (computedProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.completeTradeOrderWithDetails(order.id, units, uBuy, uSell)
                    orderToComplete = null
                }) {
                    Text("Buchen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { orderToComplete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Dialog to edit completed order
    orderToEdit?.let { order ->
        EditCompletedOrderDialog(
            order = order,
            uiState = uiState,
            onDismiss = { orderToEdit = null },
            onSave = { units, buyPrice, sellPrice ->
                viewModel.updateCompletedOrderWithDetails(order.id, units, buyPrice, sellPrice)
                orderToEdit = null
            }
        )
    }
}

@Composable
fun EditCompletedOrderDialog(
    order: TradeOrder,
    uiState: ResourceUiState,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var unitsInput by remember(order.id) { mutableStateOf((order.actualUnits ?: order.plannedUnits).toString()) }
    var buyPriceInput by remember(order.id) { mutableStateOf((order.actualBuyPrice ?: order.buyPrice).toString()) }
    var sellPriceInput by remember(order.id) { mutableStateOf((order.actualSellPrice ?: order.sellPrice).toString()) }

    val units = unitsInput.toIntOrNull() ?: order.plannedUnits
    val uBuy = buyPriceInput.toIntOrNull() ?: order.buyPrice
    val uSell = sellPriceInput.toIntOrNull() ?: order.sellPrice

    val taxPerUnit = (uSell * (uiState.marketTaxPercent / 100.0)).toLong()
    val setupFeePerUnit = (uSell * 0.025).toLong()
    val netSellPrice = uSell.toLong() - taxPerUnit - setupFeePerUnit

    val computedSpent = units.toLong() * uBuy.toLong()
    val computedEarned = units.toLong() * netSellPrice
    val computedProfit = computedEarned - computedSpent

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ Abgeschlossenen Auftrag bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${order.resourceNameDe} (${order.tierText})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Route: ${order.buyCity} ➔ ${order.sellCity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = unitsInput,
                    onValueChange = { unitsInput = it },
                    label = { Text("Gekaufte Stückzahl / Menge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = buyPriceInput,
                    onValueChange = { buyPriceInput = it },
                    label = { Text("Kauf-Stückpreis (Silber)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellPriceInput,
                    onValueChange = { sellPriceInput = it },
                    label = { Text("Verkauf-Stückpreis (Silber)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📊 Umsatz (Netto): ${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedEarned)} Silber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ausgegeben: ${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedSpent)} Silber",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gewinn/Verlust: ${if (computedProfit >= 0) "+" else ""}${NumberFormat.getNumberInstance(Locale.GERMANY).format(computedProfit)} Silber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (computedProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(units, uBuy, uSell)
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

// ----------------------------------------------------
// TAB 1: RECHNER & MARGE (TOP 50 DEALS)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    val context = LocalContext.current
    var silverInput by remember(uiState.silverBudget) { mutableStateOf(uiState.silverBudget.toString()) }
    var goldBudgetInput by remember { mutableStateOf("") }
    var capacityInput by remember(uiState.carryCapacityKg) { mutableStateOf(uiState.carryCapacityKg.toInt().toString()) }
    var marginInput by remember(uiState.targetMarginPercent) { mutableStateOf(uiState.targetMarginPercent.toString()) }
    var maxZonesInput by remember(uiState.maxZonesFilter) { mutableStateOf(uiState.maxZonesFilter.toString()) }
    var showLoadoutDialog by remember { mutableStateOf(value = false) }

    val highContrastFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Live Real-time Status Header & Controls
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.lastFetchTime?.let { time ->
                            Text("⏱️ Letzte Marktdaten-Aktualisierung: $time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Server Selection
                    Text("Server auswählen:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AlbionServer.entries.forEachIndexed { index, srv ->
                            SegmentedButton(
                                selected = uiState.server == srv,
                                onClick = { viewModel.onServerChanged(srv) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AlbionServer.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = Color.Black,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(srv.displayName, fontWeight = if (uiState.server == srv) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inputs Row 1: Budget, Gold & Capacity Input
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = silverInput,
                            onValueChange = { str ->
                                val cleanStr = str.filter { it.isDigit() }
                                silverInput = cleanStr
                                cleanStr.toLongOrNull()?.let { viewModel.onSilverBudgetChanged(it) }
                            },
                            label = { Text("Silber") },
                            colors = highContrastFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = goldBudgetInput,
                            onValueChange = { str ->
                                val cleanStr = str.filter { it.isDigit() }
                                goldBudgetInput = cleanStr
                                val gold = cleanStr.toLongOrNull() ?: 0L
                                val silverVal = gold * uiState.currentGoldPrice
                                silverInput = silverVal.toString()
                                viewModel.onSilverBudgetChanged(silverVal)
                            },
                            label = { Text("🪙 Gold") },
                            colors = highContrastFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = capacityInput,
                            onValueChange = { str ->
                                val cleanStr = str.filter { it.isDigit() }
                                capacityInput = cleanStr
                                cleanStr.toDoubleOrNull()?.let {
                                    viewModel.onCarryCapacityChanged(it)
                                }
                            },
                            label = { Text("Traglast kg") },
                            colors = highContrastFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Preset Buttons for Max Traglast
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                capacityInput = "1800"
                                viewModel.onCarryCapacityChanged(1800.0)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ochs 1.8t", fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                capacityInput = "3800"
                                viewModel.onCarryCapacityChanged(3800.0)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("T7 Ochs 3.8t", fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                capacityInput = "6500"
                                viewModel.onCarryCapacityChanged(6500.0)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🐢 6.5t", fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                capacityInput = "20000"
                                viewModel.onCarryCapacityChanged(20000.0)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🦣 Mammut 20t", fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = { showLoadoutDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚙️ Ausrüstung", fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inputs Row 2: Margin & Manual Priority Input
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = marginInput,
                            onValueChange = { str ->
                                val cleanStr = str.filter { it.isDigit() || it == '.' }
                                marginInput = cleanStr
                                val margin = cleanStr.toDoubleOrNull() ?: 0.0
                                viewModel.onTargetMarginChanged(margin)
                            },
                            label = { Text("Mindest-Marge (%)") },
                            colors = highContrastFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = maxZonesInput,
                            onValueChange = { str ->
                                val cleanStr = str.filter { it.isDigit() }
                                maxZonesInput = cleanStr
                                val zones = cleanStr.toIntOrNull() ?: 99
                                viewModel.onMaxZonesFilterChanged(zones)
                            },
                            label = { Text("🗺️ Max. Zonen") },
                            colors = highContrastFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safe Zone Filter Switch (Gefährliche Zonen meiden)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, if (uiState.avoidDangerousZones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.avoidDangerousZones) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gefährliche Zonen (Caerleon / Rot) meiden", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Schließt Caerleon & Full-Loot PvP Zonen aus", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.avoidDangerousZones,
                                onCheckedChange = { viewModel.onAvoidDangerousZonesToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Brecilien Filter Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, if (uiState.hideBrecilien) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = if (uiState.hideBrecilien) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Brecilien ausblenden", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Schließt Brecilien aus allen Handelschancen aus", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.hideBrecilien,
                                onCheckedChange = { viewModel.onHideBrecilienToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Schmuggler-Handelsposten (Schwarzmarkt) Filter Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, if (uiState.hideBlackMarket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.hideBlackMarket) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Schmuggler-Handelsposten (Schwarzmarkt) ausblenden", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Schließt den Schwarzmarkt in Caerleon aus", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.hideBlackMarket,
                                onCheckedChange = { viewModel.onHideBlackMarketToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section Title & City Filter Category
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lukrativste Handelschancen Top 10 (${uiState.tradeOpportunities.size} / Max 10)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Bar (Waffen, Offhand, Helme, Rüstung, Stiefel, Umhänge, Taschen, Reittiere, Essen, Tränke, Rohstoffe, Veredelt, Artefakte)
                Text("📦 Kategorie-Filter für Handelschancen:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ResourceCategory.entries.toTypedArray()) { cat ->
                        val isSelected = uiState.selectedOpportunityCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onOpportunityCategorySelected(cat) },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (uiState.selectedCityFilter != "ALLE") "🏙️ Standpunkt / Kaufort gewählt: ${uiState.selectedCityFilter}" else "🏙️ Filter nach Stadt oder Handelsposten:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                val citiesList = listOf(
                    "ALLE", "Bridgewatch", "Caerleon", "Fort Sterling",
                    "Lymhurst", "Martlock", "Thetford", "Schwarzmarkt",
                    "Brecilien", "Arthur's Rest", "Merlyn's Rest", "Morgana's Rest"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(citiesList) { city ->
                        val isSelected = uiState.selectedCityFilter == city
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCityFilterSelected(city) },
                            label = { Text(if (city == "ALLE") "Alle Städte & Dörfer" else city, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                // Tier Filter (T1 - T8)
                Text("⭐ Tier Filter:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                val tiersList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tiersList) { tier ->
                        val isSelected = uiState.selectedTierFilter == tier
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onTierFilterChanged(tier) },
                            label = { Text(if (tier == 0) "Alle Tiers" else "T$tier", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Enchantment Filter Bar (-1 = Alle, 0..4 = .0 to .4)
                Text("🔮 Verzauberungs-Filter:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                val enchantmentsList = listOf(-1, 0, 1, 2, 3, 4)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(enchantmentsList) { enc ->
                        val isSelected = uiState.selectedOpportunityEnchantmentFilter == enc
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onOpportunityEnchantmentFilterChanged(enc) },
                            label = { Text(if (enc == -1) "Alle Verzauberungen" else ".$enc", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sorting Options (Meiste Marge vs Neueste Angebote vs Wenigste auf Lager)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.onOpportunitySortChanged(OpportunitySort.HIGHEST_MARGIN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.sortOption == OpportunitySort.HIGHEST_MARGIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔥 Marge", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.sortOption == OpportunitySort.HIGHEST_MARGIN) Color.Black else MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = { viewModel.onOpportunitySortChanged(OpportunitySort.NEWEST) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.sortOption == OpportunitySort.NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⏰ Neueste", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.sortOption == OpportunitySort.NEWEST) Color.Black else MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = { viewModel.onOpportunitySortChanged(OpportunitySort.FEWEST_STOCK) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.sortOption == OpportunitySort.FEWEST_STOCK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📦 Wenigster Bestand", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (uiState.sortOption == OpportunitySort.FEWEST_STOCK) Color.Black else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }



        // List of Trade Opportunities
        if (uiState.tradeOpportunities.isEmpty()) {
            if (uiState.isLoadingPrices) {
                items(3) {
                    ShimmerLoadingCard(height = 110.dp)
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.priceFetchError != null)
                                    "Fehler: ${uiState.priceFetchError}\n\nAutomatischer Reconnect wird ausgeführt..."
                                else
                                    "Keine gewinnbringenden Handelschancen für deine Filterkriterien gefunden.\n\nErhöhe dein Silber-Budget, wähle einen anderen Server oder passe die Mindest-Marge an.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(uiState.tradeOpportunities, key = { "${it.resource.fullId}_${it.buyCity}_${it.sellCity}_${it.quality}_${it.buyPrice}_${it.sellPrice}" }) { opportunity ->
                TradeOpportunityCard(
                    opportunity = opportunity,
                    onAcceptOrder = { opp ->
                        viewModel.acceptTradeOpportunity(opp)
                        if (uiState.orderErrorMsg == null) {
                            Toast.makeText(context, "✅ Auftrag '${opp.resource.nameDe}' zu aktiven Aufträgen hinzugefügt!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDirectBookOrder = { opp, units, buyPrice, sellPrice ->
                        viewModel.bookAndCompleteOpportunity(opp, units, buyPrice, sellPrice)
                        Toast.makeText(context, "✅ Trade '${opp.resource.nameDe}' direkt in Historie gebucht!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showLoadoutDialog) {
        LoadoutSelectionDialog(
            currentMount = uiState.selectedMount,
            currentBag = uiState.selectedBag,
            currentBoots = uiState.selectedBoots,
            onDismiss = { showLoadoutDialog = false },
            onConfirm = { mount, bag, boots ->
                viewModel.onEquipmentLoadoutChanged(mount, bag, boots)
                showLoadoutDialog = false
            }
        )
    }
}



@Composable
fun LoadoutSelectionDialog(
    currentMount: String,
    currentBag: String,
    currentBoots: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var mount by remember { mutableStateOf(currentMount) }
    var mountQuality by remember { mutableStateOf(ItemQuality.NORMAL) }

    var bagTier by remember { mutableStateOf(currentBag) }
    var bagEnchantment by remember { mutableStateOf(ItemEnchantment.UNCRAFTED) }
    var bagQuality by remember { mutableStateOf(ItemQuality.NORMAL) }

    var bootsTier by remember { mutableStateOf(currentBoots) }
    var bootsEnchantment by remember { mutableStateOf(ItemEnchantment.UNCRAFTED) }
    var bootsQuality by remember { mutableStateOf(ItemQuality.NORMAL) }

    val mountOptions = listOf(
        "Transportmammut T8 (+20.000 kg)",
        "Riesenschildkröte T8 (+6.500 kg)",
        "Grizzlybär T8 (+4.500 kg)",
        "Transportochs T8 (+5.500 kg)",
        "Transportochs T7 (+3.800 kg)",
        "Transportochs T6 (+2.700 kg)",
        "Transportochs T5 (+1.800 kg)",
        "Transportochs T4 (+1.200 kg)",
        "Transportochs T3 (+600 kg)",
        "Wildschwein T7 (+1.500 kg)",
        "Gepanzertes Pferd T8 (+1.200 kg)",
        "Gepanzertes Pferd T7 (+1.000 kg)",
        "Gepanzertes Pferd T6 (+800 kg)",
        "Gepanzertes Pferd T5 (+600 kg)",
        "Reitpferd T8 (+600 kg)",
        "Reitpferd T7 (+500 kg)",
        "Reitpferd T6 (+400 kg)",
        "Reitpferd T5 (+300 kg)",
        "Reitpferd T4 (+250 kg)",
        "Esel T3 (+150 kg)"
    )

    val bagOptions = listOf(
        "Tasche T8 (+950 kg Base)",
        "Tasche T7 (+600 kg Base)",
        "Tasche T6 (+380 kg Base)",
        "Tasche T5 (+220 kg Base)",
        "Tasche T4 (+120 kg Base)",
        "Tasche T3 (+60 kg Base)",
        "Keine Tasche (+0 kg)"
    )

    val bootsOptions = listOf(
        "Transport-Schuhe T8 (+300 kg Base)",
        "Transport-Schuhe T7 (+220 kg Base)",
        "Transport-Schuhe T6 (+160 kg Base)",
        "Transport-Schuhe T5 (+120 kg Base)",
        "Transport-Schuhe T4 (+80 kg Base)",
        "Transport-Schuhe T3 (+20 kg Base)",
        "Standard-Schuhe (+0 kg)"
    )

    val calculatedCapacity = remember(mount, mountQuality, bagTier, bagEnchantment, bagQuality, bootsTier, bootsEnchantment, bootsQuality) {
        TradeCalculator.calculateTotalCapacityWithDetails(
            mountType = mount,
            mountQuality = mountQuality,
            bagTier = bagTier,
            bagEnchantment = bagEnchantment,
            bagQuality = bagQuality,
            bootsTier = bootsTier,
            bootsEnchantment = bootsEnchantment,
            bootsQuality = bootsQuality
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🐴 Ausrüstung, Verzauberung & Qualität", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Berechnete Gesamt-Traglast:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${NumberFormat.getNumberInstance(Locale.GERMANY).format(calculatedCapacity.toInt())} kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // 1. Mount Selection
                Text("🐴 Reittier (Mount) wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                mountOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mount = option }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = (mount.startsWith(option) || option.startsWith(mount.substringBefore(" ("))), onClick = { mount = option })
                        Text(option, fontSize = 12.sp)
                    }
                }

                Text("Reittier-Qualität:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemQuality.entries.toTypedArray()) { q ->
                        FilterChip(
                            selected = (mountQuality == q),
                            onClick = { mountQuality = q },
                            label = { Text(q.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                HorizontalDivider()

                // 2. Bag Selection
                Text("🎒 Tasche (Bag) wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                bagOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bagTier = option }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = (bagTier.startsWith(option) || option.startsWith(bagTier.substringBefore(" ("))), onClick = { bagTier = option })
                        Text(option, fontSize = 12.sp)
                    }
                }

                Text("Taschen-Verzauberung:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemEnchantment.entries.toTypedArray()) { enc ->
                        FilterChip(
                            selected = (bagEnchantment == enc),
                            onClick = { bagEnchantment = enc },
                            label = { Text(enc.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Taschen-Qualität:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemQuality.entries.toTypedArray()) { q ->
                        FilterChip(
                            selected = (bagQuality == q),
                            onClick = { bagQuality = q },
                            label = { Text(q.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                HorizontalDivider()

                // 3. Boots Selection
                Text("🥾 Schuhe (Boots) wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                bootsOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bootsTier = option }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = (bootsTier.startsWith(option) || option.startsWith(bootsTier.substringBefore(" ("))), onClick = { bootsTier = option })
                        Text(option, fontSize = 12.sp)
                    }
                }

                Text("Schuh-Verzauberung:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemEnchantment.entries.toTypedArray()) { enc ->
                        FilterChip(
                            selected = (bootsEnchantment == enc),
                            onClick = { bootsEnchantment = enc },
                            label = { Text(enc.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Schuh-Qualität:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemQuality.entries.toTypedArray()) { q ->
                        FilterChip(
                            selected = (bootsQuality == q),
                            onClick = { bootsQuality = q },
                            label = { Text(q.displayName, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val formattedMount = "$mount (${mountQuality.displayName})"
                val formattedBag = "$bagTier (${bagEnchantment.displayName}, ${bagQuality.displayName})"
                val formattedBoots = "$bootsTier (${bootsEnchantment.displayName}, ${bootsQuality.displayName})"
                onConfirm(formattedMount, formattedBag, formattedBoots)
            }) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun TradeOpportunityCard(
    opportunity: TradeOpportunity,
    onAcceptOrder: (TradeOpportunity) -> Unit,
    onDirectBookOrder: ((TradeOpportunity, Int, Int, Int) -> Unit)? = null
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    var showDialog by remember { mutableStateOf(false) }
    var showDirectBookDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("📋 Marktchance buchen", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Möchtest du den Auftrag für ${opportunity.resource.nameDe} buchen?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    HorizontalDivider()
                    Text("📍 Kaufen in: ${opportunity.buyCity} (${numberFormat.format(opportunity.buyPrice)} Silber)", fontSize = 12.sp)
                    Text("📍 Verkaufen in: ${opportunity.sellCity} (${numberFormat.format(opportunity.sellPrice)} Silber)", fontSize = 12.sp)
                    Text("📊 Umsatz: ${numberFormat.format(opportunity.totalNetRevenue)} Silber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A))
                    Text("📦 Menge: ${numberFormat.format(opportunity.tradeUnits)} Stk. | Traglast: ${String.format(Locale.GERMANY, "%.1f", opportunity.totalWeightKg)} kg", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showDialog = false
                            onAcceptOrder(opportunity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 Als aktiven Auftrag annehmen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (onDirectBookOrder != null) {
                        Button(
                            onClick = {
                                showDialog = false
                                showDirectBookDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚡ Direkt als erledigt buchen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abbrechen")
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showDirectBookDialog && onDirectBookOrder != null) {
        var unitsInput by remember { mutableStateOf(opportunity.tradeUnits.toString()) }
        var buyPriceInput by remember { mutableStateOf(opportunity.buyPrice.toString()) }
        var sellPriceInput by remember { mutableStateOf(opportunity.sellPrice.toString()) }

        val uBuy = buyPriceInput.toIntOrNull() ?: opportunity.buyPrice
        val uSell = sellPriceInput.toIntOrNull() ?: opportunity.sellPrice
        val units = unitsInput.toIntOrNull() ?: opportunity.tradeUnits

        val taxPerUnit = (uSell * 0.065).toLong() // Estimated market tax + setup fee
        val netSellPrice = uSell.toLong() - taxPerUnit
        val computedSpent = units.toLong() * uBuy.toLong()
        val computedEarned = units.toLong() * netSellPrice
        val computedProfit = computedEarned - computedSpent

        AlertDialog(
            onDismissRequest = { showDirectBookDialog = false },
            title = { Text("⚡ Direkt in Historie buchen", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${opportunity.resource.nameDe} (T${opportunity.resource.tier})", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Gekaufte Menge") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = buyPriceInput,
                        onValueChange = { buyPriceInput = it },
                        label = { Text("Kaufpreis pro Stk. (Silber)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sellPriceInput,
                        onValueChange = { sellPriceInput = it },
                        label = { Text("Verkaufspreis pro Stk. (Silber)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📊 Erwarteter Gewinn: ${if (computedProfit >= 0) "+" else ""}${numberFormat.format(computedProfit)} Silber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (computedProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDirectBookDialog = false
                        onDirectBookOrder(opportunity, units, uBuy, uSell)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Buchen & Speichern", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDirectBookDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    val routeBorderColor = ZoneThemeColors.getOpportunityBorderColor(opportunity.buyCity, opportunity.sellCity)
    val routeContainerBg = ZoneThemeColors.getOpportunityContainerBg(opportunity.buyCity, opportunity.sellCity)
    val buyCityColor = ZoneThemeColors.getCityZoneColor(opportunity.buyCity)
    val sellCityColor = ZoneThemeColors.getCityZoneColor(opportunity.sellCity)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = routeContainerBg
        ),
        border = BorderStroke(1.5.dp, routeBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // MAIN TITLE HEADER: Item Name + Tier Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = opportunity.resource.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).padding(end = 6.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getTierColor(opportunity.resource.tier)
                ) {
                    Text(
                        text = "${opportunity.resource.tierText}${opportunity.resource.enchantmentText}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (opportunity.quality > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = opportunity.resource.qualityText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = opportunity.resource.nameDe,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NET PROFIT & STATS BADGES (Below Title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Umsatz: ${numberFormat.format(opportunity.totalNetRevenue)} Silber",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF66BB6A)
                    )
                    Text(
                        text = "+${String.format(Locale.GERMANY, "%.1f", opportunity.roiPercent)}% Marge (ROI)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF81C784)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF81D4FA)
                    ) {
                        Text(
                            text = "🗺️ ${opportunity.zonesWalkedCount} Gebiete",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF4DB6AC)
                    ) {
                        Text(
                            text = "📦 Bestand: ${opportunity.stockAvailable}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🪙 ~${numberFormat.format(opportunity.equivalentGoldProfit)} Gold",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROUTE DETAILS CARD (Kaufort -> Verkaufort)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = routeContainerBg,
                border = BorderStroke(1.dp, routeBorderColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Buy City
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kaufen in:", fontSize = 11.sp, color = Color.LightGray)
                            val buyBadge = ZoneThemeColors.getCityZoneBadgeText(opportunity.buyCity)
                            if (buyBadge.startsWith("🔴") || buyBadge.startsWith("✨") || buyBadge.startsWith("🏴‍☠️")) {
                                Text(" $buyBadge", color = buyCityColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(opportunity.buyCity, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = buyCityColor)
                        Text("${numberFormat.format(opportunity.buyPrice)} Silber", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "nach",
                        tint = routeBorderColor,
                        modifier = Modifier.size(24.dp)
                    )

                    // Sell City
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Verkaufen in:", fontSize = 11.sp, color = Color.LightGray)
                            val sellBadge = ZoneThemeColors.getCityZoneBadgeText(opportunity.sellCity)
                            if (sellBadge.startsWith("🔴") || sellBadge.startsWith("✨") || sellBadge.startsWith("🏴‍☠️")) {
                                Text(" $sellBadge", color = sellCityColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(opportunity.sellCity, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = sellCityColor)
                        Text("${numberFormat.format(opportunity.sellPrice)} Silber", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INVESTMENT, WEIGHT & UNITS
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Handelsmenge: ${numberFormat.format(opportunity.tradeUnits)} Stk.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Traglast: ${String.format(Locale.GERMANY, "%.1f", opportunity.totalWeightKg)} kg", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Investition: ${numberFormat.format(opportunity.totalInvestment)} Silber", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("⏰ Angebot her: ${opportunity.foundTimeStr}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ALBION2D MARKET STATS, LIQUIDITY & DIRECT 2D STATS LINK
            val context = LocalContext.current
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (opportunity.liquidityScore.isNotBlank()) opportunity.liquidityScore else "📊 24h Markt-Liquidität",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        if (opportunity.focusProfitPerPoint > 0) {
                            Text(
                                text = "✨ +${opportunity.focusProfitPerPoint} S./Fokus",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    if (opportunity.isScamPriceWarning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF7F1D1D)
                        ) {
                            Text(
                                text = "⚠️ Warnung: Preis liegt >80% über 7-Tage-Schnitt (Möglicher Scam)",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        Text(
                            text = "🌐 europe.albiononline2d.com",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, opportunity.albion2dUrl.toUri()).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("🌐 2D Stats", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👆 Tippe auf das Item, um den Auftrag anzunehmen",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun CatalogTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Suchname oder Item ID (z.B. T4_WOOD)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Suche") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Löschen")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = ResourceCategory.entries.indexOf(uiState.selectedCategory),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ResourceCategory.entries.forEach { category ->
                Tab(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.onCategorySelected(category) },
                    text = {
                        Text(
                            text = category.displayName,
                            fontWeight = if (uiState.selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedTier == 0,
                    onClick = { viewModel.onTierSelected(0) },
                    label = { Text("Alle Stufen") }
                )
            }
            items((2..8).toList()) { tier ->
                val isSelected = uiState.selectedTier == tier
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onTierSelected(tier) },
                    label = {
                        Text(
                            text = "Tier $tier",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else getTierColor(tier),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getTierColor(tier)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${uiState.filteredResources.size} Ressourcen im Katalog (Live-Preise)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (uiState.filteredResources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Ressourcen gefunden.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredResources, key = { it.fullId }) { resource ->
                    val prices = uiState.marketPrices[resource.fullId] ?: uiState.marketPrices[resource.id] ?: emptyList()
                    ResourceCard(
                        resource = resource,
                        prices = prices,
                        allMarketPrices = uiState.marketPrices,
                        onClick = { viewModel.onResourceClicked(resource) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: KI AUSRÜSTUNG & BUILD-SETS
// ----------------------------------------------------
@Composable
fun EquipmentBuildsTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Compact Header Card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "⚔️ Bestes Klassen-Equipment & Stärkste Waffen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Kategorisiert nach Schadensart mit empfohlenen Fertigkeiten & Stärkster Waffe",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(BuildCategory.entries.toTypedArray()) { cat ->
                val isSelected = uiState.selectedBuildCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onBuildCategorySelected(cat) },
                    label = { Text("${cat.icon} ${cat.displayName}", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val filteredBuilds = uiState.equipmentBuilds
            .filter { it.category == uiState.selectedBuildCategory }
            .sortedByDescending { it.estimatedMarginPercent }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(filteredBuilds, key = { _, build -> build.id }) { index, build ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, if (index < 3) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (index < 3) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF059669)
                                    ) {
                                        Text(
                                            text = "🔥 Top Deal (#${index + 1} Marge: +${String.format(Locale.GERMANY, "%.1f", build.estimatedMarginPercent)}%)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                                Text(build.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "~${NumberFormat.getNumberInstance(Locale.GERMANY).format(build.estimatedCostSilver)} S.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (build.strongestWeaponHighlight.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = build.strongestWeaponHighlight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (build.useCaseFunction.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text(
                                    text = build.useCaseFunction,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("⚔️ Waffe: ${build.weapon}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("🪖 Kopf: ${build.head} | 🥋 Brust: ${build.armor}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("🥾 Schuhe: ${build.shoes} | 🐴 Reittier: ${build.mount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("🧪 Trank: ${build.potion} | 🥩 Essen: ${build.food}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("⚡ Auszuwählende Fertigkeiten & Abilities:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                if (build.recommendedSkills.isNotEmpty()) Text(build.recommendedSkills, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (build.headSkill.isNotEmpty()) Text(build.headSkill, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (build.armorSkill.isNotEmpty()) Text(build.armorSkill, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (build.shoesSkill.isNotEmpty()) Text(build.shoesSkill, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))

                        Text("⭐ Ratings: Schaden ${build.damageRating}/100 | Heal ${build.healingRating}/100 | Def ${build.defenseRating}/100 | Tempo ${build.mobilityRating}/100", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(build.aiEvaluationDe, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(2.dp))
                        Text("🔄 Rotation: ${build.combatRotationDe}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Advantages & Disadvantages (Pros & Cons)
                        if (build.vorteileDe.isNotEmpty() || build.nachteileDe.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("✅ Vorteile:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    build.vorteileDe.forEach { v ->
                                        Text("• $v", fontSize = 9.sp, color = Color(0xFF81C784))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("❌ Nachteile:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    build.nachteileDe.forEach { n ->
                                        Text("• $n", fontSize = 9.sp, color = Color(0xFFFF8A80))
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

// ----------------------------------------------------
// TAB 6: EVENTS & MONSTER (10 MINUTEN INTERVALL)
// ----------------------------------------------------
@Composable
fun EventsAndMonstersTabContent(
    uiState: ResourceUiState
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("ALLE") }
    var selectedSortMode by remember { mutableStateOf(MonsterSortMode.MOST_LUCRATIVE) }
    var selectedCategoryType by remember { mutableStateOf("ALL") }
    var selectedPlayerCategory by remember { mutableStateOf(PlayerCategory.ALL) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val regions = listOf(
        "ALLE", "Outlands", "Steppe", "Wald", "Sumpf", "Gebirge", "Hochland", "Caerleon", "Roads of Avalon", "The Mists", "Statische Dungeons"
    )

    val filteredMonsters = remember(searchQuery, selectedRegion, selectedSortMode, selectedCategoryType, selectedPlayerCategory) {
        val base = AlbionMonsterRepository.getFilteredAndSorted(
            query = searchQuery,
            regionFilter = selectedRegion,
            playerCategoryFilter = selectedPlayerCategory,
            sortMode = selectedSortMode
        )
        when (selectedCategoryType) {
            "BOSS" -> base.filter { 
                it.type == MonsterType.WORLD_BOSS || 
                it.type == MonsterType.ROAMING_BOSS || 
                it.type == MonsterType.ASPECT || 
                it.type == MonsterType.HELLGATE_BOSS 
            }
            "DUNGEON" -> base.filter { 
                it.type == MonsterType.DUNGEON_BOSS || 
                it.type == MonsterType.STATIC_BOSS || 
                it.type == MonsterType.MISTS_BOSS || 
                it.type == MonsterType.ROADS_BOSS || 
                it.type == MonsterType.ELITE_MOB 
            }
            else -> base
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Live Events Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🎆", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Live Events & Aktivitäten",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Automatische Aktualisierung alle 10 Minuten (neue Events hinzugefügt, abgelaufene entfernt)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        items(uiState.liveEventsList) { event ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "Aktiv: ${event.remainingMinutes} Min",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("📍 Zone: ${event.zoneName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("🛡️ Sicherheit: ${event.zoneSafety.displayName}", fontSize = 11.sp, color = Color(0xFFFFB74D))
                    Text(event.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("🎁 Belohnung: ${event.rewardSummary}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👹 Monster & Bosse Guide (nach Region & Zonen)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Übersicht aller Weltbosse, Aspekte & Dungeons mit exakten Drop-Chancen und Standorten.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(LanguageManager.getString("monster_search", uiState.appLanguage)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Player Count Category Filter Chips (Solo, Duo, Gruppe, Raid)
                    Text("👥 Benötigte Spielerzahl (Kategorien):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PlayerCategory.entries.toTypedArray()) { cat ->
                            val isSelected = selectedPlayerCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlayerCategory = cat },
                                label = { Text("${cat.iconEmoji} ${cat.displayNameDe}", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // AI Decision & Recommendation Card for Events and Boss Loot
                    val aiAdvice = remember(selectedPlayerCategory) {
                        AiEventAndBossAdvisor.getAdviceForCategory(selectedPlayerCategory)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = aiAdvice.aiRecommendationDe,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💎 Erwarteter Gewinn: ${aiAdvice.estimatedProfitScore} | Risiko: ${aiAdvice.riskLevelDe}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Split Filter Chips (Weltbosse vs Dungeons & Schatztruhen)
                    Text("⚔️ Kategorie-Filter (Splitting):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedCategoryType == "ALL",
                            onClick = { selectedCategoryType = "ALL" },
                            label = { Text("Alle (Bosse & Dungeons)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryType == "BOSS",
                            onClick = { selectedCategoryType = "BOSS" },
                            label = { Text("👹 Weltbosse & Raids", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryType == "DUNGEON",
                            onClick = { selectedCategoryType = "DUNGEON" },
                            label = { Text("🏰 Dungeons & Truhen", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("🔥 Sortierung wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MonsterSortMode.entries.toTypedArray()) { mode ->
                            val isSelected = selectedSortMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSortMode = mode },
                                label = { Text(if (uiState.appLanguage == "DE") mode.labelDe else mode.labelEn, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("📍 Region wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(regions) { reg ->
                            val isSelected = selectedRegion == reg
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRegion = reg },
                                label = { Text(if (reg == "ALLE") "Alle Regionen" else reg, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        items(filteredMonsters) { monster ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Player Count Category Badge & Profit/Hour
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (monster.playerCategory) {
                                PlayerCategory.SOLO -> Color(0xFF10B981)
                                PlayerCategory.DUO -> Color(0xFF38BDF8)
                                PlayerCategory.GROUP -> Color(0xFF8B5CF6)
                                PlayerCategory.RAID -> Color(0xFFEC4899)
                                else -> Color(0xFF64748B)
                            }
                        ) {
                            Text(
                                text = "${monster.playerCategory.iconEmoji} ${monster.recommendedPlayerCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = monster.estimatedSilverPerHour,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = monster.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (uiState.appLanguage == "DE") monster.nameDe else monster.nameEn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "📍 ${if (uiState.appLanguage == "DE") monster.zoneLocationDe else monster.zoneLocationEn}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (monster.zoneSafety) {
                                ZoneSafety.SAFE_BLUE -> Color(0xFF1E88E5)
                                ZoneSafety.SAFE_YELLOW -> Color(0xFFFDD835)
                                ZoneSafety.DANGEROUS_RED -> Color(0xFFE53935)
                                ZoneSafety.DANGEROUS_BLACK -> Color(0xFF424242)
                            }
                        ) {
                            Text(
                                text = "T${monster.tier} ${monster.zoneSafety.displayName.split(" ")[0]}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chest Drop Summary Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                            Text(
                                text = monster.chestDropSummaryDe,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Geschätzter Loot / Profit:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "💰 ~${numberFormat.format(monster.estimatedProfitSilver)} Silber",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF66BB6A)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Schwierigkeit & Respawn:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (uiState.appLanguage == "DE") monster.difficultyDe else monster.difficultyEn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "⏱️ ${if (uiState.appLanguage == "DE") monster.respawnTimeDe else monster.respawnTimeEn}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("🎯 Drop-Chancen & Beute-Tabelle:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    monster.drops.forEach { drop ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "• ${if (uiState.appLanguage == "DE") drop.itemNameDe else drop.itemNameEn}",
                                    fontSize = 11.sp,
                                    fontWeight = if (drop.isRare) FontWeight.Bold else FontWeight.Normal,
                                    color = if (drop.isRare) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                )
                                if (drop.isRare) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFD700)) {
                                        Text("SELTEN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(
                                text = "${drop.dropChancePercent}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Mögliche Schatztruhen & Truhen-Dropchancen (%)
                    val matchingBoss = remember(monster.id) {
                        AlbionWorldData.bossLootList.find { b ->
                            b.name.contains(monster.nameDe, ignoreCase = true) || monster.nameDe.contains(b.name, ignoreCase = true)
                        }
                    }
                    if (matchingBoss != null && matchingBoss.possibleChests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🎁 Mögliche Schatztruhen & Truhen-Dropchancen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(4.dp))
                        matchingBoss.possibleChests.forEach { chest ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text("• ${chest.nameDe} (${chest.rarity})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${chest.dropChancePercent}% Chance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("⚔️ Kampf-Tipps & Mechanik:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(if (uiState.appLanguage == "DE") monster.combatTipsDe else monster.combatTipsEn, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerDownloadsChart(
    totalDownloads: Int,
    hourlyStats: List<HourlyDownloadStat>,
    modifier: Modifier = Modifier
) {
    if (hourlyStats.isEmpty()) return

    val maxVal = remember(hourlyStats) { (hourlyStats.maxOfOrNull { it.downloads } ?: 1).coerceAtLeast(1) }
    val total24h = remember(hourlyStats) { hourlyStats.sumOf { it.downloads } }

    var selectedIndex by remember(hourlyStats) { mutableStateOf<Int?>(hourlyStats.size - 1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "📥 Gesamte Server Downloads: $totalDownloads",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "⏱️ Letzte 24 Stunden: $total24h Downloads (Live-Sync)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF10B981)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        selectedIndex?.let { idx ->
            if (idx in hourlyStats.indices) {
                val stat = hourlyStats[idx]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Uhrzeit: ${stat.hour} Uhr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stat.downloads} Downloads",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(hourlyStats) {
                    detectTapGestures { offset ->
                        if (hourlyStats.isNotEmpty()) {
                            val widthPerBar = size.width.toFloat() / hourlyStats.size.toFloat()
                            if (widthPerBar > 0f) {
                                val clickedIndex = (offset.x / widthPerBar).toInt().coerceIn(0, hourlyStats.size - 1)
                                selectedIndex = clickedIndex
                            }
                        }
                    }
                }
        ) {
            val barColor = MaterialTheme.colorScheme.primary
            val highlightColor = Color(0xFF10B981)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20f
                val paddingTop = 10f
                val chartHeight = height - paddingBottom - paddingTop

                if (hourlyStats.isEmpty()) return@Canvas

                val barWidth = (width / hourlyStats.size) * 0.7f
                val spacing = (width / hourlyStats.size) * 0.3f

                for (i in 0..3) {
                    val y = paddingTop + (chartHeight / 3) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                hourlyStats.forEachIndexed { index, stat ->
                    val x = index * (barWidth + spacing) + spacing / 2
                    val barHeightFraction = (stat.downloads.toFloat() / maxVal.toFloat()).coerceAtLeast(0.05f)
                    val barH = chartHeight * barHeightFraction
                    val y = height - paddingBottom - barH

                    val isSelected = selectedIndex == index
                    val currentColor = if (isSelected) highlightColor else barColor

                    drawRoundRect(
                        color = currentColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            val firstHour = hourlyStats.firstOrNull()?.hour ?: "00:00"
            val lastHour = hourlyStats.lastOrNull()?.hour ?: "23:00"
            Text(
                text = "Vor 24h ($firstHour)",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tippen für Details",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Jetzt ($lastHour)",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GoldStockChart(
    goldPrices: List<GoldPrice>,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    if (goldPrices.isEmpty()) return

    // Chronological order (oldest to newest)
    val sortedPrices = remember(goldPrices) { goldPrices.reversed() }
    val prices = sortedPrices.map { it.price }
    val minPrice = prices.minOrNull() ?: 0
    val maxPrice = prices.maxOrNull() ?: 1
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1)

    val firstPrice = prices.firstOrNull() ?: 0
    val lastPrice = prices.lastOrNull() ?: 0
    val priceDiff = lastPrice - firstPrice
    val percentChange = if (firstPrice > 0) (priceDiff.toDouble() / firstPrice) * 100.0 else 0.0
    val isPositive = priceDiff >= 0
    val chartColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)

    var selectedIndex by remember(goldPrices) { mutableStateOf<Int?>(sortedPrices.size - 1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Aktuell: ${numberFormat.format(lastPrice)} Silber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = chartColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${numberFormat.format(priceDiff)} (${String.format(Locale.GERMANY, "%.2f", percentChange)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = chartColor
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Hoch: ${numberFormat.format(maxPrice)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tief: ${numberFormat.format(minPrice)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        selectedIndex?.let { idx ->
            if (idx in sortedPrices.indices) {
                val pt = sortedPrices[idx]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Zeit: ${if (pt.timestamp.length >= 16) pt.timestamp.replace("T", " ").substring(0, 16) else pt.timestamp}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${numberFormat.format(pt.price)} Silber/Gold",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(sortedPrices) {
                    detectTapGestures { offset ->
                        if (sortedPrices.isNotEmpty()) {
                            val widthPerPoint = if (sortedPrices.size > 1) size.width.toFloat() / (sortedPrices.size - 1).toFloat() else size.width.toFloat()
                            if (widthPerPoint > 0f) {
                                val clickedIndex = (offset.x / widthPerPoint).roundToInt().coerceIn(0, sortedPrices.size - 1)
                                selectedIndex = clickedIndex
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20f
                val paddingTop = 20f
                val chartHeight = height - paddingBottom - paddingTop

                if (sortedPrices.size < 2) return@Canvas

                val stepX = width / (sortedPrices.size - 1)

                val points = sortedPrices.mapIndexed { index, item ->
                    val x = index * stepX
                    val normalizedY = (item.price - minPrice).toFloat() / priceRange
                    val y = paddingTop + chartHeight * (1f - normalizedY)
                    Offset(x, y)
                }

                // Grid lines
                for (i in 0..4) {
                    val y = paddingTop + (chartHeight / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Smooth cubic bezier path (Bogen)
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            chartColor.copy(alpha = 0.45f),
                            chartColor.copy(alpha = 0.0f)
                        ),
                        startY = paddingTop,
                        endY = height
                    )
                )

                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points (Punkte)
                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    if (isSelected) {
                        drawCircle(
                            color = chartColor.copy(alpha = 0.4f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = chartColor,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            val firstTime = sortedPrices.firstOrNull()?.timestamp ?: ""
            val lastTime = sortedPrices.lastOrNull()?.timestamp ?: ""
            Text(
                text = if (firstTime.length >= 16) firstTime.replace("T", " ").substring(5, 16) else firstTime,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tippen für Werte & Punkte",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (lastTime.length >= 16) lastTime.replace("T", " ").substring(5, 16) else lastTime,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------
// STOCK TRADING CHART COMPOSABLE FOR ITEM MARKET PRICES (STÄDTE)
// ----------------------------------------------------
@Composable
fun MarketPriceStockChart(
    prices: List<MarketPrice>,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val validPrices = remember(prices) { prices.filter { it.sellPriceMin > 0 } }
    if (validPrices.isEmpty()) return

    val priceValues = validPrices.map { it.sellPriceMin }
    val minPrice = priceValues.minOrNull() ?: 0
    val maxPrice = priceValues.maxOrNull() ?: 1
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1)

    val chartColor = Color(0xFF00BCD4) // Cyan/Gold trading color

    var selectedIndex by remember(validPrices) { mutableStateOf<Int?>(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Städte-Kursverlauf",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Spanne: ${numberFormat.format(minPrice)} - ${numberFormat.format(maxPrice)} Silber",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Hoch: ${numberFormat.format(maxPrice)}", fontSize = 10.sp, color = Color(0xFF10B981))
                Text("Tief: ${numberFormat.format(minPrice)}", fontSize = 10.sp, color = Color(0xFFEF4444))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        selectedIndex?.let { idx ->
            if (idx in validPrices.indices) {
                val pr = validPrices[idx]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📍 ${pr.city}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${numberFormat.format(pr.sellPriceMin)} Silber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF66BB6A)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(validPrices) {
                    detectTapGestures { offset ->
                        if (validPrices.isNotEmpty()) {
                            val widthPerPoint = if (validPrices.size > 1) size.width.toFloat() / (validPrices.size - 1).toFloat() else size.width.toFloat()
                            if (widthPerPoint > 0f) {
                                val clickedIndex = (offset.x / widthPerPoint).roundToInt().coerceIn(0, validPrices.size - 1)
                                selectedIndex = clickedIndex
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 24f
                val paddingTop = 20f
                val chartHeight = height - paddingBottom - paddingTop

                if (validPrices.size < 2) return@Canvas

                val stepX = width / (validPrices.size - 1)

                val points = validPrices.mapIndexed { index, item ->
                    val x = index * stepX
                    val normalizedY = (item.sellPriceMin - minPrice).toFloat() / priceRange
                    val y = paddingTop + chartHeight * (1f - normalizedY)
                    Offset(x, y)
                }

                // Grid lines
                for (i in 0..4) {
                    val y = paddingTop + (chartHeight / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Smooth cubic bezier path (Bogen)
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            chartColor.copy(alpha = 0.4f),
                            chartColor.copy(alpha = 0.0f)
                        ),
                        startY = paddingTop,
                        endY = height
                    )
                )

                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Points (Punkte)
                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    if (isSelected) {
                        drawCircle(
                            color = chartColor.copy(alpha = 0.5f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = chartColor,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            val firstCity = validPrices.firstOrNull()?.city ?: ""
            val lastCity = validPrices.lastOrNull()?.city ?: ""
            Text(
                text = firstCity,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tippen für Punkte & Werte",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = lastCity,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------
// TAB 7: GOLDMARKT TAB (MIT LIVE ECHTZEIT-KURSEN FÜR EUROPA, AMERIKAS & ASIEN)
// ----------------------------------------------------
@Composable
fun GoldMarketTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    var goldAmountInput by remember { mutableStateOf("") }
    var goldPriceInput by remember { mutableStateOf("") }
    var goldDateInput by remember { mutableStateOf("") }
    var isBuyMode by remember { mutableStateOf(true) }
    var selectedHistoryFilter by remember { mutableIntStateOf(0) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Real-Time Live Status Banner & Manual Refresh
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ECHTZEIT GOLDMARKT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "Live-Sync: ${uiState.lastGoldFetchTime ?: "Aktualisiert..."} (30s Polling)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isLoadingGold) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { viewModel.fetchGoldPrices() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Jetzt aktualisieren",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Live Gold Price Cards for ALL 3 Servers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🌐 Live Goldpreise nach Server (albiononlinebuilds.com)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val servers = listOf(
                        Triple(AlbionServer.EUROPE, "🇪🇺 Europa", "EU")
                    )

                    for ((srv, label, _) in servers) {
                        val isCurrent = uiState.server == srv
                        val livePrice = uiState.serverGoldPrices[srv] ?: if (isCurrent) uiState.currentGoldPrice else 0

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onServerChanged(srv) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)

                                Spacer(modifier = Modifier.height(4.dp))

                                if (livePrice > 0) {
                                    Text(
                                        text = numberFormat.format(livePrice),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Text("Silber/Gold", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Text("Laden...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            "AKTIV",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Server Live Gold Detail Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🪙 ${uiState.server.displayName} Live Goldkurs",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Einkaufspreis:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${numberFormat.format(uiState.goldBuyPrice)} Silber",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Verkaufspreis (nach Steuer):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${numberFormat.format(uiState.goldSellPrice)} Silber",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF66BB6A)
                            )
                        }
                    }
                }
            }
        }

        // 🤖 KI Gold-Handelsbot Card (Tagestiefpunkt, Kauf- & Verkauf-Order)
        item {
            val botAnalysis = remember(uiState.goldPrices, uiState.currentGoldPrice) {
                GoldBotCalculator.analyzeGoldMarket(uiState.goldPrices, uiState.currentGoldPrice)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🤖 KI Gold-Bot (Monats-Trend & Prognose)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD700)
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFD700)
                        ) {
                            Text(
                                text = botAnalysis.trendForecastDe,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("📉 24h / 30-Tage Tiefpunkt:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(botAnalysis.dailyLow)} S. (30T: ${numberFormat.format(botAnalysis.monthlyLow)} S.)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("📈 24h / 30-Tage Hochpunkt:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(botAnalysis.dailyHigh)} S. (30T: ${numberFormat.format(botAnalysis.monthlyHigh)} S.)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF66BB6A))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forecast Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Column {
                                Text("🔮 Prognose 3 Tage:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("~${numberFormat.format(botAnalysis.forecast3Days)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("🔮 Prognose 7 Tage (Woche):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("~${numberFormat.format(botAnalysis.forecast7Days)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("🎯 KI Order-Empfehlung für Max-Profit (Letzte Wochen-Statistik):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🛍️ Akkurate Kauf-Order:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${numberFormat.format(botAnalysis.recommendedBuyOrderPrice)} S.", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF10B981))
                                Text("📉 Dip-Fall: -${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedPriceDropPercent)}% (${botAnalysis.buyOrderProbabilityStr})", fontSize = 9.sp, color = Color(0xFF81C784))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🏷️ Max-Profit Verkauf-Order:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${numberFormat.format(botAnalysis.recommendedSellOrderPrice)} S.", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFFFB74D))
                                Text("📈 Peak-Anstieg: +${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedPriceRisePercent)}% (${botAnalysis.sellOrderProbabilityStr})", fontSize = 9.sp, color = Color(0xFFFFB74D))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (botAnalysis.aiOrderRecommendationTextDe.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = botAnalysis.aiOrderRecommendationTextDe,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "💡 Erwarteter Reingewinn: +${numberFormat.format(botAnalysis.expectedNetProfitPerGold)} Silber/Gold (+${String.format(Locale.GERMANY, "%.1f", botAnalysis.expectedRoiPercent)}% Marge nach Marktsteuer)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        // Recent Gold Price History Stock Chart (Aktienhandel-Style mit Punkten, Werten und Bogen)
        if (uiState.goldPrices.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📈 Aktien-Chart (${uiState.server.displayName})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${uiState.goldPrices.size} Punkte",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        GoldStockChart(
                            goldPrices = uiState.goldPrices,
                            numberFormat = numberFormat
                        )
                    }
                }
            }
        }

        // Live Einnahmen & Portfolio Summary Card
        item {
            val netProfitSilver = uiState.totalGoldNetProfitSilver
            val roiPercent = if (uiState.totalGoldCostSilver > 0) (netProfitSilver.toDouble() / uiState.totalGoldCostSilver) * 100.0 else 0.0

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, if (netProfitSilver >= 0) Color(0xFF10B981) else Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📈 Gold-Portfolio & Live-Einnahmen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "ECHTZEIT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Restlicher Goldbestand", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(uiState.totalGoldOwned)} Gold", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Käufe: ${uiState.totalGoldBought} | Verkäufe: ${uiState.totalGoldSold}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Echtzeit Reingewinn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${if (netProfitSilver >= 0) "+" else ""}${numberFormat.format(netProfitSilver)} Silber (${String.format(Locale.GERMANY, "%.1f", roiPercent)}%)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (netProfitSilver >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                            )
                            Text("Live-Kurs: ${numberFormat.format(uiState.currentGoldPrice)} S./Gold", fontSize = 10.sp, color = Color(0xFFFFD700))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Einkaufswert Restbestand:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(uiState.remainingGoldCostSilver)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Marktwert Restbestand:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(uiState.totalGoldCurrentValueSilver)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Gesamte Einkaufskosten:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(uiState.totalGoldCostSilver)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bisherige Verkaufserlöse:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${numberFormat.format(uiState.totalGoldEarnedSilver)} Silber", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF66BB6A))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.sendLiveGoldNotification(force = true) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔔 Live Einnahmen-Benachrichtigung auf Smartphone senden", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // Goldkauf & Goldverkauf erfassen Formular
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✍️ Gold-Transaktion protokollieren", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = isBuyMode,
                            onClick = {
                                isBuyMode = true
                                if (goldPriceInput.isBlank()) goldPriceInput = uiState.goldBuyPrice.toString()
                            },
                            label = { Text("🛒 Goldkauf", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = !isBuyMode,
                            onClick = {
                                isBuyMode = false
                                if (goldPriceInput.isBlank()) goldPriceInput = uiState.goldSellPrice.toString()
                            },
                            label = { Text("💰 Goldverkauf", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = goldAmountInput,
                            onValueChange = { goldAmountInput = it },
                            label = { Text("Gold Menge") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = goldPriceInput,
                            onValueChange = { goldPriceInput = it },
                            label = { Text(if (isBuyMode) "Kaufpreis/Gold" else "Verkaufspreis/Gold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isBuyMode) {
                        val amount = goldAmountInput.toIntOrNull() ?: 0
                        val price = goldPriceInput.toIntOrNull() ?: uiState.goldSellPrice
                        if (amount > 0 && price > 0) {
                            val avgBuy = if (uiState.totalGoldBought > 0) (uiState.totalGoldCostSilver / uiState.totalGoldBought).toInt() else uiState.currentGoldPrice
                            val totalEarned = amount.toLong() * price
                            val cost = amount.toLong() * avgBuy
                            val previewProfit = totalEarned - cost
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Live Verkauf: +${numberFormat.format(totalEarned)} Silber | Profit: ${if (previewProfit >= 0) "+" else ""}${numberFormat.format(previewProfit)} Silber",
                                color = if (previewProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val amount = goldAmountInput.toIntOrNull() ?: 0
                            val price = goldPriceInput.toIntOrNull() ?: 0
                            if (amount > 0 && price > 0) {
                                if (isBuyMode) {
                                    viewModel.addGoldPurchase(amount, price, goldDateInput)
                                    Toast.makeText(viewModel.getApplication(), "🛒 Goldkauf erfasst & gespeichert!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addGoldSale(amount, price, goldDateInput)
                                    Toast.makeText(viewModel.getApplication(), "💰 Goldverkauf erfasst & gespeichert!", Toast.LENGTH_SHORT).show()
                                }
                                goldAmountInput = ""
                                goldPriceInput = ""
                                goldDateInput = ""
                            } else {
                                Toast.makeText(viewModel.getApplication(), "Bitte gültige Menge & Preis eingeben", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBuyMode) MaterialTheme.colorScheme.primary else Color(0xFFFFB74D)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isBuyMode) Icons.Default.Add else Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBuyMode) "🛒 Goldkauf erfassen & speichern" else "💰 Goldverkauf erfassen & speichern",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Gold Kauf & Verkauf Historie & Protokoll
        val hasHistory = uiState.goldPurchases.isNotEmpty() || uiState.goldSales.isNotEmpty()
        if (hasHistory) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📜 Gold-Transaktions-Protokoll & Historie",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedHistoryFilter == 0,
                            onClick = { selectedHistoryFilter = 0 },
                            label = { Text("Alle (${uiState.goldPurchases.size + uiState.goldSales.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedHistoryFilter == 1,
                            onClick = { selectedHistoryFilter = 1 },
                            label = { Text("🛒 Käufe (${uiState.goldPurchases.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedHistoryFilter == 2,
                            onClick = { selectedHistoryFilter = 2 },
                            label = { Text("💰 Verkäufe (${uiState.goldSales.size})", fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (selectedHistoryFilter == 0 || selectedHistoryFilter == 1) {
                items(uiState.goldPurchases) { purchase ->
                    val netProfit = purchase.netProfitSilver(uiState.currentGoldPrice)
                    val roi = purchase.roiPercent(uiState.currentGoldPrice)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF38BDF8)
                                    ) {
                                        Text(
                                            "🛒 KAUF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${numberFormat.format(purchase.amountGold)} Gold", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                Text(
                                    text = "${if (netProfit >= 0) "+" else ""}${numberFormat.format(netProfit)} S. (${String.format(Locale.GERMANY, "%.1f", roi)}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (netProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Gekauft zu: ${numberFormat.format(purchase.buyPricePerGold)} Silber/Gold | Datum: ${purchase.purchaseDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Einkaufswert: ${numberFormat.format(purchase.totalCostSilver)} Silber", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row {
                                    IconButton(onClick = { 
                                        goldAmountInput = purchase.amountGold.toString()
                                        goldPriceInput = purchase.buyPricePerGold.toString()
                                        goldDateInput = purchase.purchaseDate
                                        isBuyMode = true
                                        viewModel.deleteGoldPurchase(purchase.id)
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deleteGoldPurchase(purchase.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedHistoryFilter == 0 || selectedHistoryFilter == 2) {
                items(uiState.goldSales) { sale ->
                    val realizedProfit = sale.realizedProfit(uiState.avgGoldBuyPrice)
                    val realizedRoi = sale.realizedRoiPercent(uiState.avgGoldBuyPrice)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFB74D)
                                    ) {
                                        Text(
                                            "💰 VERKAUF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${numberFormat.format(sale.amountGold)} Gold", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFFB74D))
                                }

                                Text(
                                    text = "${if (realizedProfit >= 0) "+" else ""}${numberFormat.format(realizedProfit)} S. (${String.format(Locale.GERMANY, "%.1f", realizedRoi)}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (realizedProfit >= 0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Verkauft zu: ${numberFormat.format(sale.sellPricePerGold)} Silber/Gold | Datum: ${sale.saleDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Verkaufserlös: ${numberFormat.format(sale.totalEarnedSilver)} Silber", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row {
                                    IconButton(onClick = { 
                                        goldAmountInput = sale.amountGold.toString()
                                        goldPriceInput = sale.sellPricePerGold.toString()
                                        goldDateInput = sale.saleDate
                                        isBuyMode = false
                                        viewModel.deleteGoldSale(sale.id)
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deleteGoldSale(sale.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
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
fun CraftingTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("Martlock") }
    var selectedStation by remember { mutableStateOf("ALLE STATIONEN") }
    val cities = listOf("ALLE", "Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Caerleon", "Brecilien")
    val stations = listOf("ALLE STATIONEN", "🪵 Sägewerk", "⚒️ Schmelze", "🧥 Gerberei", "🧵 Weberei", "🪨 Steinmetz", "🍳 Kochstelle", "🧪 Alchemielabor")
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    val allResources = remember { AlbionResourceRepository.resources }
    val filteredItems = remember(searchQuery, selectedStation) {
        allResources.filter { res ->
            val matchesQuery = searchQuery.isBlank() ||
                    res.nameDe.lowercase().contains(searchQuery.trim().lowercase()) ||
                    res.id.lowercase().contains(searchQuery.trim().lowercase())

            val matchesStation = when {
                selectedStation.contains("Sägewerk") -> res.id.contains("PLANKS") || res.id.contains("WOOD")
                selectedStation.contains("Schmelze") -> res.id.contains("METALBAR") || res.id.contains("ORE")
                selectedStation.contains("Gerberei") -> res.id.contains("LEATHER") || res.id.contains("HIDE")
                selectedStation.contains("Weberei") -> res.id.contains("CLOTH") || res.id.contains("FIBER")
                selectedStation.contains("Steinmetz") -> res.id.contains("STONE")
                selectedStation.contains("Kochstelle") -> res.category == ResourceCategory.FOOD
                selectedStation.contains("Alchemielabor") -> res.category == ResourceCategory.POTIONS
                else -> true
            }
            matchesQuery && matchesStation
        }
    }
    val priceMap = uiState.marketPrices.ifEmpty { AlbionMarketApi.getFallbackMarketPrices() }

    fun getBonusCityFor(res: AlbionResource): String {
        return when {
            res.id.contains("WOOD") || res.id.contains("PLANKS") -> "Lymhurst"
            res.id.contains("ORE") || res.id.contains("METALBAR") -> "Thetford"
            res.id.contains("HIDE") || res.id.contains("LEATHER") -> "Martlock"
            res.id.contains("FIBER") || res.id.contains("CLOTH") -> "Fort Sterling"
            res.id.contains("STONE") -> "Bridgewatch"
            res.category == ResourceCategory.FOOD || res.category == ResourceCategory.POTIONS -> "Caerleon"
            else -> "Martlock"
        }
    }

    val top3BonusCrafts = remember(filteredItems, selectedCity, priceMap) {
        try {
            filteredItems.asSequence().take(30).map { res ->
                val targetCity = if (selectedCity == "ALLE") getBonusCityFor(res) else selectedCity
                val recipe = CraftingRepository.getRecipeFor(res)
                val itemSellPrice = CraftingRepository.getPriceInCity(res.fullId, targetCity, priceMap)
                val totalCost = recipe.ingredients.sumOf { ing ->
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, targetCity, priceMap).toLong()
                }
                val profit = itemSellPrice.toLong() - totalCost
                Triple(res, profit, targetCity)
            }.sortedByDescending { it.second }.distinctBy { it.first.id }.take(3).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🔥 Top 3 Gewinn in Königsstadt mit Veredelungs-Bonus (+56% RRR)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    top3BonusCrafts.forEachIndexed { idx, (res, profit, bonusCity) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = res.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${idx + 1}. ${res.nameDe} ($bonusCity)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "+${fmt.format(profit)} Silber",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛠️ Crafting & Herstellungs-Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wähle eine Crafting-Station und Standort-Stadt zur automatischen Berechnung der profitabelsten Herstellungs-Deals, Materialkosten und Zutatenpreise.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Suche Item im Handwerksguide...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Löschen")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🏚️ Crafting-Station / Gebäude:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stations) { st ->
                            val isSelected = selectedStation == st
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStation = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🏙️ Standort-Stadt für Preise:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cities) { c ->
                            val isSelected = selectedCity == c
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCity = c },
                                label = { Text(c) }
                            )
                        }
                    }
                }
            }
        }

        val royalCities = listOf("Bridgewatch", "Fort Sterling", "Lymhurst", "Martlock", "Thetford", "Caerleon", "Brecilien")

        items(filteredItems.asSequence().distinctBy { it.fullId }.take(25).toList(), key = { "${it.fullId}_${it.tier}_${it.category.name}" }) { res ->
            val recipe = try { CraftingRepository.getRecipeFor(res) } catch (_: Exception) { CraftingRecipe(res.fullId, res.nameDe, res.nameEn, res.tier, res.category, emptyList()) }
            val itemSellPrice = try { CraftingRepository.getPriceInCity(res.fullId, selectedCity, priceMap) } catch (_: Exception) { 1500 }
            val totalCraftingCost = recipe.ingredients.sumOf { ing ->
                try {
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, selectedCity, priceMap).toLong()
                } catch (_: Exception) {
                    0L
                }
            }
            val profit = itemSellPrice.toLong() - totalCraftingCost
            val bestSellMarket = try { CraftingRepository.getHighestSellMarketDetails(res.fullId, priceMap) } catch (_: Exception) { "Caerleon" }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = res.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = res.nameDe,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val sellCityName = if (bestSellMarket.contains(" ")) bestSellMarket.split(" ")[0] else selectedCity
                                        viewModel.acceptTradeOpportunity(
                                            TradeOpportunity(
                                                resource = res,
                                                buyCity = selectedCity,
                                                buyPrice = totalCraftingCost.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt(),
                                                sellCity = sellCityName,
                                                sellPrice = itemSellPrice,
                                                unitNetProfit = profit.toInt(),
                                                unitWeightKg = 1.0,
                                                maxUnitsBySilver = 10,
                                                maxUnitsByWeight = 10,
                                                tradeUnits = 1,
                                                totalInvestment = totalCraftingCost,
                                                totalGrossRevenue = itemSellPrice.toLong(),
                                                totalNetRevenue = itemSellPrice.toLong(),
                                                totalNetProfit = profit,
                                                totalWeightKg = 1.0,
                                                roiPercent = if (totalCraftingCost > 0) (profit.toDouble() / totalCraftingCost) * 100.0 else 0.0,
                                                priorityScore = 100,
                                                zonesWalkedCount = 1,
                                                quality = 1,
                                                stockAvailable = 10,
                                                updatedDateFormatted = "Jetzt"
                                            )
                                        )
                                        Toast.makeText(viewModel.getApplication(), "${res.nameDe} zu Aufträgen hinzugefügt!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Auftrag hinzufügen", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = "💰 Höchster Verkauf: $bestSellMarket",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⚒️ Herstellungskosten in $selectedCity: ${fmt.format(totalCraftingCost)} S. | Gewinn: ${if (profit >= 0) "+" else ""}${fmt.format(profit)} S.",
                        color = if (profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📦 Benötigte Zutaten & Stückpreise in ALLEN Städten:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    recipe.ingredients.forEach { ing ->
                        val ingPrice = CraftingRepository.getPriceInCity(ing.resourceId, selectedCity, priceMap)
                        val cheapestInfo = CraftingRepository.getCheapestMarketDetails(ing.resourceId, priceMap)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "• ${ing.amount}x ${if (uiState.appLanguage == "DE") ing.nameDe else ing.nameEn} (Gesamt benötigt: ${ing.amount})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "In $selectedCity: ${fmt.format(ingPrice * ing.amount)} S. (${fmt.format(ingPrice)}/Stk)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Prices across ALL royal cities
                            val cityPricesText = royalCities.joinToString(" | ") { c ->
                                val p = CraftingRepository.getPriceInCity(ing.resourceId, c, priceMap)
                                val shortCode = when {
                                    c.contains("Bridgewatch") -> "BW"
                                    c.contains("Fort Sterling") -> "FS"
                                    c.contains("Lymhurst") -> "LH"
                                    c.contains("Martlock") -> "ML"
                                    c.contains("Thetford") -> "TF"
                                    c.contains("Caerleon") -> "CL"
                                    c.contains("Brecilien") -> "BC"
                                    else -> c.take(2)
                                }
                                "$shortCode: ${if (p > 0) "${fmt.format(p)} S." else "-"}"
                            }

                            Text(
                                text = "  🏙️ Städte: $cityPricesText",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "  📍 Günstigster Markt: $cheapestInfo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: INSEL GUIDE
// ----------------------------------------------------
@Composable
fun IslandTabContent(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("ALLE") }
    var selectedTierFilter by remember { mutableIntStateOf(0) }
    var activeTimers by remember { mutableStateOf(IslandTimerManager.getTimers(context)) }
    val cities = listOf("ALLE", "Caerleon", "Martlock", "Lymhurst", "Bridgewatch", "Fort Sterling", "Thetford", "Brecilien")
    val tiers = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val buildings = IslandRepository.buildings

    val filteredBuildings = remember(searchQuery, selectedCity) {
        buildings.filter { bldg ->
            val matchesQuery = searchQuery.isBlank() ||
                    bldg.nameDe.lowercase().contains(searchQuery.trim().lowercase()) ||
                    bldg.nameEn.lowercase().contains(searchQuery.trim().lowercase()) ||
                    bldg.cityBonusCity.lowercase().contains(searchQuery.trim().lowercase())

            val matchesCity = selectedCity == "ALLE" || bldg.cityBonusCity.contains(selectedCity, ignoreCase = true)
            matchesQuery && matchesCity
        }.sortedByDescending { it.estimatedRoiPercent }
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 🌾 Insel-Ernte & Tierzucht-Timer Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🌾 Insel-Ernte & Tierzucht Timer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF10B981)
                        )
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF10B981)) {
                            Text(
                                text = "${activeTimers.size} AKTIV",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeTimers.isEmpty()) {
                        Text(
                            text = "Bisher keine aktiven Insel-Timer. Starte einen Ernte- oder Tierzucht-Timer unten!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activeTimers.forEach { timer ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (timer.isReady()) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(timer.nameDe, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(timer.remainingFormatted(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (timer.isReady()) Color(0xFF10B981) else Color(0xFFFFB74D))
                                        Text("Fertig am: ${timer.expectedHarvestDateFormatted()}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            IslandTimerManager.removeTimer(context, timer.id)
                                            activeTimers = IslandTimerManager.getTimers(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Timer löschen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("➕ Neuen Insel-Timer starten:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(IslandTimerManager.standardCropOptions) { (cropName, hours) ->
                            OutlinedButton(
                                onClick = {
                                    IslandTimerManager.addTimer(context, cropName, TimerCategory.CROP, hours)
                                    activeTimers = IslandTimerManager.getTimers(context)
                                    Toast.makeText(context, "$cropName Timer ($hours h) gestartet!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(cropName, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(IslandTimerManager.standardAnimalOptions) { (animalName, hours) ->
                            OutlinedButton(
                                onClick = {
                                    IslandTimerManager.addTimer(context, animalName, TimerCategory.ANIMAL, hours)
                                    activeTimers = IslandTimerManager.getTimers(context)
                                    Toast.makeText(context, "$animalName Timer ($hours h) gestartet!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(animalName, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏝️ Insel-Bau & Upgrade Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Übersicht aller Insel-Gebäude, benötigte Upgrade-Ressourcen (T1 bis Max T8) und Städten-Crafting-Boni.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Suche Insel-Gebäude / Bonus...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Löschen")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "⭐ Stufe / Tier Ausbaustufe wählen:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tiers) { t ->
                            val isSelected = selectedTierFilter == t
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTierFilter = t },
                                label = { Text(if (t == 0) "Alle Stufen (T1-T8)" else "T$t") }
                            )
                        }
                    }

                    Text(
                        text = "🏙️ Filter nach Königsstadt-Bonus:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cities) { c ->
                            val isSelected = selectedCity == c
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCity = c },
                                label = { Text(if (c == "ALLE") "Alle Städt-Boni" else c) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🏆 Top 3 Einnahme-Gebäude auf Inseln mit Marge",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val top3Income = remember { buildings.asSequence().sortedByDescending { it.estimatedDailyIncomeSilver }.take(3).toList() }
                    top3Income.forEachIndexed { idx, bldg ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = "${idx + 1}. ${bldg.nameDe}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~${fmt.format(bldg.estimatedDailyIncomeSilver)} S./Tag (+${bldg.estimatedRoiPercent}% Marge)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(filteredBuildings, key = { _, bldg -> bldg.id }) { index, bldg ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, if (index < 3) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (index < 3) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text(
                                        text = "🔥 Top Deal (#${index + 1} Marge: +${String.format(Locale.GERMANY, "%.1f", bldg.estimatedRoiPercent)}%)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = bldg.nameDe,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    viewModel.acceptIslandBuildingOrder(bldg)
                                    Toast.makeText(viewModel.getApplication(), "Insel-Auftrag '${bldg.nameDe}' angenommen!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Auftrag annehmen", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Max T${bldg.maxTier}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "⚡ Funktion: ${bldg.abilityDescDe}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "💰 Schätzung Einnahmen: ~${fmt.format(bldg.estimatedDailyIncomeSilver)} Silber/Tag (+${bldg.estimatedRoiPercent}% Marge)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // City Bonus Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🏙️ Königsstadt-Bonus: ${bldg.cityBonusCity}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (uiState.appLanguage == "DE") bldg.cityBonusDescDe else bldg.cityBonusDescEn,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🏗️ Baukosten & Upgrades (T1-T8 Material & Silber):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val displayedUpgrades = if (selectedTierFilter == 0) bldg.upgrades else bldg.upgrades.filter { it.tier == selectedTierFilter }
                    displayedUpgrades.forEach { up ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = up.tierName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• Baukosten: ${if (up.silverCost > 0) "${fmt.format(up.silverCost)} Silber | " else ""}${up.woodReq} | ${up.stoneReq}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

// ----------------------------------------------------
// TAB 8: WELTKARTE TAB (INTERAKTIV MIT BONI & PREISEN)
// ----------------------------------------------------
@Composable
fun WorldMapTabContent(
    viewModel: AlbionResourceViewModel
) {
    var selectedRegionName by remember { mutableStateOf("Bridgewatch") }
    val regions = AlbionWorldData.worldRegions
    val activeRegion = regions.find { it.name == selectedRegionName } ?: regions.first()
    val uiState by viewModel.uiState.collectAsState()

    val cityName = activeRegion.name.split(" ")[0]
    val refinedRes = remember(uiState.marketPrices) {
        AlbionResourceRepository.resources.filter { it.category == ResourceCategory.REFINED }
    }
    val matchingRes = remember(refinedRes, activeRegion.name) {
        refinedRes.filter { res ->
            when {
                activeRegion.name.contains("Bridgewatch") -> res.id.contains("LEATHER")
                activeRegion.name.contains("Fort Sterling") -> res.id.contains("METALBAR")
                activeRegion.name.contains("Lymhurst") -> res.id.contains("PLANKS")
                activeRegion.name.contains("Martlock") -> res.id.contains("STONEBLOCK")
                activeRegion.name.contains("Thetford") -> res.id.contains("CLOTH")
                else -> true
            }
        }.take(10)
    }

    val avgCost = remember(matchingRes, cityName, uiState.marketPrices) {
        if (matchingRes.isNotEmpty()) {
            matchingRes.map { res ->
                val recipe = CraftingRepository.getRecipeFor(res)
                recipe.ingredients.sumOf { ing ->
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, cityName, uiState.marketPrices).toLong()
                }
            }.average().toLong()
        } else 0L
    }

    val avgNetProfit = remember(matchingRes, cityName, uiState.marketPrices) {
        if (matchingRes.isNotEmpty()) {
            matchingRes.map { res ->
                val recipe = CraftingRepository.getRecipeFor(res)
                val cost = recipe.ingredients.sumOf { ing ->
                    ing.amount.toLong() * CraftingRepository.getPriceInCity(ing.resourceId, cityName, uiState.marketPrices).toLong()
                }
                val sell = CraftingRepository.getPriceInCity(res.fullId, cityName, uiState.marketPrices)
                val netEarn = (sell * 0.96).toLong() // 4% market tax
                netEarn - cost
            }.average().toLong()
        } else 0L
    }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🗺️ Interaktive Albion Online Weltkarte",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Wähle eine Stadt oder Region zur Anzeige von Veredelungs-Boni, Zonen-Gefahren & Live-Marktpreisen",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Region Selection Chips
        item {
            Text("🏙️ Region oder Portalstadt wählen:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(regions) { reg ->
                    val isSelected = selectedRegionName == reg.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRegionName = reg.name },
                        label = { Text(reg.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Active Region Details Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeRegion.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (activeRegion.safety) {
                                ZoneSafety.SAFE_BLUE -> Color(0xFF1E88E5)
                                ZoneSafety.SAFE_YELLOW -> Color(0xFFFDD835)
                                ZoneSafety.DANGEROUS_RED -> Color(0xFFE53935)
                                ZoneSafety.DANGEROUS_BLACK -> Color(0xFF424242)
                            }
                        ) {
                            Text(
                                text = activeRegion.safety.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Typ: ${activeRegion.type}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(activeRegion.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("🌱 Veredelungs-Bonus & Spezialisierung:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val refiningBonusText = when {
                        activeRegion.name.contains("Bridgewatch") -> "🔥 +15% Veredelungs-Bonus auf Leder & Haut"
                        activeRegion.name.contains("Fort Sterling") -> "🔥 +15% Veredelungs-Bonus auf Erz & Metall"
                        activeRegion.name.contains("Lymhurst") -> "🔥 +15% Veredelungs-Bonus auf Holz & Planken"
                        activeRegion.name.contains("Martlock") -> "🔥 +15% Veredelungs-Bonus auf Stein & Blöcke"
                        activeRegion.name.contains("Thetford") -> "🔥 +15% Veredelungs-Bonus auf Faser & Stoff"
                        activeRegion.name.contains("Caerleon") -> "💀 Schwarzmarkt-Konto (Black Market) & Werkzeug-Veredelung"
                        activeRegion.name.contains("Brecilien") -> "✨ Nebel-Artefakte & Feen-Handel (+15%)"
                        else -> "⭐ Standard Veredelung"
                    }
                    Text(refiningBonusText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💰 Veredelungskosten & Netto-Gewinn in $cityName:", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Ø Veredelungskosten (Rohstoffe):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text("${fmt.format(avgCost)} Silber", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Ø Netto-Gewinn (nach Steuern):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text("${if (avgNetProfit >= 0) "+" else ""}${fmt.format(avgNetProfit)} Silber", color = if (avgNetProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("⛏️ Vorkommende Ressourcen:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    activeRegion.resourcesFound.forEach { res ->
                        Text("• $res", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("🛣️ Verbundene Handelsrouten:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(activeRegion.connectsTo.joinToString(" ➔ "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Set as Standpunkt button
                    Button(
                        onClick = {
                            val shortName = activeRegion.name.split(" ")[0]
                            viewModel.onCityFilterSelected(shortName)
                            viewModel.onTabSelected(1) // Jump to Calculator tab
                            Toast.makeText(viewModel.getApplication(), "Standpunkt $shortName gesetzt!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Als Kaufort/Standpunkt im Rechner setzen")
                    }
                }
            }
        }
    }
}

@Composable
fun RainbowMadeByDnnxText() {
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTransition")
    val hueOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HueOffset"
    )

    val rainbowColors = listOf(
        Color.hsv((hueOffset + 0f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 60f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 120f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 180f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 240f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 300f) % 360f, 0.95f, 1f),
        Color.hsv((hueOffset + 360f) % 360f, 0.95f, 1f)
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(2.dp, Brush.horizontalGradient(rainbowColors)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MadeByDnnx",
                style = TextStyle(
                    brush = Brush.horizontalGradient(rainbowColors),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun AppSettingsDialog(
    viewModel: AlbionResourceViewModel,
    uiState: ResourceUiState,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var sysNotifs by remember { mutableStateOf(uiState.systemNotificationsEnabled) }
    var goldNotifs by remember { mutableStateOf(uiState.goldNotificationsEnabled) }
    val selectedLang by remember { mutableStateOf(uiState.appLanguage) }

    val prefs = remember { AppPreferences(context) }
    var botNameInput by remember { mutableStateOf(prefs.aiBotName) }
    var showAdminControlDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LanguageManager.getString("settings_title", selectedLang), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Rainbow Animated Banner
                RainbowMadeByDnnxText()

                // Website & Support Button directly under MadeByDnnx
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, "https://albionmarketv2-1.onrender.com".toUri())
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Webseite konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "🌐 Offizielle Webseite:\nalbionmarketv2-1.onrender.com",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }

                // AlbionDataProAdmin Button (Only visible for admin accounts)
                if (prefs.isAdmin) {
                    Button(
                        onClick = { showAdminControlDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ AlbionDataProAdmin - Live-Zentrale",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (showAdminControlDialog) {
                    AdminControlDialog(
                        viewModel = viewModel,
                        onDismiss = { showAdminControlDialog = false }
                    )
                }

                HorizontalDivider()

                // Zugangsdaten & Konto Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔐 Zugangsdaten & Konto", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val savedEmail = LicenseManager.getSavedUserEmail(context)
                        val activatedCode = LicenseManager.getActivatedCode(context)
                        val hwId = LicenseManager.getHardwareId(context)

                        Text("Benutzername / E-Mail:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(savedEmail.ifBlank { "Nicht hinterlegt / Standard" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aktivierter Lizenzschlüssel:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(activatedCode.ifBlank { "Kein Schlüssel aktiv" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hardware-ID:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(hwId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                    }
                }

                // KI Bot Name Card (Real-Time Reactive)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🤖 KI Bot Name (Benachrichtigungen)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = botNameInput,
                            onValueChange = {
                                botNameInput = it
                                prefs.aiBotName = it
                            },
                            label = { Text("Bot Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                // System Notifications (Real-Time Reactive)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(LanguageManager.getString("sys_notif_toggle", selectedLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Sendet Benachrichtigungen bei Top-Deals in Echtzeit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = sysNotifs,
                        onCheckedChange = {
                            sysNotifs = it
                            viewModel.onToggleSystemNotifications(it)
                        }
                    )
                }

                // App bei Bubble-Aktivierung verbergen
                var hideAppOnBubble by remember { mutableStateOf(prefs.hideAppOnBubbleActivate) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App bei Bubble-Aktivierung verbergen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Minimiert die App automatisch, wenn die Overlay Bubble gestartet wird", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hideAppOnBubble,
                        onCheckedChange = {
                            hideAppOnBubble = it
                            prefs.hideAppOnBubbleActivate = it
                        }
                    )
                }

                HorizontalDivider()

                // Bubble Deckkraft / Transparenz
                var bubbleOpacityVal by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(LanguageManager.getString("bubble_opacity", selectedLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(0.65f to "65%", 0.80f to "80%", 0.90f to "90%", 1.0f to "100%").forEach { (valOp, labelOp) ->
                            val isSelected = abs(bubbleOpacityVal - valOp) < 0.04f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    bubbleOpacityVal = valOp
                                    prefs.bubbleOpacity = valOp
                                },
                                label = { Text(labelOp) }
                            )
                        }
                    }
                }

                // Bubble Skalierung / Größe
                var bubbleScaleVal by remember { mutableFloatStateOf(prefs.bubbleScale) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(LanguageManager.getString("bubble_scale", selectedLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(0.80f to "80%", 0.90f to "90%", 1.0f to "100%", 1.15f to "115%").forEach { (valSc, labelSc) ->
                            val isSelected = abs(bubbleScaleVal - valSc) < 0.04f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    bubbleScaleVal = valSc
                                    prefs.bubbleScale = valSc
                                },
                                label = { Text(labelSc) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Gold Portfolio Notifications (Real-Time Reactive)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(LanguageManager.getString("gold_notif_toggle", selectedLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(LanguageManager.getString("gold_notif_desc", selectedLang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = goldNotifs,
                        onCheckedChange = {
                            goldNotifs = it
                            viewModel.onToggleGoldNotifications(it)
                        }
                    )
                }

                HorizontalDivider(color = Color(0xFF334155))

                // Logout Button
                Button(
                    onClick = {
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔒 Abmelden / Logout", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(LanguageManager.getString("close", selectedLang))
            }
        }
    )
}

@Composable
fun ResourceCard(
    resource: AlbionResource,
    prices: List<MarketPrice>,
    allMarketPrices: Map<String, List<MarketPrice>> = emptyMap(),
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    var selectedEnc by remember(resource.id) { mutableIntStateOf(resource.enchantment) }
    var selectedQual by remember(resource.id) { mutableIntStateOf(resource.quality) }

    val effectiveResource = remember(resource, selectedEnc, selectedQual) {
        resource.copy(enchantment = selectedEnc, quality = selectedQual)
    }

    val baseList = remember(effectiveResource.fullId, prices, allMarketPrices) {
        if (allMarketPrices.isNotEmpty()) {
            allMarketPrices[effectiveResource.fullId] ?: allMarketPrices[effectiveResource.id] ?: prices
        } else {
            prices
        }
    }
    val mult = when (effectiveResource.enchantment) {
        1 -> 2.2
        2 -> 4.5
        3 -> 9.0
        4 -> 18.0
        else -> 1.0
    }
    val currentPrices = if (allMarketPrices.isNotEmpty() && allMarketPrices[effectiveResource.fullId] == null && effectiveResource.enchantment > 0) {
        baseList.map { it.copy(sellPriceMin = (it.sellPriceMin * mult).toInt(), buyPriceMax = (it.buyPriceMax * mult).toInt()) }
    } else {
        baseList
    }

    val validPrices = currentPrices.filter { it.sellPriceMin > 0 }
    val bestBuy = validPrices.minByOrNull { it.sellPriceMin }
    val bestSell = validPrices.maxByOrNull { it.sellPriceMin }
    val avgPrice = if (validPrices.isNotEmpty()) validPrices.asSequence().map { it.sellPriceMin }.average().toInt() else 0
    val ratioPercent = if (avgPrice > 0 && bestBuy != null) ((bestBuy.sellPriceMin.toDouble() / avgPrice) * 100).toInt() else 100

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                // Tier & Enchantment Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getTierColor(effectiveResource.tier),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "${effectiveResource.tierText}${effectiveResource.enchantmentText}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (bestBuy != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (ratioPercent <= 90) Color(0xFF10B981) else if (ratioPercent >= 150) Color(0xFFEF4444) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "$ratioPercent%",
                            color = if (ratioPercent !in 91..149) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Item Image
                AsyncImage(
                    model = effectiveResource.imageUrl,
                    contentDescription = effectiveResource.nameDe,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = effectiveResource.nameDe,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // ENCHANTMENT SELECTOR CHIPS (.0, .1, .2, .3, .4)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                (0..4).forEach { enc ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selectedEnc == enc) Color(0xFF10B981) else Color(0xFF1E3A4C),
                        modifier = Modifier.clickable { selectedEnc = enc }
                    ) {
                        Text(
                            text = ".$enc",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (bestBuy != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🛒 Kaufort: ${bestBuy.city}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${numberFormat.format(bestBuy.sellPriceMin)} Silber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    if (bestSell != null && bestSell.city != bestBuy.city) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "🏷️ Verkaufort: ${bestSell.city}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${numberFormat.format(bestSell.sellPriceMin)} Silber",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Bisher kein Preis",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ResourceDetailContent(
    resource: AlbionResource,
    prices: List<MarketPrice>,
    onCopyId: (String) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = resource.imageUrl,
            contentDescription = resource.nameDe,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getTierColor(resource.tier)
            ) {
                Text(
                    text = resource.tierText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = resource.nameDe,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Text(
            text = resource.nameEn,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = resource.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (prices.isNotEmpty()) {
            Text(
                text = "Preise in den Hauptstädten:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MarketPriceStockChart(
                        prices = prices,
                        numberFormat = numberFormat
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    val sortedPrices = prices.sortedByDescending { TradeCalculator.parseIsoToEpochMs(it.sellPriceMinDate) }
                    sortedPrices.forEach { pr ->
                        if (pr.sellPriceMin > 0) {
                            val isDangerous = listOf("Caerleon", "Arthur's Rest", "Merlyn's Rest", "Morgana's Rest", "Cairn Drain").any { pr.city.equals(it, ignoreCase = true) }
                            val ageStr = TradeCalculator.formatPriceAge(TradeCalculator.parseIsoToEpochMs(pr.sellPriceMinDate))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pr.city, fontWeight = FontWeight.SemiBold)
                                    if (pr.quality > 1) {
                                        Text(" (Q${pr.quality})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("⏱️ $ageStr", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    if (isDangerous) {
                                        Text(" 🔴", color = Color(0xFFFF5252), fontSize = 10.sp)
                                    }
                                }
                                Text("${numberFormat.format(pr.sellPriceMin)} Silber", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Crafting Recipe & Ingredients with lowest costs
        val recipe = remember(resource) { CraftingRepository.getRecipeFor(resource) }
        val priceMap = remember { AlbionMarketApi.getFallbackMarketPrices() }

        Text(
            text = "🛠️ Benötigte Herstellungsmaterialien & geringste Kosten:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                recipe.ingredients.forEach { ing ->
                    val cheapestInfo = CraftingRepository.getCheapestMarketDetails(ing.resourceId, priceMap)
                    val unitPrice = CraftingRepository.getPriceInCity(ing.resourceId, "ALLE", priceMap)
                    val totalCost = ing.amount.toLong() * unitPrice.toLong()

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• ${ing.amount}x ${ing.nameDe}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~${numberFormat.format(totalCost)} Silber",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "  📍 Günstigster Markt: $cheapestInfo",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ID: ${resource.fullId}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { onCopyId(resource.fullId) },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Item ID kopieren")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun getTierColor(tier: Int): Color {
    return when (tier) {
        2 -> Color(0xFF4CAF50) // Green
        3 -> Color(0xFF2196F3) // Blue
        4 -> Color(0xFF9C27B0) // Purple
        5 -> Color(0xFFFF9800) // Orange
        6 -> Color(0xFFE91E63) // Red/Pink
        7 -> Color(0xFF00BCD4) // Cyan/Gold
        8 -> Color(0xFFFFD700) // Gold
        else -> Color.Gray
    }
}

