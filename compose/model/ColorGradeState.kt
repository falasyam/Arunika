package com.lumina.studio.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class WheelTarget(val label: String, val description: String) {
    LIFT("Lift", "Shadows / Black Point"),
    GAMMA("Gamma", "Midtones / Exposure"),
    GAIN("Gain", "Highlights / White Point"),
    OFFSET("Offset", "Global Balance")
}

/**
 * Tipe DaVinci Node
 */
enum class NodeType(val label: String) {
    SERIAL("Serial"),
    PARALLEL("Parallel")
}

/**
 * Representasi satu Node Grading DaVinci Resolve Style
 */
class GradeNode(
    val id: Int,
    initialName: String,
    initialEnabled: Boolean = true,
    val type: NodeType = NodeType.SERIAL
) {
    var name by mutableStateOf(initialName)
    var isEnabled by mutableStateOf(initialEnabled)

    // Node-specific grading parameters
    val lift = ColorWheelValue()
    val gamma = ColorWheelValue()
    val gain = ColorWheelValue()
    val offset = ColorWheelValue()

    var temperature by mutableFloatStateOf(0f)
    var tint by mutableFloatStateOf(0f)
    var contrast by mutableFloatStateOf(1.0f)
    var saturation by mutableFloatStateOf(1.0f)
    var selectedLut by mutableStateOf<LutPreset?>(null)
    var lutIntensity by mutableFloatStateOf(0.8f)
    var vignette by mutableFloatStateOf(0f)

    fun reset() {
        lift.reset()
        gamma.reset()
        gain.reset()
        offset.reset()
        temperature = 0f
        tint = 0f
        contrast = 1.0f
        saturation = 1.0f
        selectedLut = null
        lutIntensity = 0.8f
        vignette = 0f
    }
}

/**
 * Profil Color Space Transform (CST) untuk kamera HP dan Mirrorless
 */
enum class CameraLogProfile(val label: String, val dynamicRangeStops: Float) {
    REC709("Rec.709 Standard SDR", 8.0f),
    SAMSUNG_LOG("Samsung D-Log (S24/S25 Ultra)", 13.5f),
    APPLE_LOG("Apple Log (ProRes 10-bit)", 14.0f),
    SONY_SLOG3("Sony S-Log3 / S-Gamut3.Cine", 15.0f),
    CANON_CLOG("Canon C-Log3", 14.0f)
}

/**
 * Pilihan 3D LUT Cinematic (Built-in & Custom Imported LUTs)
 */
data class LutPreset(
    val id: String,
    val name: String,
    val description: String,
    val filterColor: Long = 0xFFF59E0B,
    val format: LutFormat = LutFormat.CUBE_3D,
    val isCustom: Boolean = false,
    val parsedLut: ParsedLut? = null
)

/**
 * Mode tampilan Scopes teknis
 */
enum class ScopeMode(val label: String) {
    OFF("Hidden"),
    WAVEFORM("Waveform (Luma)"),
    RGB_PARADE("RGB Parade"),
    VECTORSCOPE("Vectorscope (Hue/Sat)")
}

/**
 * Titik kurva HSL interaktif
 */
data class HslCurvePoint(
    var inputX: Float, // 0.0 to 1.0 (Hue angle or Luminance)
    var outputY: Float // 0.0 to 1.0 (Saturation or Adjusted Value)
)

/**
 * Menyimpan nilai koordinat warna dan luminance untuk satu roda warna.
 */
class ColorWheelValue(
    initialX: Float = 0f,
    initialY: Float = 0f,
    initialLuminance: Float = 0f
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var luminance by mutableFloatStateOf(initialLuminance)

    val saturation: Float
        get() = sqrt((x * x + y * y).toDouble()).toFloat().coerceIn(0f, 1f)

    val hueDegrees: Float
        get() {
            val angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
            return (angle + 360f) % 360f
        }

    fun setPolar(hueDegrees: Float, saturation: Float) {
        val rad = Math.toRadians(hueDegrees.toDouble())
        val sat = saturation.coerceIn(0f, 1f)
        x = (cos(rad) * sat).toFloat()
        y = (sin(rad) * sat).toFloat()
    }

    fun reset() {
        x = 0f
        y = 0f
        luminance = 0f
    }

    val isNeutral: Boolean
        get() = x == 0f && y == 0f && luminance == 0f
}

/**
 * State global untuk Color Studio terpadu dengan DaVinci Node Tree, CST, 3D LUT, dan Pro Scopes
 */
