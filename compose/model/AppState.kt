package com.lumina.studio.model

/**
 * Representasi 4 Tab Kerja Utama dalam workflow Lumina Studio.
 */
enum class LuminaTab(val title: String, val subtitle: String) {
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
