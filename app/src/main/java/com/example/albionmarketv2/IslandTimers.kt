package com.example.albionmarketv2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimerCategory(val displayName: String, val icon: String) {
    CROP("Insel-Ernte (Gemüse & Kräuter)", "🌾"),
    ANIMAL("Tierzucht (Reittiere & Vieh)", "🐄")
}

data class IslandTimerItem(
    val id: String,
    val nameDe: String,
    val category: TimerCategory,
    val durationHours: Int,
    val startTimeMs: Long,
    val expectedHarvestTimeMs: Long
) {
    fun remainingTimeMs(): Long {
        return (expectedHarvestTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isReady(): Boolean {
        return System.currentTimeMillis() >= expectedHarvestTimeMs
    }

    fun remainingFormatted(): String {
        val remMs = remainingTimeMs()
        if (remMs <= 0) return "✅ HEUTE ERNTEBEREIT!"
        val hours = remMs / (1000 * 3600)
        val mins = (remMs % (1000 * 3600)) / (1000 * 60)
        val secs = (remMs % (1000 * 60)) / 1000
        return String.format(Locale.GERMANY, "⏱️ %02dh %02dm %02ds", hours, mins, secs)
    }

    fun expectedHarvestDateFormatted(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        return sdf.format(Date(expectedHarvestTimeMs))
    }
}

object IslandTimerManager {

    private const val PREFS_KEY = "island_harvest_timers_json"

    val standardCropOptions = listOf(
        Pair("🌾 Karotten-Feld (Carrots)", 22),
        Pair("🌿 Kräuter-Garten (Herbs)", 22),
        Pair("🌽 Mais-Feld (Corn)", 22),
        Pair("🌾 Weizen-Feld (Wheat)", 22),
        Pair("🎃 Kürbis-Feld (Pumpkins)", 22)
    )

    val standardAnimalOptions = listOf(
        Pair("🐴 Pferde-Fohlen (Foal T5)", 44),
        Pair("🐂 Ochsen-Kalb (Calf T5)", 44),
        Pair("🐖 Schweine-Ferkel (Piglet T5)", 22),
        Pair("🐺 Wildwolf-Welpe (Direwolf T6)", 88),
        Pair("🐻 Bären-Junges (Grizzly T8)", 132)
    )

    fun getTimers(context: Context): List<IslandTimerItem> {
        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
        val list = mutableListOf<IslandTimerItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                val nameDe = obj.optString("nameDe", "")
                if (id.isBlank() || nameDe.isBlank()) continue
                val catStr = obj.optString("category", TimerCategory.CROP.name)
                val categoryEnum = try { TimerCategory.valueOf(catStr) } catch (_: Exception) { TimerCategory.CROP }
                list.add(
                    IslandTimerItem(
                        id = id,
                        nameDe = nameDe,
                        category = categoryEnum,
                        durationHours = obj.optInt("durationHours", 22),
                        startTimeMs = obj.optLong("startTimeMs", System.currentTimeMillis()),
                        expectedHarvestTimeMs = obj.optLong("expectedHarvestTimeMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveTimers(context: Context, timers: List<IslandTimerItem>) {
        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (t in timers) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("nameDe", t.nameDe)
                put("category", t.category.name)
                put("durationHours", t.durationHours)
                put("startTimeMs", t.startTimeMs)
                put("expectedHarvestTimeMs", t.expectedHarvestTimeMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREFS_KEY, arr.toString()).apply()
    }

    fun addTimer(context: Context, nameDe: String, category: TimerCategory, durationHours: Int): IslandTimerItem {
        val now = System.currentTimeMillis()
        val harvestMs = now + (durationHours * 3600 * 1000L)
        val item = IslandTimerItem(
            id = "timer_${Date().time}",
            nameDe = nameDe,
            category = category,
            durationHours = durationHours,
            startTimeMs = now,
            expectedHarvestTimeMs = harvestMs
        )
        val current = getTimers(context).toMutableList()
        current.add(item)
        saveTimers(context, current)
        return item
    }

    fun removeTimer(context: Context, timerId: String) {
        val current = getTimers(context).filter { it.id != timerId }
        saveTimers(context, current)
    }
}
