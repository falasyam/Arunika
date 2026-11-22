package com.falasyam.arunika.model

import java.io.InputStream
import kotlin.math.floor
import java.io.Serializable

/**
 * Format LUT yang didukung oleh Arunika
 */
enum class LutFormat(val extension: String, val displayName: String) {
    CUBE_3D(".cube", "Adobe / DaVinci 3D LUT (.cube)"),
    CUBE_1D(".cube", "1D LUT Curve (.cube)"),
    AUTODESK_3DL(".3dl", "Autodesk / Lustre (.3dl)"),
    PANASONIC_VLT(".vlt", "Panasonic Varicam (.vlt)"),
    HALD_CLUT(".png", "Hald CLUT Image (.png)")
}

/**
 * Representasi 3D LUT hasil parsing file dengan dukungan interpolasi trilinear
 */
data class ParsedLut(
    val id: String,
    val name: String,
    val format: LutFormat,
    val size: Int, // e.g. 17 or 33
    val is3D: Boolean = true,
    val data: FloatArray, // Array R, G, B datar: [size * size * size * 3]
    val title: String = name
) : Serializable {
    /**
     * Melakukan Trilinear Interpolation untuk memetakan warna input (r, g, b in [0, 1])
     * ke ruang warna hasil grading LUT.
     */
    fun sample(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        if (!is3D || data.isEmpty()) return Triple(r, g, b)

        val maxIdx = (size - 1).toFloat()
        val rx = (r.coerceIn(0f, 1f) * maxIdx)
        val gy = (g.coerceIn(0f, 1f) * maxIdx)
        val bz = (b.coerceIn(0f, 1f) * maxIdx)

        val r0 = floor(rx).toInt().coerceIn(0, size - 1)
        val r1 = (r0 + 1).coerceAtMost(size - 1)
        val g0 = floor(gy).toInt().coerceIn(0, size - 1)
        val g1 = (g0 + 1).coerceAtMost(size - 1)
        val b0 = floor(bz).toInt().coerceIn(0, size - 1)
        val b1 = (b0 + 1).coerceAtMost(size - 1)

        val dr = rx - r0
        val dg = gy - g0
        val db = bz - b0

        fun getLutRgb(ir: Int, ig: Int, ib: Int): FloatArray {
            val idx = (ir + ig * size + ib * size * size) * 3
            return floatArrayOf(
                data.getOrElse(idx) { 0f },
                data.getOrElse(idx + 1) { 0f },
                data.getOrElse(idx + 2) { 0f }
            )
        }

        val c000 = getLutRgb(r0, g0, b0)
        val c100 = getLutRgb(r1, g0, b0)
        val c010 = getLutRgb(r0, g1, b0)
        val c110 = getLutRgb(r1, g1, b0)
        val c001 = getLutRgb(r0, g0, b1)
        val c101 = getLutRgb(r1, g0, b1)
        val c011 = getLutRgb(r0, g1, b1)
        val c111 = getLutRgb(r1, g1, b1)

        val outR = (1 - dr) * (1 - dg) * (1 - db) * c000[0] +
                   dr * (1 - dg) * (1 - db) * c100[0] +
                   (1 - dr) * dg * (1 - db) * c010[0] +
                   dr * dg * (1 - db) * c110[0] +
                   (1 - dr) * (1 - dg) * db * c001[0] +
                   dr * (1 - dg) * db * c101[0] +
                   (1 - dr) * dg * db * c011[0] +
                   dr * dg * db * c111[0]

        val outG = (1 - dr) * (1 - dg) * (1 - db) * c000[1] +
                   dr * (1 - dg) * (1 - db) * c100[1] +
                   (1 - dr) * dg * (1 - db) * c010[1] +
                   dr * dg * (1 - db) * c110[1] +
                   (1 - dr) * (1 - dg) * db * c001[1] +
                   dr * (1 - dg) * db * c101[1] +
                   (1 - dr) * dg * db * c011[1] +
                   dr * dg * db * c111[1]

        val outB = (1 - dr) * (1 - dg) * (1 - db) * c000[2] +
                   dr * (1 - dg) * (1 - db) * c100[2] +
                   (1 - dr) * dg * (1 - db) * c010[2] +
                   dr * dg * (1 - db) * c110[2] +
                   (1 - dr) * (1 - dg) * db * c001[2] +
                   dr * (1 - dg) * db * c101[2] +
                   (1 - dr) * dg * db * c011[2] +
                   dr * dg * db * c111[2]

        return Triple(outR.coerceIn(0f, 1f), outG.coerceIn(0f, 1f), outB.coerceIn(0f, 1f))
    }
}

