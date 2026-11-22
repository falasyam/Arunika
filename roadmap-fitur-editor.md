# Roadmap Fitur Arunika

## Visi Produk

Arunika diarahkan menjadi editor video Android yang menggabungkan:

- **Kemudahan produksi cepat**: impor, potong, caption, format sosial, dan ekspor dengan langkah yang jelas.
- **Kendali pascaproduksi**: color grading berbasis node, scopes, keyframe, serta kontrol audio per track.
- **Workflow mobile-first** yang tetap produktif pada tablet dan layar lebar.

CapCut dan DaVinci Resolve menjadi inspirasi kategori workflow—bukan target untuk menyalin tampilan atau aset mereka. Arunika perlu mempertahankan identitas dan pola interaksinya sendiri.

## Prinsip Produk

1. **Stabil sebelum canggih** — tidak boleh kehilangan proyek atau force-close saat mengimpor, mengedit, maupun mengekspor.
2. **Cepat untuk pemula, bertahap untuk profesional** — tampilkan tindakan umum lebih dulu; kontrol lanjutan tetap mudah ditemukan.
3. **Non-destruktif** — trim, split, grading, title, dan audio disimpan sebagai instruksi edit, bukan mengubah media sumber.
4. **Jujur tentang kemampuan** — jangan menampilkan hasil simulasi sebagai pemrosesan nyata; jelaskan bila fitur atau codec tidak tersedia.
5. **Offline-first dan privat** — pemrosesan AI lokal menjadi pilihan utama; jelaskan dan minta persetujuan sebelum memakai layanan cloud.
6. **Konsisten dari preview ke export** — warna, timing, audio, dan rasio aspek harus sesuai hasil render.

## Kondisi Saat Ini

Rincian status implementasi ada di [bug-audit.md](./bug-audit.md). Audit mencatat fondasi timeline, split, speed, titling, grading, autosave, dan export; sebagian jalur runtime masih perlu diuji dengan media nyata.

Prioritas yang masih terbuka:

- Timeline saat ini berpusat pada satu track video/foto V1 dan satu track audio A1; V2 dipakai untuk title, belum menjadi track overlay video umum.
- Preview audio A1 belum diputar dan disinkronkan pada timeline.
- Klip video/foto yang bertumpuk dan beberapa track audio yang diputar bersamaan belum menjadi workflow multitrack penuh.
- Voice Isolation, De-Reverb, dan Auto-Ducking belum menjadi pemrosesan audio nyata.
- Kontrol untuk mengedit seluruh properti keyframe belum lengkap.
- Log perangkat baru saja mencatat beberapa crash `OutOfMemoryError`; perbaikan memori dan autosave menunggu validasi ulang pada perangkat.
- Export title, audio, dan klip beresolusi tinggi perlu pengujian end-to-end.

## Roadmap Prioritas

### P0 — Stabilitas dan Kepercayaan

Selesaikan sebelum menambah efek atau fitur AI baru.

- **Validasi OOM dan lifecycle media**
  - Uji proyek dengan banyak klip, video resolusi tinggi, perpindahan tab, dan kembali dari background.
  - Pastikan thumbnail dibatasi ukurannya, dilepas saat tidak dipakai, dan tidak menghambat UI.
  - Pantau heap, bitmap, decoder, dan proses export; tampilkan error yang dapat dipulihkan.
- **Autosave yang bisa dipulihkan**
  - Uji migrasi data autosave lama, force-stop, restart perangkat, dan pergantian proyek.
  - Simpan snapshot secara atomik; jangan menganggap save berhasil sebelum operasi selesai.
  - Pertahankan backup snapshot terakhir yang valid untuk pemulihan setelah data rusak.
- **Status media yang jelas**
  - Tampilkan indikator ketika URI sumber tidak lagi dapat dibaca.
  - Berikan tindakan untuk mencari ulang file tanpa menghapus edit timeline.
- **Export nyata sebagai release gate**
  - Uji V1 dengan klip overlay V2, title/keyframe, serta A1 dan A2 yang bertumpuk, termasuk trim/split, mute, gain, dan master volume dalam satu render.
  - Tampilkan progress, estimasi ukuran, ruang penyimpanan, pembatalan, dan pesan kegagalan dengan tindakan lanjut.

**Kriteria selesai:** proyek tersimpan dan dapat dibuka kembali; tidak ada kehilangan edit; pengujian perangkat tidak menghasilkan OOM; hasil export cocok dengan preview dan parameter yang dipilih.

