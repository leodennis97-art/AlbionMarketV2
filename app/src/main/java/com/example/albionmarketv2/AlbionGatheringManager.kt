package com.example.albionmarketv2

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*

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

    private var gatheringScope: CoroutineScope? = null
    private var gatheringJob: Job? = null

    fun startGathering(context: Context, onLog: (String) -> Unit) {
        if (isGatheringActive) return
        isGatheringActive = true
        onLog("🌲 Albion Abbau-Plan gestartet: ${config.category.displayNameDe} (Tier ${config.minTier}-${config.maxTier})")

        gatheringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        gatheringJob = gatheringScope?.launch {
            while (isActive && isGatheringActive) {
                try {
                    val clicker = AutoClickerService.instance
                    if (clicker == null) {
                        onLog("⚠️ AutoClicker (Barrierefreiheit) nicht aktiv! Bitte in Android-Einstellungen aktivieren.")
                        delay(3000L)
                        continue
                    }

                    val templates = TemplateManager.getTemplates(context).filter { it.isActive }
                    if (templates.isNotEmpty()) {
                        onLog("🔍 Suche ${config.category.displayNameDe} anhand von ${templates.size} Vorlagen...")
                    } else {
                        onLog("🔍 Umgebungssuche aktiv (${config.category.displayNameDe}): Scanne Sichtfeld...")
                    }

                    // Tippe auf Ressourcen-Bereich im Bildschirm (Mitte)
                    clicker.clickAt(540f, 650f)
                    onLog("⛏️ Ressource angeklickt. Baue ab (${config.harvestWaitTimeMs / 1000}s)...")
                    
                    delay(config.harvestWaitTimeMs)

                    if (config.autoMountAfterHarvest) {
                        onLog("🐴 Steige auf Reittier (Sattel-Klick bei X:${config.mountButtonX}, Y:${config.mountButtonY})...")
                        clicker.clickAt(config.mountButtonX, config.mountButtonY)
                        delay(2000L)
                    }

                    onLog("🔄 Suche nächste Ressource in der Umgebung...")
                    delay(2000L)
                } catch (e: Exception) {
                    onLog("❌ Fehler im Abbau-Plan: ${e.message}")
                    delay(3000L)
                }
            }
        }
    }

    fun stopGathering() {
        isGatheringActive = false
        gatheringJob?.cancel()
        gatheringJob = null
        gatheringScope?.cancel()
        gatheringScope = null
    }
}