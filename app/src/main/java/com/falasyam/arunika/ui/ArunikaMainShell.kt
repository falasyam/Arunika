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
import androidx.compose.ui.unit.dp
import com.falasyam.arunika.model.*
import com.falasyam.arunika.ui.screens.AudioScreen
import com.falasyam.arunika.ui.screens.CutScreen
import com.falasyam.arunika.ui.screens.DeliverScreen

/**
 * Shell Navigasi Global Arunika:
 * - Mengintegrasikan 4 Tab Kerja (Cut, Color, Audio, Deliver)
 * - M3 Expressive: NavigationBar pada Portrait, otomatis NavigationRail pada Landscape.
 * - Transisi animasi halus saat perpindahan tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArunikaMainShell() {
    val haptic = LocalHapticFeedback.current
    var activeTab by remember { mutableStateOf(ArunikaTab.COLOR) }

    // Shared Project States
    val colorStudioState = remember { ColorStudioState() }
    val audioState = remember { AudioStudioState() }
    val deliverState = remember { DeliverExportState() }
    var isPlaying by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Arunika",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${activeTab.title} • ${activeTab.subtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Undo / Redo
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(Icons.Rounded.Redo, contentDescription = "Redo")
                        }

                        // Status Badge
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
                                isPlaying = isPlaying,
                                onTogglePlay = { isPlaying = !isPlaying }
                            )
                            ArunikaTab.COLOR -> ColorStudioScreen(
                                state = colorStudioState,
                                onNavigateTab = { tabName ->
                                    activeTab = ArunikaTab.valueOf(tabName)
                                }
                            )
                            ArunikaTab.AUDIO -> AudioScreen(state = audioState)
                            ArunikaTab.DELIVER -> DeliverScreen(state = deliverState)
                        }
                    }
                }
            }
        }
    }
}
