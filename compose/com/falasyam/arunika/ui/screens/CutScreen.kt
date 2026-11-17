package com.falasyam.arunika.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.ClipMediaType
import com.falasyam.arunika.model.TimelineClip
import com.falasyam.arunika.ui.components.VideoPlayerView

/**
 * Tab 1: Cut & Assembly Editor
 * - Multi-Track Magnetic Timeline (V1: Video & Foto, A1: Audio Track)
 * - Dukungan Import Video, Foto, dan Audio dari Galeri / Penyimpanan HP maupun Preset Cepat
 * - Floating Island Action Bar (Split, Ripple Delete, Speed/Duration, Duplicate, Mute/Volume)
 * - Tampilan preview responsif dengan penanda aspek rasio & HUD Status
 */
@Composable
fun CutScreen(
    initialClips: List<TimelineClip> = remember {
        listOf(
            TimelineClip("v1", "A001_C001_SamsungLog.mp4", 4200, 0, 0xFF3B82F6, ClipMediaType.VIDEO),
            TimelineClip("v2", "A001_C002_Sunset_B-Roll.mp4", 6800, 4200, 0xFF10B981, ClipMediaType.VIDEO),
            TimelineClip("v3", "Still_Tokyo_Tower.jpg", 3500, 11000, 0xFF8B5CF6, ClipMediaType.PHOTO),
            TimelineClip("v4", "A001_C003_Interview_A_Cam.mp4", 8500, 14500, 0xFFF59E0B, ClipMediaType.VIDEO)
        )
    },
    initialAudioClips: List<TimelineClip> = remember {
        listOf(
            TimelineClip("a1", "BGM_Cinematic_Ambient.mp3", 14000, 0, 0xFF059669, ClipMediaType.AUDIO),
            TimelineClip("a2", "SFX_Whoosh_Transition.wav", 1500, 4100, 0xFFEA580C, ClipMediaType.AUDIO),
            TimelineClip("a3", "Voiceover_Dialogue_01.m4a", 7400, 14500, 0xFFD97706, ClipMediaType.AUDIO)
        )
    },
    currentVideoUri: String? = null,
    onVideoUriChanged: (String?) -> Unit = {},
    currentTimeMs: Long = 2400,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    // State Klip Video & Foto (Track V1)
    var videoClips by remember { mutableStateOf(initialClips) }

    // State Klip Audio (Track A1) pada Timeline yang sama
    var audioClips by remember { mutableStateOf(initialAudioClips) }

    // ID dan Tipe klip yang sedang aktif dipilih
    var selectedClipId by remember { mutableStateOf("v1") }
    var selectedClipMediaType by remember { mutableStateOf(ClipMediaType.VIDEO) }

    var nextIdCounter by remember { mutableStateOf(10) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importDialogInitialTab by remember { mutableStateOf(0) } // 0: Video/Foto, 1: Audio

    // Android ActivityResult Pickers untuk file fisik dari perangkat
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "Video_${System.currentTimeMillis()}.mp4"
            val newId = "v${nextIdCounter++}"
            val newClip = TimelineClip(
                id = newId,
                name = fileName,
                durationMs = 5000,
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
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
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
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "Audio_${System.currentTimeMillis()}.mp3"
            val newId = "a${nextIdCounter++}"
            val newClip = TimelineClip(
                id = newId,
                name = fileName,
                durationMs = 8000,
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

    // Informasi Klip Terpilih
    val activeClip = remember(selectedClipId, videoClips, audioClips) {
        videoClips.find { it.id == selectedClipId } ?: audioClips.find { it.id == selectedClipId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ========================================================
        // 1. AREA MONITOR PREVIEW VIDEO & HUD
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F0F11)),
            contentAlignment = Alignment.Center
        ) {
            // Bingkai aspek rasio video (16:9)
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                // Tampilan preview video nyata menggunakan VideoView
                VideoPlayerView(
                    uri = currentVideoUri,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )

                // Timestamp HUD di sudut video
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    val durSec = ((activeClip?.durationMs ?: 0L) / 1000.0)
                    Text(
                        text = "00:00:02:12 / ${String.format(java.util.Locale.US, "%.1fs", durSec)}",
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
                // 1. Split Clip (Video/Foto atau Audio)
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = audioClips[idx]
                            if (target.durationMs >= 1000) {
                                val half = target.durationMs / 2
                                val part1 = target.copy(durationMs = half, name = "${target.name.substringBeforeLast(".")}_pt1")
                                val newId = "a${nextIdCounter++}"
                                val part2 = target.copy(id = newId, durationMs = half, name = "${target.name.substringBeforeLast(".")}_pt2")
                                val mutable = audioClips.toMutableList()
                                mutable.removeAt(idx)
                                mutable.add(idx, part1)
                                mutable.add(idx + 1, part2)
                                audioClips = mutable
                                selectedClipId = newId
                            }
                        }
                    } else {
                        val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = videoClips[idx]
                            if (target.durationMs >= 1000) {
                                val half = target.durationMs / 2
                                val part1 = target.copy(durationMs = half, name = "${target.name.substringBeforeLast(".")}_part1")
                                val newId = "v${nextIdCounter++}"
                                val part2 = target.copy(id = newId, durationMs = half, name = "${target.name.substringBeforeLast(".")}_part2")
                                val mutable = videoClips.toMutableList()
                                mutable.removeAt(idx)
                                mutable.add(idx, part1)
                                mutable.add(idx + 1, part2)
                                videoClips = mutable
                                selectedClipId = newId
                            }
                        }
                    }
                }) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = "Split Klip")
                }

                // 2. Ripple Delete
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        if (audioClips.size > 1) {
                            val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = audioClips.toMutableList()
                                mutable.removeAt(idx)
                                audioClips = mutable
                                selectedClipId = audioClips[maxOf(0, idx - 1)].id
                            }
                        }
                    } else {
                        if (videoClips.size > 1) {
                            val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                            if (idx != -1) {
                                val mutable = videoClips.toMutableList()
                                mutable.removeAt(idx)
                                videoClips = mutable
                                selectedClipId = videoClips[maxOf(0, idx - 1)].id
                            }
                        }
                    }
                }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Ripple Delete")
                }

                // 3. Speed / Duration Toggle
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = audioClips[idx]
                            val newVol = if (target.volume > 0.5f) 0.3f else 1.0f
                            val mutable = audioClips.toMutableList()
                            mutable[idx] = target.copy(volume = newVol)
                            audioClips = mutable
                        }
                    } else {
                        val idx = videoClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = videoClips[idx]
                            val newDur = if (target.durationMs > 3000) target.durationMs / 2 else target.durationMs * 2
                            val mutable = videoClips.toMutableList()
                            mutable[idx] = target.copy(durationMs = newDur)
                            videoClips = mutable
                        }
                    }
                }) {
                    val icon = if (selectedClipMediaType == ClipMediaType.AUDIO) Icons.Rounded.VolumeUp else Icons.Rounded.Speed
                    Icon(icon, contentDescription = "Speed / Volume")
                }

                // 4. Duplicate
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (selectedClipMediaType == ClipMediaType.AUDIO) {
                        val idx = audioClips.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val target = audioClips[idx]
                            val newId = "a${nextIdCounter++}"
                            val dup = target.copy(id = newId, name = "${target.name.substringBeforeLast(".")}_copy")
                            val mutable = audioClips.toMutableList()
                            mutable.add(idx + 1, dup)
                            audioClips = mutable
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
                            videoClips = mutable
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
                // Shared scroll state untuk menyinkronkan seluruh track timeline
                val timelineScroll = rememberScrollState()

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

                    // Kontainer Kedua Track (V1 Video/Foto & A1 Audio) yang tersinkronisasi horizontal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(timelineScroll)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Spacer(modifier = Modifier.width(60.dp))

                        // ============================================
                        // TRACK V1 (VIDEO & FOTO)
                        // ============================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(72.dp)
                        ) {
                            Spacer(modifier = Modifier.width(60.dp))

                            videoClips.forEach { clip ->
                                val isSelected = clip.id == selectedClipId && selectedClipMediaType != ClipMediaType.AUDIO
                                val clipWidth = (clip.durationMs / 30).toInt().dp

                                Box(
                                    modifier = Modifier
                                        .width(clipWidth.coerceAtLeast(100.dp))
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
                                            onVideoUriChanged(clip.uriString)
                                        }
                                        .padding(8.dp)
                                ) {
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
                                            text = "${clip.durationMs / 1000.0}s",
                                            color = Color.White.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(60.dp))
                        }

                        // ============================================
                        // TRACK A1 (AUDIO & SFX)
                        // ============================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(56.dp)
                        ) {
                            Spacer(modifier = Modifier.width(60.dp))

                            audioClips.forEach { clip ->
                                val isSelected = clip.id == selectedClipId && selectedClipMediaType == ClipMediaType.AUDIO
                                val clipWidth = (clip.durationMs / 30).toInt().dp

                                Box(
                                    modifier = Modifier
                                        .width(clipWidth.coerceAtLeast(90.dp))
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

                            Spacer(modifier = Modifier.width(60.dp))
                        }
                    }
                }

                // Playhead (Jarum Penunjuk Posisi Tengah Vertikal Melintasi V1 dan A1)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .padding(top = 40.dp)
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    // Kepala playhead bulat
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopCenter)
                    ) {}
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
                videoPickerLauncher.launch("video/*")
            },
            onPickPhoto = {
                showImportDialog = false
                photoPickerLauncher.launch("image/*")
            },
            onPickAudio = {
                showImportDialog = false
                audioPickerLauncher.launch("audio/*")
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
