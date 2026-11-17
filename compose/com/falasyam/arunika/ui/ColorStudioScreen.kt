package com.falasyam.arunika.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.components.ColorGradingOverlay
import com.falasyam.arunika.ui.components.InteractiveColorWheel
import com.falasyam.arunika.ui.components.RealtimeScopeMonitor
import com.falasyam.arunika.ui.components.SplitScreenCompareOverlay
import com.falasyam.arunika.ui.components.VideoPlayerView

/**
 * Color Studio Pro:
 * - 32-bit Floating Point Color Engine
 * - Modul 1: Pro Scopes (Waveform / Vectorscope) & Split-Screen Wipe Compare
 * - Modul 2: CST (Color Space Transform) & 3D LUT Engine
 * - Modul 3: Media Ingestion Picker (Import Video/Foto Sendiri)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorStudioScreen(
    state: ColorStudioState = remember { ColorStudioState() },
    videoUri: String? = null,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Launcher untuk memilih file LUT (.cube, .3dl) dari file manager
    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "custom_lut.cube"
                state.importLut(content = content, fileName = fileName)
            } catch (_: Exception) {}
        }
    }

    var isBypassed by remember { mutableStateOf(false) }
    var selectedColorToolTab by remember { mutableStateOf(0) } // 0: Wheels, 1: CST & LUT, 2: Curves

    val lutPresets = remember {
        listOf(
            LutPreset("1", "Teal & Orange", "Look bioskop Hollywood modern", 0xFF0EA5E9),
            LutPreset("2", "Portra 400", "Emulasi film analog Kodak hangat", 0xFFF59E0B),
            LutPreset("3", "Fuji Eterna", "Soft shadow dan skin-tone natural", 0xFF10B981),
            LutPreset("4", "Cyberpunk Neon", "Warna kontras tinggi malam hari", 0xFFA855F7),
            LutPreset("5", "Film Noir", "Monokrom dramatis high-contrast", 0xFF64748B)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ========================================================
        // 1. MONITOR PREVIEW VIDEO & PRO SCOPES OVERLAY
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0E0E11)),
            contentAlignment = Alignment.Center
        ) {
            // Viewport Video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A20)),
                contentAlignment = Alignment.Center
            ) {
                // Video preview nyata — URI dibagikan dari timeline CutScreen
                VideoPlayerView(
                    uri = videoUri,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )

                // Real-time Color Grading Effect Overlay (Wheels, LUT, CST, Luma)
                ColorGradingOverlay(
                    state = state,
                    isBypassed = isBypassed,
                    isSplitEnabled = state.isSplitCompareEnabled,
                    splitPosition = state.splitPosition,
                    modifier = Modifier.fillMaxSize()
                )

                // Split Screen Compare Overlay
                SplitScreenCompareOverlay(
                    isSplitEnabled = state.isSplitCompareEnabled,
                    splitPosition = state.splitPosition,
                    onSplitPositionChange = { state.splitPosition = it }
                )

                // Tombol Play / Pause di sudut bawah tengah monitor
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(36.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlay()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pro Scopes Monitor di sudut kanan atas video
                RealtimeScopeMonitor(
                    mode = state.scopeMode,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                // Floating Toolbar di atas monitor (Scopes, Split-Compare, Import Media)
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tombol Ganti Scopes
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                state.scopeMode = when (state.scopeMode) {
                                    ScopeMode.OFF -> ScopeMode.WAVEFORM
                                    ScopeMode.WAVEFORM -> ScopeMode.VECTORSCOPE
                                    ScopeMode.VECTORSCOPE -> ScopeMode.OFF
                                    else -> ScopeMode.OFF
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Insights,
                                contentDescription = "Toggle Scopes",
                                tint = if (state.scopeMode != ScopeMode.OFF) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Tombol Split Compare
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                state.isSplitCompareEnabled = !state.isSplitCompareEnabled
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Compare,
                                contentDescription = "Split Screen Compare",
                                tint = if (state.isSplitCompareEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Tombol Import Video / Foto
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.userMediaUri = "gallery_video_sample_01.mp4"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ========================================================
        // 1.5 DAVINCI RESOLVE STYLE NODE GRAPH STRIP
        // ========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // In connection point
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("IN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }

                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )

                // List of Nodes in Serial Chain
                state.nodes.forEachIndexed { index, node ->
                    val isSelected = state.activeNodeIndex == index
                    val isEnabled = node.isEnabled

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.activeNodeIndex = index
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Bypass LED Toggle
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) Color(0xFF22C55E) else Color(0xFFEF4444))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        state.toggleNodeBypass(index)
                                    }
                            )

                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (isEnabled) "Active" else "Bypassed",
                                    fontSize = 9.sp,
                                    color = if (isEnabled) MaterialTheme.colorScheme.outline else Color(0xFFEF4444)
                                )
                            }

                            if (state.nodes.size > 1) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        state.deleteNode(index)
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Delete Node",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    if (index < state.nodes.lastIndex) {
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )

                // Out connection point
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                        .border(1.5.dp, Color(0xFF22C55E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OUT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // + Serial Node Button
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.addSerialNode()
                    },
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Serial", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ========================================================
        // 2. BOTTOM CONTROL SHEET (WHEELS, CST/LUT, CURVES)
        // ========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp, max = 390.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Tool Tabs: Wheels | CST & LUT | Curves
                PrimaryTabRow(
                    selectedTabIndex = selectedColorToolTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedColorToolTab == 0,
                        onClick = { selectedColorToolTab = 0 },
                        text = { Text("Color Wheels", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedColorToolTab == 1,
                        onClick = { selectedColorToolTab = 1 },
                        text = { Text("CST & 3D LUT", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedColorToolTab == 2,
                        onClick = { selectedColorToolTab = 2 },
                        text = { Text("HSL Curves", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedColorToolTab) {
                    // TAB 0: 3-WAY COLOR WHEELS
                    0 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Segmented Button: Lift | Gamma | Gain | Offset
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                WheelTarget.values().forEachIndexed { index, target ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            state.activeTarget = target
                                        },
                                        selected = state.activeTarget == target
                                    ) {
                                        Text(text = target.label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Canvas Interactive Wheel (sudah mencakup info roda, kanvas roda, dan slider luminance Y rapi tanpa tumpang tindih)
                            InteractiveColorWheel(
                                value = state.activeValue(),
                                label = state.activeTarget.label,
                                wheelSize = 145f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // TAB 1: COLOR SPACE TRANSFORM & 3D LUT ENGINE
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // CST Input Profile Dropdown/Chips
                            Text(
                                text = "Camera Input Profile (CST)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CameraLogProfile.values().forEach { profile ->
                                    FilterChip(
                                        selected = state.inputLogProfile == profile,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            state.inputLogProfile = profile
                                        },
                                        label = { Text(profile.label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // 3D LUT Header with Import Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "3D LUTs & Film Emulation (.cube, .3dl, .vlt)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Buka file manager untuk memilih file LUT (.cube, .3dl)
                                        lutPickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import LUT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Horizontal list combining Built-in & Custom LUTs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val allLuts = lutPresets + state.customLuts

                                allLuts.forEach { lut ->
                                    val isSelected = state.selectedLut?.id == lut.id
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                state.selectedLut = if (isSelected) null else lut
                                            }
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = lut.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (lut.isCustom) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = lut.format.extension.uppercase(),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = lut.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // LUT Opacity / Mix Slider
                            if (state.selectedLut != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("LUT Opacity", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = state.lutIntensity,
                                        onValueChange = { state.lutIntensity = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${(state.lutIntensity * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // TAB 2: HSL CURVES
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Hue vs Saturation Curve",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Interactive Spline Canvas
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "[Interactive HSL Spline: 6-Color Hue Anchors]\nRed • Yellow • Green • Cyan • Blue • Magenta",
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
