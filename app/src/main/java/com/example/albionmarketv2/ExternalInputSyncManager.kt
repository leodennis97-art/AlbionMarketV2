package com.example.albionmarketv2

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

object ExternalInputSyncManager {

    private fun getExternalFolder(context: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val folder = File(docsDir, "AlbionDataPro")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun saveLastInputs(context: Context, silverBudget: Long, carryCapacity: Double, margin: Double, search: String) {
        try {
            val folder = getExternalFolder(context)
            val file = File(folder, "user_inputs_state.json")
            val json = JSONObject().apply {
                put("silverBudget", silverBudget)
                put("carryCapacityKg", carryCapacity)
                put("targetMarginPercent", margin)
                put("searchQuery", search)
                put("lastSavedAt", System.currentTimeMillis())
            }
            file.writeText(json.toString(4), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLastInputs(context: Context): JSONObject? {
        try {
            val folder = getExternalFolder(context)
            val file = File(folder, "user_inputs_state.json")
            if (file.exists() && file.canRead()) {
                return JSONObject(file.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
