package com.example.albionmarketv2

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

object LocalSnifferBridge {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var listenerJob: Job? = null

    interface LiveMarketDataListener {
        fun onLiveMarketPacketsReceived(prices: List<MarketPrice>)
    }

    private var dataListener: LiveMarketDataListener? = null

    fun setListener(listener: LiveMarketDataListener?) {
        this.dataListener = listener
    }

    fun startBridgeServer(port: Int = 9090) {
        if (isRunning) return
        isRunning = true

        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("LocalSnifferBridge", "Echtzeit-Sniffer Server gestartet auf Port $port")

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClientConnection(clientSocket)
                }
            } catch (e: Exception) {
                Log.e("LocalSnifferBridge", "Fehler im Sniffer Bridge Server", e)
            } finally {
                stopBridgeServer()
            }
        }
    }

    private suspend fun handleClientConnection(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val line = reader.readLine()

            if (!line.isNullOrEmpty()) {
                val parsedPrices = parsePacketJson(line)
                if (parsedPrices.isNotEmpty()) {
                    dataListener?.onLiveMarketPacketsReceived(parsedPrices)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun parsePacketJson(rawJson: String): List<MarketPrice> {
        val list = mutableListOf<MarketPrice>()
        try {
            if (rawJson.trim().startsWith("[")) {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val price = parseSinglePriceObject(obj)
                    if (price != null) list.add(price)
                }
            } else if (rawJson.trim().startsWith("{")) {
                val obj = JSONObject(rawJson)
                val price = parseSinglePriceObject(obj)
                if (price != null) list.add(price)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSinglePriceObject(obj: JSONObject): MarketPrice? {
        val itemId = obj.optString("item_id", obj.optString("ItemId", ""))
        val city = obj.optString("location", obj.optString("City", ""))
        val quality = obj.optInt("quality", obj.optInt("Quality", 1))
        val sellPriceMin = obj.optInt("sell_price_min", obj.optInt("SellPriceMin", 0))
        val buyPriceMax = obj.optInt("buy_price_max", obj.optInt("BuyPriceMax", 0))

        if (itemId.isBlank() || city.isBlank()) return null

        return MarketPrice(
            itemId = itemId,
            city = city,
            quality = quality,
            sellPriceMin = sellPriceMin,
            sellPriceMinDate = "Echtzeit (PC-Sniffer)",
            buyPriceMax = buyPriceMax,
            buyPriceMaxDate = "Echtzeit (PC-Sniffer)"
        )
    }

    fun stopBridgeServer() {
        isRunning = false
        listenerJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
