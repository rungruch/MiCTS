package com.parallelc.micts.domain

enum class RecognitionScript {
    LATIN,
    CHINESE,
}

data class RecognizedTextLine(
    val text: String,
    val bounds: FloatRect,
    val script: RecognitionScript,
    val sourceOrder: Int,
)

sealed interface RecognitionResult {
    data class Success(val lines: List<RecognizedTextLine>) : RecognitionResult
    data class Failure(val cause: Throwable? = null) : RecognitionResult
}

object RecognitionMerger {
    fun merge(
        latin: List<RecognizedTextLine>,
        chinese: List<RecognizedTextLine>,
    ): List<RecognizedTextLine> {
        val candidates = (latin + chinese).filter { it.text.isNotBlank() }
        val selected = mutableListOf<RecognizedTextLine>()
        candidates.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left })).forEach { candidate ->
            val overlappingIndex = selected.indexOfFirst { existing ->
                overlapRatio(existing.bounds, candidate.bounds) >= 0.5f
            }
            if (overlappingIndex < 0) {
                selected += candidate
            } else {
                selected[overlappingIndex] = prefer(selected[overlappingIndex], candidate)
            }
        }
        return selected.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
    }

    private fun prefer(
        first: RecognizedTextLine,
        second: RecognizedTextLine,
    ): RecognizedTextLine {
        val eitherContainsCjk = first.text.containsCjk() || second.text.containsCjk()
        if (eitherContainsCjk) {
            return listOf(first, second).firstOrNull { it.script == RecognitionScript.CHINESE }
                ?: longer(first, second)
        }
        return listOf(first, second).firstOrNull { it.script == RecognitionScript.LATIN }
            ?: longer(first, second)
    }

    private fun longer(first: RecognizedTextLine, second: RecognizedTextLine): RecognizedTextLine =
        if (first.text.count(Char::isLetterOrDigit) >= second.text.count(Char::isLetterOrDigit)) {
            first
        } else {
            second
        }

    private fun overlapRatio(first: FloatRect, second: FloatRect): Float {
        val intersection = first.intersectionArea(second)
        val smallerArea = minOf(first.width * first.height, second.width * second.height)
        return if (smallerArea <= 0f) 0f else intersection / smallerArea
    }

    private fun String.containsCjk(): Boolean = any { character ->
        Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
    }
}

object TextSelection {
    fun linesInside(
        selection: FloatRect,
        lines: List<RecognizedTextLine>,
    ): List<RecognizedTextLine> = lines
        .filter { selection.contains(it.bounds.center) }
        .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))

    fun textInside(
        selection: FloatRect,
        lines: List<RecognizedTextLine>,
    ): String = linesInside(selection, lines).joinToString("\n") { it.text.trim() }
}

data class EditorActionAvailability(
    val copy: Boolean,
    val search: Boolean,
    val translate: Boolean,
    val lens: Boolean,
)

object EditorActionPolicy {
    fun availability(selectedText: String, isActing: Boolean): EditorActionAvailability {
        val textActionEnabled = selectedText.isNotBlank() && !isActing
        return EditorActionAvailability(
            copy = textActionEnabled,
            search = textActionEnabled,
            translate = textActionEnabled,
            lens = !isActing,
        )
    }
}
