package com.parallelc.micts.data

import com.parallelc.micts.BuildConfig

internal enum class CaptureImageFormat {
    JPEG,
    PNG,
}

/** Keeps the encoded bytes, cache filename, and share MIME type in sync. */
internal object CaptureEncoding {
    val format: CaptureImageFormat = if (BuildConfig.CAPTURE_AS_JPEG) {
        CaptureImageFormat.JPEG
    } else {
        CaptureImageFormat.PNG
    }

    val quality: Int
        get() = if (format == CaptureImageFormat.JPEG) 90 else 100

    val fileName: String
        get() = if (format == CaptureImageFormat.JPEG) "capture.jpg" else "capture.png"

    val mimeType: String
        get() = if (format == CaptureImageFormat.JPEG) "image/jpeg" else "image/png"

    val legacyFileNames: Set<String>
        get() = if (format == CaptureImageFormat.JPEG) setOf("capture.png") else emptySet()
}
