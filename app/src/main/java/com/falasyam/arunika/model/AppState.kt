package com.falasyam.arunika.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Ukuran resolusi video (Lebar x Tinggi dalam piksel) dengan dukungan kustom
 */
data class VideoDimensions(
    var width: Int = 1920,
    var height: Int = 1080,
    var isCustom: Boolean = false
) {
    val aspectRatioFloat: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f

    val description: String
        get() = "${width}x${height} (${getNearestRatioLabel()})"

    private fun getNearestRatioLabel(): String {
        val r = aspectRatioFloat
        return when {
            kotlin.math.abs(r - 16f / 9f) < 0.05f -> "16:9"
            kotlin.math.abs(r - 9f / 16f) < 0.05f -> "9:16"
            kotlin.math.abs(r - 1f) < 0.05f -> "1:1"
            kotlin.math.abs(r - 4f / 5f) < 0.05f -> "4:5"
            kotlin.math.abs(r - 4f / 3f) < 0.05f -> "4:3"
            kotlin.math.abs(r - 21f / 9f) < 0.1f -> "21:9"
            kotlin.math.abs(r - 2f) < 0.05f -> "2:1"
            else -> "Custom"
        }
    }
}

/**
 * Pilihan Format Aspek Rasio Lengkap
 */
enum class AspectRatioMode(
    val label: String,
    val defaultWidth: Int,
    val defaultHeight: Int,
    val widthRatio: Int,
    val heightRatio: Int,
    val isVertical: Boolean,
    val platformTag: String
) {
    CINEMA_16_9("16:9 Cinema / YouTube", 1920, 1080, 16, 9, false, "YouTube / Landscape"),
    REELS_9_16("9:16 Shorts / TikTok / Reels", 1080, 1920, 9, 16, true, "TikTok / Reels / Shorts"),
    IG_PORTRAIT_4_5("4:5 Instagram Portrait Feed", 1080, 1350, 4, 5, true, "Instagram Feed"),
    SQUARE_1_1("1:1 Square Feed", 1080, 1080, 1, 1, false, "Square Post"),
    CLASSIC_4_3("4:3 Retro / iPad Academy", 1440, 1080, 4, 3, false, "Retro / TV / iPad"),
    UNIVISIUM_2_1("2:1 (18:9) Univisium / Netflix", 2160, 1080, 2, 1, false, "Netflix / Streaming"),
    ANAMORPHIC_21_9("21:9 (2.39:1) Scope Widescreen", 2560, 1080, 21, 9, false, "Ultra-wide Cinema"),
    CUSTOM("Custom Width & Height", 1920, 1080, 0, 0, false, "Custom Dimensions");

    val ratioFloat: Float
        get() = if (heightRatio > 0 && widthRatio > 0) widthRatio.toFloat() / heightRatio.toFloat() else defaultWidth.toFloat() / defaultHeight.toFloat()
}

/**
 * Representasi 4 Tab Kerja Utama dalam workflow Arunika.
 */
enum class ArunikaTab(val title: String, val subtitle: String) {
    CUT("Cut", "Assembly & Magnetic Timeline"),
    COLOR("Color", "32-bit Floating Point Color Wheels"),
    AUDIO("Audio", "Fairlight Lite & AI Voice Isolator"),
    TITLING("Titling", "Fusion Lite Text & Caption Overlay"),
    DELIVER("Deliver", "Render & 10-bit HDR Export")
}

/**
 * Tipe media klip pada timeline
 */
enum class ClipMediaType(val label: String) {
    VIDEO("Video"),
    PHOTO("Foto"),
    AUDIO("Audio")
}

/**
 * Model data klip di timeline (Video, Foto, atau Audio)
 */
data class TimelineClip(
    val id: String,
    val name: String,
    val durationMs: Long,
    val startOffsetMs: Long,
    val colorTag: Long = 0xFF3B82F6, // Blue accent
    val mediaType: ClipMediaType = ClipMediaType.VIDEO,
    val uriString: String? = null,
    val volume: Float = 1.0f,
    val speedMultiplier: Float = 1.0f
)

/**
 * Model data sebuah projek dalam Arunika
 */
data class ProjectItem(
    val id: String,
    val name: String,
    val lastModified: String,
    val durationMs: Long,
    val resolutionLabel: String = "4K UHD",
    val fps: Int = 24,
    val aspectRatio: AspectRatioMode = AspectRatioMode.CINEMA_16_9,
    val videoClips: List<TimelineClip> = emptyList(),
    val audioClips: List<TimelineClip> = emptyList(),
    val gradientColors: List<Long> = listOf(0xFF1E3A8A, 0xFF3B82F6)
)

/**
 * Factory untuk data sampel projek
 */
