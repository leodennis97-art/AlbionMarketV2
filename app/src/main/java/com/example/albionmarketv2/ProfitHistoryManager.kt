package com.example.albionmarketv2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
                val obj = arr.getJSONObject(i)
                list.add(
                    ProfitEntry(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        netProfitSilver = obj.getLong("netProfitSilver"),
                        dateStr = obj.getString("dateStr"),
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
}
