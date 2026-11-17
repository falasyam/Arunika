package com.falasyam.arunika.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Properti transform yang dapat di-keyframe.
 */
enum class AnimProperty(val label: String, val defaultValue: Float, val minVal: Float, val maxVal: Float) {
    POSITION_X("Pan X", 0f, -100f, 100f),
    POSITION_Y("Pan Y", 0f, -100f, 100f),
    SCALE("Zoom / Scale", 1.0f, 0.2f, 3.0f),
    ROTATION("Rotation", 0f, -180f, 180f),
    OPACITY("Opacity", 1.0f, 0.0f, 1.0f)
}

/**
 * Tipe kurva easing untuk transisi keyframe yang mulus
 */
enum class EasingType(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN_OUT("Smooth Ease In/Out"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out")
}

/**
 * Satu titik keyframe pada timeline.
 */
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val property: AnimProperty,
    val value: Float,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

/**
 * Kumpulan nilai transform hasil interpolasi pada satu titik waktu
 */
data class TransformValues(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f
)

/**
 * State holder untuk manajemen keyframe & motion graphics.
 */
class KeyframeState {
    val keyframes = mutableStateListOf<Keyframe>()

    fun addOrUpdateKeyframe(timeMs: Long, property: AnimProperty, value: Float, easing: EasingType = EasingType.EASE_IN_OUT): Keyframe {
        val existingIndex = keyframes.indexOfFirst { it.timeMs == timeMs && it.property == property }
        val kf = Keyframe(timeMs = timeMs, property = property, value = value, easing = easing)
        if (existingIndex >= 0) {
            keyframes[existingIndex] = kf
        } else {
            keyframes.add(kf)
            keyframes.sortBy { it.timeMs }
        }
        return kf
    }

    fun removeKeyframe(id: String) {
        keyframes.removeIf { it.id == id }
    }

    fun hasKeyframeAt(timeMs: Long, property: AnimProperty? = null): Boolean {
        return keyframes.any {
            it.timeMs == timeMs && (property == null || it.property == property)
        }
    }

    /**
     * Hitung nilai transform terinterpolasi pada timeMs tertentu
     */
    fun interpolateTransform(timeMs: Long): TransformValues {
        val posX = interpolateProperty(timeMs, AnimProperty.POSITION_X)
        val posY = interpolateProperty(timeMs, AnimProperty.POSITION_Y)
        val scale = interpolateProperty(timeMs, AnimProperty.SCALE)
        val rotation = interpolateProperty(timeMs, AnimProperty.ROTATION)
        val opacity = interpolateProperty(timeMs, AnimProperty.OPACITY)

        return TransformValues(
            posX = posX,
            posY = posY,
            scale = scale,
            rotation = rotation,
            opacity = opacity
        )
    }

    private fun interpolateProperty(timeMs: Long, property: AnimProperty): Float {
        val propKfs = keyframes.filter { it.property == property }.sortedBy { it.timeMs }
        if (propKfs.isEmpty()) return property.defaultValue
        if (timeMs <= propKfs.first().timeMs) return propKfs.first().value
        if (timeMs >= propKfs.last().timeMs) return propKfs.last().value

        // Cari dua keyframe yang mengapit timeMs
        var prev = propKfs.first()
        var next = propKfs.last()
        for (i in 0 until propKfs.size - 1) {
            if (timeMs >= propKfs[i].timeMs && timeMs <= propKfs[i + 1].timeMs) {
                prev = propKfs[i]
                next = propKfs[i + 1]
                break
            }
        }

        val duration = (next.timeMs - prev.timeMs).toFloat()
        if (duration <= 0) return prev.value

        var t = (timeMs - prev.timeMs) / duration

        // Easing calculations
        t = when (prev.easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN_OUT -> (0.5f * (1f - cos(t * PI.toFloat())))
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> sin(t * (PI.toFloat() / 2f))
        }

        return prev.value + (next.value - prev.value) * t
    }

    // ========================================================
    // PRESET KEN BURNS MOTION GENERATORS
    // ========================================================
    fun applyKenBurnsSlowZoom(durationMs: Long = 4000) {
        keyframes.clear()
        addOrUpdateKeyframe(0, AnimProperty.SCALE, 1.0f)
        addOrUpdateKeyframe(durationMs, AnimProperty.SCALE, 1.25f)
    }

    fun applyKenBurnsPan(durationMs: Long = 4000) {
        keyframes.clear()
        addOrUpdateKeyframe(0, AnimProperty.POSITION_X, -20f)
        addOrUpdateKeyframe(durationMs, AnimProperty.POSITION_X, 20f)
        addOrUpdateKeyframe(0, AnimProperty.SCALE, 1.15f)
        addOrUpdateKeyframe(durationMs, AnimProperty.SCALE, 1.15f)
    }
}
