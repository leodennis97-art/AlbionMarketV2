package com.example.albionmarketv2

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MacroAction(val x: Float, val y: Float, val delayMs: Long)
data class RecordedMacro(val id: String, val name: String, val actions: List<MacroAction>)

object MacroRecorder {
    private const val PREFS = "macro_prefs"
    private const val KEY = "macros_json"

    var isRecording = false
        private set

    private val currentActions = mutableListOf<MacroAction>()
    private var lastTime = 0L

    fun startRecording() {
        currentActions.clear()
        isRecording = true
        lastTime = SystemClock.uptimeMillis()
    }

    fun recordAction(x: Float, y: Float) {
        if (!isRecording) return
        val now = SystemClock.uptimeMillis()
        val delay = if (currentActions.isEmpty()) 300L else (now - lastTime).coerceAtMost(4000L)
        currentActions.add(MacroAction(x, y, delay))
        lastTime = now
    }

    fun saveMacro(context: Context, name: String): RecordedMacro? {
        isRecording = false
        if (currentActions.isEmpty()) return null
        val macro = RecordedMacro(id = UUID.randomUUID().toString(), name = name, actions = currentActions.toList())
        
        val macros = getMacros(context).toMutableList()
        macros.add(macro)
        
        val array = JSONArray()
        macros.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            val actArray = JSONArray()
            m.actions.forEach { act ->
                val actObj = JSONObject()
                actObj.put("x", act.x.toDouble())
                actObj.put("y", act.y.toDouble())
                actObj.put("delayMs", act.delayMs)
                actArray.put(actObj)
            }
            obj.put("actions", actArray)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
        return macro
    }

    fun getMacros(context: Context): List<RecordedMacro> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val list = mutableListOf<RecordedMacro>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val actsArray = obj.getJSONArray("actions")
                val actions = mutableListOf<MacroAction>()
                for (j in 0 until actsArray.length()) {
                    val actObj = actsArray.getJSONObject(j)
                    actions.add(MacroAction(actObj.getDouble("x").toFloat(), actObj.getDouble("y").toFloat(), actObj.getLong("delayMs")))
                }
                list.add(RecordedMacro(obj.getString("id"), obj.getString("name"), actions))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun deleteMacro(context: Context, id: String) {
        val macros = getMacros(context).filter { it.id != id }
        val array = JSONArray()
        macros.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            val actArray = JSONArray()
            m.actions.forEach { act ->
                val actObj = JSONObject()
                actObj.put("x", act.x.toDouble())
                actObj.put("y", act.y.toDouble())
                actObj.put("delayMs", act.delayMs)
                actArray.put(actObj)
            }
            obj.put("actions", actArray)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    suspend fun replayMacro(macro: RecordedMacro) {
        if (macro.actions.isEmpty()) return
        for (action in macro.actions) {
            delay(action.delayMs)
            AutoClickerService.instance?.clickAt(action.x, action.y)
        }
    }
}