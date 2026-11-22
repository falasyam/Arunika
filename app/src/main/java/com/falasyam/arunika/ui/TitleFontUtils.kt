package com.falasyam.arunika.ui

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import com.falasyam.arunika.model.TitleElement
import com.falasyam.arunika.model.TitleFont
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CUSTOM_FONT_SIZE_BYTES = 20L * 1024L * 1024L

internal data class ImportedTitleFont(val displayName: String)

internal fun titleFontDirectory(context: Context): File =
    File(context.filesDir, "title-fonts")

internal fun customTitleFontFile(fontDirectory: File, elementId: String): File {
    val safeId = elementId.replace(Regex("[^A-Za-z0-9_-]"), "_")
    return File(fontDirectory, "$safeId.font")
}

internal fun customTitleFontFile(context: Context, elementId: String): File =
    customTitleFontFile(titleFontDirectory(context), elementId)

internal fun resolveTitleFontFamily(context: Context, element: TitleElement): FontFamily =
    when (element.font) {
        TitleFont.SANS -> FontFamily.Default
        TitleFont.SERIF, TitleFont.CINEMATIC -> FontFamily.Serif
        TitleFont.MONO -> FontFamily.Monospace
        TitleFont.HANDWRITTEN -> FontFamily.Cursive
        TitleFont.CUSTOM -> {
            val fontFile = customTitleFontFile(context, element.id)
            if (!fontFile.isFile) {
                FontFamily.Default
            } else {
                try {
                    FontFamily(Typeface.createFromFile(fontFile))
                } catch (_: RuntimeException) {
                    FontFamily.Default
                }
            }
        }
    }

internal suspend fun importCustomTitleFont(
    context: Context,
    uri: Uri,
    elementId: String
): ImportedTitleFont = withContext(Dispatchers.IO) {
    val displayName = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: throw IOException("Nama file font tidak dapat dibaca.")

    val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (extension != "ttf" && extension != "otf") {
        throw IOException("Pilih file font berformat .ttf atau .otf.")
    }

    val directory = titleFontDirectory(context)
    if (!directory.isDirectory && !directory.mkdirs()) {
        throw IOException("Folder penyimpanan font tidak dapat dibuat.")
    }

    val destination = customTitleFontFile(directory, elementId)
    val temporaryFile = File(directory, "${destination.name}.tmp")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("File font tidak dapat dibuka.")
        input.use { source ->
            FileOutputStream(temporaryFile).use { output ->
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                while (true) {
                    val bytesRead = source.read(buffer)
                    if (bytesRead < 0) break
                    totalBytes += bytesRead
                    if (totalBytes > MAX_CUSTOM_FONT_SIZE_BYTES) {
                        throw IOException("Ukuran font melebihi batas 20 MB.")
                    }
                    output.write(buffer, 0, bytesRead)
                }
            }
        }

        if (temporaryFile.length() == 0L) {
            throw IOException("File font kosong.")
        }
        try {
            Typeface.createFromFile(temporaryFile)
        } catch (exception: RuntimeException) {
            throw IOException("File tidak berisi font TTF/OTF yang valid.", exception)
        }

        if (destination.exists() && !destination.delete()) {
            throw IOException("Font lama tidak dapat diganti.")
        }
        if (!temporaryFile.renameTo(destination)) {
            throw IOException("Font tidak dapat disimpan ke perangkat.")
        }
        ImportedTitleFont(displayName)
    } catch (exception: IOException) {
        temporaryFile.delete()
        throw exception
    } catch (exception: SecurityException) {
        temporaryFile.delete()
        throw IOException("Akses ke file font ditolak.", exception)
    }
}
