package com.example.albionmarketv2

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream
import kotlinx.coroutines.launch

@Composable
fun AiImageBotSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var botManager by remember { mutableStateOf<AiImageBotManager?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf("🤖 KI-Bot bereit. Füge ausgeschnittene Referenzbilder hinzu.")) }
    var targets by remember { mutableStateOf(listOf<BotTarget>()) }
    val threshold by remember { mutableFloatStateOf(0.85f) }

    LaunchedEffect(Unit) {
        botManager = AiImageBotManager(
            onLog = { msg -> logs = (listOf(msg) + logs).take(50) },
            onTapAction = { x, y ->
                try {
                    val clicker = AutoClickerService.instance
                    if (clicker != null) {
                        clicker.clickAt(x.toFloat(), y.toFloat())
                    } else {
                        val process = Runtime.getRuntime().exec(arrayOf("input", "tap", x.toString(), y.toString()))
                        process.waitFor()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val target = BotTarget(
                        name = "Ziel #${targets.size + 1}",
                        templateBitmap = bitmap,
                        threshold = threshold.toDouble(),
                        enabled = true
                    )
                    botManager?.addTarget(target)
                    targets = botManager?.getTargets() ?: emptyList()
                }
            } catch (e: Exception) {
                logs = (listOf("❌ Fehler beim Laden des Bildes: ${e.message}") + logs).take(50)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 Programmierbarer KI-Bild-Bot",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = isRunning,
                    onCheckedChange = { running ->
                        isRunning = running
                        if (running) {
                            botManager?.startBot { null }
                        } else {
                            botManager?.stopBot()
                        }
                    }
                )
            }

            Text(
                text = "Schneide Gegenstände, Buttons oder Symbole aus und lasse den Bot den Bildschirm danach absuchen und automatisch tippen.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var userPrompt by remember { mutableStateOf("") }
            var isThinking by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                label = { Text("Anforderung an KI (z.B. 'Klicke auf Holz')") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (userPrompt.isNotBlank()) {
                        isThinking = true
                        coroutineScope.launch {
                            logs = (listOf("🧠 KI analysiert Anforderung: '$userPrompt'...") + logs)
                            val interpretation = AiBotAgent.interpretPrompt(userPrompt)
                            logs = (listOf("💡 KI-Selbstprogrammierung: $interpretation") + logs)
                            isThinking = false
                        }
                    }
                },
                enabled = !isThinking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isThinking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KI programmiert Bot...")
                } else {
                    Text("🧠 KI-Anforderung anwenden & programmieren")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bild hinzufügen")
                }

                Button(
                    onClick = {
                        if (isRunning) {
                            botManager?.stopBot()
                            isRunning = false
                        } else {
                            botManager?.startBot { null }
                            isRunning = botManager?.isRunning ?: false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color.Red else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "Bot Stoppen" else "Bot Starten")
                }
            }

            if (targets.isNotEmpty()) {
                Text(text = "Aktive Ziele (${targets.size}):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    targets.forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = target.templateBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                )
                                Text(target.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            IconButton(onClick = {
                                botManager?.removeTarget(target.id)
                                targets = botManager?.getTargets() ?: emptyList()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }
            }

            Text(text = "Log-Konsole:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = Color.Green,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
// V3.5.0
