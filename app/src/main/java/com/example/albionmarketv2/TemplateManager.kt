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

data class BotStep(
    val templateId: String,
    val delayAfterMs: Long = 2000L
)

data class BotWorkflow(
    val id: String,
    val name: String,
    val steps: List<BotStep>
)

object TemplateManager {
    private const val PREFS_NAME = "bot_templates_prefs"
    private const val KEY_TEMPLATES = "templates_json"
    private const val KEY_WORKFLOWS = "workflows_json"

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

    // Workflows
    fun getWorkflows(context: Context): List<BotWorkflow> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_WORKFLOWS, "[]") ?: "[]"
        val list = mutableListOf<BotWorkflow>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val stepsArray = obj.getJSONArray("steps")
                val steps = mutableListOf<BotStep>()
                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    steps.add(BotStep(stepObj.getString("templateId"), stepObj.getLong("delayAfterMs")))
                }
                list.add(BotWorkflow(obj.getString("id"), obj.getString("name"), steps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveWorkflows(context: Context, workflows: List<BotWorkflow>) {
        val array = JSONArray()
        workflows.forEach { wf ->
            val obj = JSONObject()
            obj.put("id", wf.id)
            obj.put("name", wf.name)
            val stepsArray = JSONArray()
            wf.steps.forEach { step ->
                val stepObj = JSONObject()
                stepObj.put("templateId", step.templateId)
                stepObj.put("delayAfterMs", step.delayAfterMs)
                stepsArray.put(stepObj)
            }
            obj.put("steps", stepsArray)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WORKFLOWS, array.toString())
            .apply()
    }
}