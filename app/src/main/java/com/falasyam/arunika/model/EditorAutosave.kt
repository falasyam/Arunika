package com.falasyam.arunika.model

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

object EditorAutosave {
    private const val PREFERENCES_NAME = "arunika_autosave"
    private const val PROJECTS_KEY = "projects"
    private const val TAG = "EditorAutosave"

    fun loadProjects(context: Context): List<ProjectItem>? {
        val savedProjects = read(context, PROJECTS_KEY) as? List<*> ?: return null
        if (savedProjects.any { it !is ProjectItem }) {
            Log.e(TAG, "Autosaved project list contains an unexpected value.")
            return null
        }
        return savedProjects.filterIsInstance<ProjectItem>()
    }

    fun saveProjects(context: Context, projects: List<ProjectItem>) {
        write(context, PROJECTS_KEY, ArrayList(projects))
    }

    fun loadEditorState(context: Context, projectId: String): Serializable? =
        read(context, "editor_$projectId") as? Serializable

    fun saveEditorState(context: Context, projectId: String, state: Serializable) {
        write(context, "editor_$projectId", state)
    }

    private fun read(context: Context, key: String): Any? {
        val encoded = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(key, null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        } catch (exception: Exception) {
            Log.e(TAG, "Could not restore autosaved data for $key", exception)
            null
        }
    }

    private fun write(context: Context, key: String, value: Serializable) {
        try {
            val output = ByteArrayOutputStream()
            ObjectOutputStream(output).use { it.writeObject(value) }
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP))
                .apply()
        } catch (exception: Exception) {
            Log.e(TAG, "Could not autosave data for $key", exception)
            throw exception
        }
    }
}
