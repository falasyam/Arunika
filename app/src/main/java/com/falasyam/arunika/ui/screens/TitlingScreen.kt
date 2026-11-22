package com.falasyam.arunika.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.customTitleFontFile
import com.falasyam.arunika.ui.importCustomTitleFont
import com.falasyam.arunika.ui.resolveTitleFontFamily
import kotlinx.coroutines.launch
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
    isPlaying: Boolean = false,
    sharedExoPlayer: ExoPlayer? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fontPickerTargetElementId by remember { mutableStateOf<String?>(null) }
    var fontImportError by remember { mutableStateOf<String?>(null) }
    val importedFontNames = remember { mutableStateMapOf<String, String>() }
    val customFontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetElementId = fontPickerTargetElementId
        if (uri != null && targetElementId != null) {
            coroutineScope.launch {
                try {
                    val importedFont = importCustomTitleFont(context, uri, targetElementId)
                    val targetElement = titlingState.elements.find { it.id == targetElementId }
                    if (targetElement != null) {
                        titlingState.updateElement(targetElement.copy(font = TitleFont.CUSTOM))
                        importedFontNames[targetElementId] = importedFont.displayName
                        fontImportError = null
                    }
                } catch (exception: java.io.IOException) {
                    fontImportError = exception.message ?: "Font tidak dapat diimpor."
                } catch (exception: SecurityException) {
                    fontImportError = "Akses ke file font ditolak."
                }
            }
        }
    }
    var selectedSubtab by remember(titlingState) {
        mutableIntStateOf(if (titlingState.selectedElementId != null) 1 else 0)
    } // 0: Templates, 1: Text & Style, 2: Animation & Captions

    val selectedElement = titlingState.elements.find { it.id == titlingState.selectedElementId }
        ?: titlingState.elements.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
                        isPlaying = isPlaying,
                        sharedExoPlayer = sharedExoPlayer,
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
                    val latestElement by rememberUpdatedState(element)
                    val titleFontFamily = remember(
                        element.id,
                        element.font,
                        importedFontNames[element.id]
                    ) {
                        resolveTitleFontFamily(context, element)
                    }
                    val transform = keyframeState.interpolateTransform(0L)

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
                            .alpha((element.opacity * transform.opacity).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(element.bgColor))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                titlingState.selectedElementId = element.id
                            }
                            .pointerInput(element.id, canvasWidthPx, canvasHeightPx) {
                                var offsetX = latestElement.x
                                var offsetY = latestElement.y
                                detectDragGestures(
                                    onDragStart = {
                                        offsetX = latestElement.x
                                        offsetY = latestElement.y
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount.x / canvasWidthPx).coerceIn(0f, 1f)
                                    offsetY = (offsetY + dragAmount.y / canvasHeightPx).coerceIn(0f, 1f)
                                    titlingState.updateElement(latestElement.copy(x = offsetX, y = offsetY))
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
                                fontFamily = titleFontFamily
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
                .weight(1f, fill = false)
                .heightIn(max = 340.dp),
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
                            OutlinedButton(
                                onClick = {
                                    val element = titlingState.addPlainText()
                                    titlingState.selectedElementId = element.id
                                    selectedSubtab = 1
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Teks kosong")
                            }

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
                                        titlingState.selectedElementId = el.id
                                        selectedSubtab = 1
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
                                        titlingState.selectedElementId = el.id
                                        selectedSubtab = 1
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
                                        titlingState.selectedElementId = el.id
                                        selectedSubtab = 1
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
                                            titlingState.selectedElementId = el.id
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

                                var fontMenuExpanded by remember(selectedElement.id) { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Font bawaan", fontSize = 12.sp)
                                    Box {
                                        OutlinedButton(onClick = { fontMenuExpanded = true }) {
                                            Text(selectedElement.font.displayName)
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(
                                            expanded = fontMenuExpanded,
                                            onDismissRequest = { fontMenuExpanded = false }
                                        ) {
                                            TitleFont.values().filter { it != TitleFont.CUSTOM }.forEach { font ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            font.displayName,
                                                            fontFamily = when (font) {
                                                                TitleFont.SERIF, TitleFont.CINEMATIC -> FontFamily.Serif
                                                                TitleFont.MONO -> FontFamily.Monospace
                                                                TitleFont.HANDWRITTEN -> FontFamily.Cursive
                                                                TitleFont.SANS -> FontFamily.Default
                                                                TitleFont.CUSTOM -> FontFamily.Default
                                                            }
                                                        )
                                                    },
                                                    onClick = {
                                                        titlingState.updateElement(selectedElement.copy(font = font))
                                                        fontMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        fontPickerTargetElementId = selectedElement.id
                                        fontImportError = null
                                        customFontPicker.launch(
                                            arrayOf(
                                                "font/*",
                                                "application/x-font-ttf",
                                                "application/x-font-opentype",
                                                "application/octet-stream"
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pilih font dari file")
                                }
                                if (selectedElement.font == TitleFont.CUSTOM) {
                                    val customFontFile = customTitleFontFile(context, selectedElement.id)
                                    Text(
                                        text = importedFontNames[selectedElement.id]
                                            ?: if (customFontFile.isFile) "Font custom aktif" else "File font tidak ditemukan. Impor ulang.",
                                        color = if (customFontFile.isFile) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                fontImportError?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

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
                                        valueRange = 0.0f..1.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(selectedElement.opacity * 100).toInt()}%", fontSize = 11.sp, modifier = Modifier.width(40.dp))
                                }
                            }
                        } else {
                            Text("Tambah teks kosong atau pilih salah satu template.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Tambah teks kosong atau pilih salah satu template.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
