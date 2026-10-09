package com.example.albionmarketv2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class BotTemplate(
    val id: String,
    val name: String,
    val category: String,
    var isActive: Boolean
)

object TemplateManager {
    private const val PREFS_NAME = "bot_templates_prefs"
    private const val KEY_TEMPLATES = "templates_json"

    fun getTemplates(context: Context): List<BotTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_TEMPLATES, "[]") ?: "[]"
        val list = mutableListOf<BotTemplate>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BotTemplate(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        category = obj.getString("category"),
                        isActive = obj.getBoolean("isActive")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveTemplates(context: Context, templates: List<BotTemplate>) {
        val array = JSONArray()
        templates.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("category", it.category)
            obj.put("isActive", it.isActive)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEMPLATES, array.toString())
            .apply()
    }

    fun saveNewTemplate(context: Context, bitmap: Bitmap, name: String, category: String) {
        val id = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "bot_templates")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "$id.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val list = getTemplates(context).toMutableList()
        list.add(BotTemplate(id, name, category, true))
        saveTemplates(context, list)
    }

    fun loadTemplateBitmap(context: Context, id: String): Bitmap? {
        val file = File(File(context.filesDir, "bot_templates"), "$id.png")
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        return null
    }

    fun deleteTemplate(context: Context, id: String) {
        val file = File(File(context.filesDir, "bot_templates"), "$id.png")
        if (file.exists()) file.delete()
        val list = getTemplates(context).filter { it.id != id }
        saveTemplates(context, list)
    }
}