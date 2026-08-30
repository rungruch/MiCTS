package com.parallelc.micts.capture

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class PixelBufferExtractorTest {

    @Test
    fun unpaddedBufferReturnsOriginalContent() {
        val width = 2
        val height = 2
        val pixelStride = 4
        val rowStride = 8 // width * 4 = 8, so rowPadding = 0

        val sourceBytes = byteArrayOf(
            1, 2, 3, 4,   5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16,
        )
        val sourceBuffer = ByteBuffer.wrap(sourceBytes)

        val result = PixelBufferExtractor.extractCompactBytes(
            source = sourceBuffer,
            width = width,
            height = height,
            pixelStride = pixelStride,
            rowStride = rowStride,
        )

        val outputBytes = ByteArray(width * height * pixelStride)
        result.get(outputBytes)
        assertArrayEquals(sourceBytes, outputBytes)
    }

    @Test
    fun paddedBufferSkipsRowPaddingCorrectly() {
        val width = 2
        val height = 2
        val pixelStride = 4
        val rowStride = 12 // 8 bytes of pixels + 4 bytes padding per row

        val sourceBytes = byteArrayOf(
            // Row 0: 8 pixel bytes + 4 padding bytes
            1, 2, 3, 4, 5, 6, 7, 8, 99, 99, 99, 99,
            // Row 1: 8 pixel bytes + 4 padding bytes
            9, 10, 11, 12, 13, 14, 15, 16, 88, 88, 88, 88,
        )
        val sourceBuffer = ByteBuffer.wrap(sourceBytes)

        val result = PixelBufferExtractor.extractCompactBytes(
            source = sourceBuffer,
            width = width,
            height = height,
            pixelStride = pixelStride,
            rowStride = rowStride,
        )

        val expectedBytes = byteArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16,
        )
        val outputBytes = ByteArray(width * height * pixelStride)
        result.get(outputBytes)
        assertArrayEquals(expectedBytes, outputBytes)
    }

    @Test
    fun invalidDimensionsThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelBufferExtractor.extractCompactBytes(
                source = ByteBuffer.allocate(16),
                width = 0,
                height = 2,
                pixelStride = 4,
                rowStride = 8,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PixelBufferExtractor.extractCompactBytes(
                source = ByteBuffer.allocate(16),
                width = 2,
                height = -1,
                pixelStride = 4,
                rowStride = 8,
            )
        }
    }

    @Test
    fun invalidPixelStrideThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelBufferExtractor.extractCompactBytes(
                source = ByteBuffer.allocate(16),
                width = 2,
                height = 2,
                pixelStride = 2,
                rowStride = 8,
            )
        }
    }

    @Test
    fun rowStrideSmallerThanRowBytesThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelBufferExtractor.extractCompactBytes(
                source = ByteBuffer.allocate(16),
                width = 4,
                height = 2,
                pixelStride = 4,
                rowStride = 8, // rowBytes = 16 > 8
            )
        }
    }
}
