package com.falasyam.arunika.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.TimelineClip

/**
 * Tab 1: Cut & Assembly
 * - Magnetic Timeline dengan scrubbing presisi dan haptic ticks
 * - Floating Island Action Bar (Split, Ripple Delete, Speed, Duplicate)
 * - Tampilan preview responsif dengan penanda aspek rasio
 */
@Composable
fun CutScreen(
    initialClips: List<TimelineClip> = remember {
        listOf(
            TimelineClip("1", "A001_C001_SamsungLog.mp4", 4200, 0, 0xFF3B82F6),
            TimelineClip("2", "A001_C002_Sunset_B-Roll.mp4", 6800, 4200, 0xFF10B981),
            TimelineClip("3", "A001_C003_Interview_A_Cam.mp4", 8500, 11000, 0xFFF59E0B)
        )
    },
    currentTimeMs: Long = 2400,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var clipList by remember { mutableStateOf(initialClips) }
    var selectedClipId by remember { mutableStateOf("1") }
    var nextIdCounter by remember { mutableStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Area Video Monitor Preview
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
                Text(
                    text = "4K 10-bit Proxy Source\n[16:9 UHD]",
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Timestamp HUD di sudut video
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "00:00:02:12 / 00:00:19:15",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Floating Action Island (Tombol Cepat Editing M3 Expressive)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Split Clip
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val idx = clipList.indexOfFirst { it.id == selectedClipId }
                    if (idx != -1) {
                        val target = clipList[idx]
                        if (target.durationMs >= 1000) {
                            val half = target.durationMs / 2
                            val part1 = target.copy(durationMs = half, name = "${target.name.substringBeforeLast(".")}_part1.mp4")
                            val newId = (nextIdCounter++).toString()
                            val part2 = target.copy(id = newId, durationMs = half, name = "${target.name.substringBeforeLast(".")}_part2.mp4")
                            val mutable = clipList.toMutableList()
                            mutable.removeAt(idx)
                            mutable.add(idx, part1)
                            mutable.add(idx + 1, part2)
                            clipList = mutable
                            selectedClipId = newId
                        }
                    }
                }) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = "Split Klip")
                }

                // 2. Ripple Delete
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (clipList.size > 1) {
                        val idx = clipList.indexOfFirst { it.id == selectedClipId }
                        if (idx != -1) {
                            val mutable = clipList.toMutableList()
                            mutable.removeAt(idx)
                            clipList = mutable
                            selectedClipId = clipList[maxOf(0, idx - 1)].id
                        }
                    }
                }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Ripple Delete")
                }

                // 3. Speed / Ramp Toggle
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val idx = clipList.indexOfFirst { it.id == selectedClipId }
                    if (idx != -1) {
                        val target = clipList[idx]
                        val newDur = if (target.durationMs > 3000) target.durationMs / 2 else target.durationMs * 2
                        val mutable = clipList.toMutableList()
                        mutable[idx] = target.copy(durationMs = newDur)
                        clipList = mutable
                    }
                }) {
                    Icon(Icons.Rounded.Speed, contentDescription = "Speed / Ramp")
                }

                // 4. Duplicate
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val idx = clipList.indexOfFirst { it.id == selectedClipId }
                    if (idx != -1) {
                        val target = clipList[idx]
                        val newId = (nextIdCounter++).toString()
                        val dup = target.copy(id = newId, name = "${target.name.substringBeforeLast(".")}_copy.mp4")
                        val mutable = clipList.toMutableList()
                        mutable.add(idx + 1, dup)
                        clipList = mutable
                        selectedClipId = newId
                    }
                }) {
                    Icon(Icons.Rounded.ControlPointDuplicate, contentDescription = "Duplicate")
                }
            }
        }

        // 3. Area Magnetic Timeline
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Header playback controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAGNETIC TRACK V1",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tombol Play / Pause Utama
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlay()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }
                }

                // Strip Klip Timeline Horizontal Scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(60.dp))

                    clipList.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val clipWidth = (clip.durationMs / 30).dp

                        Box(
                            modifier = Modifier
                                .width(clipWidth.coerceAtLeast(100.dp))
                                .height(80.dp)
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
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = clip.name,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "${clip.durationMs / 1000.0}s",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(60.dp))
                }

                // Playhead (Jarum Penunjuk Posisi Tengah)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    // Indikator kepala playhead segitiga / pil
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
}
