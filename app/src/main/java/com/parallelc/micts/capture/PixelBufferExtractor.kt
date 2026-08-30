package com.parallelc.micts.capture

import android.graphics.Bitmap
import java.nio.ByteBuffer

object PixelBufferExtractor {
    /**
     * Extracts a [Bitmap] of dimensions [width] x [height] from the given pixel plane [buffer].
     *
     * When [rowStride] == [width] * [pixelStride], extracts in a single pass without intermediate allocations.
     * When [rowStride] > [width] * [pixelStride], transfers pixels row-by-row using a single reusable row buffer,
     * avoiding duplicate full-frame [Bitmap] or [ByteBuffer] allocations.
     */
    fun extract(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        pixelStride: Int,
        rowStride: Int,
    ): Bitmap {
        require(width > 0 && height > 0) { "Invalid dimensions: ${width}x$height" }
        require(pixelStride == 4) { "Unsupported pixel stride: $pixelStride (expected 4)" }

        val rowBytes = width * pixelStride
        val rowPadding = rowStride - rowBytes
        require(rowPadding >= 0) { "rowStride ($rowStride) cannot be less than rowBytes ($rowBytes)" }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            if (rowPadding == 0) {
                buffer.position(0)
                bitmap.copyPixelsFromBuffer(buffer)
            } else {
                val rowInts = IntArray(width)
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    for (col in 0 until width) {
                        val r = buffer.get().toInt() and 0xFF
                        val g = buffer.get().toInt() and 0xFF
                        val b = buffer.get().toInt() and 0xFF
                        val a = buffer.get().toInt() and 0xFF
                        rowInts[col] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    bitmap.setPixels(rowInts, 0, width, 0, row, width, 1)
                }
            }
            return bitmap
        } catch (t: Throwable) {
            bitmap.recycle()
            throw t
        }
    }

    /**
     * Helper to extract compact row bytes from a padded buffer into a contiguous byte buffer.
     * Accessible for pure JVM unit testing.
     */
    internal fun extractCompactBytes(
        source: ByteBuffer,
        width: Int,
        height: Int,
        pixelStride: Int,
        rowStride: Int,
    ): ByteBuffer {
        require(width > 0 && height > 0) { "Invalid dimensions: ${width}x$height" }
        require(pixelStride == 4) { "Unsupported pixel stride: $pixelStride" }
        val rowBytes = width * pixelStride
        val rowPadding = rowStride - rowBytes
        require(rowPadding >= 0) { "rowStride ($rowStride) < rowBytes ($rowBytes)" }

        if (rowPadding == 0) {
            source.position(0)
            return source
        }

        val target = ByteBuffer.allocate(rowBytes * height)
        val rowTemp = ByteArray(rowBytes)
        for (row in 0 until height) {
            source.position(row * rowStride)
            source.get(rowTemp, 0, rowBytes)
            target.put(rowTemp, 0, rowBytes)
        }
        target.rewind()
        return target
    }
}
