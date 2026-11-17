package com.falasyam.arunika.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.ColorWheelValue
import kotlin.math.*

/**
 * Komponen Roda Warna Interaktif (3-Way Color Wheel) berbasis Material Design 3 Expressive
 * Mendukung drag halus, polar clamping, double tap reset, serta haptic feedback.
 */
@Composable
fun InteractiveColorWheel(
    value: ColorWheelValue,
    label: String = "",
    modifier: Modifier = Modifier,
    wheelSize: Float = 220f, // dalam dp
    onValueChanged: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var wasAtCenter by remember { mutableStateOf(value.isNeutral) }

    // Animasi posisi thumb saat di-reset
    val animatedX by animateFloatAsState(targetValue = value.x, animationSpec = spring(stiffness = 600f), label = "wheelX")
    val animatedY by animateFloatAsState(targetValue = value.y, animationSpec = spring(stiffness = 600f), label = "wheelY")

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Info Roda
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hue: ${value.hueDegrees.roundToInt()}° • Sat: ${(value.saturation * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tombol Reset Cepat
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    value.reset()
                },
                enabled = !value.isNeutral
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset Color Wheel",
                    tint = if (!value.isNeutral) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kanvas Roda Warna
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(wheelSize.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            value.reset()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f - 24f // padding thumb

                            val dx = (change.position.x - center.x) / radius
                            val dy = (change.position.y - center.y) / radius
                            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                            // Snap to center bila mendekati titik netral (< 0.06f)
                            if (dist < 0.06f) {
                                if (!wasAtCenter) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    wasAtCenter = true
                                }
                                value.x = 0f
                                value.y = 0f
                            } else {
                                wasAtCenter = false
                                if (dist <= 1f) {
                                    value.x = dx
                                    value.y = dy
                                } else {
                                    // Clamping ke pinggir lingkaran
                                    value.x = dx / dist
                                    value.y = dy / dist
                                }
                            }
                        }
                    )
                }
        ) {
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val thumbColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 24f

                // 1. Lingkaran Dasar Netral
                drawCircle(
                    color = surfaceColor,
                    radius = radius,
                    center = center
                )

                // 2. Gradien Spektrum Warna (SweepGradient)
                val rainbowColors = listOf(
                    Color(0xFFFF0000), // Merah (0°)
                    Color(0xFFFFFF00), // Kuning (60°)
                    Color(0xFF00FF00), // Hijau (120°)
                    Color(0xFF00FFFF), // Cyan (180°)
                    Color(0xFF0000FF), // Biru (240°)
                    Color(0xFFFF00FF), // Magenta (300°)
                    Color(0xFFFF0000)  // Kembali ke Merah
                )
                val sweepBrush = Brush.sweepGradient(rainbowColors, center)
                drawCircle(
                    brush = sweepBrush,
                    radius = radius,
                    center = center,
                    alpha = 0.45f
                )

                // 3. Radial Gradient Putih/Netral di Tengah (Pusat desaturasi)
                val radialBrush = Brush.radialGradient(
                    colors = listOf(surfaceColor, Color.Transparent),
                    center = center,
                    radius = radius
                )
                drawCircle(
                    brush = radialBrush,
                    radius = radius,
                    center = center
                )

                // 4. Garis Batas Roda & Crosshair Titik Netral
                drawCircle(
                    color = outlineColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(
                    color = outlineColor.copy(alpha = 0.5f),
                    start = Offset(center.x - 12.dp.toPx(), center.y),
                    end = Offset(center.x + 12.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = outlineColor.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - 12.dp.toPx()),
                    end = Offset(center.x, center.y + 12.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                // 5. Thumb Indicator (Posisi Nilai Warna)
                val thumbPos = Offset(
                    x = center.x + animatedX * radius,
                    y = center.y + animatedY * radius
                )

                // Bayangan Thumb
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 16.dp.toPx(),
                    center = thumbPos + Offset(0f, 2.dp.toPx())
                )

                // Cincin Luar Thumb M3
                drawCircle(
                    color = Color.White,
                    radius = 14.dp.toPx(),
                    center = thumbPos
                )
                drawCircle(
                    color = thumbColor,
                    radius = 12.dp.toPx(),
                    center = thumbPos
                )

                // Titik Warna Aktif di Dalam Thumb
                val tintColor = if (value.isNeutral) {
                    Color.White
                } else {
                    Color.hsv(value.hueDegrees, value.saturation, 1.0f)
                }
                drawCircle(
                    color = tintColor,
                    radius = 7.dp.toPx(),
                    center = thumbPos
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Master Luminance Dial (Slider Pengatur Terang/Gelap per Wheel)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Y",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = value.luminance,
                onValueChange = { newVal ->
                    // Haptic tick saat melewati 0
                    if ((value.luminance < 0f && newVal >= 0f) || (value.luminance > 0f && newVal <= 0f)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    value.luminance = newVal
                },
                valueRange = -1.0f..1.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Text(
                text = String.format("%+.2f", value.luminance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(44.dp)
            )
        }
    }
}