### P1 — Fondasi Editing yang Nyaman

- **Timeline**
  - Tambahkan trim handles untuk awal/akhir klip, drag untuk menyusun ulang, dan snap ke playhead/tepi klip/marker.
  - Sediakan zoom, fit-to-project, marker, serta navigasi cepat ke awal/akhir.
  - Bedakan dengan jelas select, move, trim, dan scrub agar gestur tidak saling bertabrakan.
- **Overlay klip video/foto**
  - Jadikan model timeline track-aware: track memiliki ID, tipe, urutan layer, dan state; klip menyimpan posisi timeline serta rentang sumbernya.
  - Tambahkan track overlay V2 dan seterusnya agar B-roll, cutaway, reaction, logo, dan gambar dapat berjalan bersamaan di atas V1.
  - Pertahankan urutan layer; sediakan visibilitas, lock, duplicate, pindah track, serta trim klip secara nondestruktif.
  - Izinkan penyesuaian posisi, skala, rotasi, crop/fit, dan opacity pada klip overlay; dukung keyframe pada properti yang relevan.
  - Preview dan export harus mengomposisikan layer dengan urutan dan timing yang sama, termasuk saat klip bertumpuk.
- **Overlay audio dan multitrack**
  - Tambahkan track audio A2 dan seterusnya untuk musik, voiceover, ambience, serta sound effect yang dapat bertumpuk pada waktu yang sama.
  - Setiap klip audio memiliki source in/out, timeline offset, trim, gain, mute, dan fade in/out; sediakan mute/solo per track.
  - Campur semua track pada preview dan export dengan sinkronisasi terhadap playhead; lindungi output dari clipping dan tampilkan waveform aktual.
  - Jadikan jumlah track dapat diperluas sesuai kemampuan perangkat, dengan batas resource yang jelas dan feedback bila batas tercapai.
- **Audio A1 pada preview**
  - Putar dan seek audio mengikuti playhead, termasuk offset, split, speed, mute, dan gain.
  - Sediakan kontrol volume per klip dan master dengan indikator mute yang jelas.
  - Tampilkan waveform hasil analisis media, bukan waveform dekoratif.
- **Media bin**
  - Kelompokkan video, foto, audio, title, dan hasil render.
  - Tampilkan durasi, resolusi, frame rate, status offline, dan penggunaan proxy.
- **Undo/Redo**
  - Kelompokkan satu gestur menjadi satu aksi riwayat.
  - Pastikan undo/redo memulihkan timeline, audio, title, keyframe, dan grading tanpa membuat entri palsu saat pindah tab.

**Kriteria selesai:** aksi edit umum dapat dilakukan tanpa dialog panjang; scrubbing tetap akurat saat zoom berubah; playback audio tidak drift secara nyata selama sesi edit panjang; klip overlay tampil di atas V1 dan beberapa klip audio dapat diputar bersamaan.

### P2 — Mode Studio: Color, Title, dan Audio

- **Keyframe editor**
  - Edit posisi, skala, rotasi, opacity, dan properti title pada playhead.
  - Tambahkan keyframe, pilih/mindahkan/menghapus marker, serta pilih interpolasi linear atau easing.
  - Tampilkan kurva sederhana untuk pengguna lanjutan tanpa menghalangi edit dasar.
- **Color Studio**
  - Lengkapi node enable/bypass, rename, reorder, dan reset per-node.
  - Tambahkan scopes yang benar-benar membaca frame aktif serta kontrol before/after.
  - Pertahankan ruang warna input/output dan LUT yang sama di preview dan export.
- **Titling dan captions**
  - Sediakan template yang dapat diedit, area aman untuk format sosial, dan kontrol durasi di timeline.
  - Beri editor caption untuk koreksi teks dan timing; dukung ekspor subtitle bila format render memungkinkan.
- **Audio Studio**
  - Tambahkan mixer per-track, pan, gain automation, dan ducking berbasis level dialog dengan opsi manual override.
  - Implementasikan De-Reverb dan Voice Isolation sebagai DSP/model nyata, dengan status pemrosesan dan batasan perangkat.
  - Jangan mengganti meter aktual dengan angka simulasi tanpa label yang terlihat.

**Kriteria selesai:** perubahan terlihat konsisten di preview dan render; setiap kontrol yang ditampilkan memengaruhi output atau secara eksplisit ditandai belum tersedia.

