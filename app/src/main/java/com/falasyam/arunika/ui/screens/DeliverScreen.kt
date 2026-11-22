package com.falasyam.arunika.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import androidx.media3.transformer.Composition as MediaComposition
import com.falasyam.arunika.model.ColorStudioState
import com.falasyam.arunika.model.ClipMediaType
import com.falasyam.arunika.model.DeliverExportState
import com.falasyam.arunika.model.ExportCodec
import com.falasyam.arunika.model.ExportResolution
import com.falasyam.arunika.model.TimelineClip
import com.falasyam.arunika.ui.components.ArunikaColorEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.File
import kotlin.math.roundToInt
import androidx.media3.effect.SpeedChangeEffect

private suspend fun publishExportToMovies(context: android.content.Context, renderedFile: File, fileName: String): String =
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/Arunika"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val outputUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Tidak dapat membuat file export di Movies/Arunika")
            try {
                resolver.openOutputStream(outputUri, "w")?.use { output ->
                    renderedFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Tidak dapat membuka file tujuan export")
                val published = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(outputUri, published, null, null)
                outputUri.toString()
            } catch (exception: Exception) {
                resolver.delete(outputUri, null, null)
                throw exception
            }
        } else {
            val moviesDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Arunika"
            )
            if (!moviesDirectory.exists() && !moviesDirectory.mkdirs()) {
                error("Tidak dapat membuat folder Movies/Arunika")
            }
            val destination = File(moviesDirectory, fileName)
            renderedFile.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(destination.absolutePath),
                arrayOf("video/mp4"),
                null
            )
            destination.absolutePath
        }
    }

/**
 * Tab 4: Deliver & Export
 * - Real hardware-accelerated video rendering via Media3 Transformer & MediaCodec
 * - Color grading baked directly into the output video via ArunikaColorEffect (GPU 4x4 matrix)
 * - Pilihan resolusi hingga 4K UHD 60fps
 * - Format H.264 (AVC) dan H.265 (HEVC 10-bit)
 * - Real-time progress monitoring & saving directly to device storage
 */
