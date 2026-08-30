package com.parallelc.micts.data

import android.content.Context
import java.io.File

object CaptureFiles {
    private const val DIRECTORY_NAME = "lens_capture"

    fun directory(context: Context): File = File(context.cacheDir, DIRECTORY_NAME)

    fun capture(context: Context): File = File(directory(context), CaptureEncoding.fileName)

    fun prepareForCapture(context: Context) {
        val directory = directory(context)
        if (!directory.exists()) directory.mkdirs()
        deleteCapture(context)
    }

    fun deleteCapture(context: Context): Boolean {
        val directory = directory(context)
        val fileNames = CaptureEncoding.legacyFileNames + CaptureEncoding.fileName
        return fileNames.fold(false) { deletedAny, fileName ->
            File(directory, fileName).delete() || deletedAny
        }
    }
}
