package com.falasyam.arunika.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import java.util.UUID
import java.io.Serializable
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
) : Serializable

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
        return interpolateKeyframes(keyframes, timeMs)
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

fun interpolateKeyframes(keyframes: List<Keyframe>, timeMs: Long): TransformValues {
    fun interpolateProperty(property: AnimProperty): Float {
        val propertyKeyframes = keyframes
            .asSequence()
            .filter { it.property == property }
            .sortedBy { it.timeMs }
            .toList()
        if (propertyKeyframes.isEmpty()) return property.defaultValue
        if (timeMs <= propertyKeyframes.first().timeMs) return propertyKeyframes.first().value
        if (timeMs >= propertyKeyframes.last().timeMs) return propertyKeyframes.last().value

        val nextIndex = propertyKeyframes.indexOfFirst { it.timeMs >= timeMs }
        val previous = propertyKeyframes[nextIndex - 1]
        val next = propertyKeyframes[nextIndex]
        val duration = (next.timeMs - previous.timeMs).toFloat()
        if (duration <= 0f) return previous.value

        var progress = (timeMs - previous.timeMs) / duration
        progress = when (previous.easing) {
            EasingType.LINEAR -> progress
            EasingType.EASE_IN_OUT -> 0.5f * (1f - cos(progress * PI.toFloat()))
            EasingType.EASE_IN -> progress * progress
            EasingType.EASE_OUT -> sin(progress * (PI.toFloat() / 2f))
        }
        return previous.value + (next.value - previous.value) * progress
    }

    return TransformValues(
        posX = interpolateProperty(AnimProperty.POSITION_X),
        posY = interpolateProperty(AnimProperty.POSITION_Y),
        scale = interpolateProperty(AnimProperty.SCALE),
        rotation = interpolateProperty(AnimProperty.ROTATION),
        opacity = interpolateProperty(AnimProperty.OPACITY)
    )
}
