package com.example.albionmarketv2

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ProfitEntry(
    val id: String,
    val title: String,
    val netProfitSilver: Long,
    val dateStr: String,
    val timestampMs: Long = System.currentTimeMillis()
)

object ProfitHistoryManager {

    private const val PREFS_KEY = "profit_history_entries_json"

    fun getHistory(context: Context): List<ProfitEntry> {
        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
        val list = mutableListOf<ProfitEntry>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (id.isBlank()) continue
                list.add(
                    ProfitEntry(
                        id = id,
                        title = obj.optString("title", "Gewinn"),
                        netProfitSilver = obj.optLong("netProfitSilver", 0L),
                        dateStr = obj.optString("dateStr", ""),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addProfitEntry(context: Context, title: String, netProfitSilver: Long, dateStr: String) {
        val current = getHistory(context).toMutableList()
        val entry = ProfitEntry(
            id = "prof_${System.currentTimeMillis()}",
            title = title,
            netProfitSilver = netProfitSilver,
            dateStr = dateStr
        )
        current.add(entry)

        val prefs = context.getSharedPreferences("albion_market_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (e in current.takeLast(100)) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("netProfitSilver", e.netProfitSilver)
                put("dateStr", e.dateStr)
                put("timestampMs", e.timestampMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREFS_KEY, arr.toString()).apply()
    }

    fun getTotalProfitSilver(context: Context): Long {
        return getHistory(context).sumOf { it.netProfitSilver }
    }

    fun getAverageProfitSilver(context: Context): Long {
        val history = getHistory(context)
        if (history.isEmpty()) return 0L
        return history.sumOf { it.netProfitSilver } / history.size
    }

    fun getWeeklyProfitSilver(context: Context): Long {
        val oneWeekAgoMs = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return getHistory(context).filter { it.timestampMs >= oneWeekAgoMs }.sumOf { it.netProfitSilver }
    }

    fun getMonthlyProfitSilver(context: Context): Long {
        val thirtyDaysAgoMs = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return getHistory(context).filter { it.timestampMs >= thirtyDaysAgoMs }.sumOf { it.netProfitSilver }
    }

    fun getTopProfitableItems(context: Context): List<Pair<String, Long>> {
        val history = getHistory(context)
        return history.groupBy { it.title }
            .mapValues { (_, entries) -> entries.sumOf { it.netProfitSilver } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    fun exportHistoryToCsv(context: Context): File? {
        val history = getHistory(context)
        if (history.isEmpty()) return null
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val csvFile = File(downloadsDir, "DataPro_Trade_History.csv")
            csvFile.bufferedWriter().use { out ->
                out.write("ID,Datum,Item,Reingewinn_Silber,Zeitstempel_Ms\n")
                for (entry in history) {
                    out.write("\"${entry.id}\",\"${entry.dateStr}\",\"${entry.title}\",${entry.netProfitSilver},${entry.timestampMs}\n")
                }
            }
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
