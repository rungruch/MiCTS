package com.parallelc.micts.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parallelc.micts.capture.BitmapCaptureWriter
import com.parallelc.micts.domain.CaptureResult
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureEncodingInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun cleanUp() {
        CaptureFiles.deleteCapture(context)
    }

    @Test
    fun writerCreatesDecodableJpegWithMatchingShareType() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF336699.toInt())
        }

        val result = try {
            BitmapCaptureWriter.write(context, bitmap)
        } finally {
            bitmap.recycle()
        }

        assertTrue(result is CaptureResult.Success)
        val capture = CaptureFiles.capture(context)
        assertEquals("capture.jpg", capture.name)
        val bytes = capture.readBytes()
        assertTrue(bytes.size >= 3)
        assertEquals(0xFF, bytes[0].toInt() and 0xFF)
        assertEquals(0xD8, bytes[1].toInt() and 0xFF)
        assertEquals(0xFF, bytes[2].toInt() and 0xFF)

        val decoded = BitmapFactory.decodeFile(capture.absolutePath)
        assertEquals(64, decoded.width)
        assertEquals(32, decoded.height)
        decoded.recycle()

        val intent = LensShareGateway(context).createShareIntent(
            Uri.parse("content://com.parallelc.micts.fileprovider/lens_capture/capture.jpg"),
        )
        assertEquals("image/jpeg", intent.type)
    }

    @Test
    fun preparationAndDeletionRemoveCurrentAndLegacyCaptures() {
        val directory = CaptureFiles.directory(context).apply { mkdirs() }
        val current = CaptureFiles.capture(context).apply { writeBytes(byteArrayOf(1)) }
        val legacy = File(directory, "capture.png").apply { writeBytes(byteArrayOf(2)) }

        CaptureFiles.prepareForCapture(context)
        assertFalse(current.exists())
        assertFalse(legacy.exists())

        current.writeBytes(byteArrayOf(1))
        legacy.writeBytes(byteArrayOf(2))
        assertTrue(CaptureFiles.deleteCapture(context))
        assertFalse(current.exists())
        assertFalse(legacy.exists())
    }
}
