package com.falasyam.arunika.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.screens.AudioScreen
import com.falasyam.arunika.ui.screens.CutScreen
import com.falasyam.arunika.ui.screens.DeliverScreen
import com.falasyam.arunika.ui.screens.ProjectListScreen
import com.falasyam.arunika.ui.screens.TitlingScreen
import com.falasyam.arunika.model.TitlingState
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import android.net.Uri
import androidx.compose.runtime.DisposableEffect

private data class GradeNodeSnapshot(
    val id: Int,
    val name: String,
    val enabled: Boolean,
    val liftX: Float,
    val liftY: Float,
    val liftLum: Float,
    val gammaX: Float,
    val gammaY: Float,
    val gammaLum: Float,
    val gainX: Float,
    val gainY: Float,
    val gainLum: Float,
    val offsetX: Float,
    val offsetY: Float,
    val offsetLum: Float,
    val temperature: Float,
    val tint: Float,
    val contrast: Float,
    val saturation: Float,
    val lut: LutPreset?,
    val lutIntensity: Float,
    val vignette: Float
)

private data class EditorSnapshot(
    val audio: AudioStudioState,
    val titling: List<TitleElement>,
    val keyframes: List<Keyframe>,
    val activeNodeIndex: Int,
    val activeTarget: WheelTarget,
    val inputLogProfile: CameraLogProfile,
    val scopeMode: ScopeMode,
    val splitCompareEnabled: Boolean,
    val splitPosition: Float,
    val nodes: List<GradeNodeSnapshot>
)

/**
 * Shell Navigasi Global Arunika:
 * - Halaman Depan / Project Manager (Daftar Projek & Buat Projek Baru)
 * - Editor Pascakomposisi dengan 4 Tab Kerja (Cut, Color, Audio, Deliver)
 * - M3 Expressive: NavigationBar pada Portrait, otomatis NavigationRail pada Landscape
 * - Transisi animasi halus saat perpindahan projek dan tab
 */
@Composable
fun ArunikaMainShell() {
    // Project Manager State
    var projects by remember { mutableStateOf(ProjectPresets.createDefaultProjects()) }
    var activeProject by remember { mutableStateOf<ProjectItem?>(null) }

    AnimatedContent(
        targetState = activeProject,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ProjectToEditorTransition"
    ) { currentProj ->
        if (currentProj == null) {
            // HALAMAN DEPAN: Daftar Projek & Pembuatan Projek Baru
            ProjectListScreen(
                projects = projects,
                onOpenProject = { proj ->
                    activeProject = proj
                },
                onCreateProject = { newProj ->
                    projects = listOf(newProj) + projects
                    activeProject = newProj
                },
                onDeleteProject = { idToDelete ->
                    projects = projects.filterNot { it.id == idToDelete }
                },
                onDuplicateProject = { proj ->
                    val dup = proj.copy(
                        id = "proj-${System.currentTimeMillis()}",
                        name = "${proj.name} (Salinan)",
                        lastModified = "Baru saja"
                    )
                    projects = listOf(dup) + projects
                },
                onRenameProject = { idToRename, newName ->
                    projects = projects.map {
                        if (it.id == idToRename) it.copy(name = newName) else it
                    }
                }
            )
        } else {
            // EDITOR SHELL: Workspace Aktif (Cut, Color, Audio, Deliver)
            ArunikaEditorShell(
                project = currentProj,
                onBackToProjects = {
                    activeProject = null
                }
            )
        }
    }
}

