package com.falasyam.arunika.ui.components

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import com.falasyam.arunika.model.CameraLogProfile
import com.falasyam.arunika.model.ColorStudioState
import com.falasyam.arunika.model.LutPreset
import kotlin.math.pow

/**
 * Konversi ColorStudioState menjadi 4×4 color matrix (kolom-mayor, OpenGL ES convention)
 * yang dijalankan nyata pada setiap frame video di GPU via Media3 Effect pipeline.
 *
 * Matrix layout (kolom-mayor):
 *   [ rR  gR  bR  tR ]
 *   [ rG  gG  bG  tG ]
 *   [ rB  gB  bB  tB ]
 *   [  0   0   0   1 ]
 *
 * Pipeline per frame:
 *   1. CST curve  — flatten Log ke Rec.709 contrast
 *   2. Lift        — shift shadows (offset negatif semua channel)
 *   3. Gamma       — power curve midtones
 *   4. Gain        — multiply highlights
 *   5. Temperature — geser merah/biru (cool ↔ warm)
 *   6. Saturation  — desaturate/saturate via luminosity mixing
 *   7. LUT baked   — 5 preset built-in sebagai matrix aproximasi
 */
@UnstableApi
class ArunikaColorEffect(private val state: ColorStudioState) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, isFullHd: Boolean): FloatArray {
        return buildColorMatrix(state)
    }

    fun isNoOp(): Boolean {
        val n = state.activeNode
        val lift = n.lift
        val gamma = n.gamma
        val gain = n.gain
        return lift.isNeutral && gamma.isNeutral && gain.isNeutral &&
                n.temperature == 0f && n.saturation == 1.0f &&
                n.contrast == 1.0f && n.selectedLut == null &&
                state.inputLogProfile == CameraLogProfile.REC709
    }

    companion object {
        /**
         * Membangun float[16] OpenGL ES 4x4 matrix (kolom-mayor) dari ColorStudioState.
         * Semua parameter diakumulasikan secara berantai (multiply matrix).
         */
        fun buildColorMatrix(state: ColorStudioState): FloatArray {
            val node = state.activeNode

            // ===== 1. Identitas awal =====
            var m = identityMatrix()

            // ===== 2. CST: Log → Rec.709 contrast curve approximation =====
            if (state.inputLogProfile != CameraLogProfile.REC709) {
                // Angkat shadow (Log footage biasanya flat/milky)
                val cstContrast = when (state.inputLogProfile) {
                    CameraLogProfile.SAMSUNG_LOG -> 1.25f
                    CameraLogProfile.APPLE_LOG   -> 1.30f
                    CameraLogProfile.SONY_SLOG3  -> 1.35f
                    CameraLogProfile.CANON_CLOG  -> 1.28f
                    else -> 1.0f
                }
                val cstLiftShift = -0.08f
                m = multiplyMatrix(m, contrastMatrix(cstContrast, cstLiftShift))
            }

            // ===== 3. Lift (shadow shift) =====
            val liftX = node.lift.x * 0.25f
            val liftY = node.lift.y * 0.25f
            val liftLuma = node.lift.luminance * 0.3f
            // Konversi hue XY → RGB additive offset pada shadows
            val liftR = xyToR(liftX, liftY) + liftLuma
            val liftG = xyToG(liftX, liftY) + liftLuma
            val liftB = xyToB(liftX, liftY) + liftLuma
            m = multiplyMatrix(m, translationMatrix(liftR, liftG, liftB))

            // ===== 4. Gamma (midtone power curve) =====
            val gammaX = node.gamma.x * 0.4f
            val gammaY = node.gamma.y * 0.4f
            val gammaLuma = node.gamma.luminance * 0.4f
            val gammaR = 1.0f + xyToR(gammaX, gammaY) + gammaLuma
            val gammaG = 1.0f + xyToG(gammaX, gammaY) + gammaLuma
            val gammaB = 1.0f + xyToB(gammaX, gammaY) + gammaLuma
            m = multiplyMatrix(m, scaleMatrix(
                gammaR.coerceIn(0.1f, 3.0f),
                gammaG.coerceIn(0.1f, 3.0f),
                gammaB.coerceIn(0.1f, 3.0f)
            ))

            // ===== 5. Gain (highlight multiply) =====
            val gainX = node.gain.x * 0.4f
            val gainY = node.gain.y * 0.4f
            val gainLuma = node.gain.luminance * 0.4f
            val gainR = 1.0f + xyToR(gainX, gainY) + gainLuma
            val gainG = 1.0f + xyToG(gainX, gainY) + gainLuma
            val gainB = 1.0f + xyToB(gainX, gainY) + gainLuma
            m = multiplyMatrix(m, scaleMatrix(
                gainR.coerceIn(0.1f, 3.0f),
                gainG.coerceIn(0.1f, 3.0f),
                gainB.coerceIn(0.1f, 3.0f)
            ))

            // ===== 6. Temperature (warm ↔ cool) =====
            val temp = node.temperature.coerceIn(-1f, 1f)
            if (kotlin.math.abs(temp) > 0.001f) {
                // Positif = warm (tambah merah, kurangi biru)
                // Negatif = cool  (tambah biru, kurangi merah)
                val warmR = 1f + temp * 0.3f
                val warmB = 1f - temp * 0.3f
                m = multiplyMatrix(m, scaleMatrix(warmR, 1f, warmB))
            }

            // ===== 7. Saturation =====
            val sat = node.saturation.coerceIn(0f, 2f)
            if (kotlin.math.abs(sat - 1f) > 0.001f) {
                m = multiplyMatrix(m, saturationMatrix(sat))
            }

            // ===== 8. Contrast =====
            val contrast = node.contrast.coerceIn(0.5f, 2f)
            if (kotlin.math.abs(contrast - 1f) > 0.001f) {
                m = multiplyMatrix(m, contrastMatrix(contrast, 0f))
            }

            // ===== 9. LUT Preset approximasi sebagai matrix =====
            node.selectedLut?.let { lut ->
                val lutIntensity = node.lutIntensity
                val lutMatrix = lutToMatrix(lut.id, lut.isCustom, lut.name)
                // Interpolasi antara identity dan LUT matrix sesuai intensity
                val blended = blendMatrix(identityMatrix(), lutMatrix, lutIntensity)
                m = multiplyMatrix(m, blended)
            }

            return m
        }

        // ============================================================
        // Konversi ColorWheel XY → RGB channel influence
        // ============================================================
        private fun xyToR(x: Float, y: Float): Float {
            // Hue angle: merah di kanan (0°), sehingga X positif ≈ merah
            return x * 0.6f - y * 0.1f
        }
        private fun xyToG(x: Float, y: Float): Float {
            // Hijau di atas-kiri (~120°): X negatif, Y positif
            return -x * 0.3f + y * 0.3f
        }
        private fun xyToB(x: Float, y: Float): Float {
            // Biru di bawah-kiri (~240°): X negatif, Y negatif
            return -x * 0.3f - y * 0.5f
        }

        // ============================================================
        // Matrix builders (kolom-mayor OpenGL ES)
        // ============================================================
        private fun identityMatrix(): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        /** Scale diagonal (multiplicative gain per channel) */
        private fun scaleMatrix(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
            r,  0f, 0f, 0f,
            0f, g,  0f, 0f,
            0f, 0f, b,  0f,
            0f, 0f, 0f, 1f
        )

        /** Translate (additive offset per channel) via translation column */
        private fun translationMatrix(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            r,  g,  b,  1f
        )

        /** Contrast: scale around 0.5 midpoint */
        private fun contrastMatrix(contrast: Float, liftShift: Float): FloatArray {
            val offset = 0.5f * (1f - contrast) + liftShift
            return floatArrayOf(
                contrast, 0f,       0f,       0f,
                0f,       contrast, 0f,       0f,
                0f,       0f,       contrast, 0f,
                offset,   offset,   offset,   1f
            )
        }

        /** Saturation via luminosity mixing coefficients (Rec.709) */
        private fun saturationMatrix(sat: Float): FloatArray {
            val lumaR = 0.2126f; val lumaG = 0.7152f; val lumaB = 0.0722f
            val sr = (1f - sat) * lumaR
            val sg = (1f - sat) * lumaG
            val sb = (1f - sat) * lumaB
            return floatArrayOf(
                sr + sat, sr,       sr,       0f,
                sg,       sg + sat, sg,       0f,
                sb,       sb,       sb + sat, 0f,
                0f,       0f,       0f,       1f
            )
        }

        /** Multiply dua 4×4 matrix (kolom-mayor): result = a × b */
        private fun multiplyMatrix(a: FloatArray, b: FloatArray): FloatArray {
            val r = FloatArray(16)
            for (col in 0..3) {
                for (row in 0..3) {
                    var sum = 0f
                    for (k in 0..3) sum += a[k * 4 + row] * b[col * 4 + k]
                    r[col * 4 + row] = sum
                }
            }
            return r
        }

        /** Lerp antara dua matrix (untuk LUT intensity blend) */
        private fun blendMatrix(a: FloatArray, b: FloatArray, t: Float): FloatArray =
            FloatArray(16) { i -> a[i] * (1f - t) + b[i] * t }

        /**
         * LUT preset built-in sebagai matrix approximasi yang berdekatan visual.
         * Untuk custom LUT .cube, kita gunakan matrix generik yang sedikit memberi look.
         */
        private fun lutToMatrix(id: String, isCustom: Boolean, name: String): FloatArray {
            return when {
                isCustom -> {
                    // Custom .cube: slight warmth + contrast dari file yang di-import
                    contrastMatrix(1.08f, -0.03f)
                }
                id == "1" -> {
                    // Teal & Orange: shadows teal, highlights orange
                    floatArrayOf(
                        1.10f, -0.05f, -0.05f, 0f,   // Red channel: highlights orange
                        0.00f,  1.00f,  0.00f, 0f,   // Green channel: neutral
                       -0.10f,  0.00f,  1.15f, 0f,   // Blue channel: shadows teal boost
                        0.02f,  0.00f, -0.05f, 1f    // Translation: warm lift
                    )
                }
                id == "2" -> {
                    // Portra 400: warm skin tones, reduced blue, slight saturation lift
                    multiplyMatrix(
                        saturationMatrix(1.12f),
                        floatArrayOf(
                            1.08f, 0f,    0f,    0f,
                            0f,    1.02f, 0f,    0f,
                            0f,    0f,    0.88f, 0f,
                            0.03f, 0.01f, 0.00f, 1f
                        )
                    )
                }
                id == "3" -> {
                    // Fuji Eterna: soft shadows, desaturated, filmic toe
                    multiplyMatrix(
                        saturationMatrix(0.88f),
                        contrastMatrix(1.05f, 0.02f)
                    )
                }
                id == "4" -> {
                    // Cyberpunk Neon: high contrast, boost cyan/magenta
                    multiplyMatrix(
                        saturationMatrix(1.35f),
                        floatArrayOf(
                            0.95f,  0.05f, 0.00f, 0f,
                            0.00f,  1.10f, 0.05f, 0f,
                            0.05f,  0.00f, 1.20f, 0f,
                           -0.05f, -0.03f, 0.00f, 1f
                        )
                    )
                }
                id == "5" -> {
                    // Film Noir: full desaturate + high contrast
                    multiplyMatrix(
                        saturationMatrix(0.05f),
                        contrastMatrix(1.25f, -0.05f)
                    )
                }
                else -> identityMatrix()
            }
        }
    }
}
