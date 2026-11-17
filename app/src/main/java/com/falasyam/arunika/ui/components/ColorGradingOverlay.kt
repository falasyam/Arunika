package com.falasyam.arunika.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import com.falasyam.arunika.model.CameraLogProfile
import com.falasyam.arunika.model.ColorStudioState

/**
 * Real-time Color Grading Overlay untuk Color Studio Arunika.
 * Menerapkan:
 * - 3-Way Color Wheels (Lift = Shadows, Gamma = Midtones, Gain = Highlights)
 * - Exposure / Luminance (Lift, Gamma, Gain sliders)
 * - Cinematic 3D LUT Looks (Teal & Orange, Portra 400, Fuji Eterna, Cyberpunk, Film Noir, Custom .cube)
 * - Color Space Transform (CST: Log to Rec.709 contrast curve)
 * - Split-Screen Compare Wipe: Memotong efek hanya pada sisi kanan slider sehingga
 *   sisi kiri menampilkan video asli (Before / RAW) dan sisi kanan menampilkan hasil grading (After / Graded)!
 */
@Composable
fun ColorGradingOverlay(
    state: ColorStudioState,
    isBypassed: Boolean = false,
    isSplitEnabled: Boolean = false,
    splitPosition: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    if (isBypassed) return

    val lift = state.lift
    val gamma = state.gamma
    val gain = state.gain
    val selectedLut = state.selectedLut
    val lutIntensity = state.activeNode.lutIntensity
    val cameraLog = state.inputLogProfile

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Area yang terkena grading:
        // Jika Split-Screen aktif: hanya dari splitX sampai lebar penuh (sisi kanan)
        // Jika Split-Screen mati: seluruh layar (0 sampai lebar penuh)
        val splitX = if (isSplitEnabled) (width * splitPosition).coerceIn(0f, width) else 0f

        clipRect(left = splitX, top = 0f, right = width, bottom = height) {
            val gradedWidth = width - splitX
            val topLeft = Offset(splitX, 0f)
            val rectSize = Size(gradedWidth, height)

            // 1. Color Space Transform (CST) Look Curve
            // Mengubah flat/milky Log profile menjadi kontras sinematik Rec.709
            if (cameraLog != CameraLogProfile.REC709) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        ),
                        startY = 0f,
                        endY = height
                    ),
                    topLeft = topLeft,
                    size = rectSize
                )
            }

            // 2. Exposure & Luminance (Luma sliders dari Lift, Gamma, Gain)
            val shadowLuma = lift.luminance      // -1f..1f
            val midtoneLuma = gamma.luminance    // -1f..1f
            val highlightLuma = gain.luminance   // -1f..1f

            val netLuma = (midtoneLuma * 0.4f + highlightLuma * 0.35f + shadowLuma * 0.25f).coerceIn(-0.85f, 0.85f)
            if (netLuma > 0.01f) {
                // Exposure naik: sapuan terang lembut
                drawRect(
                    color = Color.White.copy(alpha = netLuma * 0.55f),
                    topLeft = topLeft,
                    size = rectSize
                )
            } else if (netLuma < -0.01f) {
                // Exposure turun: sapuan gelap pekat
                drawRect(
                    color = Color.Black.copy(alpha = (-netLuma) * 0.65f),
                    topLeft = topLeft,
                    size = rectSize
                )
            }

            // 3. 3-Way Color Wheels Tints (Lift, Gamma, Gain)
            // A. Lift (Shadows) -> Gradient dari bawah ke atas
            if (lift.saturation > 0.02f) {
                val liftHueColor = Color.hsv(lift.hueDegrees, lift.saturation.coerceIn(0f, 1f), 1f)
                val liftAlpha = (lift.saturation * 0.45f).coerceIn(0f, 0.6f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, liftHueColor.copy(alpha = liftAlpha)),
                        startY = 0f,
                        endY = height
                    ),
                    topLeft = topLeft,
                    size = rectSize
                )
            }

            // B. Gamma (Midtones) -> Global tint di area tengah
            if (gamma.saturation > 0.02f) {
                val gammaHueColor = Color.hsv(gamma.hueDegrees, gamma.saturation.coerceIn(0f, 1f), 1f)
                val gammaAlpha = (gamma.saturation * 0.35f).coerceIn(0f, 0.5f)
                drawRect(
                    color = gammaHueColor.copy(alpha = gammaAlpha),
                    topLeft = topLeft,
                    size = rectSize
                )
            }

            // C. Gain (Highlights) -> Gradient dari atas ke bawah
            if (gain.saturation > 0.02f) {
                val gainHueColor = Color.hsv(gain.hueDegrees, gain.saturation.coerceIn(0f, 1f), 1f)
                val gainAlpha = (gain.saturation * 0.45f).coerceIn(0f, 0.6f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(gainHueColor.copy(alpha = gainAlpha), Color.Transparent),
                        startY = 0f,
                        endY = height
                    ),
                    topLeft = topLeft,
                    size = rectSize
                )
            }

            // 4. Cinematic 3D LUT Presets
            if (selectedLut != null) {
                val intensity = lutIntensity.coerceIn(0.1f, 1.0f)
                when (selectedLut.id) {
                    "1" -> {
                        // Teal & Orange (Hollywood Cinema)
                        // Shadows: Deep Cyan/Teal, Highlights: Warm Golden Amber
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF9800).copy(alpha = 0.32f * intensity),
                                    Color(0xFFE65100).copy(alpha = 0.15f * intensity),
                                    Color(0xFF00695C).copy(alpha = 0.35f * intensity)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                    "2" -> {
                        // Portra 400 (Warm Kodak Analog Film)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFE082).copy(alpha = 0.28f * intensity),
                                    Color(0xFFFFA726).copy(alpha = 0.22f * intensity),
                                    Color(0xFF5D4037).copy(alpha = 0.20f * intensity)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                    "3" -> {
                        // Fuji Eterna (Filmic Cinema Soft Green/Teal)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF80CBC4).copy(alpha = 0.25f * intensity),
                                    Color(0xFF26A69A).copy(alpha = 0.18f * intensity),
                                    Color(0xFF1B5E20).copy(alpha = 0.24f * intensity)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                    "4" -> {
                        // Cyberpunk Neon (Electric Magenta & Cyan)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD500F9).copy(alpha = 0.35f * intensity),
                                    Color(0xFF00E5FF).copy(alpha = 0.35f * intensity)
                                ),
                                start = topLeft,
                                end = Offset(width, height)
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                    "5" -> {
                        // Film Noir (Dramatic High-Contrast Monochrome)
                        drawRect(
                            color = Color(0xFF121212).copy(alpha = 0.55f * intensity),
                            topLeft = topLeft,
                            size = rectSize
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f * intensity),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.40f * intensity)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                    else -> {
                        // Custom Imported .cube / .3dl LUT
                        val customColor = Color(selectedLut.filterColor)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    customColor.copy(alpha = 0.30f * intensity),
                                    customColor.copy(alpha = 0.18f * intensity),
                                    Color.Black.copy(alpha = 0.25f * intensity)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            topLeft = topLeft,
                            size = rectSize
                        )
                    }
                }
            }
        }
    }
}

