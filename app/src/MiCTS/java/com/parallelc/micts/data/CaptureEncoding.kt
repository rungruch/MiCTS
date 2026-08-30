package com.parallelc.micts.data

internal enum class CaptureImageFormat {
    JPEG,
    PNG,
}

/** Keeps the encoded bytes, cache filename, and share MIME type in sync. */
internal object CaptureEncoding {
    val format: CaptureImageFormat = CaptureImageFormat.JPEG

    val quality: Int
        get() = 90

    val fileName: String
        get() = "capture.jpg"

    val mimeType: String
        get() = "image/jpeg"

    val legacyFileNames: Set<String>
        get() = setOf("capture.png")
}
