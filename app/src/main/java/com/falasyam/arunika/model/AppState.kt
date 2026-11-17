package com.falasyam.arunika.model

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
    CUSTOM("Custom Width & Height", 1920, 1080, 0, 0, false, "Custom Dimensions")
}

/**
 * Representasi 4 Tab Kerja Utama dalam workflow Arunika.
 */
enum class ArunikaTab(val title: String, val subtitle: String) {
    CUT("Cut", "Assembly & Magnetic Timeline"),
    COLOR("Color", "32-bit Floating Point Color Wheels"),
    AUDIO("Audio", "Fairlight Lite & AI Voice Isolator"),
    DELIVER("Deliver", "Render & 10-bit HDR Export")
}

/**
 * Model data klip video di timeline
 */
data class TimelineClip(
    val id: String,
    val name: String,
    val durationMs: Long,
    val startOffsetMs: Long,
    val colorTag: Long = 0xFF3B82F6 // Blue accent
)

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

data class DeliverExportState(
    var resolution: ExportResolution = ExportResolution.HD_1080P,
    var frameRate: Int = 30, // 24, 30, 60
    var codec: ExportCodec = ExportCodec.H265_10BIT,
    var bitrateMbps: Float = 28.0f,
    var colorSpaceTag: String = "Rec.709 SDR",
    var isExporting: Boolean = false,
    var exportProgress: Float = 0.0f
)
