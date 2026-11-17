package com.falasyam.arunika.ui.components

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Komponen pemutaran video modern menggunakan AndroidView + ExoPlayer (Media3).
 * - Mendukung play/pause real via ExoPlayer
 * - Seek ke posisi playhead sinkron
 * - Error state jika format tidak didukung
 * - Placeholder informatif saat belum ada klip dipilih
 */
@Composable
fun VideoPlayerView(
    uri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    seekToMs: Long = 0L,
    onDurationReady: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    if (uri != null) {
        var hasError by remember { mutableStateOf(false) }

        // Reset error state saat URI berubah
        LaunchedEffect(uri) { hasError = false }

        Box(modifier = modifier) {
            if (!hasError) {
                val exoPlayer = remember(uri) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                        repeatMode = Player.REPEAT_MODE_ALL
                        prepare()
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    onDurationReady(duration.coerceAtLeast(0L))
                                }
                            }
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                hasError = true
                            }
                        })
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                // Sync play/pause state
                LaunchedEffect(isPlaying) {
                    if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                }

                // Sync seek position jika seekToMs berubah signifikan
                LaunchedEffect(seekToMs) {
                    if (kotlin.math.abs(exoPlayer.currentPosition - seekToMs) > 250) {
                        exoPlayer.seekTo(seekToMs)
                    }
                }

                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false // Sembunyikan kontrol bawaan, gunakan kontrol custom Arunika
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
