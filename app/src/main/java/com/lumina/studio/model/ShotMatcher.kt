package com.lumina.studio.model

import kotlin.math.sqrt

/**
 * Hasil kalkulasi AI Shot Matcher untuk menyamakan warna antar dua klip kamera berbeda
 */
data class ShotMatchResult(
    val liftOffsetR: Float,
    val liftOffsetG: Float,
    val liftOffsetB: Float,
    val gammaOffsetR: Float,
    val gammaOffsetG: Float,
    val gammaOffsetB: Float,
    val gainOffsetR: Float,
    val gainOffsetG: Float,
    val gainOffsetB: Float,
    val saturationMultiplier: Float,
    val contrastMultiplier: Float
)

/**
 * Statistik warna histogram suatu frame video
 */
data class FrameColorStats(
    val meanR: Float, val stdR: Float,
    val meanG: Float, val stdG: Float,
    val meanB: Float, val stdB: Float,
    val meanLuma: Float, val stdLuma: Float
)

/**
 * DaVinci Style AI Shot Matcher & Auto Color Balance Engine
 */
object ShotMatcher {

    /**
     * Hitung statistik warna dari array piksel RGB
     */
    fun analyzeFrame(rgbPixels: IntArray, width: Int, height: Int): FrameColorStats {
        var sumR = 0.0; var sumG = 0.0; var sumB = 0.0; var sumLuma = 0.0
        val count = rgbPixels.size.toDouble()

        for (pixel in rgbPixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0
            val g = (pixel shr 8 and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

            sumR += r
            sumG += g
            sumB += b
            sumLuma += luma
        }

        val meanR = (sumR / count).toFloat()
        val meanG = (sumG / count).toFloat()
        val meanB = (sumB / count).toFloat()
        val meanLuma = (sumLuma / count).toFloat()

        var varR = 0.0; var varG = 0.0; var varB = 0.0; var varLuma = 0.0
        for (pixel in rgbPixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0
            val g = (pixel shr 8 and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

            varR += (r - meanR) * (r - meanR)
            varG += (g - meanG) * (g - meanG)
            varB += (b - meanB) * (b - meanB)
            varLuma += (luma - meanLuma) * (luma - meanLuma)
        }

        val stdR = sqrt(varR / count).toFloat()
        val stdG = sqrt(varG / count).toFloat()
        val stdB = sqrt(varB / count).toFloat()
        val stdLuma = sqrt(varLuma / count).toFloat()

        return FrameColorStats(meanR, stdR, meanG, stdG, meanB, stdB, meanLuma, stdLuma)
    }

    /**
     * Samakan warna target clip agar identik dengan reference clip (Shot Matching)
     */
    fun matchShots(reference: FrameColorStats, target: FrameColorStats): ShotMatchResult {
        // Shadow offset (Lift)
        val liftR = (reference.meanR - target.meanR) * 0.4f
        val liftG = (reference.meanG - target.meanG) * 0.4f
        val liftB = (reference.meanB - target.meanB) * 0.4f

        // Highlight ratio (Gain)
        val gainR = if (target.stdR > 0.01f) (reference.stdR / target.stdR - 1.0f) * 0.5f else 0f
        val gainG = if (target.stdG > 0.01f) (reference.stdG / target.stdG - 1.0f) * 0.5f else 0f
        val gainB = if (target.stdB > 0.01f) (reference.stdB / target.stdB - 1.0f) * 0.5f else 0f

        // Midtone exposure (Gamma)
        val gammaR = (reference.meanR - target.meanR) * 0.3f
        val gammaG = (reference.meanG - target.meanG) * 0.3f
        val gammaB = (reference.meanB - target.meanB) * 0.3f

        // Saturation multiplier
        val satMul = if (target.stdLuma > 0.01f) (reference.stdLuma / target.stdLuma).coerceIn(0.5f, 2.0f) else 1.0f
        val conMul = if (target.stdLuma > 0.01f) (reference.stdLuma / target.stdLuma).coerceIn(0.8f, 1.5f) else 1.0f

        return ShotMatchResult(
            liftOffsetR = liftR,
            liftOffsetG = liftG,
            liftOffsetB = liftB,
            gammaOffsetR = gammaR,
            gammaOffsetG = gammaG,
            gammaOffsetB = gammaB,
            gainOffsetR = gainR,
            gainOffsetG = gainG,
            gainOffsetB = gainB,
            saturationMultiplier = satMul,
            contrastMultiplier = conMul
        )
    }
}
