package com.falasyam.arunika.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.AspectRatioMode
import com.falasyam.arunika.model.ClipMediaType
import com.falasyam.arunika.model.ProjectItem
import com.falasyam.arunika.model.TimelineClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Halaman Depan / Project Manager Arunika:
 * - Menampilkan daftar projek pengguna dengan preview thumbnail, rasio aspek, durasi, dan klip
 * - Quick Action pembuatan projek baru (16:9 Cinema, 9:16 Shorts/Reels, 1:1 Feed)
 * - Fitur pencarian projek & filter kategori
 * - Menu aksi projek (Buka, Duplikat, Ganti Nama, Hapus)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projects: List<ProjectItem>,
    onOpenProject: (ProjectItem) -> Unit,
    onCreateProject: (ProjectItem) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDuplicateProject: (ProjectItem) -> Unit,
    onRenameProject: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectItem?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectItem?>(null) }

    // Filter list
    val filteredProjects = remember(projects, searchQuery, selectedFilter) {
        projects.filter { proj ->
            val matchesSearch = searchQuery.isBlank() || proj.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Semua" -> true
                "16:9 Cinema" -> proj.aspectRatio == AspectRatioMode.CINEMA_16_9
                "9:16 Vertikal" -> proj.aspectRatio == AspectRatioMode.REELS_9_16
                "1:1 Feed" -> proj.aspectRatio == AspectRatioMode.SQUARE_1_1
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.MovieFilter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Arunika",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Studio Pascakomposisi & Color Grading",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreateDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Projek Baru", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama projek...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Semua", "16:9 Cinema", "9:16 Vertikal", "1:1 Feed")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilter = filter
                            },
                            label = { Text(filter, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 2. Banner Mulai Cepat (Quick Start Presets)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mulai Cepat dengan Format Aspek Rasio:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Shortcut 16:9
                        QuickPresetButton(
                            title = "16:9 YouTube",
                            subtitle = "Cinema 4K",
                            icon = Icons.Rounded.Tv,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val newProj = ProjectItem(
                                    id = "proj-${System.currentTimeMillis()}",
                                    name = "Cinema Baru 16:9",
                                    lastModified = "Baru saja",
                                    durationMs = 0L,
                                    resolutionLabel = "4K UHD",
                                    aspectRatio = AspectRatioMode.CINEMA_16_9,
                                    videoClips = emptyList(),
                                    audioClips = emptyList(),
                                    gradientColors = listOf(0xFF1E3A8A, 0xFF3B82F6)
                                )
                                onCreateProject(newProj)
                            }
                        )

                        // Shortcut 9:16
                        QuickPresetButton(
                            title = "9:16 Shorts",
                            subtitle = "TikTok/Reels",
                            icon = Icons.Rounded.StayCurrentPortrait,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val newProj = ProjectItem(
                                    id = "proj-${System.currentTimeMillis()}",
                                    name = "Shorts Baru 9:16",
                                    lastModified = "Baru saja",
                                    durationMs = 0L,
                                    resolutionLabel = "1080p FHD",
                                    aspectRatio = AspectRatioMode.REELS_9_16,
                                    videoClips = emptyList(),
                                    audioClips = emptyList(),
                                    gradientColors = listOf(0xFF831843, 0xFFBE185D)
                                )
                                onCreateProject(newProj)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Grid Daftar Projek
            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Tidak ada projek bernama '$searchQuery'" else "Belum ada projek",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buat Projek Pertama")
                        }
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth > 600.dp) 2 else 1

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenProject(project)
                                },
                                onDuplicate = { onDuplicateProject(project) },
                                onRename = { projectToRename = project },
                                onDelete = { projectToDelete = project }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Buat Projek Baru
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { newProj ->
                showCreateDialog = false
                onCreateProject(newProj)
            }
        )
    }

    // Dialog: Ubah Nama Projek
    projectToRename?.let { proj ->
        RenameProjectDialog(
            project = proj,
            onDismiss = { projectToRename = null },
            onRename = { newName ->
                onRenameProject(proj.id, newName)
                projectToRename = null
            }
        )
    }

    // Dialog: Konfirmasi Hapus Projek
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Projek?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${proj.name}'? Aksi ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun QuickPresetButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Kartu Representasi Projek
 */
