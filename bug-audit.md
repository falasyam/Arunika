# 🐛 Arunika — Audit Fitur: Ada Tapi Belum Berfungsi

> Audit awal dibuat pada commit `50a7f20` (22 Nov 2026); status tiap item diperbarui mengikuti implementasi saat ini.

---

## 🔴 KRITIS — Fungsionalitas Inti Rusak (SEMUA SELESAI DIPERBAIKI ✅)

### 1. Color Grading Tidak Mempengaruhi Video Nyata
- **Status:** ✅ **SELESAI** (Commit `fec8a29`)
- **Solusi yang Diterapkan:** `ArunikaColorEffect` menerapkan CST, Lift, Gamma, Gain, Temperature, Saturation, Contrast, dan preset bawaan melalui matrix GPU. Custom LUT kini memakai data LUT yang diimpor melalui shader `SingleColorLut`, termasuk intensity blend, pada preview dan export.

---

### 2. GPU Shader Engine (NDK / C++)
- **Status:** ✅ **SELESAI & AMAN** (Commit `082cdc4`)
- **Solusi yang Diterapkan:** Library C++ NDK (`arunika_native_engine.cpp`) aktif dan terkompilasi melalui CMake untuk arm64-v8a, armeabi-v7a, dan x86_64. Ditambahkan safeguard try-catch dan flag `isLibraryLoaded` pada `GlslShaderEngine.kt` agar tidak crash saat runtime jika dipanggil di luar konteks GL.

---

### 3. Export Video Hanya Simulasi (Fake Progress Bar)
- **Status:** ✅ **SELESAI** (Commit `feb39fd`)
- **Solusi yang Diterapkan:** Mengimplementasikan hardware transcoding nyata menggunakan `androidx.media3.transformer.Transformer` & `MediaCodec` di `DeliverScreen.kt`. Color grading matrix dan custom LUT dibakar (*baked*) ke video hasil render, mendukung output H.264 & H.265 (HEVC), estimasi bitrate nyata, progress bar real-time dari `ProgressHolder`, penyimpanan otomatis ke direktori Movies, dan registrasi ke MediaStore.

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
- ✅ Hasil interpolasi keyframe untuk posisi, skala, rotasi, dan opasitas dibakar ke overlay title saat export klip video/foto dan jeda timeline.
- ✅ Title, pemunculan berdasarkan frame/durasi, font, warna, serta animasi masuk diterapkan pada hasil export.
- ✅ Overlay export menggunakan bitmap Canvas agar title kosong, teks biasa, dan caption tidak bergantung pada pengukuran layout teks Media3.
- ⚠️ Kontrol untuk semua properti serta editing keyframe langsung di timeline masih belum lengkap.

---

### 7. Speed/Duration Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Status:** ✅ **SELESAI**
- ✅ Dialog menyediakan pilihan 0.25x, 0.5x, 1x, 1.5x, 2x, dan 4x serta memperbarui durasi logis klip.
- ✅ Preview berpindah mengikuti klip di bawah playhead dan menerapkan source offset, speed, serta volume klip aktif.
- ✅ Export menyusun klip video/foto secara berurutan; speed diterapkan ke timestamp video dan pemrosesan audio tiap klip.

---

### 8. Volume / Mute Audio Clip Tidak Berfungsi
**Lokasi:** `CutScreen.kt` — Floating Action Island
**Status:** 🟡 **PARSIAL**
- ✅ Kontrol volume clip mengubah nilai `TimelineClip.volume`; nilai clip video aktif diterapkan ke `ExoPlayer.volume`.
- ✅ Export menerapkan volume per klip (termasuk track A1) dan master volume; mute berarti gain nol.
- ⚠️ Track A1 belum diputar dalam preview timeline; Auto-Ducking juga belum berjalan saat preview.

---

### 9. TitlingScreen — Drag Element di Canvas Tidak Presisi
**Lokasi:** `TitlingScreen.kt`
**Status:** ✅ **SELESAI**
- Canvas menggunakan ukuran aktual untuk menghitung offset dan delta drag sebagai fraksi viewport; posisi dibatasi dalam rentang 0–1.
- Overlay title pada preview Cut memakai pemetaan relatif yang sama.

