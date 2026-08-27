package com.parallelc.micts.data

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.parallelc.micts.domain.FloatRect
import com.parallelc.micts.domain.RecognitionMerger
import com.parallelc.micts.domain.RecognitionResult
import com.parallelc.micts.domain.RecognitionScript
import com.parallelc.micts.domain.RecognizedTextLine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

internal class MlKitTextRecognitionGateway : TextRecognitionGateway {
    companion object {
        private const val MAX_ANALYSIS_EDGE = 2048
    }

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val chineseRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    override suspend fun recognize(bitmap: Bitmap): RecognitionResult {
        val analysisBitmap = bitmap.downscaledForRecognition()
        val scaleX = bitmap.width.toFloat() / analysisBitmap.width
        val scaleY = bitmap.height.toFloat() / analysisBitmap.height
        val image = InputImage.fromBitmap(analysisBitmap, 0)
        return try {
            val latin = runCatching {
                latinRecognizer.process(image).awaitResult().toLines(
                    RecognitionScript.LATIN,
                    scaleX,
                    scaleY,
                )
            }
            val chinese = runCatching {
                chineseRecognizer.process(image).awaitResult().toLines(
                    RecognitionScript.CHINESE,
                    scaleX,
                    scaleY,
                )
            }
            if (latin.isFailure && chinese.isFailure) {
                val cause = latin.exceptionOrNull() ?: chinese.exceptionOrNull()
                chinese.exceptionOrNull()?.takeIf { it !== cause }?.let { secondary ->
                    cause?.addSuppressed(secondary)
                }
                RecognitionResult.Failure(cause)
            } else {
                RecognitionResult.Success(
                    RecognitionMerger.merge(
                        latin.getOrDefault(emptyList()),
                        chinese.getOrDefault(emptyList()),
                    ),
                )
            }
        } finally {
            if (analysisBitmap !== bitmap) analysisBitmap.recycle()
        }
    }

    override fun close() {
        latinRecognizer.close()
        chineseRecognizer.close()
    }

    private fun Bitmap.downscaledForRecognition(): Bitmap {
        val longestEdge = max(width, height)
        if (longestEdge <= MAX_ANALYSIS_EDGE) return this
        val scale = MAX_ANALYSIS_EDGE.toFloat() / longestEdge
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun Text.toLines(
        script: RecognitionScript,
        scaleX: Float,
        scaleY: Float,
    ): List<RecognizedTextLine> {
        var sourceOrder = 0
        return textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                RecognizedTextLine(
                    text = line.text,
                    bounds = FloatRect(
                        left = box.left * scaleX,
                        top = box.top * scaleY,
                        right = box.right * scaleX,
                        bottom = box.bottom * scaleY,
                    ),
                    script = script,
                    sourceOrder = sourceOrder++,
                )
            }
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