@Composable
fun ProjectCard(
    project: ProjectItem,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var projectThumbnail by remember(project.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(project.id, project.videoClips) {
        projectThumbnail = null
        val clip = project.videoClips.firstOrNull {
            it.mediaType == ClipMediaType.VIDEO && it.uriString != null
        }
        if (clip?.uriString != null) {
            projectThumbnail = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uriString))
                    retriever.extractThumbnailFrame(
                        (clip.sourceOffsetMs * 1000).coerceAtMost(1_000_000_000)
                    )
                } catch (exception: Exception) {
                    Log.w("ProjectListScreen", "Could not create thumbnail for ${project.name}", exception)
                    null
                } finally {
                    retriever.release()
                }
            }
        }
    }

    DisposableEffect(projectThumbnail) {
        val thumbnail = projectThumbnail
        onDispose { thumbnail?.recycle() }
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Visual Preview Thumbnail Box
            val gradientColors = project.gradientColors.map { Color(it) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                // Bingkai indikasi rasio aspek
                val frameRatio = if (project.aspectRatio.isVertical) 9f / 16f else 16f / 9f
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .aspectRatio(frameRatio)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                        projectThumbnail?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Thumbnail ${project.name}",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.PlayCircleOutline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                }

                // Badge Rasio Aspek (Kiri Atas)
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (project.aspectRatio.isVertical) "9:16 Vertikal" else "16:9 Cinema",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Badge Durasi Total (Kanan Bawah)
                val totalSeconds = (project.durationMs / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Project Details & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${project.resolutionLabel} • ${project.fps}fps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val videoCount = project.videoClips.count { it.mediaType == ClipMediaType.VIDEO }
                        val photoCount = project.videoClips.count { it.mediaType == ClipMediaType.PHOTO }
                        Text(
                            text = "$videoCount Video • $photoCount Foto • ${project.audioClips.size} Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.lastModified,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Menu Opsi Tiga Titik
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Opsi Projek",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Buka Editor") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplikat") },
                            leadingIcon = { Icon(Icons.Rounded.ControlPointDuplicate, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ganti Nama") },
                            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog Pembuatan Projek Baru
 */
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (ProjectItem) -> Unit
) {
    var name by remember { mutableStateOf("Projek Arunika Baru") }
    var selectedRatio by remember { mutableStateOf(AspectRatioMode.CINEMA_16_9) }
    var selectedResolution by remember { mutableStateOf("4K UHD") }
    var selectedFps by remember { mutableStateOf(24) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Projek Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Projek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Format Aspek Rasio:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratios = listOf(
                        AspectRatioMode.CINEMA_16_9 to "16:9 Cinema",
                        AspectRatioMode.REELS_9_16 to "9:16 Shorts",
                        AspectRatioMode.SQUARE_1_1 to "1:1 Square",
                        AspectRatioMode.IG_PORTRAIT_4_5 to "4:5 IG Feed"
                    )
                    ratios.forEach { (mode, label) ->
                        FilterChip(
                            selected = selectedRatio == mode,
                            onClick = { selectedRatio = mode },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Resolusi Master:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1080p FHD", "4K UHD").forEach { res ->
                        FilterChip(
                            selected = selectedResolution == res,
                            onClick = { selectedResolution = res },
                            label = { Text(res, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Frame Rate:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 30, 60).forEach { fps ->
                        FilterChip(
                            selected = selectedFps == fps,
                            onClick = { selectedFps = fps },
                            label = { Text("${fps} fps", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val colors = when (selectedRatio) {
                        AspectRatioMode.REELS_9_16 -> listOf(0xFF831843, 0xFFBE185D)
                        AspectRatioMode.SQUARE_1_1 -> listOf(0xFF312E81, 0xFF4F46E5)
                        else -> listOf(0xFF1E3A8A, 0xFF3B82F6)
                    }
                    val newProj = ProjectItem(
                        id = "proj-${System.currentTimeMillis()}",
                        name = if (name.isBlank()) "Projek Arunika" else name,
                        lastModified = "Baru saja",
                        durationMs = 0L,
                        resolutionLabel = selectedResolution,
                        fps = selectedFps,
                        aspectRatio = selectedRatio,
                        videoClips = emptyList(),
                        audioClips = emptyList(),
                        gradientColors = colors
                    )
                    onCreate(newProj)
                }
            ) {
                Text("Mulai Mengedit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Dialog Ubah Nama Projek
 */
@Composable
fun RenameProjectDialog(
    project: ProjectItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(project.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti Nama Projek") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nama Baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) onRename(newName)
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

