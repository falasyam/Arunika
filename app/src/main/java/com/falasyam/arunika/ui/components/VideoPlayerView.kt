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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.falasyam.arunika.model.ColorStudioState

/**
 * Komponen pemutaran video modern menggunakan AndroidView + ExoPlayer (Media3).
 *
 * ✅ Fix #1 — Real Color Grading Pipeline:
 *    - Menerima [colorState] opsional.
 *    - Jika diberikan, ExoPlayer menggunakan [setVideoEffects] dengan [ArunikaColorEffect]
 *      sehingga setiap frame video diproses nyata di GPU dengan 4×4 color matrix
 *      (Lift/Gamma/Gain/Temperature/Saturation/LUT) sebelum ditampilkan.
 *    - Efek diperbarui setiap kali parameter color berubah (ExoPlayer replaceVideoEffects).
 *
 * Fitur lain:
 *    - Play/Pause real via ExoPlayer
 *    - Seek ke posisi playhead sinkron
 *    - Error state jika format tidak didukung
 *    - Placeholder informatif saat belum ada klip dipilih
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    uri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    seekToMs: Long = 0L,
    volume: Float = 1.0f,
    speedMultiplier: Float = 1.0f,
    colorState: ColorStudioState? = null,
    onDurationReady: (Long) -> Unit = {},
    sharedExoPlayer: ExoPlayer? = null
) {
    val context = LocalContext.current

    if (uri != null) {
        var hasError by remember { mutableStateOf(false) }

        // Reset error state saat URI berubah
        LaunchedEffect(uri) { hasError = false }

        Box(modifier = modifier) {
            if (!hasError) {
                // Use shared ExoPlayer if provided, otherwise create local one
                val exoPlayer = sharedExoPlayer ?: remember(uri) {
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

                // Only dispose if we created the player locally
                if (sharedExoPlayer == null) {
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                }

                // ============================================================
                // ✅ Fix #1: Terapkan color grading effect nyata ke ExoPlayer
                //    ArunikaColorEffect membangun 4×4 matrix dari ColorStudioState
                //    yang dijalankan GPU pada setiap frame video.
                // ============================================================
                if (colorState != null) {
                    // Snapshot key — berubah setiap kali parameter warna berubah
                    val effectKey = rememberColorEffectKey(colorState)
                    LaunchedEffect(effectKey) {
                        val effect = ArunikaColorEffect(colorState)
                        // isNoOp() = true → ExoPlayer skip efek (zero overhead)
                        if (!effect.isNoOp()) {
                            exoPlayer.setVideoEffects(listOf(effect))
                        } else {
                            exoPlayer.setVideoEffects(emptyList())
                        }
                    }
                }

                // Sync play/pause state
                LaunchedEffect(isPlaying) {
                    if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                }

                // Sync volume state
                LaunchedEffect(volume) {
                    exoPlayer.volume = volume.coerceIn(0f, 1f)
                }

                // Sync speed multiplier state
                LaunchedEffect(speedMultiplier) {
                    exoPlayer.playbackParameters = PlaybackParameters(speedMultiplier.coerceIn(0.1f, 4.0f))
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

/**
 * Menghasilkan key string yang berubah setiap kali ada perubahan parameter color grading.
 * Digunakan sebagai trigger [LaunchedEffect] untuk memperbarui [ExoPlayer.setVideoEffects].
 */
@Composable
private fun rememberColorEffectKey(state: ColorStudioState): String {
    val n = state.activeNode
    return remember(
        n.lift.x, n.lift.y, n.lift.luminance,
        n.gamma.x, n.gamma.y, n.gamma.luminance,
        n.gain.x, n.gain.y, n.gain.luminance,
        n.temperature, n.saturation, n.contrast,
        n.selectedLut?.id, n.lutIntensity,
        state.inputLogProfile
    ) {
        "${n.lift.x},${n.lift.y},${n.lift.luminance}|" +
        "${n.gamma.x},${n.gamma.y},${n.gamma.luminance}|" +
        "${n.gain.x},${n.gain.y},${n.gain.luminance}|" +
        "${n.temperature},${n.saturation},${n.contrast}|" +
        "${n.selectedLut?.id},${n.lutIntensity}|${state.inputLogProfile}"
    }
}
