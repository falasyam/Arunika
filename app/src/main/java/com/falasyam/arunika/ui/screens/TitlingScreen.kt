package com.falasyam.arunika.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.components.VideoPlayerView
import kotlin.math.roundToInt

/**
 * Screen Titling & Track V2 Overlay (Fusion Lite)
 * Fitur:
 * - Interactive Draggable Canvas Overlay
 * - Presets: Lower Third, Opening Title, Subtitles/Captions, Chapter Markers
 * - Word-by-Word Viral Highlight Caption Style (Reels/Shorts/TikTok)
 * - Typography, Color, Outline, Background, and Opacity Controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitlingScreen(
    titlingState: TitlingState = remember { TitlingState() },
    keyframeState: KeyframeState = remember { KeyframeState() },
    aspectRatio: AspectRatioMode = AspectRatioMode.CINEMA_16_9,
    videoUri: String? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var selectedSubtab by remember { mutableIntStateOf(0) } // 0: Templates, 1: Text & Style, 2: Animation & Captions

    val selectedElement = titlingState.elements.find { it.id == selectedElementId } ?: titlingState.elements.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ========================================================
        // 1. VIDEO VIEWPORT WITH INTERACTIVE TEXT OVERLAYS
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0E0E11)),
            contentAlignment = Alignment.Center
        ) {
            val ratio = aspectRatio.ratioFloat
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(ratio, matchHeightConstraintsFirst = aspectRatio.isVertical)
                    .fillMaxHeight(if (aspectRatio.isVertical) 0.94f else 0.88f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16151D)),
                contentAlignment = Alignment.Center
            ) {
                val canvasWidthPx = constraints.maxWidth
                val canvasHeightPx = constraints.maxHeight

                // Background video preview jika ada, atau placeholder
                if (videoUri != null) {
                    VideoPlayerView(
                        uri = videoUri,
                        isPlaying = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.6f)
                    )
                } else {
                    Text(
                        text = "Track V1 Video Preview\n[Drag text overlay directly on screen]",
                        color = Color.White.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Render all Title Elements as overlays
                titlingState.elements.forEach { element ->
                    val isSelected = element.id == selectedElement?.id
                    var offsetX by remember(element.id) { mutableFloatStateOf(element.x) }
                    var offsetY by remember(element.id) { mutableFloatStateOf(element.y) }
                    val transform = keyframeState.interpolateTransform(0L)

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    (((offsetX - 0.5f) * canvasWidthPx) + transform.posX * canvasWidthPx / 100f).roundToInt(),
                                    (((offsetY - 0.5f) * canvasHeightPx) + transform.posY * canvasHeightPx / 100f).roundToInt()
                                )
                            }
                            .scale(transform.scale)
                            .alpha(transform.opacity)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(element.bgColor))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedElementId = element.id
                            }
                            .pointerInput(element.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount.x / canvasWidthPx).coerceIn(0f, 1f)
                                    offsetY = (offsetY + dragAmount.y / canvasHeightPx).coerceIn(0f, 1f)
                                    titlingState.updateElement(element.copy(x = offsetX, y = offsetY))
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (element.wordHighlight) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                element.text.split(" ").forEachIndexed { i, word ->
                                    Text(
                                        text = word,
                                        fontSize = element.fontSize.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (i == 1) Color(element.highlightColor) else Color(element.textColor)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = element.text,
                                fontSize = element.fontSize.sp,
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

                // Overlay Status Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7))
                        )
                        Text(
                            text = "Track V2 Overlay: ${titlingState.elements.size} Title(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ========================================================
        // 2. TITLING BOTTOM SHEET (TEMPLATES | STYLE | ANIMATION)
        // ========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtabs
                PrimaryTabRow(
                    selectedTabIndex = selectedSubtab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedSubtab == 0,
                        onClick = { selectedSubtab = 0 },
                        text = { Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSubtab == 1,
                        onClick = { selectedSubtab = 1 },
                        text = { Text("Text & Style", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSubtab == 2,
                        onClick = { selectedSubtab = 2 },
                        text = { Text("Captions & Motion", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedSubtab) {
                    // SUBTAB 0: PRESET TEMPLATES
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick Title Templates (Track V2)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val el = titlingState.addLowerThird("Dr. John Doe | Director of Photography")
                                        selectedElementId = el.id
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lower Third", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val el = titlingState.addOpeningTitle("ARUNIKA")
                                        selectedElementId = el.id
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Title, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Opening Title", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val el = titlingState.addCaption("Cinematic color grading made mobile 🔥")
                                        selectedElementId = el.id
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Viral Subtitle", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Active Overlays",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            titlingState.elements.forEach { el ->
                                val isSelected = el.id == selectedElement?.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedElementId = el.id
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(el.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${el.style.displayName} • ${el.animationType.displayName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(
                                            onClick = { titlingState.removeElement(el.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SUBTAB 1: TEXT & STYLE
                    1 -> {
                        if (selectedElement != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedElement.text,
                                    onValueChange = { titlingState.updateElement(selectedElement.copy(text = it)) },
                                    label = { Text("Overlay Text") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Font Size", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                    Slider(
                                        value = selectedElement.fontSize,
                                        onValueChange = { titlingState.updateElement(selectedElement.copy(fontSize = it)) },
                                        valueRange = 14f..60f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${selectedElement.fontSize.toInt()}sp", fontSize = 11.sp, modifier = Modifier.width(40.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Opacity", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                    Slider(
                                        value = selectedElement.opacity,
                                        onValueChange = { titlingState.updateElement(selectedElement.copy(opacity = it)) },
                                        valueRange = 0.1f..1.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(selectedElement.opacity * 100).toInt()}%", fontSize = 11.sp, modifier = Modifier.width(40.dp))
                                }
                            }
                        } else {
                            Text("Pilih atau buat title preset di tab Presets terlebih dahulu.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // SUBTAB 2: CAPTIONS & ANIMATION
                    2 -> {
                        if (selectedElement != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Word-by-Word Highlight (Viral Style)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Switch(
                                        checked = selectedElement.wordHighlight,
                                        onCheckedChange = { titlingState.updateElement(selectedElement.copy(wordHighlight = it)) }
                                    )
                                }

                                Text("In-Animation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TitleAnimation.values().forEach { anim ->
                                        FilterChip(
                                            selected = selectedElement.animationType == anim,
                                            onClick = { titlingState.updateElement(selectedElement.copy(animationType = anim)) },
                                            label = { Text(anim.displayName, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("Pilih atau buat title preset di tab Presets terlebih dahulu.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
