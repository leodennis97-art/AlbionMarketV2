package com.example.albionmarketv2

import com.ngrok.Session
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NgrokManager {

    suspend fun connectNgrok() = withContext(Dispatchers.IO) {
        try {
            Session.withAuthtokenFromEnv().connect().use { session ->
                val forwarder = session.httpEndpoint()
                    .domain("speller-importer-captivate.ngrok-free.dev")
                    .forward(URL("https://albionmarketv2-1.onrender.com"))
                println("Connected to Render via Ngrok at: ${forwarder.url}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
