package com.parallelc.micts.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureEncodingTest {
    @Test
    fun keepsLosslessPngCapturePolicy() {
        assertEquals(CaptureImageFormat.PNG, CaptureEncoding.format)
        assertEquals(100, CaptureEncoding.quality)
        assertEquals("capture.png", CaptureEncoding.fileName)
        assertEquals("image/png", CaptureEncoding.mimeType)
        assertTrue(CaptureEncoding.legacyFileNames.isEmpty())
    }
}
