package com.falasyam.arunika.ui.components

import android.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SingleColorLut
import com.falasyam.arunika.model.ParsedLut
import kotlin.math.floor
import kotlin.math.roundToInt

@UnstableApi
internal object ArunikaLutEffect {
    fun create(parsedLut: ParsedLut, intensity: Float): SingleColorLut {
        require(parsedLut.size >= 2) { "The imported LUT must contain at least two entries per dimension." }
        require(intensity.isFinite()) { "LUT intensity must be finite." }

        val sourceSize = parsedLut.size
        val requiredValues = if (parsedLut.is3D) {
            sourceSize.toLong() * sourceSize * sourceSize * 3L
        } else {
            sourceSize.toLong() * 3L
        }
        require(parsedLut.data.size.toLong() >= requiredValues) {
            "The imported LUT data is incomplete. Re-import the LUT before exporting."
        }

        val lutSize = if (parsedLut.is3D) minOf(sourceSize, MAX_LUT_SIZE) else MAX_LUT_SIZE
        val mix = intensity.coerceIn(0f, 1f)
        val cube = Array(lutSize) { Array(lutSize) { IntArray(lutSize) } }

        for (r in 0 until lutSize) {
            val inputR = r.toFloat() / (lutSize - 1)
            for (g in 0 until lutSize) {
                val inputG = g.toFloat() / (lutSize - 1)
                for (b in 0 until lutSize) {
                    val inputB = b.toFloat() / (lutSize - 1)
                    val output = if (parsedLut.is3D) {
                        sample3d(parsedLut, inputR, inputG, inputB)
                    } else {
                        sample1d(parsedLut, inputR, inputG, inputB)
                    }
                    cube[r][g][b] = Color.rgb(
                        toByte(inputR + (output[0] - inputR) * mix),
                        toByte(inputG + (output[1] - inputG) * mix),
                        toByte(inputB + (output[2] - inputB) * mix)
                    )
                }
            }
        }

        return SingleColorLut.createFromCube(cube)
    }

    private fun sample3d(lut: ParsedLut, r: Float, g: Float, b: Float): FloatArray {
        val size = lut.size
        val rPosition = r * (size - 1)
        val gPosition = g * (size - 1)
        val bPosition = b * (size - 1)
        val r0 = floor(rPosition).toInt()
        val g0 = floor(gPosition).toInt()
        val b0 = floor(bPosition).toInt()
        val r1 = (r0 + 1).coerceAtMost(size - 1)
        val g1 = (g0 + 1).coerceAtMost(size - 1)
        val b1 = (b0 + 1).coerceAtMost(size - 1)
        val dr = rPosition - r0
        val dg = gPosition - g0
        val db = bPosition - b0

        return FloatArray(3) { channel ->
            val c00 = lerp(valueAt(lut, r0, g0, b0, channel), valueAt(lut, r1, g0, b0, channel), dr)
            val c10 = lerp(valueAt(lut, r0, g1, b0, channel), valueAt(lut, r1, g1, b0, channel), dr)
            val c01 = lerp(valueAt(lut, r0, g0, b1, channel), valueAt(lut, r1, g0, b1, channel), dr)
            val c11 = lerp(valueAt(lut, r0, g1, b1, channel), valueAt(lut, r1, g1, b1, channel), dr)
            val c0 = lerp(c00, c10, dg)
            val c1 = lerp(c01, c11, dg)
            lerp(c0, c1, db).coerceIn(0f, 1f)
        }
    }

    private fun sample1d(lut: ParsedLut, r: Float, g: Float, b: Float): FloatArray =
        floatArrayOf(sample1dChannel(lut, r, 0), sample1dChannel(lut, g, 1), sample1dChannel(lut, b, 2))

    private fun sample1dChannel(lut: ParsedLut, value: Float, channel: Int): Float {
        val position = value * (lut.size - 1)
        val low = floor(position).toInt()
        val high = (low + 1).coerceAtMost(lut.size - 1)
        val fraction = position - low
        val lowValue = lut.data[low * 3 + channel]
        val highValue = lut.data[high * 3 + channel]
        return lerp(lowValue, highValue, fraction).coerceIn(0f, 1f)
    }

    private fun valueAt(lut: ParsedLut, r: Int, g: Int, b: Int, channel: Int): Float {
        // .cube data stores red as the fastest-changing axis; Media3 receives cube[r][g][b].
        val index = (r + g * lut.size + b * lut.size * lut.size) * 3 + channel
        return lut.data[index]
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    private fun toByte(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()

    private const val MAX_LUT_SIZE = 33
}
