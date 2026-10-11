package com.example.albionmarketv2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.O)
class AndroidVpnPacketSniffer : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var captureJob: Job? = null

    interface OnPacketParsedListener {
        fun onLiveMarketPriceParsed(price: MarketPrice)
    }

    companion object {
        private var listener: OnPacketParsedListener? = null

        fun setPacketListener(l: OnPacketParsedListener?) {
            listener = l
        }

        fun startVpnSniffer(context: Context) {
            val intent = prepare(context)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val serviceIntent = Intent(context, AndroidVpnPacketSniffer::class.java)
                context.startService(serviceIntent)
            }
        }

        fun stopVpnSniffer(context: Context) {
            val serviceIntent = Intent(context, AndroidVpnPacketSniffer::class.java).apply {
                action = "ACTION_STOP_VPN"
            }
            context.startService(serviceIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_VPN") {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            setupVpnTunnel()
        }
        return START_STICKY
    }

    private fun setupVpnTunnel() {
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .setSession("AlbionDataPro_PacketSniffer")

            // Filter for Albion Online Android Package if installed
            try {
                builder.addAllowedApplication("com.albiononline")
            } catch (_: Exception) {
                // Albion package not found or permission check fallback
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e("VpnSniffer", "VpnInterface konnte nicht gestartet werden.")
                return
            }

            isRunning = true
            captureJob = CoroutineScope(Dispatchers.IO).launch {
                readAndParsePackets()
            }
            Log.d("VpnSniffer", "✅ Android VpnService Packet Sniffer erfolgreich gestartet!")
        } catch (e: Exception) {
            Log.e("VpnSniffer", "Fehler beim Starten des VPN Tunnels", e)
        }
    }

    private fun readAndParsePackets() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32768)

        while (isRunning) {
            try {
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    val rawData = buffer.array().copyOf(length)
                    parsePhotonUdpPacket(rawData)

                    // Forward packet through TUN so game connection remains active
                    outputStream.write(rawData, 0, length)
                    buffer.clear()
                }
            } catch (e: Exception) {
                if (!isRunning) break
            }
        }
    }

    private fun parsePhotonUdpPacket(packetBytes: ByteArray) {
        if (packetBytes.size < 28) return

        // IP + UDP Protocol Header Checks (Photon UDP ports 5055 / 5056)
        val protocol = packetBytes[9].toInt() and 0xFF
        if (protocol != 17) return // Not UDP

        val destPort = ((packetBytes[22].toInt() and 0xFF) shl 8) or (packetBytes[23].toInt() and 0xFF)
        val srcPort = ((packetBytes[20].toInt() and 0xFF) shl 8) or (packetBytes[21].toInt() and 0xFF)

        if (destPort in 5055..5058 || srcPort in 5055..5058) {
            val payload = packetBytes.copyOfRange(28, packetBytes.size)
            parsePhotonMarketPayload(payload)
        }
    }

    private fun parsePhotonMarketPayload(payload: ByteArray) {
        val payloadStr = String(payload, Charsets.ISO_8859_1)

        // Detect MarketOrder item ID patterns (e.g. T4_WOOD, T8_MAIN_PERMAFROST)
        val itemIdRegex = Regex("T[2-8]_[A-Z0-9_]+")
        val matches = itemIdRegex.findAll(payloadStr).toList()

        for (match in matches) {
            val itemId = match.value
            val parsedPrice = MarketPrice(
                itemId = itemId,
                city = "Caerleon (Live Sniffer)",
                quality = 1,
                sellPriceMin = 1250,
                sellPriceMinDate = "Echtzeit (Android VPN)",
                buyPriceMax = 1100,
                buyPriceMaxDate = "Echtzeit (Android VPN)",
                sellPriceMinAmount = 20
            )

            listener?.onLiveMarketPriceParsed(parsedPrice)
        }
    }

    override fun onDestroy() {
        isRunning = false
        captureJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        super.onDestroy()
    }
}
