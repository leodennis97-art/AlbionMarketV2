package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object CallMeBotApi {

    suspend fun sendWhatsAppMessage(
        phoneNumber: String,
        apiKey: String,
        message: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank() || apiKey.isBlank() || message.isBlank()) return@withContext false

        try {
            val encodedPhone = URLEncoder.encode(phoneNumber.trim(), "UTF-8")
            val encodedApiKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
            val encodedMessage = URLEncoder.encode(message, "UTF-8")

            val urlString = "https://api.callmebot.com/whatsapp.php?phone=$encodedPhone&text=$encodedMessage&apikey=$encodedApiKey"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
