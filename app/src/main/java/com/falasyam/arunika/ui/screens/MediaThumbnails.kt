package com.falasyam.arunika.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlin.math.roundToInt

internal fun MediaMetadataRetriever.extractThumbnailFrame(
    timeUs: Long,
    maxDimension: Int = 320
): Bitmap? {
    val option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val sourceWidth = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val sourceHeight = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        if (sourceWidth != null && sourceHeight != null && sourceWidth > 0 && sourceHeight > 0) {
            val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val frameWidth = if (rotation % 180 != 0) sourceHeight else sourceWidth
            val frameHeight = if (rotation % 180 != 0) sourceWidth else sourceHeight
            val scale = minOf(1f, maxDimension / maxOf(frameWidth, frameHeight).toFloat())
            val targetWidth = (frameWidth * scale).roundToInt().coerceAtLeast(1)
            val targetHeight = (frameHeight * scale).roundToInt().coerceAtLeast(1)
            return getScaledFrameAtTime(timeUs, option, targetWidth, targetHeight)
        }
    }

    val frame = getFrameAtTime(timeUs, option) ?: return null
    val scale = minOf(1f, maxDimension / maxOf(frame.width, frame.height).toFloat())
    if (scale == 1f) return frame

    val thumbnail = Bitmap.createScaledBitmap(
        frame,
        (frame.width * scale).roundToInt().coerceAtLeast(1),
        (frame.height * scale).roundToInt().coerceAtLeast(1),
        true
    )
    if (thumbnail !== frame) frame.recycle()
    return thumbnail
}
