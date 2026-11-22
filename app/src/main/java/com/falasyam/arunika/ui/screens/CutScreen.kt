package com.falasyam.arunika.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.components.ColorGradingOverlay
import com.falasyam.arunika.ui.components.VideoPlayerView
import androidx.media3.exoplayer.ExoPlayer

private const val MIN_SPLITTABLE_CLIP_MS = 1000L

private fun splitOffsetAtPlayhead(clip: TimelineClip?, playheadMs: Long): Long? {
    if (clip == null || clip.durationMs < MIN_SPLITTABLE_CLIP_MS) return null
    val offsetMs = playheadMs - clip.startOffsetMs
    return offsetMs.takeIf { it > 0L && it < clip.durationMs }
}

/**
 * Tab 1: Cut & Assembly Editor
 * - Multi-Track Magnetic Timeline (V1: Video & Foto, A1: Audio Track)
 * - Dukungan Import Video, Foto, dan Audio dari Galeri / Penyimpanan HP maupun Preset Cepat
 * - Floating Island Action Bar (Split, Ripple Delete, Speed/Duration, Duplicate, Mute/Volume)
 * - Tampilan preview responsif dengan penanda aspek rasio & HUD Status
 */
@Composable
fun CutScreen(
    initialClips: List<TimelineClip> = emptyList(),
    initialAudioClips: List<TimelineClip> = emptyList(),
    onVideoClipsChanged: (List<TimelineClip>) -> Unit = {},
    onAudioClipsChanged: (List<TimelineClip>) -> Unit = {},
    aspectRatio: AspectRatioMode = AspectRatioMode.CINEMA_16_9,
    colorStudioState: ColorStudioState? = null,
    titlingState: TitlingState? = null,
    currentVideoUri: String? = null,
    onVideoUriChanged: (String?) -> Unit = {},
    currentTimeMs: Long = 0L,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {},
    sharedExoPlayer: ExoPlayer? = null,
    keyframeState: KeyframeState = remember { KeyframeState() }
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // State Klip Video & Foto (Track V1)
    var videoClips by remember(initialClips) { mutableStateOf(initialClips) }

    // State Klip Audio (Track A1) pada Timeline yang sama
    var audioClips by remember(initialAudioClips) { mutableStateOf(initialAudioClips) }

    LaunchedEffect(videoClips) {
        onVideoClipsChanged(videoClips)
    }
    LaunchedEffect(audioClips) {
        onAudioClipsChanged(audioClips)
    }
    val clipThumbnails = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(videoClips) {
            val videoClipsWithUris = videoClips.filter {
                it.uriString != null && it.mediaType == ClipMediaType.VIDEO
            }
            val currentThumbnailIds = videoClipsWithUris.mapTo(mutableSetOf()) { it.id }
            clipThumbnails.keys.filterNot { it in currentThumbnailIds }.forEach { clipId ->
                clipThumbnails.remove(clipId)?.recycle()
            }

            videoClipsWithUris.forEach { clip ->
                if (clipThumbnails.containsKey(clip.id)) return@forEach
                val thumbnail = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uriString))
                        retriever.extractThumbnailFrame(
                            (clip.sourceOffsetMs * 1000).coerceAtMost(1_000_000_000)
                        )
                    } catch (exception: Exception) {
                        Log.w("CutScreen", "Could not create thumbnail for ${clip.name}", exception)
                        null
                } finally {
                    retriever.release()
                }
            }
                thumbnail?.let { clipThumbnails[clip.id] = it }
            }
    }

    DisposableEffect(clipThumbnails) {
            onDispose {
                clipThumbnails.values.toList().forEach(Bitmap::recycle)
                clipThumbnails.clear()
            }
    }

    // ID dan Tipe klip yang sedang aktif dipilih
    var selectedClipId by remember(initialClips, initialAudioClips) {
        mutableStateOf(initialClips.firstOrNull()?.id ?: initialAudioClips.firstOrNull()?.id ?: "")
    }
    var selectedClipMediaType by remember(initialClips, initialAudioClips) {
        mutableStateOf(
            when {
                initialClips.isNotEmpty() -> initialClips.first().mediaType
                initialAudioClips.isNotEmpty() -> ClipMediaType.AUDIO
                else -> ClipMediaType.VIDEO
            }
        )
    }

    var nextIdCounter by remember { mutableStateOf(10) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importDialogInitialTab by remember { mutableStateOf(0) } // 0: Video/Foto, 1: Audio
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }

    // Helper untuk mengekstrak durasi media fisik dari URI
    fun extractMediaDurationMs(uri: Uri, defaultDuration: Long): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull()?.coerceAtLeast(500L) ?: defaultDuration
        } catch (_: Exception) {
            defaultDuration
        }
    }

    // Android ActivityResult Pickers untuk file fisik dari perangkat
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                android.util.Log.e("CutScreen", "Could not retain video permission for $uri", it)
            }
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "Video_${System.currentTimeMillis()}.mp4"
            val realDuration = extractMediaDurationMs(it, 5000L)
            val newId = "v${nextIdCounter++}"
            val newClip = TimelineClip(
                id = newId,
                name = fileName,
                durationMs = realDuration,
                startOffsetMs = videoClips.sumOf { c -> c.durationMs },
                colorTag = 0xFF0284C7,
                mediaType = ClipMediaType.VIDEO,
                uriString = it.toString()
            )
            videoClips = videoClips + newClip
            selectedClipId = newId
            selectedClipMediaType = ClipMediaType.VIDEO
            onVideoUriChanged(it.toString())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                android.util.Log.e("CutScreen", "Could not retain photo permission for $uri", it)
            }
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "Foto_${System.currentTimeMillis()}.jpg"
            val newId = "v${nextIdCounter++}"
            val newClip = TimelineClip(
                id = newId,
                name = fileName,
                durationMs = 3500, // Default 3.5 detik untuk still photo
                startOffsetMs = videoClips.sumOf { c -> c.durationMs },
                colorTag = 0xFF8B5CF6,
                mediaType = ClipMediaType.PHOTO,
                uriString = it.toString()
            )
            videoClips = videoClips + newClip
            selectedClipId = newId
            selectedClipMediaType = ClipMediaType.PHOTO
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                android.util.Log.e("CutScreen", "Could not retain audio permission for $uri", it)
            }
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "Audio_${System.currentTimeMillis()}.mp3"
            val realDuration = extractMediaDurationMs(it, 8000L)
            val newId = "a${nextIdCounter++}"
            val newClip = TimelineClip(
                id = newId,
                name = fileName,
                durationMs = realDuration,
                startOffsetMs = audioClips.sumOf { c -> c.durationMs },
                colorTag = 0xFF059669,
                mediaType = ClipMediaType.AUDIO,
                uriString = it.toString()
            )
            audioClips = audioClips + newClip
            selectedClipId = newId
            selectedClipMediaType = ClipMediaType.AUDIO
        }
    }

    // Shared scroll state untuk timeline
    val timelineScroll = rememberScrollState()

    // Posisi waktu Playhead saat ini dalam milidetik
    var currentPlayheadMs by remember { mutableStateOf(currentTimeMs) }
    val totalTimelineDurationMs = remember(videoClips, audioClips) {
        maxOf(
            videoClips.maxOfOrNull { it.startOffsetMs + it.durationMs } ?: 0L,
            audioClips.maxOfOrNull { it.startOffsetMs + it.durationMs } ?: 0L,
            1000L
        )
    }
    val density = LocalDensity.current

    // Playback loop ticker: menggerakkan playhead dan detik timecode secara real-time saat diputar
    LaunchedEffect(isPlaying, totalTimelineDurationMs) {
        if (isPlaying) {
            val startClock = System.currentTimeMillis() - currentPlayheadMs
            while (isPlaying) {
                val elapsed = System.currentTimeMillis() - startClock
                if (elapsed >= totalTimelineDurationMs) {
                    currentPlayheadMs = totalTimelineDurationMs
                    onTogglePlay()
                    break
                }
                currentPlayheadMs = elapsed
                kotlinx.coroutines.delay(33L)
            }
        }
    }

    // Keep the playhead fixed while the timeline content scrolls beneath it.
    LaunchedEffect(currentPlayheadMs, isPlaying) {
        val scrollTarget = with(density) { (currentPlayheadMs / 30f).dp.roundToPx() }
        if (kotlin.math.abs(timelineScroll.value - scrollTarget) > 1) {
            timelineScroll.scrollTo(scrollTarget.coerceAtLeast(0))
        }
    }

    LaunchedEffect(timelineScroll, totalTimelineDurationMs) {
        snapshotFlow { timelineScroll.value }.collect { scrollPx ->
            val timelineTimeMs = with(density) {
                (scrollPx / density.density * 30f).toLong()
            }
            currentPlayheadMs = timelineTimeMs.coerceIn(0L, totalTimelineDurationMs)
        }
    }

    // Informasi Klip Terpilih berdasarkan playhead saat ini atau seleksi manual
    val activeClip = remember(currentPlayheadMs, selectedClipId, videoClips, audioClips) {
        if (selectedClipMediaType == ClipMediaType.AUDIO) {
            audioClips.find { it.id == selectedClipId } ?: videoClips.firstOrNull()
        } else {
            videoClips.find { clip ->
                currentPlayheadMs >= clip.startOffsetMs && currentPlayheadMs < (clip.startOffsetMs + clip.durationMs)
            } ?: videoClips.find { it.id == selectedClipId } ?: videoClips.firstOrNull()
        }
    }
    val previewVideoClip = videoClips.find {
        currentPlayheadMs >= it.startOffsetMs && currentPlayheadMs < it.startOffsetMs + it.durationMs
    }

    LaunchedEffect(previewVideoClip?.id) {
        onVideoUriChanged(previewVideoClip?.uriString)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ============================================
        // 1. AREA MONITOR PREVIEW VIDEO & HUD
        // ============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F0F11)),
            contentAlignment = Alignment.Center
        ) {
            val ratio = aspectRatio.ratioFloat

            // Bingkai aspek rasio video responsif (16:9, 9:16, 1:1, 4:5, 21:9)
            BoxWithConstraints(
                modifier = Modifier
                    .padding(8.dp)
                    .aspectRatio(ratio, matchHeightConstraintsFirst = aspectRatio.isVertical)
                    .fillMaxHeight(if (aspectRatio.isVertical) 0.94f else 0.88f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                val canvasWidthPx = constraints.maxWidth
                val canvasHeightPx = constraints.maxHeight

                // Tampilan preview video nyata menggunakan ExoPlayer
                // ✅ Fix #1: colorStudioState diteruskan ke VideoPlayerView untuk grading GPU
                VideoPlayerView(
                    uri = currentVideoUri,
                    isPlaying = isPlaying,
                    seekToMs = if (previewVideoClip != null) {
                        previewVideoClip.sourceOffsetMs +
                            (((currentPlayheadMs - previewVideoClip.startOffsetMs).coerceAtLeast(0L) * previewVideoClip.speedMultiplier).toLong())
                    } else 0L,
                    volume = previewVideoClip?.volume ?: 1.0f,
                    speedMultiplier = previewVideoClip?.speedMultiplier ?: 1.0f,
                    colorState = colorStudioState,
                    modifier = Modifier.fillMaxSize(),
                    sharedExoPlayer = sharedExoPlayer
                )

                // 1. Real-time Color Grading Effect Overlay (Wheels, LUT, CST, Luma)
                if (colorStudioState != null) {
                    ColorGradingOverlay(
                        state = colorStudioState,
                        isBypassed = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Real-time Track V2 Title / Subtitle Overlay dari Fusion Lite
                titlingState?.elements?.forEach { element ->
                    val transform = keyframeState.interpolateTransform(currentPlayheadMs)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    (((element.x - 0.5f) * canvasWidthPx) + transform.posX * canvasWidthPx / 100f).roundToInt(),
                                    (((element.y - 0.5f) * canvasHeightPx) + transform.posY * canvasHeightPx / 100f).roundToInt()
                                )
                            }
                            .scale(transform.scale)
                            .alpha(transform.opacity)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(element.bgColor))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (element.wordHighlight) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                element.text.split(" ").forEachIndexed { i, word ->
                                    Text(
                                        text = word,
                                        fontSize = (element.fontSize * 0.85f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (i == 1) Color(element.highlightColor) else Color(element.textColor)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = element.text,
                                fontSize = (element.fontSize * 0.85f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(element.textColor),
                                fontFamily = when (element.font) {
                                    TitleFont.SERIF -> FontFamily.Serif
                                    TitleFont.MONO -> FontFamily.Monospace
                                    else -> FontFamily.Default
                                }
                            )
                        }
                    }
                }

                // Badge Format Aspek Rasio di sudut kiri atas video
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = aspectRatio.label.substringBefore(" "),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Timestamp HUD di sudut video
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    val curTotalSec = currentPlayheadMs / 1000
                    val curMins = curTotalSec / 60
                    val curSecs = curTotalSec % 60
                    val curFrames = ((currentPlayheadMs % 1000) * 30 / 1000)
                    val timeString = String.format(java.util.Locale.US, "%02d:%02d:%02d:%02d", 0, curMins, curSecs, curFrames)
                    val totalSec = ((activeClip?.durationMs ?: totalTimelineDurationMs) / 1000.0)
                    Text(
                        text = "$timeString / ${String.format(java.util.Locale.US, "%.1fs", totalSec)}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Quick Import Badge di pojok kanan atas video
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            importDialogInitialTab = 0
                            showImportDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+ Import",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // ========================================================
        // 2. FLOATING ACTION ISLAND (TOMBOL CEPAT EDITING M3)
        // ========================================================
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val hasSelectedClip = if (selectedClipMediaType == ClipMediaType.AUDIO) audioClips.any { it.id == selectedClipId } else videoClips.any { it.id == selectedClipId }
                val canDelete = if (selectedClipMediaType == ClipMediaType.AUDIO) audioClips.isNotEmpty() else videoClips.isNotEmpty()
                val selectedClip = if (selectedClipMediaType == ClipMediaType.AUDIO) {
                    audioClips.find { it.id == selectedClipId }
                } else {
                    videoClips.find { it.id == selectedClipId }
                }
                val splitOffsetMs = splitOffsetAtPlayhead(selectedClip, currentPlayheadMs)

                // 1. Split Clip (Video/Foto atau Audio)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selectedClipMediaType == ClipMediaType.AUDIO) {
                            val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                            val target = audioClips.getOrNull(idx)
                            val offsetMs = splitOffsetAtPlayhead(target, currentPlayheadMs)
                            if (target != null && offsetMs != null) {
                                val part1 = target.copy(
                                    durationMs = offsetMs,
                                    name = "${target.name.substringBeforeLast(".")}_pt1"
                                )
                                val newId = "a${nextIdCounter++}"
                                val part2 = target.copy(
                                    id = newId,
                                    durationMs = target.durationMs - offsetMs,
                                    startOffsetMs = target.startOffsetMs + offsetMs,
                                    sourceOffsetMs = target.sourceOffsetMs + (offsetMs * target.speedMultiplier).toLong(),
                                    name = "${target.name.substringBeforeLast(".")}_pt2"
                                )
                                val updatedClips = audioClips.toMutableList()
                                updatedClips[idx] = part1
                                updatedClips.add(idx + 1, part2)
                                audioClips = updatedClips
                                selectedClipId = newId
                            }
                        } else {
                            val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                            val target = videoClips.getOrNull(idx)
                            val offsetMs = splitOffsetAtPlayhead(target, currentPlayheadMs)
                            if (target != null && offsetMs != null) {
                                val part1 = target.copy(
                                    durationMs = offsetMs,
                                    name = "${target.name.substringBeforeLast(".")}_part1"
                                )
                                val newId = "v${nextIdCounter++}"
                                val part2 = target.copy(
                                    id = newId,
                                    durationMs = target.durationMs - offsetMs,
                                    startOffsetMs = target.startOffsetMs + offsetMs,
                                    sourceOffsetMs = target.sourceOffsetMs + (offsetMs * target.speedMultiplier).toLong(),
                                    name = "${target.name.substringBeforeLast(".")}_part2"
                                )
                                val updatedClips = videoClips.toMutableList()
                                updatedClips[idx] = part1
                                updatedClips.add(idx + 1, part2)
                                videoClips = updatedClips
                                selectedClipId = newId
                            }
                        }
                    },
                    enabled = splitOffsetMs != null
                ) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = "Split klip pada playhead")
                }

                // 2. Ripple Delete
                IconButton(
                    onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        if (audioClips.size > 1) {
                            val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = audioClips.toMutableList()
                                mutable.removeAt(idx)
                                var offset = 0L
                                audioClips = mutable.map { c -> c.copy(startOffsetMs = offset).also { offset += c.durationMs } }
                                selectedClipId = audioClips[maxOf(0, idx - 1)].id
                            }
                        }
                    } else {
                        if (videoClips.size > 1) {
                            val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = videoClips.toMutableList()
                                mutable.removeAt(idx)
                                var offset = 0L
                                videoClips = mutable.map { c -> c.copy(startOffsetMs = offset).also { offset += c.durationMs } }
                                selectedClipId = videoClips[maxOf(0, idx - 1)].id
                            }
                        }
                    }
                    },
                    enabled = canDelete
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Ripple Delete")
                }

                // 3. Speed / Duration Settings
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSpeedDialog = true
                    },
                    enabled = hasSelectedClip
                ) {
                    Icon(Icons.Rounded.Speed, contentDescription = "Kecepatan Klip")
                }

                // 4. Volume / Mute Settings
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showVolumeDialog = true
                    },
                    enabled = hasSelectedClip
                ) {
                    val currentVol = if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        audioClips.find { it.id == selectedClipId }?.volume ?: 1.0f
                    } else {
                        videoClips.find { it.id == selectedClipId }?.volume ?: 1.0f
                    }
                    val icon = if (currentVol == 0f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp
                    Icon(icon, contentDescription = "Volume / Mute Klip")
                }

                // 5. Tambah Keyframe Scale pada posisi playhead
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val currentTransform = keyframeState.interpolateTransform(currentPlayheadMs)
                        val nextScale = if (currentTransform.scale >= 1.2f) 1.0f else currentTransform.scale + 0.1f
                        keyframeState.addOrUpdateKeyframe(
                            timeMs = currentPlayheadMs,
                            property = AnimProperty.SCALE,
                            value = nextScale
                        )
                    },
                    enabled = hasSelectedClip
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Tambah keyframe")
                }

                // 6. Duplicate
                IconButton(
                    onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = audioClips[idx]
                            val newId = "a${nextIdCounter++}"
                            val dup = target.copy(id = newId, name = "${target.name.substringBeforeLast(".")}_copy")
                            val mutable = audioClips.toMutableList()
                            mutable.add(idx + 1, dup)
                            var offset = 0L
                            audioClips = mutable.map { c -> c.copy(startOffsetMs = offset).also { offset += c.durationMs } }
                            selectedClipId = newId
                        }
                    } else {
                        val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = videoClips[idx]
                            val newId = "v${nextIdCounter++}"
                            val dup = target.copy(id = newId, name = "${target.name.substringBeforeLast(".")}_copy")
                            val mutable = videoClips.toMutableList()
                            mutable.add(idx + 1, dup)
                            var offset = 0L
                            videoClips = mutable.map { c -> c.copy(startOffsetMs = offset).also { offset += c.durationMs } }
                            selectedClipId = newId
                        }
                    }
                }) {
                    Icon(Icons.Rounded.ControlPointDuplicate, contentDescription = "Duplicate")
                }

                // Divider vertikal tipis
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // 5. Tombol Pintas Impor Video / Foto
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    importDialogInitialTab = 0
                    showImportDialog = true
                }) {
                    Icon(Icons.Rounded.VideoCall, contentDescription = "Import Video/Foto", tint = MaterialTheme.colorScheme.primary)
                }

                // 6. Tombol Pintas Impor Audio
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    importDialogInitialTab = 1
                    showImportDialog = true
                }) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = "Tambah Audio", tint = Color(0xFF10B981))
                }
            }
        }

        // ========================================================
        // 3. AREA MULTI-TRACK MAGNETIC TIMELINE (V1 + A1)
        // ========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header timeline & Tombol Import Aksi Utama
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Badge V1 & A1
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "V1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "A1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            // Tombol Impor Video/Foto
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    importDialogInitialTab = 0
                                    showImportDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Video/Foto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Tombol Tambah Audio
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    importDialogInitialTab = 1
                                    showImportDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Tombol Play / Pause Utama
                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTogglePlay()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }
                    }

                    // Divider track
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Kontainer Kedua Track (V1 Video/Foto & A1 Audio) dengan Playhead Bergerak
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        val timelineViewportWidth = maxWidth
                        val timelineContentWidth = timelineViewportWidth + (totalTimelineDurationMs / 30f).dp
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .horizontalScroll(timelineScroll),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                        // ============================================
                        // TRACK V1 (VIDEO & FOTO)
                        // ============================================
                        Row(
                            modifier = Modifier
                                .width(timelineContentWidth)
                                .height(72.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                            if (videoClips.isEmpty()) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        importDialogInitialTab = 0
                                        showImportDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .offset(x = 60.dp)
                                        .width(300.dp)
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.AddCircleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ketuk untuk tambah video/foto",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                videoClips.forEach { clip ->
                                    val isSelected = clip.id == selectedClipId && selectedClipMediaType != ClipMediaType.AUDIO
                                    val clipWidth = (clip.durationMs / 30f).dp

                                    Box(
                                        modifier = Modifier
                                            .offset(x = 60.dp + (clip.startOffsetMs / 30f).dp)
                                            .width(clipWidth.coerceAtLeast(1.dp))
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(clip.colorTag))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedClipId = clip.id
                                                selectedClipMediaType = clip.mediaType
                                                currentPlayheadMs = clip.startOffsetMs
                                                onVideoUriChanged(clip.uriString)
                                            }
                                            .padding(8.dp)
                                    ) {
                                        clipThumbnails[clip.id]?.let { bitmap ->
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Thumbnail ${clip.name}",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .alpha(0.72f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.18f))
                                            )
                                        }
                                                keyframeState.keyframes
                                                    .filter { it.timeMs in clip.startOffsetMs..(clip.startOffsetMs + clip.durationMs) }
                                                    .forEach { keyframe ->
                                                        val fraction = if (clip.durationMs > 0) {
                                                            (keyframe.timeMs - clip.startOffsetMs).toFloat() / clip.durationMs
                                                        } else 0f
                                                        Text(
                                                            text = "◆",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier
                                                                .align(Alignment.TopStart)
                                                                .offset(x = (fraction * clipWidth.value).dp - 4.dp)
                                                        )
                                                    }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (clip.mediaType == ClipMediaType.PHOTO) Icons.Rounded.Image else Icons.Rounded.Videocam,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = clip.name,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Text(
                                                text = if (clip.speedMultiplier != 1.0f) "${clip.speedMultiplier}x • ${clip.durationMs / 1000.0}s" else "${clip.durationMs / 1000.0}s",
                                                color = Color.White.copy(alpha = 0.9f),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            }
                        }

                        // ============================================
                        // TRACK A1 (AUDIO & SFX)
                        // ============================================
                        Row(
                            modifier = Modifier
                                .width(timelineContentWidth)
                                .height(56.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                            if (audioClips.isEmpty()) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        importDialogInitialTab = 1
                                        showImportDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .offset(x = 60.dp)
                                        .width(260.dp)
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Audiotrack,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ketuk untuk tambah musik/audio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            } else {
                                audioClips.forEach { clip ->
                                val isSelected = clip.id == selectedClipId && selectedClipMediaType == ClipMediaType.AUDIO
                                val clipWidth = (clip.durationMs / 30f).dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = 60.dp + (clip.startOffsetMs / 30f).dp)
                                        .width(clipWidth.coerceAtLeast(1.dp))
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(clip.colorTag))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedClipId = clip.id
                                            selectedClipMediaType = ClipMediaType.AUDIO
                                            currentPlayheadMs = clip.startOffsetMs
                                        }
                                        .padding(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Audiotrack,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = clip.name,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                fontSize = 10.sp,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Simulasi Waveform Audio Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val barHeights = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 1f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f)
                                            barHeights.forEach { h ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(h)
                                                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Text(
                                            text = "${clip.durationMs / 1000.0}s",
                                            color = Color.White.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                            }
                        }
                    }

                    // Fixed playhead; horizontal scrolling moves the tracks underneath.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 60.dp)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        // Kepala playhead bulat di bagian atas
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(12.dp)
                                .offset(x = (-5).dp, y = 0.dp)
                        ) {}
                    }
                }
            }
        }
    }

    // ========================================================
    // 4. DIALOG / MODAL IMPORT MEDIA & AUDIO
    // ========================================================
    if (showImportDialog) {
        ImportMediaDialog(
            initialTab = importDialogInitialTab,
            onDismiss = { showImportDialog = false },
            onPickVideo = {
                showImportDialog = false
                videoPickerLauncher.launch(arrayOf("video/*"))
            },
            onPickPhoto = {
                showImportDialog = false
                photoPickerLauncher.launch(arrayOf("image/*"))
            },
            onPickAudio = {
                showImportDialog = false
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onAddPresetVideoPhoto = { name, isPhoto, durationMs, color ->
                showImportDialog = false
                val newId = "v${nextIdCounter++}"
                val newClip = TimelineClip(
                    id = newId,
                    name = name,
                    durationMs = durationMs,
                    startOffsetMs = videoClips.sumOf { it.durationMs },
                    colorTag = color,
                    mediaType = if (isPhoto) ClipMediaType.PHOTO else ClipMediaType.VIDEO
                )
                videoClips = videoClips + newClip
                selectedClipId = newId
                selectedClipMediaType = newClip.mediaType
            },
            onAddPresetAudio = { name, durationMs, color ->
                showImportDialog = false
                val newId = "a${nextIdCounter++}"
                val newClip = TimelineClip(
                    id = newId,
                    name = name,
                    durationMs = durationMs,
                    startOffsetMs = audioClips.sumOf { it.durationMs },
                    colorTag = color,
                    mediaType = ClipMediaType.AUDIO
                )
                audioClips = audioClips + newClip
                selectedClipId = newId
                selectedClipMediaType = ClipMediaType.AUDIO
            }
        )
    }

    // ========================================================
    // 5. DIALOG PENGATURAN SPEED KELIP (0.25x - 4x)
    // ========================================================
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Atur Kecepatan Klip", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val speeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)
                    speeds.forEach { speed ->
                        OutlinedButton(
                            onClick = {
                                showSpeedDialog = false
                                val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                                if (idx != -1) {
                                    val target = videoClips[idx]
                                    val baseDuration = (target.durationMs * target.speedMultiplier).toLong()
                                    val newDur = (baseDuration / speed).toLong().coerceAtLeast(500L)
                                    val mutable = videoClips.toMutableList()
                                    mutable[idx] = target.copy(durationMs = newDur, speedMultiplier = speed)
                                    var offset = 0L
                                    videoClips = mutable.map { c -> c.copy(startOffsetMs = offset).also { offset += c.durationMs } }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${speed}x Kecepatan")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Tutup") }
            }
        )
    }

    // ========================================================
    // 6. DIALOG PENGATURAN VOLUME KLIP
    // ========================================================
    if (showVolumeDialog) {
        var tempVolume by remember {
            mutableStateOf(
                if (selectedClipMediaType == ClipMediaType.AUDIO) {
                    audioClips.find { it.id == selectedClipId }?.volume ?: 1.0f
                } else {
                    videoClips.find { it.id == selectedClipId }?.volume ?: 1.0f
                }
            )
        }
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text("Atur Volume Klip", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Level Volume: ${(tempVolume * 100).toInt()}%")
                    Slider(
                        value = tempVolume,
                        onValueChange = { tempVolume = it },
                        valueRange = 0f..2f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVolumeDialog = false
                        if (selectedClipMediaType == ClipMediaType.AUDIO) {
                            val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = audioClips.toMutableList()
                                mutable[idx] = mutable[idx].copy(volume = tempVolume)
                                audioClips = mutable
                            }
                        } else {
                            val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = videoClips.toMutableList()
                                mutable[idx] = mutable[idx].copy(volume = tempVolume)
                                videoClips = mutable
                            }
                        }
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeDialog = false }) { Text("Batal") }
            }
        )
    }
}
}

