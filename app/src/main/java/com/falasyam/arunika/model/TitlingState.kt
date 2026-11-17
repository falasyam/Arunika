package com.falasyam.arunika.model

import androidx.compose.runtime.mutableStateListOf
import java.util.UUID

/**
 * Gaya visual title element.
 */
enum class TitleStyle(val displayName: String) {
    LOWER_THIRD("Lower Third"),
    CHAPTER_MARKER("Chapter Marker"),
    CAPTION("Caption"),
    OPENING_TITLE("Opening Title"),
    CREDIT("End Credit")
}

/**
 * Jenis animasi masuk untuk title element.
 */
enum class TitleAnimation(val displayName: String) {
    FADE_IN("Fade In"),
    SLIDE_UP("Slide Up"),
    TYPEWRITER("Typewriter"),
    WORD_BY_WORD("Word by Word"),
    NONE("None")
}

/**
 * Font family pilihan untuk titling.
 */
enum class TitleFont(val displayName: String, val cssFamily: String) {
    SANS("Sans", "Plus Jakarta Sans, system-ui, sans-serif"),
    SERIF("Serif", "Georgia, 'Times New Roman', serif"),
    MONO("Mono", "'JetBrains Mono', monospace"),
    CINEMATIC("Cinematic", "'Playfair Display', Georgia, serif"),
    HANDWRITTEN("Handwritten", "'Dancing Script', cursive")
}

/**
 * Satu elemen teks yang muncul di video overlay.
 *
 * @param id           ID unik elemen (UUID string)
 * @param text         Isi teks yang ditampilkan
 * @param x            Posisi horizontal relatif (0.0 = kiri, 1.0 = kanan)
 * @param y            Posisi vertikal relatif (0.0 = atas, 1.0 = bawah)
 * @param style        Preset gaya visual (lower third, caption, dsb.)
 * @param animationType Animasi masuk yang digunakan
 * @param startFrame   Frame awal kemunculan di timeline
 * @param durationFrames Durasi tampil dalam frame
 * @param fontSize     Ukuran font (sp / dp)
 * @param textColor    Warna teks (0xAARRGGBB)
 * @param bgColor      Warna latar belakang (0xAARRGGBB)
 * @param outlineColor Warna outline/stroke (0xAARRGGBB)
 * @param opacity      Opasitas 0.0–1.0
 * @param font         Pilihan font family
 * @param wordHighlight Aktifkan highlight per-kata (gaya Reels/Shorts)
 * @param highlightColor Warna highlight kotak per kata
 */
data class TitleElement(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Title",
    val x: Float = 0.5f,
    val y: Float = 0.8f,
    val style: TitleStyle = TitleStyle.LOWER_THIRD,
    val animationType: TitleAnimation = TitleAnimation.FADE_IN,
    val startFrame: Int = 0,
    val durationFrames: Int = 90,  // 3 detik pada 30fps
    val fontSize: Float = 24f,
    val textColor: Long = 0xFFFFFFFF,
    val bgColor: Long = 0xCC000000,
    val outlineColor: Long = 0x00000000,
    val opacity: Float = 1.0f,
    val font: TitleFont = TitleFont.SANS,
    val wordHighlight: Boolean = false,
    val highlightColor: Long = 0xFFFFD600
)

/**
 * State holder untuk modul Titling.
 * Menyimpan semua TitleElement yang ada di project.
 */
class TitlingState {
    /** Daftar semua title element aktif (observable oleh Compose). */
    val elements = mutableStateListOf<TitleElement>()

    /** Tambah elemen baru dengan preset Lower Third. */
    fun addLowerThird(text: String = "Speaker Name | Role"): TitleElement {
        val el = TitleElement(
            text = text,
            x = 0.1f,
            y = 0.82f,
            style = TitleStyle.LOWER_THIRD,
            animationType = TitleAnimation.SLIDE_UP,
            fontSize = 22f,
            bgColor = 0xDD1A1A2E,
            textColor = 0xFFFFFFFF
        )
        elements.add(el)
        return el
    }

    /** Tambah subtitle/caption gaya Reels word-by-word. */
    fun addCaption(text: String = "Subtitle teks di sini..."): TitleElement {
        val el = TitleElement(
            text = text,
            x = 0.5f,
            y = 0.88f,
            style = TitleStyle.CAPTION,
            animationType = TitleAnimation.WORD_BY_WORD,
            fontSize = 20f,
            bgColor = 0x00000000,
            textColor = 0xFFFFFFFF,
            wordHighlight = true,
            highlightColor = 0xFFFFD600
        )
        elements.add(el)
        return el
    }

    /** Tambah judul pembuka sinematik. */
    fun addOpeningTitle(text: String = "JUDUL FILM"): TitleElement {
        val el = TitleElement(
            text = text,
            x = 0.5f,
            y = 0.5f,
            style = TitleStyle.OPENING_TITLE,
            animationType = TitleAnimation.FADE_IN,
            fontSize = 36f,
            bgColor = 0x00000000,
            textColor = 0xFFFFFFFF,
            font = TitleFont.CINEMATIC,
            durationFrames = 150
        )
        elements.add(el)
        return el
    }

    /** Hapus elemen berdasarkan ID. */
    fun removeElement(id: String) {
        elements.removeIf { it.id == id }
    }

    /** Update elemen yang sudah ada. */
    fun updateElement(updated: TitleElement) {
        val idx = elements.indexOfFirst { it.id == updated.id }
        if (idx >= 0) elements[idx] = updated
    }

    /** Hitung total durasi proyek dalam frame. */
    fun totalDurationFrames(): Int =
        elements.maxOfOrNull { it.startFrame + it.durationFrames } ?: 0
}
