# 🐛 Arunika — Audit Fitur: Ada Tapi Belum Berfungsi

> Status per commit `50a7f20` (22 Nov 2026)

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
**Status:** 🟡 **PARSIAL**
- ✅ EQ Bass/Mid/Treble dan master volume dihubungkan ke audio session ExoPlayer melalui Android `Equalizer` dan `ExoPlayer.volume`.
- ✅ VU Meter membaca capture FFT dari Android `Visualizer` saat tersedia.
- ⚠️ Voice Isolation, De-Reverb, dan Auto-Ducking masih hanya mengubah state UI; belum ada pemrosesan DSP/model audio.
- ⚠️ Meter masih fallback ke simulasi jika `Visualizer`/audio session tidak tersedia dan belum mengukur kanal stereo terpisah secara akurat.
**Sisa:** Implementasikan pemrosesan Voice Isolation, De-Reverb, Auto-Ducking, serta validasi audio processing pada perangkat.

---

### 6. Keyframe Engine Tidak Terhubung ke Timeline
**Lokasi:** `KeyframeState.kt`
**Status:** 🟡 **PARSIAL**
- ✅ Marker `◆` ditampilkan pada klip timeline dan tombol keyframe dapat menambah keyframe scale di playhead.
- ✅ Hasil interpolasi diterapkan pada transform overlay title di preview Cut saat posisi playhead berubah.
- ⚠️ Keyframe belum diterapkan ke hasil export; kontrol untuk semua properti dan editing keyframe di timeline juga belum lengkap.

---

### 7. Speed/Duration Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Status:** 🟡 **PARSIAL**
- ✅ Dialog menyediakan pilihan 0.25x, 0.5x, 1x, 1.5x, 2x, dan 4x serta memperbarui durasi logis klip.
- ✅ Speed dan source offset disinkronkan ke preview ExoPlayer.
- ⚠️ Speed belum diterapkan pada export dan belum membentuk playback multi-clip berurutan yang sesungguhnya.

---

### 8. Volume / Mute Audio Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Status:** 🟡 **PARSIAL**
- ✅ Kontrol volume clip mengubah nilai `TimelineClip.volume`; nilai clip video aktif diterapkan ke `ExoPlayer.volume`.
- ⚠️ Audio clip pada track A1 belum diputar oleh engine timeline, sehingga volume/mute audio clip belum memengaruhi playback.
- ⚠️ Volume per clip belum diterapkan saat export.

---

### 9. TitlingScreen — Drag Element di Canvas Tidak Presisi
**Lokasi:** `TitlingScreen.kt`
**Status:** ✅ **SELESAI**
- Canvas menggunakan ukuran aktual untuk menghitung offset dan delta drag sebagai fraksi viewport; posisi dibatasi dalam rentang 0–1.
- Overlay title pada preview Cut memakai pemetaan relatif yang sama.

---

### 10. Undo/Redo Stack Hanya Label, Tidak Ada State Rollback
**Lokasi:** `ArunikaMainShell.kt`
**Status:** 🔴 **BELUM SELESAI**
- Snapshot untuk sebagian state Audio, Color, Titling, dan Keyframe serta restore dasar sudah ditambahkan.
- ⚠️ Snapshot belum disimpan sebelum setiap perubahan editor, timeline belum termasuk snapshot, dan perubahan jumlah node Color tidak dipulihkan.
- Karena itu tombol Undo/Redo belum dapat membatalkan/mengulangi seluruh aksi editor dengan benar.
**Sisa:** Catat snapshot sebelum mutasi state, cakup timeline dan semua state editor, lalu uji urutan undo/redo.

---

## 🟡 MINOR — UI/UX Belum Sempurna

### 11. Seek Bar / Scrubbing di Timeline Belum Ada
**Lokasi:** `CutScreen.kt`
**Status:** ✅ **SELESAI**
- Drag pada area timeline memindahkan playhead secara manual dan membatasi posisi ke durasi timeline.

### 12. Thumbnail Klip di Timeline Kosong
**Lokasi:** `CutScreen.kt`
**Status:** ✅ **SELESAI**
- Thumbnail frame video diekstrak dengan `MediaMetadataRetriever` dan ditampilkan pada klip yang memiliki URI video.
- Klip preset/foto tanpa URI video tetap menggunakan warna dasar.

### 13. Split Clip Tidak Memotong URI / Durasi Fisik
**Lokasi:** `CutScreen.kt`
**Status:** 🟡 **PARSIAL**
- ✅ Bagian kedua menyimpan `sourceOffsetMs`; preview menghitung posisi seek sumber berdasarkan offset dan speed.
- ⚠️ URI tetap sama dan hasil export belum memotong sumber menjadi dua media fisik; transisi playback multi-clip belum terintegrasi.

### 14. ProjectListScreen — Thumbnail & Preview Statis
**Lokasi:** `ProjectListScreen.kt`
**Status:** ✅ **SELESAI**
- Thumbnail frame diambil dari klip pertama yang memiliki URI dan ditampilkan pada kartu project.
- Project yang belum memiliki URI video tetap menggunakan gradient bawaan.

### 15. Klip Import Hilang Setelah Berpindah Tab
**Lokasi:** `ArunikaMainShell.kt`, `CutScreen.kt`
**Status:** ✅ **SELESAI DI WORKTREE**
- State klip video/foto dan audio sekarang di-hoist ke editor shell dan diteruskan kembali ke Cut saat tab dibuka lagi.
- Berlaku selama sesi editor/projek aktif; persistensi setelah aplikasi ditutup atau projek dibuka ulang belum termasuk perbaikan ini.

### 16. Lokasi File Export
**Lokasi:** `DeliverScreen.kt`
**Status:** ✅ **SELESAI DI WORKTREE**
- File render dipublikasikan ke `Movies/Arunika` agar terlihat di penyimpanan media perangkat.
- Android 10+ menggunakan MediaStore; Android versi lama menggunakan folder publik dan meminta izin penyimpanan bila diperlukan.

---

## 📋 Prioritas Perbaikan yang Disarankan

> Prioritas lama di bawah ini mencerminkan audit awal; fitur yang sudah diperbaiki tercatat dengan status masing-masing di atas.

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