/**
 * Shell Editor untuk Projek yang Sedang Aktif
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArunikaEditorShell(
    project: ProjectItem,
    onBackToProjects: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
        val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ArunikaTab.CUT) }

    // Shared Project States
    val colorStudioState = remember { ColorStudioState() }
    val audioState = remember { AudioStudioState() }
    val titlingState = remember { TitlingState() }
    val keyframeState = remember { KeyframeState() }
    val deliverState = remember { DeliverExportState() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentVideoUri by remember(project) { mutableStateOf<String?>(project.videoClips.firstOrNull()?.uriString) }

    // Shared ExoPlayer instance for both video playback and audio processing
    val sharedExoPlayer = remember(currentVideoUri) {
        ExoPlayer.Builder(context).build().apply {
            if (currentVideoUri != null) {
                setMediaItem(MediaItem.fromUri(Uri.parse(currentVideoUri)))
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                prepare()
            }
        }
    }

    // Update media item when URI changes
    LaunchedEffect(currentVideoUri) {
        if (currentVideoUri != null) {
            sharedExoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(currentVideoUri)))
            sharedExoPlayer.prepare()
        }
    }

    // Sync play/pause state
    LaunchedEffect(isPlaying) {
        if (isPlaying) sharedExoPlayer.play() else sharedExoPlayer.pause()
    }

    // Release player on dispose
    DisposableEffect(sharedExoPlayer) {
        onDispose { sharedExoPlayer.release() }
    }

    fun captureSnapshot(): EditorSnapshot {
        val nodeSnapshots = colorStudioState.nodes.map { node ->
            GradeNodeSnapshot(
                id = node.id,
                name = node.name,
                enabled = node.isEnabled,
                liftX = node.lift.x,
                liftY = node.lift.y,
                liftLum = node.lift.luminance,
                gammaX = node.gamma.x,
                gammaY = node.gamma.y,
                gammaLum = node.gamma.luminance,
                gainX = node.gain.x,
                gainY = node.gain.y,
                gainLum = node.gain.luminance,
                offsetX = node.offset.x,
                offsetY = node.offset.y,
                offsetLum = node.offset.luminance,
                temperature = node.temperature,
                tint = node.tint,
                contrast = node.contrast,
                saturation = node.saturation,
                lut = node.selectedLut,
                lutIntensity = node.lutIntensity,
                vignette = node.vignette
            )
        }
        return EditorSnapshot(
            audio = audioState.copy(),
            titling = titlingState.elements.toList(),
            keyframes = keyframeState.keyframes.toList(),
            activeNodeIndex = colorStudioState.activeNodeIndex,
            activeTarget = colorStudioState.activeTarget,
            inputLogProfile = colorStudioState.inputLogProfile,
            scopeMode = colorStudioState.scopeMode,
            splitCompareEnabled = colorStudioState.isSplitCompareEnabled,
            splitPosition = colorStudioState.splitPosition,
            nodes = nodeSnapshots
        )
    }

    fun restoreSnapshot(snapshot: EditorSnapshot) {
        audioState.masterVolume = snapshot.audio.masterVolume
        audioState.voiceIsolation = snapshot.audio.voiceIsolation
        audioState.deReverb = snapshot.audio.deReverb
        audioState.autoDucking = snapshot.audio.autoDucking
        audioState.duckingThresholdDb = snapshot.audio.duckingThresholdDb
        audioState.bassGainDb = snapshot.audio.bassGainDb
        audioState.midGainDb = snapshot.audio.midGainDb
        audioState.trebleGainDb = snapshot.audio.trebleGainDb

        titlingState.elements.clear()
        titlingState.elements.addAll(snapshot.titling)
        keyframeState.keyframes.clear()
        keyframeState.keyframes.addAll(snapshot.keyframes)

        colorStudioState.activeNodeIndex = snapshot.activeNodeIndex
        colorStudioState.activeTarget = snapshot.activeTarget
        colorStudioState.inputLogProfile = snapshot.inputLogProfile
        colorStudioState.scopeMode = snapshot.scopeMode
        colorStudioState.isSplitCompareEnabled = snapshot.splitCompareEnabled
        colorStudioState.splitPosition = snapshot.splitPosition

        snapshot.nodes.forEachIndexed { index, snap ->
            if (index in colorStudioState.nodes.indices) {
                val node = colorStudioState.nodes[index]
                node.name = snap.name
                node.isEnabled = snap.enabled
                node.lift.x = snap.liftX
                node.lift.y = snap.liftY
                node.lift.luminance = snap.liftLum
                node.gamma.x = snap.gammaX
                node.gamma.y = snap.gammaY
                node.gamma.luminance = snap.gammaLum
                node.gain.x = snap.gainX
                node.gain.y = snap.gainY
                node.gain.luminance = snap.gainLum
                node.offset.x = snap.offsetX
                node.offset.y = snap.offsetY
                node.offset.luminance = snap.offsetLum
                node.temperature = snap.temperature
                node.tint = snap.tint
                node.contrast = snap.contrast
                node.saturation = snap.saturation
                node.selectedLut = snap.lut
                node.lutIntensity = snap.lutIntensity
                node.vignette = snap.vignette
            }
        }
    }

    // Undo/Redo History menyimpan snapshot nyata state editor.
    val undoStack = remember { ArrayDeque<EditorSnapshot>() }
    val redoStack = remember { ArrayDeque<EditorSnapshot>() }
    var lastActionLabel by remember { mutableStateOf<String?>(null) }
    var showUndoToast by remember { mutableStateOf(false) }

    LaunchedEffect(showUndoToast) {
        if (showUndoToast) {
            kotlinx.coroutines.delay(1500)
            showUndoToast = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackToProjects()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Kembali ke Daftar Projek",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = project.resolutionLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${activeTab.title} • ${activeTab.subtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Undo / Redo
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (undoStack.isNotEmpty()) {
                                    val snapshot = undoStack.removeLast()
                                    redoStack.addLast(captureSnapshot())
                                    restoreSnapshot(snapshot)
                                    lastActionLabel = "Undo: state editor"
                                    showUndoToast = true
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (redoStack.isNotEmpty()) {
                                    val snapshot = redoStack.removeLast()
                                    undoStack.addLast(captureSnapshot())
                                    restoreSnapshot(snapshot)
                                    lastActionLabel = "Redo: state editor"
                                    showUndoToast = true
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Redo, contentDescription = "Redo")
                        }

                        // Export Button
                        FilledTonalButton(
                            onClick = { activeTab = ArunikaTab.DELIVER },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Tampilkan NavigationBar hanya di mode Portrait
                if (!isLandscape) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 4.dp
                    ) {
                        ArunikaTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (activeTab != tab) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        undoStack.addLast(captureSnapshot())
                                        redoStack.clear()
                                        activeTab = tab
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                             ArunikaTab.CUT -> Icons.Rounded.ContentCut
                                            ArunikaTab.COLOR -> Icons.Rounded.Palette
                                            ArunikaTab.AUDIO -> Icons.Rounded.GraphicEq
                                            ArunikaTab.TITLING -> Icons.Rounded.Title
                                            ArunikaTab.DELIVER -> Icons.Rounded.FileDownload
                                        },
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NavigationRail otomatis di mode Landscape (Tablet / HP diputar horizontal)
                if (isLandscape) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        ArunikaTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (activeTab != tab) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        undoStack.addLast(captureSnapshot())
                                        redoStack.clear()
                                        activeTab = tab
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            ArunikaTab.CUT -> Icons.Rounded.ContentCut
                                            ArunikaTab.COLOR -> Icons.Rounded.Palette
                                            ArunikaTab.AUDIO -> Icons.Rounded.GraphicEq
                                            ArunikaTab.TITLING -> Icons.Rounded.Title
                                            ArunikaTab.DELIVER -> Icons.Rounded.FileDownload
                                        },
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }

                // Area Konten Aktif dengan Animasi Transisi Halus
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            ArunikaTab.CUT -> CutScreen(
                                initialClips = project.videoClips,
                                initialAudioClips = project.audioClips,
                                aspectRatio = project.aspectRatio,
                                colorStudioState = colorStudioState,
                                titlingState = titlingState,
                                keyframeState = keyframeState,
                                currentVideoUri = currentVideoUri,
                                onVideoUriChanged = { currentVideoUri = it },
                                isPlaying = isPlaying,
                                                            onTogglePlay = { isPlaying = !isPlaying },
                                                            sharedExoPlayer = sharedExoPlayer
                            )
                            ArunikaTab.COLOR -> ColorStudioScreen(
                                state = colorStudioState,
                                aspectRatio = project.aspectRatio,
                                videoUri = currentVideoUri,
                                isPlaying = isPlaying,
                                onTogglePlay = { isPlaying = !isPlaying },
                                onNavigateTab = { tabName ->
                                    activeTab = ArunikaTab.valueOf(tabName)
                                                            },
                                                            sharedExoPlayer = sharedExoPlayer
                            )
                            ArunikaTab.AUDIO -> AudioScreen(state = audioState, exoPlayer = sharedExoPlayer)
                            ArunikaTab.TITLING -> TitlingScreen(
                                titlingState = titlingState,
                                keyframeState = keyframeState,
                                aspectRatio = project.aspectRatio,
                                videoUri = currentVideoUri
                            )
                            ArunikaTab.DELIVER -> DeliverScreen(
                                state = deliverState,
                                videoUri = currentVideoUri,
                                colorStudioState = colorStudioState
                            )
                        }
                    }
                }
            }
        }

        // Floating HUD Feedback saat Undo / Redo
        androidx.compose.animation.AnimatedVisibility(
            visible = showUndoToast && lastActionLabel != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Text(
                    text = lastActionLabel ?: "",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
