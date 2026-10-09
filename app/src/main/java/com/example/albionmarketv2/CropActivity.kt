package com.example.albionmarketv2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class CropActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val file = File(cacheDir, "full_screenshot.png")
        if (!file.exists()) {
            Toast.makeText(this, "Kein Screenshot gefunden", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        
        setContent {
            var startOffset by remember { mutableStateOf<Offset?>(null) }
            var endOffset by remember { mutableStateOf<Offset?>(null) }

            var showSaveDialog by remember { mutableStateOf(false) }
            var templateName by remember { mutableStateOf("") }
            var templateCategory by remember { mutableStateOf("") }
            var finalStartOffset by remember { mutableStateOf<Offset?>(null) }
            var finalEndOffset by remember { mutableStateOf<Offset?>(null) }

            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Bild speichern") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bitte gib dem erkannten Ziel einen Namen und weise es einer Kategorie zu (z. B. 'Gathering' oder 'Crafting').")
                            OutlinedTextField(
                                value = templateName,
                                onValueChange = { templateName = it },
                                label = { Text("Name") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = templateCategory,
                                onValueChange = { templateCategory = it },
                                label = { Text("Kategorie") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (templateName.isNotBlank() && finalStartOffset != null && finalEndOffset != null) {
                                    saveCroppedImage(bitmap, finalStartOffset!!, finalEndOffset!!, templateName, templateCategory)
                                } else {
                                    Toast.makeText(this@CropActivity, "Name darf nicht leer sein!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Speichern")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showSaveDialog = false }) {
                            Text("Abbrechen")
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startOffset = it; endOffset = it },
                                onDrag = { change, _ -> endOffset = change.position },
                                onDragEnd = { }
                            )
                        }
                ) {
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )

                    if (startOffset != null && endOffset != null) {
                        val rect = Rect(startOffset!!, endOffset!!)
                        drawRect(
                            color = Color.Red,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(width = 5f)
                        )
                        // Abdunkeln des unwichtigen Bereichs
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset.Zero, size = Size(size.width, rect.top))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, rect.bottom), size = Size(size.width, size.height - rect.bottom))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(rect.right, rect.top), size = Size(size.width - rect.right, rect.height))
                    }
                }

                if (startOffset != null && endOffset != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                startOffset = null
                                endOffset = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Neu markieren")
                        }
                        Button(
                            onClick = {
                                finalStartOffset = startOffset
                                finalEndOffset = endOffset
                                showSaveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Speichern")
                        }
                    }
                }
            }
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap, start: Offset, end: Offset, name: String, category: String) {
        val metrics = resources.displayMetrics
        val scaleX = bitmap.width.toFloat() / metrics.widthPixels
        val scaleY = bitmap.height.toFloat() / metrics.heightPixels

        val left = min(start.x, end.x) * scaleX
        val top = min(start.y, end.y) * scaleY
        val right = max(start.x, end.x) * scaleX
        val bottom = max(start.y, end.y) * scaleY

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return

        try {
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                left.toInt().coerceIn(0, bitmap.width - 1),
                top.toInt().coerceIn(0, bitmap.height - 1),
                width.toInt().coerceAtMost(bitmap.width - left.toInt()),
                height.toInt().coerceAtMost(bitmap.height - top.toInt())
            )

            TemplateManager.saveNewTemplate(this, croppedBitmap, name, category)
            Toast.makeText(this, "Ziel '$name' gespeichert!", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Fehler beim Zuschneiden.", Toast.LENGTH_SHORT).show()
        }
    }
}