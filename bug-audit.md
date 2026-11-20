# 🐛 Arunika — Audit Fitur: Ada Tapi Belum Berfungsi

> Status per commit `9714fa4` (20 Nov 2026)

---

## 🔴 KRITIS — Fungsionalitas Inti Rusak (SEMUA SELESAI DIPERBAIKI ✅)

### 1. Color Grading Tidak Mempengaruhi Video Nyata
- **Status:** ✅ **SELESAI** (Commit `fec8a29`)
- **Solusi yang Diterapkan:** Membuat `ArunikaColorEffect` mengimplementasikan `androidx.media3.effect.RgbMatrix` dengan pipeline matrix 4×4 kolom-mayor GPU (CST, Lift, Gamma, Gain, Temperature, Saturation, Contrast, LUT Preset). Diterapkan langsung ke ExoPlayer via `setVideoEffects()`.

---

### 2. GPU Shader Engine (NDK / C++)
- **Status:** ✅ **SELESAI & AMAN** (Commit `082cdc4`)
- **Solusi yang Diterapkan:** Library C++ NDK (`arunika_native_engine.cpp`) aktif dan terkompilasi melalui CMake untuk arm64-v8a, armeabi-v7a, dan x86_64. Ditambahkan safeguard try-catch dan flag `isLibraryLoaded` pada `GlslShaderEngine.kt` agar tidak crash saat runtime jika dipanggil di luar konteks GL.

---

### 3. Export Video Hanya Simulasi (Fake Progress Bar)
- **Status:** ✅ **SELESAI** (Commit `feb39fd`)
- **Solusi yang Diterapkan:** Mengimplementasikan hardware transcoding nyata menggunakan `androidx.media3.transformer.Transformer` & `MediaCodec` di `DeliverScreen.kt`. Efek color grading dibakar (*baked*) langsung ke video hasil render via `ArunikaColorEffect`, mendukung output H.264 & H.265 (HEVC), estimasi bitrate nyata, progress bar real-time dari `ProgressHolder`, penyimpanan otomatis ke direktori Movies, dan registrasi ke MediaStore.

---

### 4. AI Shot Matcher Tidak Terhubung ke UI
- **Status:** ✅ **SELESAI** (Commit `082cdc4`)
- **Solusi yang Diterapkan:** Tombol AI Shot Matcher (`AutoFixHigh`) dihubungkan langsung ke analisis frame video nyata melalui `MediaMetadataRetriever`. Frame diekstrak dan di-downsample menjadi 120×90 piksel, dihitung statistik warna RGB + Luma oleh `ShotMatcher.analyzeFrame()`, lalu hasil penyeimbangan `matchShots()` langsung diterapkan ke node color grading aktif dengan notifikasi Toast.

---

## 🟠 SEDANG — Fitur Parsial / Tidak Lengkap

### 5. Tab Audio — Semua Proses Hanya Simulasi
**Lokasi:** `AudioScreen.kt`
**Masalah:**
- VU Meter bergerak **dengan angka random**, bukan membaca level audio dari ExoPlayer.
- Voice Isolation, De-Reverb, Auto-Ducking — toggle switch hanya mengubah state boolean, **tidak ada DSP/audio processing** di baliknya.
- EQ Bass/Mid/Treble slider tidak mempengaruhi output audio.
**Seharusnya:** Gunakan `AudioEffect` / `Equalizer` Android API, atau integrasikan `ExoPlayer` dengan custom `AudioProcessor`.

---

### 6. Keyframe Engine Tidak Terhubung ke Timeline
**Lokasi:** `KeyframeState.kt`
**Masalah:** Model `KeyframeState` dengan diamond marker `◆` sudah ada, tapi tidak tampil di timeline `CutScreen` dan tidak dianimasikan saat playback.
**Seharusnya:** Keyframe perlu dirender di atas track klip di timeline dan diterapkan interpolasi per frame saat export.

---

