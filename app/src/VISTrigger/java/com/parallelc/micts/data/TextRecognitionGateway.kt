package com.parallelc.micts.data

import android.graphics.Bitmap
import com.parallelc.micts.domain.RecognitionResult
import java.io.Closeable

interface TextRecognitionGateway : Closeable {
    suspend fun recognize(bitmap: Bitmap): RecognitionResult
}