---

### 10. Undo/Redo Stack Hanya Label, Tidak Ada State Rollback
**Lokasi:** `ArunikaMainShell.kt`
**Status:** ✅ **SELESAI**
- Riwayat mencatat otomatis perubahan state editor yang diamati Compose; perpindahan tab tidak lagi membuat entri Undo palsu.
- Snapshot mencakup state Audio, Color beserta jumlah/tipe node dan LUT kustom, Titling, Keyframe, serta klip video dan audio pada timeline.
- Undo/Redo memulihkan snapshot penuh; riwayat dibatasi hingga 100 langkah.

---

## 🟡 MINOR — UI/UX Belum Sempurna

### 11. Seek Bar / Scrubbing di Timeline Belum Ada
**Lokasi:** `CutScreen.kt`
**Status:** ✅ **SELESAI**
- Drag pada area timeline menggeser track di bawah playhead yang tetap; klip diposisikan menurut `startOffsetMs` pada skala waktu yang sama. Uji pada Pixel 6a memastikan klip terakhir tetap terlihat saat playhead mencapai ujung timeline, dan playback berhenti tanpa mengulang.

### 12. Thumbnail Klip di Timeline Kosong
**Lokasi:** `CutScreen.kt`
**Status:** ✅ **SELESAI**
- Thumbnail frame video diekstrak dengan `MediaMetadataRetriever` dan ditampilkan pada klip yang memiliki URI video.
- Klip preset/foto tanpa URI video tetap menggunakan warna dasar.

### 13. Split Clip Tidak Mengikuti Playhead
**Lokasi:** `CutScreen.kt`
**Status:** ✅ **SELESAI**
- Posisi split mengikuti playhead selama berada di dalam klip, bukan selalu membagi durasi klip menjadi dua; tombol dinonaktifkan di luar batas klip.
- Bagian kedua menyimpan `sourceOffsetMs` berdasarkan posisi split dan kecepatan klip. Offset klip lain dipertahankan agar overlap audio maupun susunan track tidak berubah.
- Export membentuk segmen dengan clipping start/end sumber dan menyusun hasilnya berurutan; tidak membuat file sumber antara.

### 14. ProjectListScreen — Thumbnail & Preview Statis
**Lokasi:** `ProjectListScreen.kt`
**Status:** ✅ **SELESAI**
- Thumbnail frame diambil dari klip pertama yang memiliki URI dan ditampilkan pada kartu project.
- Project yang belum memiliki URI video tetap menggunakan gradient bawaan.

### 15. Klip Import Hilang Setelah Berpindah Tab
**Lokasi:** `ArunikaMainShell.kt`, `CutScreen.kt`
**Status:** ✅ **SELESAI**
- State klip video/foto dan audio sekarang di-hoist ke editor shell dan diteruskan kembali ke Cut saat tab dibuka lagi.
- Daftar projek dan state editor terakhir (termasuk klip, audio, titling, keyframe, serta grading) disimpan otomatis dan dipulihkan saat projek dibuka lagi.
- Import memakai pemilih dokumen dengan izin baca URI persisten agar media tetap dapat diakses setelah aplikasi ditutup.

### 16. Lokasi File Export
**Lokasi:** `DeliverScreen.kt`
**Status:** ✅ **SELESAI DI WORKTREE**
- File render dipublikasikan ke `Movies/Arunika` agar terlihat di penyimpanan media perangkat.
- Android 10+ menggunakan MediaStore; Android versi lama menggunakan folder publik dan meminta izin penyimpanan bila diperlukan.