### P3 — Performa, AI, dan Workflow Sosial

- **Proxy dan optimized media** untuk edit lancar pada video resolusi tinggi, dengan relink ke sumber saat export.
- **Analisis media opsional**: transkripsi caption, deteksi scene, reframing, dan shot matching.
- **AI lokal lebih dulu** untuk materi privat; tampilkan kebutuhan ruang, baterai, waktu proses, dan dukungan perangkat sebelum memulai.
- **Preset export yang dapat disimpan** untuk 16:9, 9:16, 1:1, dan rasio kustom; validasi dimensi, frame rate, codec, audio, dan metadata.
- **Antrian render** untuk beberapa export, dengan cancel/retry dan file parsial yang dibersihkan dengan aman.

**Kriteria selesai:** fitur AI dan proxy tidak mengubah media sumber, dapat dibatalkan, dan memberi fallback yang jelas pada perangkat yang tidak mendukung.

## Arah UI/UX

### Ponsel

- Pertahankan preview sebagai fokus, dengan timeline dan transport control mudah dijangkau.
- Letakkan alat kontekstual dekat objek terpilih; gunakan bottom sheet untuk kontrol lanjutan.
- Gunakan label pada tindakan penting seperti Split, Trim, Mute, dan Export; jangan mengandalkan ikon atau warna saja.
- Sediakan **Quick Edit** untuk template, caption, rasio, dan export cepat; buka kontrol Studio secara bertahap.

### Tablet dan Layar Lebar

- Gunakan layout adaptif berdasarkan lebar window, bukan sekadar orientasi portrait/landscape.
- Saat ruang cukup, tampilkan Media Bin di kiri, preview di tengah, Inspector di kanan, dan timeline di bawah.
- Tampilkan V1 dan track overlay sebagai lane terpisah dengan kontrol layer; tampilkan A1, A2, dan track audio tambahan dengan kontrol mute/solo yang mudah dibedakan.
- Pada ponsel, gunakan tombol eksplisit seperti **Tambah sebagai overlay** atau **Tambah ke track audio** selain drag agar fungsi multitrack tetap dapat ditemukan dan diakses.
- Di Color Studio, gunakan ruang tambahan untuk node graph dan scopes berdampingan.
- Pertahankan pilihan klip, posisi playhead, scroll, dan panel aktif saat resize atau berpindah orientasi.

### Navigasi dan Sistem Visual

- Jaga struktur workspace proyek tetap konsisten: Cut, Color, Audio, Titling, dan Deliver adalah area kerja dalam proyek yang sama.
- Gunakan navigasi bawah pada window ringkas dan rail pada window lebar; jangan menampilkan bar, rail, dan drawer sekaligus.
- Ikuti token Material 3 semantik untuk warna, tipografi, elevasi, dan state; gunakan warna brand untuk penekanan, bukan seluruh permukaan.
- Sediakan state loading, kosong, offline, error, sukses, dan disabled dengan pesan pemulihan yang konkret.
- Target sentuh minimal 48dp; dukung text scaling, screen reader, kontras, dan reduced motion.

## Urutan Implementasi yang Disarankan

1. Validasi perbaikan OOM/autosave dan export title/audio pada perangkat nyata.
2. Refactor model timeline menjadi track-aware untuk V1, overlay video/foto, dan beberapa lane audio.
3. Implementasikan compositing klip overlay yang konsisten pada preview dan export.
4. Selesaikan preview audio tersinkron dan mixing beberapa track; lanjutkan dengan trim handles, reorder, snap, zoom, serta waveform aktual.
5. Lengkapi kontrol keyframe, mixer lanjutan, dan pemrosesan audio nyata.
6. Perluas proxy, preset sosial, dan antrian export setelah fondasi multitrack stabil.

## Batasan dan Keputusan Produk

- Jangan menandai fitur selesai hanya karena kontrol UI atau simulasi tersedia.
- Jangan menyalin layout, ikon, template, atau aset produk lain; gunakan pola umum editor video dengan visual Arunika sendiri.
- Setiap fitur berat harus memiliki target perangkat minimum, batas memori, jalur pembatalan, dan uji perangkat.
- Catat status implementasi serta hasil validasi aktual di [bug-audit.md](./bug-audit.md); roadmap ini adalah arah produk, bukan klaim bahwa semua fitur sudah tersedia.