/**
 * Universal Multi-Format LUT Parser (Mendukung .cube, .3dl, .vlt)
 */
object LutParser {

    /**
     * Mendeteksi format berdasarkan ekstensi atau isi header, lalu mem-parsing LUT
     */
    fun parse(content: String, fileName: String): ParsedLut {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".3dl") -> parse3dl(content, fileName)
            lowerName.endsWith(".vlt") -> parseVlt(content, fileName)
            else -> parseCube(content, fileName)
        }
    }

    /**
     * Parser untuk format standar Adobe / DaVinci Resolve `.cube`
     */
    fun parseCube(content: String, fileName: String): ParsedLut {
        var size = 33
        var is3D = true
        var title = fileName.substringBeforeLast(".")
        val rgbList = ArrayList<Float>(33 * 33 * 33 * 3)

        val lines = content.lineSequence()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val upper = line.uppercase()
            when {
                upper.startsWith("TITLE") -> {
                    title = line.substringAfter("TITLE").trim().replace("\"", "")
                }
                upper.startsWith("LUT_3D_SIZE") -> {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        size = parts[1].toIntOrNull() ?: 33
                        is3D = true
                    }
                }
                upper.startsWith("LUT_1D_SIZE") -> {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        size = parts[1].toIntOrNull() ?: 1024
                        is3D = false
                    }
                }
                upper.startsWith("DOMAIN_MIN") || upper.startsWith("DOMAIN_MAX") -> {
                    // Min/Max range (biasanya 0.0 - 1.0)
                }
                else -> {
                    // Baris data warna RGB: "0.0123 0.4567 0.8901"
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 3) {
                        val r = tokens[0].toFloatOrNull()
                        val g = tokens[1].toFloatOrNull()
                        val b = tokens[2].toFloatOrNull()
                        if (r != null && g != null && b != null) {
                            rgbList.add(r)
                            rgbList.add(g)
                            rgbList.add(b)
                        }
                    }
                }
            }
        }

        return ParsedLut(
            id = "custom_lut_${System.currentTimeMillis()}",
            name = title.ifEmpty { fileName },
            format = if (is3D) LutFormat.CUBE_3D else LutFormat.CUBE_1D,
            size = size,
            is3D = is3D,
            data = rgbList.toFloatArray(),
            title = title
        )
    }

    /**
     * Parser untuk format Autodesk / Discreet `.3dl`
     */
    fun parse3dl(content: String, fileName: String): ParsedLut {
        var size = 17
        var title = fileName.substringBeforeLast(".")
        val rgbList = ArrayList<Float>()
        var foundHeader = false

        val lines = content.lineSequence()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            // Baris header pertama pada .3dl sering berupa daftar titik mesh (misal 0 64 128 ... 1023)
            val tokens = line.split("\\s+".toRegex())
            if (!foundHeader && tokens.size > 4) {
                size = tokens.size
                foundHeader = true
                continue
            }

            if (tokens.size >= 3) {
                // Nilai .3dl sering kali berskala integer 0-1023 atau float 0.0-1.0
                val rRaw = tokens[0].toFloatOrNull() ?: continue
                val gRaw = tokens[1].toFloatOrNull() ?: continue
                val bRaw = tokens[2].toFloatOrNull() ?: continue

                val scale = if (rRaw > 1.0f || gRaw > 1.0f || bRaw > 1.0f) 1023.0f else 1.0f
                rgbList.add(rRaw / scale)
                rgbList.add(gRaw / scale)
                rgbList.add(bRaw / scale)
            }
        }

        return ParsedLut(
            id = "custom_3dl_${System.currentTimeMillis()}",
            name = title,
            format = LutFormat.AUTODESK_3DL,
            size = size,
            is3D = true,
            data = rgbList.toFloatArray(),
            title = title
        )
    }

    /**
     * Parser untuk format Panasonic Varicam `.vlt`
     */
    fun parseVlt(content: String, fileName: String): ParsedLut {
        // Panasonic VLT secara internal menggunakan format ASCII 3D LUT (mirip .cube dengan header VLT)
        val parsed = parseCube(content, fileName)
        return parsed.copy(format = LutFormat.PANASONIC_VLT)
    }
}
