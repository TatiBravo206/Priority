package com.example.priori

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

object TaskManager {
    private const val PREFS_NAME = "priori_tasks_prefs"
    private const val KEY_TASKS = "key_tasks_list"

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer<LocalDate> { json, _, _ ->
            LocalDate.parse(json.asString)
        })
        .create()

    // Guardar la lista de tareas
    fun saveTasks(context: Context, tasks: List<Task>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(tasks)
        editor.putString(KEY_TASKS, json)
        editor.apply()
    }

    // Cargar la lista de tareas guardadas
    fun loadTasks(context: Context): MutableList<Task> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_TASKS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }
}