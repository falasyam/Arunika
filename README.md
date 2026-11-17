# Lumina Studio — Pro Mobile Video Suite

Lumina Studio adalah rangkaian produksi video sinematik mobile kelas profesional yang dirancang khusus untuk smartphone flagship (Android / Jetpack Compose & NDK).

---

## 🚀 Fitur Unggulan

### 1. ✂️ Cut Page (Magnetic Timeline)
- **Multi-Track Editing**: Track V1 (Video Utama) dan Track V2 (Titles, Subtitle Overlays).
- **Magnetic Trimming & Ripple Delete**: Klip otomatis merapat saat dipotong (*Split*) atau dihapus.
- **Speed Ramping**: Kontrol kecepatan 0.5x, 1.0x, 1.5x, 2.0x.
- **Diamond Keyframing (`◆`)**: Kemampuan memberi titik animasi pada klip untuk Pan (X/Y), Zoom/Scale, Rotasi, dan Opacity dengan kurva *Cosine Smooth Ease*.
- **Ken Burns Motion**: Preset otomatis kamera sinematik *Slow Push-in Zoom*.

### 2. 🎨 Color Studio (DaVinci Resolve Style)
- **DaVinci Node Tree Architecture**: Pipeline koreksi serial modular (`[IN] ➔ [01: CST] ➔ [02: Primaries] ➔ [03: Film Look] ➔ [04: Vignette] ➔ [OUT]`).
- **Per-Node Bypass (LED Switch)**: Saklar on/off instan pada tiap node grading tanpa merusak koreksi lainnya.
- **3-Way Color Wheels**: Lift (Shadows), Gamma (Midtones), Gain (Highlights), dan Offset (Global).
- **Universal Multi-Format LUT Importer**:
  - `.cube` (Adobe / DaVinci Resolve 3D & 1D LUT)
  - `.3dl` (Autodesk Lustre / Flame)
  - `.vlt` (Panasonic Varicam / Lumix)
  - `.png` / `.jpg` (Identity Hald CLUT)
- **Pro Scopes**: Real-time Waveform Luma dan Vectorscope IRE.
- **Split-Screen Wipe Before/After**: Pembanding live hasil grading vs raw log.
- **Camera Log CST**: Profil input Samsung D-Log, Apple Log, Sony S-Log3, dan Rec.709.

### 3. 💬 Fusion Lite Titling & Captions
- **On-Screen Interactive Draggable Overlay**: Drag & drop posisi teks langsung di atas monitor video.
- **Cinematic Lower-Thirds & Opening Titles**: Template tipografi siap pakai.
- **Auto-Subtitles / Viral Captions**: Subtitle dinamis dengan highlight kata per kata (warna kuning mencolok ala Reels/Shorts/TikTok).

### 4. 🎚️ Fairlight Lite Audio Suite
- **Stereo VU Meters**: Animasi L & R level bar RMS dBFS dan Peak Hold.
- **3-Band Equalizer**: Bass (Low-shelf), Mid (Parametric bell), Treble (High-shelf).
- **AI Voice Isolation & De-Reverb**: Simulasi isolasi vokal jernih bebas noise latar.

### 5. ⚡ 32-bit Floating-Point GPU Shader Engine & NDK
- **GLSL ES 3.0 Fragment Shaders**: Kalkulasi matriks warna 32-bit presisi tinggi, White Balance Kelvin, interpolasi 3D LUT Tetrahedral hardware, ACES Filmic tone mapping, vignette, dan film grain analog.
- **Android Zero-Copy Pipeline**: MediaCodec hardware decoding langsung ke OpenGL ES 3.2 FBO tanpa buffer copy di CPU.

### 6. 📦 Deliver Page
- Master rendering ke 4K UHD (2160p), 1080p FHD, dan 9:16 Shorts/Reels.
- Pilihan codec H.265 (HEVC 10-bit), H.264, dan ProRes.
- Kontrol bitrate adaptif (8–80 Mbps).

---

## 📁 Struktur Repositori

```
lumina-studio/
├── compose/
│   ├── model/
│   │   ├── AppState.kt           # State global aplikasi & timeline
│   │   ├── ColorGradeState.kt    # DaVinci Node Tree & Wheels state
│   │   ├── LutParser.kt          # Parser multi-format (.cube, .3dl, .vlt)
│   │   ├── TitlingState.kt       # State overlay teks & subtitle
│   │   ├── KeyframeState.kt      # Interpolator transform keyframe
│   │   └── GlslShaderEngine.kt   # JNI bridge ke native GPU engine
│   ├── ui/
│   │   ├── LuminaMainShell.kt    # Navigation shell M3
│   │   ├── ColorStudioScreen.kt  # Layar Color Grading & Node strip
│   │   ├── components/           # Color Wheels, Scopes, Spline Curves
│   │   └── screens/
│   │       ├── CutScreen.kt      # Magnetic Timeline & Keyframing
│   │       ├── AudioScreen.kt    # Fairlight VU Meters & 3-Band EQ
│   │       ├── DeliverScreen.kt  # 4K 10-bit Export dialog
│   │       └── TitlingScreen.kt  # Draggable Title overlay editor
│   ├── shaders/
│   │   ├── passthrough.vert      # Vertex shader passthrough
│   │   └── color_grade_32bit.frag# 32-bit GPU fragment shader
│   └── NDK_ARCHITECTURE.md       # Dokumentasi C++ Vulkan/OpenGL ES
├── sample-luts/                  # Sampel file nyata .cube, .3dl, .vlt
└── web-preview/
    ├── index.html                # Prototipe interaktif web lengkap
    └── sample-luts/              # Asset LUT untuk pengujian browser
```