### 7. Speed/Duration Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Masalah:** Tombol Speed di Action Island ada tapi hanya menampilkan icon tanpa dialog atau aksi nyata. Tidak ada logic untuk mengubah `durationMs` klip sesuai speed multiplier.
**Seharusnya:** Dialog pilihan speed (0.25x, 0.5x, 1x, 1.5x, 2x, 4x) yang mengupdate `durationMs` klip dan diterapkan saat playback/export.

---

### 8. Volume / Mute Audio Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Masalah:** Tombol Mute/Volume ada tapi tidak memengaruhi `ExoPlayer.volume` pada clip yang bersangkutan.
**Seharusnya:** Sinkronkan `isMuted` / `volumeLevel` di `TimelineClip` model ke `ExoPlayer.volume`.

---

### 9. TitlingScreen — Drag Element di Canvas Tidak Presisi
**Lokasi:** `TitlingScreen.kt`
**Masalah:** `detectDragGestures` untuk memindahkan title element sudah ada, tapi offset `IntOffset` dihitung dengan nilai hardcoded `* 240` / `* 160` yang tidak sebanding dengan ukuran layar nyata — drag terasa melompat / tidak akurat.
**Seharusnya:** Gunakan `BoxWithConstraints` untuk mendapatkan `maxWidth`/`maxHeight` piksel nyata dan hitung offset sebagai fraksi dari ukuran video viewport.

---

### 10. Undo/Redo Stack Hanya Label, Tidak Ada State Rollback
**Lokasi:** `ArunikaMainShell.kt`
**Masalah:** `undoStack` dan `redoStack` menyimpan string label saja. Tombol Undo/Redo menampilkan toast "Undo: ..." tapi **tidak membalikkan perubahan apapun** (tidak ada snapshot state timeline/color/titling).
**Seharusnya:** Simpan snapshot `List<TimelineClip>` / `ColorStudioState` per aksi, lalu restore saat undo.

---

## 🟡 MINOR — UI/UX Belum Sempurna

### 11. Seek Bar / Scrubbing di Timeline Belum Ada
**Lokasi:** `CutScreen.kt`
**Masalah:** Tidak ada cara untuk men-drag playhead di timeline ke posisi tertentu secara manual (scrubbing).

### 12. Thumbnail Klip di Timeline Kosong
**Lokasi:** `CutScreen.kt`
**Masalah:** Klip video di timeline hanya menampilkan warna solid + nama file. Tidak ada thumbnail frame yang diambil via `MediaMetadataRetriever`.

### 13. Split Clip Tidak Memotong URI / Durasi Fisik
**Lokasi:** `CutScreen.kt`
**Masalah:** Split Clip memotong `durationMs` secara logis tapi kedua bagian masih mereferensikan URI yang sama dengan offset tidak diatur — saat playback, kedua bagian memutar dari awal.

### 14. ProjectListScreen — Thumbnail & Preview Statis
**Lokasi:** `ProjectListScreen.kt`
**Masalah:** Thumbnail projek menggunakan `Box` berwarna solid. Tidak ada thumbnail dari frame pertama video klip pertama.

---

## 📋 Prioritas Perbaikan yang Disarankan

| Prioritas | Fitur | Kompleksitas |
|-----------|-------|-------------|
| 1 | Color Grade pipeline nyata (ExoPlayer → GLSurfaceView / SurfaceTexture + Canvas Shader) | Tinggi |
| 2 | AI Shot Matcher — sambungkan ke tombol Auto-Match | Sedang |
| 3 | Export nyata via MediaCodec + MediaMuxer | Tinggi |
| 4 | Audio EQ/DSP via Android `Equalizer` API | Sedang |
| 5 | Speed/Duration clip + Volume/Mute | Rendah |
| 6 | Undo/Redo dengan state snapshot | Sedang |
| 7 | Thumbnail klip timeline via MediaMetadataRetriever | Rendah |
| 8 | Scrubbing playhead (drag) | Rendah |
