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
    var activeTab by remember { mutableStateOf(ArunikaTab.CUT) }

    // Shared Project States
    val colorStudioState = remember { ColorStudioState() }
    val audioState = remember { AudioStudioState() }
    val titlingState = remember { TitlingState() }
    val deliverState = remember { DeliverExportState() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentVideoUri by remember { mutableStateOf<String?>(null) }

    // Undo/Redo History (action labels untuk feedback visual)
    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
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
                                    val action = undoStack.removeLast()
                                    redoStack.addLast(action)
                                    lastActionLabel = "Undo: $action"
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
                                    val action = redoStack.removeLast()
                                    undoStack.addLast(action)
                                    lastActionLabel = "Redo: $action"
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
                                currentVideoUri = currentVideoUri,
                                onVideoUriChanged = { currentVideoUri = it },
                                isPlaying = isPlaying,
                                onTogglePlay = { isPlaying = !isPlaying }
                            )
                            ArunikaTab.COLOR -> ColorStudioScreen(
                                state = colorStudioState,
                                videoUri = currentVideoUri,
                                isPlaying = isPlaying,
                                onTogglePlay = { isPlaying = !isPlaying },
                                onNavigateTab = { tabName ->
                                    activeTab = ArunikaTab.valueOf(tabName)
                                }
                            )
                            ArunikaTab.AUDIO -> AudioScreen(state = audioState)
                            ArunikaTab.TITLING -> TitlingScreen(titlingState = titlingState)
                            ArunikaTab.DELIVER -> DeliverScreen(state = deliverState)
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
