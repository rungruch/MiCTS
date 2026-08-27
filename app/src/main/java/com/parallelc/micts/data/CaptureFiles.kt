package com.parallelc.micts.data

import android.content.Context
import java.io.File

object CaptureFiles {
    private const val DIRECTORY_NAME = "lens_capture"
    private const val CAPTURE_NAME = "capture.png"

    fun directory(context: Context): File = File(context.cacheDir, DIRECTORY_NAME)

    fun capture(context: Context): File = File(directory(context), CAPTURE_NAME)

    fun prepareForCapture(context: Context) {
        val directory = directory(context)
        if (!directory.exists()) directory.mkdirs()
        capture(context).delete()
    }
}
