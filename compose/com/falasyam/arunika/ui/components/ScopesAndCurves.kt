package com.falasyam.arunika.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falasyam.arunika.model.ScopeMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualisator Scopes Real-time (Waveform Luma / Vectorscope)
 * Dirancang untuk membaca distribusi sinyal video 0-100 IRE.
 */
@Composable
fun RealtimeScopeMonitor(
    mode: ScopeMode,
    modifier: Modifier = Modifier
) {
    if (mode == ScopeMode.OFF) return

    Surface(
        modifier = modifier
            .width(160.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xCC0A0A0C),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = mode.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (mode) {
                    ScopeMode.WAVEFORM -> {
                        // Garis referensi IRE (0%, 50%, 100%)
                        drawLine(Color(0x33FFFFFF), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                        drawLine(Color(0x22FFFFFF), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                        drawLine(Color(0x33FFFFFF), Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                        // Sinyal densitas waveform hijau/luma
                        for (i in 0 until w.toInt() step 2) {
                            val x = i.toFloat()
                            val baseNoise = (sin(x * 0.08) * 0.2 + cos(x * 0.03) * 0.3 + 0.5).toFloat()
                            val topY = (baseNoise * 0.7f * h).coerceIn(4f, h - 4f)
                            val bottomY = (topY + (h * 0.35f)).coerceIn(topY, h)

                            drawLine(
                                color = Color(0x9910B981),
                                start = Offset(x, topY),
                                end = Offset(x, bottomY),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                    ScopeMode.VECTORSCOPE -> {
                        val cx = w / 2
                        val cy = h / 2
                        val radius = (h / 2) - 4

                        // Lingkaran referensi saturasi 75%
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = radius * 0.75f,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )

                        // Crosshairs Q / I axis
                        drawLine(Color(0x22FFFFFF), Offset(cx, 4f), Offset(cx, h - 4f))
                        drawLine(Color(0x22FFFFFF), Offset(4f, cy), Offset(w - 4f, cy))

                        // Trace jejak warna kulit (Skin tone indicator)
                        val skinAngle = Math.toRadians(-57.0)
                        drawLine(
                            color = Color(0x88F59E0B),
                            start = Offset(cx, cy),
                            end = Offset(
                                (cx + cos(skinAngle) * radius).toFloat(),
                                (cy + sin(skinAngle) * radius).toFloat()
                            ),
                            strokeWidth = 1.5f
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

/**
 * Slider Split-Screen Wipe (Before vs After) interaktif langsung di atas monitor preview
 */
@Composable
fun SplitScreenCompareOverlay(
    isSplitEnabled: Boolean,
    splitPosition: Float,
    onSplitPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSplitEnabled) return

    val haptic = LocalHapticFeedback.current
    val currentSplitPosition by rememberUpdatedState(splitPosition)
    val currentOnChange by rememberUpdatedState(onSplitPositionChange)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val splitXDp = with(density) { (totalWidthPx * currentSplitPosition).toDp() }

        // Touch gesture capture di seluruh area viewport preview video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalWidthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaNorm = dragAmount.x / totalWidthPx
                        val newPos = (currentSplitPosition + deltaNorm).coerceIn(0.02f, 0.98f)
                        if ((newPos * 100).toInt() % 10 == 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentOnChange(newPos)
                    }
                }
        ) {
            // Label HUD "BEFORE (RAW)" di kiri dan "AFTER (GRADED)" di kanan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "BEFORE (RAW)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "AFTER (GRADED)",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Garis Pembatas Vertikal
            Box(
                modifier = Modifier
                    .offset(x = splitXDp - 1.dp)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.White)
            )

            // Handle bulat di tengah garis pemisah
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset(x = splitXDp - 18.dp)
                    .size(36.dp)
                    .align(Alignment.CenterStart)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = "Slide to Compare",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