### 17. App Force-Close karena OutOfMemoryError
**Lokasi:** `CutScreen.kt`, `ProjectListScreen.kt`, `EditorAutosave.kt`
**Status:** 🟡 **PERBAIKAN DI WORKTREE, VALIDASI PERANGKAT PENDING**
- Log Pixel 6a mencatat tiga crash `OutOfMemoryError` pada 12:39 dan 12:42; satu terjadi di thread ExoPlayer, satu pada Binder, dan satu saat SharedPreferences menulis autosave. Stack trace menunjukkan heap sudah hampir habis; belum membuktikan ExoPlayer sebagai sumber awal tekanan memori.
- Thumbnail video sebelumnya menyimpan frame resolusi penuh untuk banyak klip. Ekstraksi kini dibatasi hingga 320 px, dijalankan di IO, dan bitmap dibersihkan saat klip atau layar dilepas.
- Autosave kini menulis stream serialisasi secara atomik ke file, dengan migrasi dari format SharedPreferences lama.
- Perlu diuji pada perangkat dengan proyek berisi media resolusi tinggi untuk memastikan crash tidak berulang.

### 18. Force-Close saat Mengedit Text & Style
**Lokasi:** `TitlingScreen.kt`, `VideoPlayerView.kt`
**Status:** ✅ **DIPERBAIKI & DIUJI DI PERANGKAT**
- Log perangkat saat reproduksi menunjukkan heap mencapai 255/256 MB sebelum `OutOfMemoryError`. Sebelumnya Cut menggunakan ExoPlayer bersama, sedangkan preview Titling membuat player/decoder lokal kedua.
- Preview Titling kini memakai ExoPlayer bersama editor agar perpindahan tab tidak membuka decoder video tambahan.
- Pixel 6a: setelah instalasi build terbaru, membuka Text & Style dan mengetik teks tidak menutup aplikasi; proses tetap hidup dan tidak ada `FATAL EXCEPTION`/OOM pada validasi tersebut.

### 19. Project List Tidak Menampilkan Thumbnail dan Jumlah Media
**Lokasi:** `ArunikaMainShell.kt`, `ProjectListScreen.kt`
**Status:** ✅ **DIPERBAIKI & DIUJI DI PERANGKAT**
- Klip hasil edit hanya disimpan di state editor, tidak diperbarui ke `ProjectItem` yang ditampilkan Project List.
- Perubahan klip kini disinkronkan kembali ke project beserta durasi; kartu menampilkan jumlah video, foto, dan audio secara terpisah.
- Thumbnail diambil dari klip bertipe video yang memiliki URI. Pixel 6a menampilkan thumbnail aktual dan hitungan `1 Video • 0 Foto • 1 Audio` setelah kembali dari editor.

### 20. Custom LUT Tidak Ikut ke Export
**Lokasi:** `ArunikaColorEffect.kt`, `ArunikaLutEffect.kt`, `VideoPlayerView.kt`, `DeliverScreen.kt`
**Status:** ✅ **DIPERBAIKI; EXPORT SDR DIUJI DI PERANGKAT**
- Sebelumnya custom `.cube` hanya mendapat matrix kontras generik; nilai LUT yang diparsing tidak diterapkan ke frame export.
- Data LUT 1D/3D kini diubah menjadi LUT GPU Media3, dengan intensity blend, dan dipasang pada pipeline preview serta export. Export memberi error eksplisit bila data custom LUT tidak tersedia.
- Pixel 6a: export H.264 SDR dengan custom LUT selesai dan file muncul di `Movies/Arunika`. HDR masih mengikuti keterbatasan dukungan `SingleColorLut` pada Media3 1.4.1.

### 21. Titling: Opacity, Drag, Keyboard, dan Timeline
**Lokasi:** `TitlingScreen.kt`, `CutScreen.kt`, `TitlingState.kt`
**Status:** ✅ **DIPERBAIKI; BUILD BERHASIL**
- Opacity elemen teks diterapkan pada preview Titling dan Cut; drag memakai state elemen terbaru agar perubahan teks/gaya tidak tertimpa posisi lama.
- Keyboard mendapat ruang melalui inset IME; editor teks tetap dapat digulir pada tinggi layar yang tersisa.
- Teks dapat dibuat tanpa template dan memilih font bawaan Sans, Serif, Mono, Cinematic, atau Handwritten.
- Lane title V2 di timeline utama dapat menambah teks pada playhead, memindahkan klip teks, serta mengatur durasi lewat handle kanan. Preview Cut dan export mengikuti rentang waktu title.
- Belum divalidasi secara interaktif pada perangkat setelah perubahan ini.

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