/**
 * Dialog Tabbed Import Media & Audio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMediaDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onPickVideo: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickAudio: () -> Unit,
    onAddPresetVideoPhoto: (String, Boolean, Long, Long) -> Unit,
    onAddPresetAudio: (String, Long, Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Media ke Timeline",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Pilihan: Video & Foto vs Audio / Musik
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Video & Foto (V1)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Audio / Musik (A1)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: IMPORT VIDEO & FOTO
                    Text(
                        text = "Pilih dari Penyimpanan / Galeri HP:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickVideo,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor Video", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = onPickPhoto,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor Foto", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Koleksi Preset & B-Roll Cepat:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        Quadruple("Drone_Sunset_Coast.mp4", false, 6500L, 0xFF0284C7),
                        Quadruple("Tokyo_City_Neon_Night.mp4", false, 5000L, 0xFF3B82F6),
                        Quadruple("Still_Camera_Portrait.jpg", true, 3500L, 0xFF8B5CF6),
                        Quadruple("Still_Product_Hero.jpg", true, 4000L, 0xFFA855F7),
                        Quadruple("Macro_Coffee_Pouring.mp4", false, 4800L, 0xFFF59E0B)
                    )

                    presets.forEach { (name, isPhoto, dur, col) ->
                        Surface(
                            onClick = { onAddPresetVideoPhoto(name, isPhoto, dur, col) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPhoto) Icons.Rounded.Image else Icons.Rounded.Videocam,
                                        contentDescription = null,
                                        tint = Color(col),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(if (isPhoto) "Foto Still • ${(dur / 1000.0)}s" else "Video Clip • ${(dur / 1000.0)}s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    // TAB 1: IMPORT AUDIO
                    Text(
                        text = "Pilih dari Penyimpanan HP:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onPickAudio,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Impor File Audio (MP3 / WAV / M4A)")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Koleksi BGM & Sound Effects (SFX):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val audioPresets = listOf(
                        Triple("Cinematic_Ambient_Score.mp3", 16000L, 0xFF059669),
                        Triple("Lo-Fi_Chill_Beats.mp3", 14000L, 0xFF10B981),
                        Triple("Whoosh_Fast_Transition.wav", 1800L, 0xFFEA580C),
                        Triple("Camera_Shutter_Click.wav", 900L, 0xFF0284C7),
                        Triple("Voiceover_Narrator_Intro.m4a", 7500L, 0xFFD97706)
                    )

                    audioPresets.forEach { (name, dur, col) ->
                        Surface(
                            onClick = { onAddPresetAudio(name, dur, col) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Audiotrack,
                                        contentDescription = null,
                                        tint = Color(col),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Audio Track • ${(dur / 1000.0)}s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = Color(col))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

// Helper container data untuk 4 parameter
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
