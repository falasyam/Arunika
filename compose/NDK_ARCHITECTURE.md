# Lumina Studio — Native NDK & 32-Bit GPU Shader Architecture

Dokumen ini mendefinisikan arsitektur rendering performa tinggi berbasis C++ NDK, OpenGL ES 3.2, dan Vulkan Compute untuk Lumina Studio di Android.

---

## 1. Zero-Copy Hardware Pipeline Overview

```
[Camera / Storage 4K 10-bit Video]
             │
             ▼
[Android MediaCodec (Hardware H.265/AV1 Decoder)]
             │ SurfaceTexture (GL_TEXTURE_EXTERNAL_OES)
             ▼
[OpenGL ES 3.2 / EGL Context (EGL_EXT_image_dma_buf)]
             │
             ├──► [Vertex Shader: Transform, Rotation & Keyframing]
             │
             ├──► [Fragment Shader: 32-bit Float Color Grading]
             │      ├── 3-Way Color Wheels (Lift, Gamma, Gain, Offset)
             │      ├── Kelvin White Balance Matrix
             │      ├── Tetrahedral 3D LUT Interpolation (`sampler3D`)
             │      └── ACES Filmic Tone Mapping Curve
             │
             ├──► [Track V2 Compositor: Titles & Subtitle Overlays]
             │
             ▼
[Hardware Encoder (MediaCodec Surface Input)] ──► [Master 4K Video Output]
```

---

## 2. Fitur Utama

1. **32-Bit Floating Point FBO (`GL_RGBA32F` / `GL_RGBA16F`)**:
   - Menghindari *color banding* dan pemotongan bit (*clipping*) saat menggrade video 10-bit Apple Log dan Samsung D-Log.
2. **Hardware 3D Texture Interpolator (`GL_TEXTURE_3D`)**:
   - LUT 3D format `.cube` dan `.3dl` diunggah ke VRAM GPU sebagai texture 3D, memungkinkan interpolasi trilinear hardware tanpa membebani CPU.
3. **Target Performa**:
   - 4K UHD 60fps rendering real-time pada chipset Snapdragon 8 Gen 2 / Gen 3 / Exynos 2400.
   - Zero-copy memory sharing antar decoder dan renderer via `AHardwareBuffer`.
