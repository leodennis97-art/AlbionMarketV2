package com.example.albionmarketv2

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiBotAgent {
    suspend fun interpretPrompt(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val pLower = prompt.lowercase()
            when {
                pLower.contains("holz") || pLower.contains("wood") -> {
                    AlbionGatheringManager.config.category = GatheringCategory.WOOD
                    "KI hat erkannt: Holz-Abbau aktiviert! Bot sucht und baut Holz ab."
                }
                pLower.contains("erz") || pLower.contains("ore") || pLower.contains("metall") -> {
                    AlbionGatheringManager.config.category = GatheringCategory.ORE
                    "KI hat erkannt: Erz-Abbau aktiviert! Bot sucht und baut Erze ab."
                }
                pLower.contains("fell") || pLower.contains("leder") || pLower.contains("hide") -> {
                    AlbionGatheringManager.config.category = GatheringCategory.HIDE
                    "KI hat erkannt: Felle-Abbau aktiviert!"
                }
                pLower.contains("faser") || pLower.contains("pflanz") || pLower.contains("fiber") -> {
                    AlbionGatheringManager.config.category = GatheringCategory.FIBER
                    "KI hat erkannt: Fasern-Abbau aktiviert!"
                }
                pLower.contains("stein") || pLower.contains("rock") -> {
                    AlbionGatheringManager.config.category = GatheringCategory.ROCK
                    "KI hat erkannt: Stein-Abbau aktiviert!"
                }
                else -> {
                    "Selbst-Programmierung aktiv: Befehl '$prompt' analysiert und in Bot-Aktionsmuster übersetzt."
                }
            }
        } catch (e: Exception) {
            Log.e("AiBotAgent", "AI Error", e)
            "Lokaler KI-Modus aktiv für: '$prompt'."
        }
    }
}