object ProjectPresets {
    fun createDefaultProjects(): List<ProjectItem> = listOf(
        ProjectItem(
            id = "proj-1",
            name = "Cinematic S-Log3 Tokyo",
            lastModified = "Diedit 15 menit lalu",
            durationMs = 19500,
            resolutionLabel = "4K UHD",
            fps = 24,
            aspectRatio = AspectRatioMode.CINEMA_16_9,
            videoClips = listOf(
                TimelineClip("v1", "A001_C001_SamsungLog.mp4", 4200, 0, 0xFF3B82F6, ClipMediaType.VIDEO),
                TimelineClip("v2", "A001_C002_Sunset_B-Roll.mp4", 6800, 4200, 0xFF10B981, ClipMediaType.VIDEO),
                TimelineClip("v3", "Still_Tokyo_Tower.jpg", 3500, 11000, 0xFF8B5CF6, ClipMediaType.PHOTO),
                TimelineClip("v4", "A001_C003_Interview_A_Cam.mp4", 5000, 14500, 0xFFF59E0B, ClipMediaType.VIDEO)
            ),
            audioClips = listOf(
                TimelineClip("a1", "BGM_Cinematic_Ambient.mp3", 14000, 0, 0xFF059669, ClipMediaType.AUDIO),
                TimelineClip("a2", "SFX_Whoosh_Transition.wav", 1500, 4100, 0xFFEA580C, ClipMediaType.AUDIO),
                TimelineClip("a3", "Voiceover_Dialogue_01.m4a", 5000, 14500, 0xFFD97706, ClipMediaType.AUDIO)
            ),
            gradientColors = listOf(0xFF1E3A8A, 0xFF3B82F6)
        ),
        ProjectItem(
            id = "proj-2",
            name = "Shorts Fashion Week 2026",
            lastModified = "Kemarin, 21:40",
            durationMs = 15000,
            resolutionLabel = "1080p FHD",
            fps = 30,
            aspectRatio = AspectRatioMode.REELS_9_16,
            videoClips = listOf(
                TimelineClip("v1", "Reels_Runway_Model.mp4", 5000, 0, 0xFFEC4899, ClipMediaType.VIDEO),
                TimelineClip("v2", "Lookbook_Outfit_01.jpg", 3000, 5000, 0xFFA855F7, ClipMediaType.PHOTO),
                TimelineClip("v3", "Slowmo_Backstage.mp4", 7000, 8000, 0xFF8B5CF6, ClipMediaType.VIDEO)
            ),
            audioClips = listOf(
                TimelineClip("a1", "Phonk_Beat_HighEnergy.mp3", 15000, 0, 0xFF10B981, ClipMediaType.AUDIO)
            ),
            gradientColors = listOf(0xFF831843, 0xFFBE185D)
        ),
        ProjectItem(
            id = "proj-3",
            name = "Travel Vlog Bali Hidden Gems",
            lastModified = "3 hari yang lalu",
            durationMs = 28000,
            resolutionLabel = "4K UHD",
            fps = 60,
            aspectRatio = AspectRatioMode.CINEMA_16_9,
            videoClips = listOf(
                TimelineClip("v1", "Drone_NusaPenida_Beach.mp4", 8000, 0, 0xFF0284C7, ClipMediaType.VIDEO),
                TimelineClip("v2", "Sunset_Uluwatu.jpg", 4000, 8000, 0xFFF97316, ClipMediaType.PHOTO),
                TimelineClip("v3", "Waterfall_Tegenungan.mp4", 9000, 12000, 0xFF0D9488, ClipMediaType.VIDEO)
            ),
            audioClips = listOf(
                TimelineClip("a1", "Acoustic_Guitar_Tropical.mp3", 22000, 0, 0xFF059669, ClipMediaType.AUDIO),
                TimelineClip("a2", "Ocean_Waves_Nature_SFX.wav", 8000, 0, 0xFF0284C7, ClipMediaType.AUDIO)
            ),
            gradientColors = listOf(0xFF065F46, 0xFF059669)
        )
    )
}

/**
 * Model data kontrol audio klip & master
 */
data class AudioStudioState(
    var masterVolume: Float = 1.0f,
    var voiceIsolation: Float = 0.0f, // 0 - 100%
    var deReverb: Float = 0.0f,       // 0 - 100%
    var autoDucking: Boolean = false,
    var duckingThresholdDb: Float = -24f,
    var bassGainDb: Float = 0f,
    var midGainDb: Float = 0f,
    var trebleGainDb: Float = 0f
)

/**
 * Model konfigurasi export & render
 */
enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    HD_1080P("1080p FHD", 1920, 1080),
    QHD_1440P("1440p 2K", 2560, 1440),
    UHD_4K("2160p 4K", 3840, 2160)
}

enum class ExportCodec(val label: String, val description: String) {
    H264("H.264 (AVC)", "Kompatibilitas universal SDR"),
    H265_10BIT("H.265 (HEVC 10-bit)", "Standar kualitas tinggi HDR / Log"),
    PRORES_PROXY("Apple ProRes Proxy", "Master arsip pascaproduksi")
}

class DeliverExportState {
    var resolution by mutableStateOf(ExportResolution.HD_1080P)
    var frameRate by mutableIntStateOf(30)
    var codec by mutableStateOf(ExportCodec.H265_10BIT)
    var bitrateMbps by mutableFloatStateOf(28.0f)
    var colorSpaceTag by mutableStateOf("Rec.709 SDR")
    var isExporting by mutableStateOf(false)
    var exportProgress by mutableFloatStateOf(0.0f)
}
