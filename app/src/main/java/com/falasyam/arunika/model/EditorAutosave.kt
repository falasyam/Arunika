package com.falasyam.arunika.model

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

object EditorAutosave {
    private const val PREFERENCES_NAME = "arunika_autosave"
    private const val PROJECTS_KEY = "projects"
    private const val TAG = "EditorAutosave"
    private const val FILE_PREFIX = "arunika_autosave_"

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
        val file = autosaveFile(context, key)
        val saved = try {
            ObjectInputStream(file.openRead()).use { it.readObject() }
        } catch (_: FileNotFoundException) {
            null
        } catch (exception: Exception) {
            Log.e(TAG, "Could not restore autosaved data for $key", exception)
            return null
        }
        if (saved != null) return saved

        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val encoded = preferences.getString(key, null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
            if (restored is Serializable) {
                try {
                    writeFile(file, restored)
                    preferences.edit().remove(key).apply()
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not migrate autosaved data for $key", exception)
                }
            }
            restored
        } catch (exception: Exception) {
            Log.e(TAG, "Could not restore autosaved data for $key", exception)
            null
        }
    }

    private fun write(context: Context, key: String, value: Serializable) {
        try {
            writeFile(autosaveFile(context, key), value)
        } catch (exception: Exception) {
            Log.e(TAG, "Could not autosave data for $key", exception)
            throw exception
        }
    }

    private fun autosaveFile(context: Context, key: String): AtomicFile {
        val encodedKey = Base64.encodeToString(
            key.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return AtomicFile(File(context.filesDir, "$FILE_PREFIX$encodedKey.bin"))
    }

    private fun writeFile(file: AtomicFile, value: Serializable) {
        var output: FileOutputStream? = null
        try {
            output = file.startWrite()
            ObjectOutputStream(output).apply {
                writeObject(value)
                flush()
            }
            file.finishWrite(output)
        } catch (exception: Exception) {
            output?.let(file::failWrite)
            throw exception
        }
    }
}