class ColorStudioState {
    var activeTarget by mutableStateOf(WheelTarget.GAMMA)

    // ========================================================
    // DaVinci Style Node Pipeline
    // ========================================================
    val nodes = mutableStateListOf<GradeNode>().apply {
        add(GradeNode(1, "01: CST Input", initialEnabled = true))
        add(GradeNode(2, "02: Primaries", initialEnabled = true))
        add(GradeNode(3, "03: Film Look", initialEnabled = true))
        add(GradeNode(4, "04: Vignette", initialEnabled = true))
    }
    var activeNodeIndex by mutableIntStateOf(1) // Default to Node 02 Primaries

    val activeNode: GradeNode
        get() = if (nodes.isNotEmpty() && activeNodeIndex in nodes.indices) nodes[activeNodeIndex] else nodes.first()

    fun addSerialNode(customName: String? = null) {
        val nextId = (nodes.maxOfOrNull { it.id } ?: 0) + 1
        val nodeName = customName ?: String.format("%02d: Serial Grade", nextId)
        val newNode = GradeNode(nextId, nodeName, initialEnabled = true)
        nodes.add(newNode)
        activeNodeIndex = nodes.lastIndex
    }

    fun toggleNodeBypass(index: Int) {
        if (index in nodes.indices) {
            nodes[index].isEnabled = !nodes[index].isEnabled
        }
    }

    fun deleteNode(index: Int) {
        if (nodes.size > 1 && index in nodes.indices) {
            nodes.removeAt(index)
            activeNodeIndex = activeNodeIndex.coerceAtMost(nodes.lastIndex)
        }
    }

    // Shortcut untuk mengakses roda dan slider pada node yang sedang aktif
    val lift: ColorWheelValue get() = activeNode.lift
    val gamma: ColorWheelValue get() = activeNode.gamma
    val gain: ColorWheelValue get() = activeNode.gain
    val offset: ColorWheelValue get() = activeNode.offset

    var temperature: Float
        get() = activeNode.temperature
        set(value) { activeNode.temperature = value }

    var tint: Float
        get() = activeNode.tint
        set(value) { activeNode.tint = value }

    var contrast: Float
        get() = activeNode.contrast
        set(value) { activeNode.contrast = value }

    var saturation: Float
        get() = activeNode.saturation
        set(value) { activeNode.saturation = value }

    var selectedLut: LutPreset?
        get() = activeNode.selectedLut
        set(value) { activeNode.selectedLut = value }

    var lutIntensity: Float
        get() = activeNode.lutIntensity
        set(value) { activeNode.lutIntensity = value }

    var vignette: Float
        get() = activeNode.vignette
        set(value) { activeNode.vignette = value }

    // Modul 1: Pro Scopes & Split Screen
    var scopeMode by mutableStateOf(ScopeMode.OFF)
    var isSplitCompareEnabled by mutableStateOf(false)
    var splitPosition by mutableFloatStateOf(0.5f) // 0.0 (full before) to 1.0 (full after)

    // Modul 2: CST & 3D LUT
    var inputLogProfile by mutableStateOf(CameraLogProfile.SAMSUNG_LOG)
    val customLuts = mutableStateListOf<LutPreset>()

    fun importLut(content: String, fileName: String): LutPreset {
        val parsed = LutParser.parse(content, fileName)
        val preset = LutPreset(
            id = parsed.id,
            name = parsed.name,
            description = "${parsed.format.displayName} (${parsed.size}³)",
            filterColor = 0xFF8B5CF6,
            format = parsed.format,
            isCustom = true,
            parsedLut = parsed
        )
        customLuts.add(preset)
        selectedLut = preset
        return preset
    }

    fun removeCustomLut(preset: LutPreset) {
        customLuts.remove(preset)
        if (selectedLut?.id == preset.id) {
            selectedLut = null
        }
    }

    // Modul 3: Media Ingestion
    var userMediaUri by mutableStateOf<String?>(null)
    var userMediaType by mutableStateOf("sample") // "sample", "video", "image"

    fun activeValue(): ColorWheelValue = when (activeTarget) {
        WheelTarget.LIFT -> lift
        WheelTarget.GAMMA -> gamma
        WheelTarget.GAIN -> gain
        WheelTarget.OFFSET -> offset
    }

    fun resetActive() {
        activeValue().reset()
    }

    fun resetAll() {
        for (node in nodes) {
            node.reset()
        }
        isSplitCompareEnabled = false
    }
}
