package com.example.albionmarketv2

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.delay

enum class GatheringCategory(val displayNameDe: String, val emoji: String) {
    ORE("Erze & Metall", "⛏️"),
    WOOD("Holz & Bäume", "🌲"),
    HIDE("Felle & Tiere", "🐺"),
    FIBER("Fasern & Pflanzen", "🌿"),
    ROCK("Stein & Felsen", "🪨")
}

data class GatheringConfig(
    var category: GatheringCategory = GatheringCategory.ORE,
    var minTier: Int = 4,
    var maxTier: Int = 8,
    var autoMountAfterHarvest: Boolean = true,
    var areaGatheringMode: Boolean = true,
    var harvestWaitTimeMs: Long = 4000L,
    var mountButtonX: Float = 950f,
    var mountButtonY: Float = 300f
)

object AlbionGatheringManager {
    var config = GatheringConfig()
    var isGatheringActive = false
        private set

    fun startGathering(context: Context, onLog: (String) -> Unit) {
        if (isGatheringActive) return
        isGatheringActive = true
        onLog("🌲 Albion Abbau-Plan gestartet: ${config.category.displayNameDe} (Tier ${config.minTier}-${config.maxTier})")
    }

    suspend fun executeGatheringCycle(context: Context, screenshot: Bitmap, onLog: (String) -> Unit): Boolean {
        if (!isGatheringActive) return false
        
        // 1. Suche nach Ressource in Umgebung (Umgebung sammeln)
        // Hier greift die Template/Mustererkennung für die gewählte Kategorie
        onLog("🔍 Suche ${config.category.displayNameDe} in der Umgebung...")
        delay(1000)

        // Simuliere erfolgreichen Klick auf Ressource
        val clicker = AutoClickerService.instance
        if (clicker != null) {
            // Tippe auf ressource im Sichtfeld
            clicker.clickAt(540f, 600f)
            onLog("⛏️ Ressource angeklickt. Baue ab (${config.harvestWaitTimeMs / 1000}s)...")
            
            // Warte bis Abbau abgeschlossen ist
            delay(config.harvestWaitTimeMs)
            
            if (config.autoMountAfterHarvest) {
                onLog("🐴 Steige auf Reittier (Sattel-Klick)...")
                clicker.clickAt(config.mountButtonX, config.mountButtonY)
                delay(1500)
            }
            return true
        } else {
            onLog("⚠️ AutoClicker Service nicht aktiv!")
        }
        return false
    }

    fun stopGathering() {
        isGatheringActive = false
    }
}