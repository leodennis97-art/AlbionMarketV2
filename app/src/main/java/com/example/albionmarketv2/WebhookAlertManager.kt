package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

object WebhookAlertManager {

    suspend fun sendDiscordMarketAlert(
        webhookUrl: String,
        opportunity: TradeOpportunity
    ): Boolean = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

            val embed = JSONObject().apply {
                put("title", "🚨 High-End Markt Arbitrage Fund: ${opportunity.resource.nameDe}")
                put("color", 0x00FF7F) // Green hex
                put("description", "Ein gewinnbringender Trade-Flip wurde automatisch auf dem Markt identifiziert!")

                val fields = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "🛒 Einkaufstadt")
                        put("value", "${opportunity.buyCity}\nPrice: ${fmt.format(opportunity.buyPrice)} S.")
                        put("inline", true)
                    })
                    put(JSONObject().apply {
                        put("name", "📈 Verkaufsstadt")
                        put("value", "${opportunity.sellCity}\nPrice: ${fmt.format(opportunity.sellPrice)} S.")
                        put("inline", true)
                    })
                    put(JSONObject().apply {
                        put("name", "💰 Netto-Gewinn")
                        put("value", "+${fmt.format(opportunity.totalNetProfit)} S. (${String.format(Locale.GERMANY, "%.1f", opportunity.roiPercent)}% ROI)")
                        put("inline", true)
                    })
                    put(JSONObject().apply {
                        put("name", "⚖️ Einheiten & Gewicht")
                        put("value", "${opportunity.tradeUnits}x Stk. | ${opportunity.totalWeightKg.toInt()} kg")
                        put("inline", true)
                    })
                    put(JSONObject().apply {
                        put("name", "💡 KI-Strategie")
                        put("value", opportunity.aiOrderStrategy.ifBlank { "Sofort-Kauf & Transport" })
                        put("inline", false)
                    })
                }

                put("fields", fields)
                put("footer", JSONObject().apply {
                    put("text", "Albion DataPro Companion | High-Frequency Scanner")
                })
            }

            val payload = JSONObject().apply {
                put("username", "Albion DataPro Bot")
                put("avatar_url", "https://albiononline.com/favicon.ico")
                put("embeds", JSONArray().apply { put(embed) })
            }

            val url = URL(webhookUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            return@withContext (code in 200..299)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun sendTelegramMarketAlert(
        botToken: String,
        chatId: String,
        opportunity: TradeOpportunity
    ): Boolean = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
            val msgText = "🚨 *Albion DataPro Arbitrage Alert*\n\n" +
                    "📦 *Item:* ${opportunity.resource.nameDe}\n" +
                    "🛒 *Kaufen in:* ${opportunity.buyCity} (${fmt.format(opportunity.buyPrice)} S.)\n" +
                    "📈 *Verkaufen in:* ${opportunity.sellCity} (${fmt.format(opportunity.sellPrice)} S.)\n" +
                    "💰 *Netto-Profit:* +${fmt.format(opportunity.totalNetProfit)} Silber (${String.format(Locale.GERMANY, "%.1f", opportunity.roiPercent)}% ROI)\n" +
                    "💡 *Strategie:* ${opportunity.aiOrderStrategy}"

            val urlString = "https://api.telegram.org/bot$botToken/sendMessage"
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", msgText)
                put("parse_mode", "Markdown")
            }

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            return@withContext (code in 200..299)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }
}
