package com.parallelc.micts.capture

import android.content.Context
import android.graphics.Bitmap
import com.parallelc.micts.data.CaptureEncoding
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.CaptureImageFormat
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CaptureResult
import java.io.FileOutputStream

object BitmapCaptureWriter {
    fun write(context: Context, bitmap: Bitmap): CaptureResult {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            CaptureFiles.deleteCapture(context)
            return CaptureResult.Failure(CaptureFailureReason.EMPTY_IMAGE)
        }

        return runCatching {
            CaptureFiles.prepareForCapture(context)
            val probablyProtected = isProbablyProtected(bitmap)
            val written = FileOutputStream(CaptureFiles.capture(context)).use { stream ->
                val format = when (CaptureEncoding.format) {
                    CaptureImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    CaptureImageFormat.PNG -> Bitmap.CompressFormat.PNG
                }
                bitmap.compress(format, CaptureEncoding.quality, stream)
            }
            if (!written) error("Could not encode capture")
            CaptureResult.Success(probablyProtected)
        }.getOrElse {
            CaptureFiles.deleteCapture(context)
            CaptureResult.Failure(CaptureFailureReason.WRITE_FAILED)
        }
    }

    private fun isProbablyProtected(bitmap: Bitmap): Boolean {
        val samplesPerAxis = 12
        for (xIndex in 0 until samplesPerAxis) {
            for (yIndex in 0 until samplesPerAxis) {
                val x = ((xIndex + 0.5f) * bitmap.width / samplesPerAxis)
                    .toInt().coerceIn(0, bitmap.width - 1)
                val y = ((yIndex + 0.5f) * bitmap.height / samplesPerAxis)
                    .toInt().coerceIn(0, bitmap.height - 1)
                if ((bitmap.getPixel(x, y) and 0x00FFFFFF) > 0x00010101) return false
            }
        }
        return true
    }
}
