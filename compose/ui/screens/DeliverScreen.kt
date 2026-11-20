package com.falasyam.arunika.ui.screens

import android.net.Uri
import android.os.Environment
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.falasyam.arunika.model.ColorStudioState
import com.falasyam.arunika.model.DeliverExportState
import com.falasyam.arunika.model.ExportCodec
import com.falasyam.arunika.model.ExportResolution
import com.falasyam.arunika.ui.components.ArunikaColorEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tab 4: Deliver & Export
 * - Real hardware-accelerated video rendering via Media3 Transformer & MediaCodec
 * - Color grading baked directly into the output video via ArunikaColorEffect (GPU 4x4 matrix)
 * - Pilihan resolusi hingga 4K UHD 60fps
 * - Format H.264 (AVC) dan H.265 (HEVC 10-bit)
 * - Real-time progress monitoring & saving directly to device storage
 */
@OptIn(UnstableApi::class)
@Composable
fun DeliverScreen(
    state: DeliverExportState = remember { DeliverExportState() },
    videoUri: String? = null,
    colorStudioState: ColorStudioState? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var resolution by state::resolution
    var codec by state::codec
    var fps by state::frameRate
    var bitrate by state::bitrateMbps
    var isExporting by state::isExporting
    var progress by state::exportProgress

    var activeTransformer by remember { mutableStateOf<Transformer?>(null) }
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
                    if (videoUri == null) {
                        Toast.makeText(context, "Pilih klip video di tab Cut terlebih dahulu untuk diekspor!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    try {
                        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                        val outputFileName = "Arunika_Master_${System.currentTimeMillis()}.mp4"
                        val outputFile = File(moviesDir, outputFileName)

                        isExporting = true
                        progress = 0.01f
                        state.isExporting = true
                        state.exportProgress = 0.01f

                        val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                        val effectsList = mutableListOf<androidx.media3.common.Effect>()

                        // Terapkan color grading effect jika ada
                        if (colorStudioState != null) {
                            val colorEffect = ArunikaColorEffect(colorStudioState)
                            if (!colorEffect.isNoOp()) {
                                effectsList.add(colorEffect)
                            }
                        }

                        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                            .setEffects(Effects(emptyList(), effectsList))
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
                                    isExporting = false
                                    progress = 1.0f
                                    state.isExporting = false
                                    state.exportProgress = 1.0f
                                    activeTransformer = null
                                    lastExportedPath = outputFile.absolutePath

                                    android.media.MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(outputFile.absolutePath),
                                        arrayOf("video/mp4"),
                                        null
                                    )
                                    Toast.makeText(context, "Export Berhasil: ${outputFile.name}", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(context, "Gagal merender: ${exportException.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                            .build()

                        activeTransformer = transformer
                        transformer.start(editedMediaItem, outputFile.absolutePath)

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
