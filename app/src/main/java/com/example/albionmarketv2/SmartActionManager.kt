package com.example.albionmarketv2

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SmartAction(
    val id: String,
    val name: String, // z.B. "Bäume fällen", "Erz abbauen", "Looten"
    val templateId: String,
    val category: String, // "Holz", "Erz", "Felle", etc.
    var isActive: Boolean = true
)

object SmartActionManager {
    private const val PREFS = "smart_actions_prefs"
    private const val KEY = "smart_actions_json"

    fun getActions(context: Context): List<SmartAction> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val list = mutableListOf<SmartAction>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SmartAction(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        templateId = obj.getString("templateId"),
                        category = obj.getString("category"),
                        isActive = obj.getBoolean("isActive")
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun saveActions(context: Context, actions: List<SmartAction>) {
        val array = JSONArray()
        actions.forEach { act ->
            val obj = JSONObject()
            obj.put("id", act.id)
            obj.put("name", act.name)
            obj.put("templateId", act.templateId)
            obj.put("category", act.category)
            obj.put("isActive", act.isActive)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun addAction(context: Context, name: String, templateId: String, category: String): SmartAction {
        val action = SmartAction(id = UUID.randomUUID().toString(), name = name, templateId = templateId, category = category, isActive = true)
        val list = getActions(context).toMutableList()
        list.add(action)
        saveActions(context, list)
        return action
    }

    fun deleteAction(context: Context, id: String) {
        val list = getActions(context).filter { it.id != id }
        saveActions(context, list)
    }
}