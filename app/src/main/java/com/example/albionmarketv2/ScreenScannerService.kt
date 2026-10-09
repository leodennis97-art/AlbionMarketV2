package com.example.albionmarketv2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.nio.ByteBuffer

class ScreenScannerService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var currentWorkflow: BotWorkflow? = null
    private var currentStepIndex = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopScanning()
            return START_NOT_STICKY
        }

        val workflowId = intent?.getStringExtra("WORKFLOW_ID")
        if (workflowId != null) {
            currentWorkflow = TemplateManager.getWorkflows(this).find { it.id == workflowId }
            currentStepIndex = 0
            if (currentWorkflow == null) {
                Log.e("BotScanner", "Workflow nicht gefunden!")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("DATA")

        if (resultCode != 0 && data != null) {
            startForegroundService()
            setupMediaProjection(resultCode, data)
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "bot_scanner_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bot Scanner", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DataPro Bot")
            .setContentText("Ablauf '${currentWorkflow?.name ?: "Unbekannt"}' aktiv...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(2001, notification)
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)
        
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "BotScanner", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        isRunning = true
        startScanningLoop()
    }

    private fun startScanningLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                if (currentWorkflow == null || currentWorkflow!!.steps.isEmpty()) return

                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image)
                        image.close()
                        
                        val step = currentWorkflow!!.steps[currentStepIndex]
                        val template = TemplateManager.loadTemplateBitmap(this@ScreenScannerService, step.templateId)

                        if (template != null) {
                            val pt = ImageMatcher.findTemplate(bitmap, template, 0.85)
                            if (pt != null) {
                                Log.d("BotScanner", "Ziel gefunden bei X:${pt.x}, Y:${pt.y}. Klicke...")
                                AutoClickerService.instance?.clickAt(pt.x.toFloat(), pt.y.toFloat())
                                
                                // Gehe zum nächsten Schritt
                                currentStepIndex++
                                if (currentStepIndex >= currentWorkflow!!.steps.size) {
                                    currentStepIndex = 0 // Ablauf wiederholen
                                }
                                
                                // Pause bis zum nächsten Schritt
                                handler.postDelayed(this, step.delayAfterMs)
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BotScanner", "Fehler im Scan-Loop", e)
                }
                
                // Falls Bild nicht gefunden wurde: bald nochmal scannen
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Return a clean copy without padding
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun stopScanning() {
        isRunning = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        stopForeground(true)
        stopSelf()
    }
    
    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }
}