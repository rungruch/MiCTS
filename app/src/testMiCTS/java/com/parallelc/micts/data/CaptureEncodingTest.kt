package com.parallelc.micts.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureEncodingTest {
    @Test
    fun usesJpegCapturePolicy() {
        assertEquals(CaptureImageFormat.JPEG, CaptureEncoding.format)
        assertEquals(90, CaptureEncoding.quality)
        assertEquals("capture.jpg", CaptureEncoding.fileName)
        assertEquals("image/jpeg", CaptureEncoding.mimeType)
        assertEquals(setOf("capture.png"), CaptureEncoding.legacyFileNames)
    }
}
