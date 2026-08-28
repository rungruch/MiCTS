package com.parallelc.micts.data

import android.graphics.Bitmap
import android.util.Base64
import com.parallelc.micts.domain.IntCropRect
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object AiImageEncoder {
    const val MAX_IMAGE_EDGE = 1568
    private const val JPEG_QUALITY = 85

    fun encodeRegionToBase64(bitmap: Bitmap, crop: IntCropRect? = null): String {
        val cropped = if (crop != null && (crop.width < bitmap.width || crop.height < bitmap.height)) {
            val safeLeft = crop.left.coerceIn(0, bitmap.width - 1)
            val safeTop = crop.top.coerceIn(0, bitmap.height - 1)
            val safeWidth = crop.width.coerceIn(1, bitmap.width - safeLeft)
            val safeHeight = crop.height.coerceIn(1, bitmap.height - safeTop)
            Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
        } else {
            bitmap
        }

        val downscaled = try {
            val longestEdge = max(cropped.width, cropped.height)
            if (longestEdge <= MAX_IMAGE_EDGE) {
                cropped
            } else {
                val scale = MAX_IMAGE_EDGE.toFloat() / longestEdge
                Bitmap.createScaledBitmap(
                    cropped,
                    (cropped.width * scale).roundToInt().coerceAtLeast(1),
                    (cropped.height * scale).roundToInt().coerceAtLeast(1),
                    true,
                )
            }
        } finally {
            // Keep reference for recycle logic below
        }

        val outputStream = ByteArrayOutputStream()
        downscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()

        if (cropped !== bitmap) cropped.recycle()
        if (downscaled !== bitmap && downscaled !== cropped) downscaled.recycle()

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }
}
