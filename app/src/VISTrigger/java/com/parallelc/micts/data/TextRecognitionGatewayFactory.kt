package com.parallelc.micts.data

import android.graphics.Bitmap
import com.parallelc.micts.domain.RecognitionResult

object TextRecognitionGatewayFactory {
    fun create(): TextRecognitionGateway = object : TextRecognitionGateway {
        override suspend fun recognize(bitmap: Bitmap): RecognitionResult =
            RecognitionResult.Success(emptyList())

        override fun close() = Unit
    }
}