@UnstableApi
@Composable
fun DeliverScreen(
    state: DeliverExportState = remember { DeliverExportState() },
    videoClips: List<TimelineClip> = emptyList(),
    audioClips: List<TimelineClip> = emptyList(),
    masterVolume: Float = 1f,
    colorStudioState: ColorStudioState? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Izin penyimpanan diperlukan untuk export ke Movies/Arunika", Toast.LENGTH_LONG).show()
        }
    }
    var resolution by state::resolution
    var codec by state::codec
    var fps by state::frameRate
    var bitrate by state::bitrateMbps
    var isExporting by state::isExporting
    var progress by state::exportProgress

    var activeTransformer by remember { mutableStateOf<Transformer?>(null) }
    var activeOutputFile by remember { mutableStateOf<File?>(null) }
    var activeExportAssetsDirectory by remember { mutableStateOf<File?>(null) }
    var lastExportedPath by remember { mutableStateOf<String?>(null) }

    // Estimasi ukuran file (durasi 19 detik)
    val estimatedSizeMb = (19 * bitrate / 8).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Kartu Ringkasan Target Export
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Arunika Master Export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${resolution.label} • ${fps} FPS • ${codec.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "~$estimatedSizeMb MB",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. Pemilihan Resolusi Video
        Text(
            text = "Resolusi Render",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportResolution.values().forEach { res ->
                val isSelected = res == resolution
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        resolution = res
                        state.resolution = res
                    },
                    label = { Text(res.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Pemilihan Framerate (FPS)
        Text(
            text = "Frame Rate (FPS)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(24, 30, 60).forEach { targetFps ->
                val isSelected = targetFps == fps
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        fps = targetFps
                        state.frameRate = targetFps
                    },
                    label = { Text("$targetFps fps") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Codec & Pipeline Color Space
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Codec & Pipeline Ekspor",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExportCodec.values().forEach { c ->
                    val isSelected = c == codec
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                codec = c
                                state.codec = c
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                codec = c
                                state.codec = c
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = c.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = c.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 5. Bitrate Target Slider
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Target Bitrate", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(text = "${bitrate.toInt()} Mbps", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = bitrate,
                    onValueChange = {
                        bitrate = it
                        state.bitrateMbps = it
                    },
                    valueRange = 8f..80f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Informasi file hasil export terakhir
        if (lastExportedPath != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ekspor Berhasil!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = lastExportedPath ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Tombol Aksi Ekspor / Progress Bar Nyata
        if (isExporting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Merender Video (${(progress * 100).toInt()}%)...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeTransformer?.cancel()
                            activeTransformer = null
                            activeOutputFile?.delete()
                            isExporting = false
                            progress = 0f
                            state.isExporting = false
                            state.exportProgress = 0f
                            Toast.makeText(context, "Export dibatalkan", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Batal")
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (videoClips.none { it.uriString != null && it.mediaType != ClipMediaType.AUDIO }) {
                        Toast.makeText(context, "Pilih klip video di tab Cut terlebih dahulu untuk diekspor!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    ) {
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        return@Button
                    }

                    try {
                        val outputFileName = "Arunika_Master_${System.currentTimeMillis()}.mp4"
                        val outputFile = File(context.cacheDir, outputFileName)
                        activeOutputFile = outputFile
                        val assetsDirectory = File(
                            context.cacheDir,
                            "arunika-export-assets-${System.currentTimeMillis()}"
                        )
                        check(assetsDirectory.mkdirs()) { "Could not create temporary export assets." }
                        activeExportAssetsDirectory = assetsDirectory

                        isExporting = true
                        progress = 0.01f
                        state.isExporting = true
                        state.exportProgress = 0.01f

                        val videoSequenceItems = mutableListOf<EditedMediaItem>()
                        var timelineCursorMs = 0L
                        val renderableVideoClips = videoClips
                            .filter { it.mediaType != ClipMediaType.AUDIO }
                            .sortedBy { it.startOffsetMs }
                        renderableVideoClips.forEach { clip ->
                            val gapMs = clip.startOffsetMs - timelineCursorMs
                            require(gapMs >= 0) { "Video clips overlap; adjust the timeline before exporting." }
                            if (gapMs > 0) {
                                videoSequenceItems.add(
                                    buildBlackFrameItem(assetsDirectory, gapMs)
                                )
                            }
                            val uri = clip.uriString
                            if (uri == null) {
                                videoSequenceItems.add(buildBlackFrameItem(assetsDirectory, clip.durationMs))
                            } else {
                                videoSequenceItems.add(
                                    buildEditedClip(
                                        clip = clip,
                                        includeVideo = true,
                                        includeAudio = clip.mediaType == ClipMediaType.VIDEO,
                                        masterVolume = masterVolume,
                                        colorStudioState = colorStudioState
                                    )
                                )
                            }
                            timelineCursorMs = maxOf(
                                timelineCursorMs,
                                clip.startOffsetMs + clip.durationMs
                            )
                        }

                        val compositionSequences = mutableListOf(
                            EditedMediaItemSequence(videoSequenceItems)
                        )
                        audioClips
                            .filter { it.uriString != null && it.mediaType == ClipMediaType.AUDIO }
                            .sortedBy { it.startOffsetMs }
                            .forEach { clip ->
                                val audioSequenceItems = mutableListOf<EditedMediaItem>()
                                if (clip.startOffsetMs > 0) {
                                    audioSequenceItems.add(
                                        buildSilentAudioItem(assetsDirectory, clip.startOffsetMs)
                                    )
                                }
                                audioSequenceItems.add(
                                    buildEditedClip(
                                        clip = clip,
                                        includeVideo = false,
                                        includeAudio = true,
                                        masterVolume = masterVolume,
                                        colorStudioState = null
                                    )
                                )
                                compositionSequences.add(EditedMediaItemSequence(audioSequenceItems))
                            }
                        val composition = MediaComposition.Builder(compositionSequences)
                            .experimentalSetForceAudioTrack(true)
                            .build()

                        val selectedVideoMime = if (codec == ExportCodec.H265_10BIT) {
                            MimeTypes.VIDEO_H265
                        } else {
                            MimeTypes.VIDEO_H264
                        }

                        val transformer = Transformer.Builder(context)
                            .setVideoMimeType(selectedVideoMime)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                                    activeTransformer = null
                                    scope.launch {
                                        try {
                                            val savedPath = publishExportToMovies(context, outputFile, outputFile.name)
                                            lastExportedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                "Movies/Arunika/${outputFile.name}"
                                            } else {
                                                savedPath
                                            }
                                            progress = 1.0f
                                            state.exportProgress = 1.0f
                                            Toast.makeText(
                                                context,
                                                "Export berhasil: Movies/Arunika/${outputFile.name}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (exception: Exception) {
                                            progress = 0f
                                            state.exportProgress = 0f
                                            Toast.makeText(
                                                context,
                                                "Gagal menyimpan ke Movies/Arunika: ${exception.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            outputFile.delete()
                                            activeOutputFile = null
                                            activeExportAssetsDirectory?.deleteRecursively()
                                            activeExportAssetsDirectory = null
                                            isExporting = false
                                            state.isExporting = false
                                        }
                                    }
                                }

                                override fun onError(
                                    composition: androidx.media3.transformer.Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException
                                ) {
                                    isExporting = false
                                    progress = 0f
                                    state.isExporting = false
                                    state.exportProgress = 0f
                                    activeTransformer = null
                                    outputFile.delete()
                                    activeOutputFile = null
                                    activeExportAssetsDirectory?.deleteRecursively()
                                    activeExportAssetsDirectory = null
                                    Toast.makeText(context, "Gagal merender: ${exportException.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                            .build()

                        activeTransformer = transformer
                        transformer.start(composition, outputFile.absolutePath)

                        // Polling progress MediaCodec nyata
                        scope.launch {
                            val progressHolder = ProgressHolder()
                            while (isExporting) {
                                val progressState = transformer.getProgress(progressHolder)
                                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                                    progress = (progressHolder.progress / 100f).coerceIn(0f, 0.99f)
                                    state.exportProgress = progress
                                }
                                delay(150L)
                            }
                        }

                    } catch (e: Exception) {
                        isExporting = false
                        progress = 0f
                        state.isExporting = false
                        state.exportProgress = 0f
                        activeExportAssetsDirectory?.deleteRecursively()
                        activeExportAssetsDirectory = null
                        Toast.makeText(context, "Error memulai export: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Master Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun buildEditedClip(
    clip: TimelineClip,
    includeVideo: Boolean,
    includeAudio: Boolean,
    masterVolume: Float,
    colorStudioState: ColorStudioState?
): EditedMediaItem {
            val uri = Uri.parse(requireNotNull(clip.uriString))
            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (clip.mediaType == ClipMediaType.PHOTO) {
                mediaItemBuilder.setImageDurationMs(clip.durationMs)
            } else {
                require(clip.speedMultiplier > 0f && clip.speedMultiplier.isFinite()) {
                    "Clip speed must be greater than zero."
                }
                val sourceDurationMs = (clip.durationMs * clip.speedMultiplier).roundToInt().toLong()
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.sourceOffsetMs)
                        .setEndPositionMs(clip.sourceOffsetMs + sourceDurationMs)
                        .build()
                )
            }

            val audioProcessors = mutableListOf<AudioProcessor>()
            if (includeAudio && clip.speedMultiplier != 1f) {
                audioProcessors.add(SonicAudioProcessor().apply { setSpeed(clip.speedMultiplier) })
            }
            val combinedVolume = clip.volume * masterVolume
            if (includeAudio && combinedVolume != 1f) {
                audioProcessors.add(ClipGainAudioProcessor(combinedVolume))
            }
            val videoEffects = mutableListOf<androidx.media3.common.Effect>()
            if (includeVideo && clip.speedMultiplier != 1f) {
                videoEffects.add(SpeedChangeEffect(clip.speedMultiplier))
            }
            if (includeVideo && colorStudioState != null) {
                val colorEffect = ArunikaColorEffect(colorStudioState)
                if (!colorEffect.isNoOp()) videoEffects.add(colorEffect)
            }

            return EditedMediaItem.Builder(mediaItemBuilder.build())
                .setRemoveAudio(!includeAudio)
                .setRemoveVideo(!includeVideo)
                .setEffects(Effects(audioProcessors, videoEffects))
                .build()
}

private class ClipGainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {
            override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
                require(
                    inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
                        inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
                ) { "Per-clip volume requires 16-bit or float PCM audio." }
                return inputAudioFormat
            }

            override fun queueInput(inputBuffer: ByteBuffer) {
                val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
                    .order(ByteOrder.nativeOrder())
                val orderedInput = inputBuffer.order(ByteOrder.nativeOrder())
                if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
                    while (orderedInput.hasRemaining()) {
                        outputBuffer.putFloat((orderedInput.float * gain).coerceIn(-1f, 1f))
                    }
                } else {
                    while (orderedInput.hasRemaining()) {
                        val sample = orderedInput.short * gain
                        outputBuffer.putShort(
                            sample.roundToInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        )
                    }
                }
                outputBuffer.flip()
            }
}

private fun buildBlackFrameItem(assetsDirectory: File, durationMs: Long): EditedMediaItem {
    val frameFile = File(assetsDirectory, "black-frame.png")
    if (!frameFile.exists()) {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        FileOutputStream(frameFile).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not create a black placeholder frame."
            }
        }
        bitmap.recycle()
    }
    val mediaItem = MediaItem.Builder()
        .setUri(Uri.fromFile(frameFile))
        .setImageDurationMs(durationMs.coerceAtLeast(1L))
        .build()
    return EditedMediaItem.Builder(mediaItem)
        .setRemoveAudio(true)
        .build()
}

private fun buildSilentAudioItem(assetsDirectory: File, durationMs: Long): EditedMediaItem {
    val sampleRate = 48_000
    val channelCount = 1
    val bytesPerSample = 2
    val dataSize = durationMs * sampleRate * channelCount * bytesPerSample / 1000
    require(dataSize <= 0xFFFFFFFFL - 36) { "Audio gap is too long to export." }
    val silenceFile = File(assetsDirectory, "silence-${durationMs}ms.wav")
    if (!silenceFile.exists() || silenceFile.length() != dataSize + 44) {
        FileOutputStream(silenceFile).use { output ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt((36 + dataSize).toInt())
            header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(channelCount.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channelCount * bytesPerSample)
            header.putShort((channelCount * bytesPerSample).toShort())
            header.putShort((bytesPerSample * 8).toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize.toInt())
            output.write(header.array())
            val zeros = ByteArray(16 * 1024)
            var remaining = dataSize
            while (remaining > 0) {
                val chunkSize = minOf(remaining, zeros.size.toLong()).toInt()
                output.write(zeros, 0, chunkSize)
                remaining -= chunkSize
            }
        }
    }
    return EditedMediaItem.Builder(
        MediaItem.fromUri(Uri.fromFile(silenceFile))
    ).setRemoveVideo(true).build()
}
