package com.lumina.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumina.studio.model.AudioStudioState

/**
 * Tab 3: Sound & Fairlight Lite
 * - Voice Isolator berbasis neural model on-device
 * - De-Reverb & Dialogue Clarity
 * - Auto-Ducking musik saat vokal terdeteksi
 * - Realtime Stereo Peak Audio Meters
 */
@Composable
fun AudioScreen(
    state: AudioStudioState = remember { AudioStudioState() }
) {
    val haptic = LocalHapticFeedback.current
    var voiceIso by remember { mutableStateOf(state.voiceIsolation) }
    var deReverb by remember { mutableStateOf(state.deReverb) }
    var autoDuck by remember { mutableStateOf(state.autoDucking) }
    var masterVol by remember { mutableStateOf(state.masterVolume) }
    var bassGain by remember { mutableStateOf(state.bassGainDb) }
    var midGain by remember { mutableStateOf(state.midGainDb) }
    var trebleGain by remember { mutableStateOf(state.trebleGainDb) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Stereo Master Audio Level Meters (L & R)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MASTER PEAK METERS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "-6.2 dB (True Peak)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bar meter L
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF26262B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.72f)
                                .background(Color(0xFF10B981))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bar meter R
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF26262B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.68f)
                                .background(Color(0xFF10B981))
                        )
                    }
                }
            }
        }

        // 2. AI Voice Processing Suite (Fitur Andalan)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI Voice Isolation & Clarity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider AI Voice Isolation
                Text(
                    text = "Voice Isolation: ${(voiceIso * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = voiceIso,
                    onValueChange = {
                        voiceIso = it
                        state.voiceIsolation = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Slider De-Reverb
                Text(
                    text = "De-Reverb (Hilangkan Gema Ruangan): ${(deReverb * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = deReverb,
                    onValueChange = {
                        deReverb = it
                        state.deReverb = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Auto-Ducking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Ducking Musik",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Kecilkan musik otomatis saat ada suara orang berbicara",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDuck,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            autoDuck = it
                            state.autoDucking = it
                        }
                    )
                }
            }
        }

        // 3. 3-Band Parametric Equalizer (Fairlight Lite DSP)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3-Band Parametric EQ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bass
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BASS (100Hz)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = bassGain,
                            onValueChange = { bassGain = it; state.bassGainDb = it },
                            valueRange = -12f..12f,
                            steps = 24
                        )
                        Text("${if (bassGain > 0) "+" else ""}${bassGain.toInt()} dB", style = MaterialTheme.typography.bodySmall)
                    }

                    // Mid / Voice
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VOCAL (1.2kHz)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = midGain,
                            onValueChange = { midGain = it; state.midGainDb = it },
                            valueRange = -12f..12f,
                            steps = 24
                        )
                        Text("${if (midGain > 0) "+" else ""}${midGain.toInt()} dB", style = MaterialTheme.typography.bodySmall)
                    }

                    // Treble / Air
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AIR (8kHz)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = trebleGain,
                            onValueChange = { trebleGain = it; state.trebleGainDb = it },
                            valueRange = -12f..12f,
                            steps = 24
                        )
                        Text("${if (trebleGain > 0) "+" else ""}${trebleGain.toInt()} dB", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 4. Master Volume & Dynamics
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Master Level: ${((masterVol - 1f) * 12).toInt()} dB",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = masterVol,
                    onValueChange = {
                        masterVol = it
                        state.masterVolume = it
                    },
                    valueRange = 0f..2f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
