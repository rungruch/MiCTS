package com.parallelc.micts.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parallelc.micts.capture.BitmapCaptureWriter
import com.parallelc.micts.domain.CaptureResult
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun writerKeepsDecodablePngWithMatchingShareType() {
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
        assertEquals("capture.png", capture.name)
        val bytes = capture.readBytes()
        assertTrue(bytes.size >= 8)
        assertEquals(
            listOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            bytes.take(8).map { it.toInt() and 0xFF },
        )

        val decoded = BitmapFactory.decodeFile(capture.absolutePath)
        assertEquals(64, decoded.width)
        assertEquals(32, decoded.height)
        decoded.recycle()

        val intent = LensShareGateway(context).createShareIntent(
            Uri.parse("content://com.parallelc.vistrigger.fileprovider/lens_capture/capture.png"),
        )
        assertEquals("image/png", intent.type)
    }
}
