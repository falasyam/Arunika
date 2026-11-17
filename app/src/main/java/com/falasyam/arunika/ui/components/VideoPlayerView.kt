package com.falasyam.arunika.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Komponen pemutaran video nyata menggunakan AndroidView + VideoView bawaan Android.
 * - Mendukung play/pause real via VideoView.start()/pause()
 * - Error state jika format tidak didukung
 * - Placeholder informatif saat belum ada klip dipilih
 */
@Composable
fun VideoPlayerView(
    uri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onDurationReady: (Long) -> Unit = {}
) {
    if (uri != null) {
        var hasError by remember { mutableStateOf(false) }
        LaunchedEffect(uri) { hasError = false }
        Box(modifier = modifier) {
            if (!hasError) {
                key(uri) {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoURI(Uri.parse(uri))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    onDurationReady(mp.duration.toLong())
                                    if (isPlaying) start()
                                }
                                setOnErrorListener { _, _, _ ->
                                    hasError = true
                                    true
                                }
                            }
                        },
                        update = { videoView ->
                            if (isPlaying && !videoView.isPlaying) videoView.start()
                            else if (!isPlaying && videoView.isPlaying) videoView.pause()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Format tidak didukung
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E24)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.VideocamOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Format video tidak didukung",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    } else {
        // Belum ada video dipilih — tampilkan placeholder
        Box(
            modifier = modifier.background(Color(0xFF1A1A20)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.VideoFile,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Belum ada video dipilih\nImpor klip untuk mulai pratinjau",
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
