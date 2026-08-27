package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionModelsTest {
    @Test
    fun overlappingLatinTextPrefersLatinRecognizer() {
        val latin = line("Hello world", FloatRect(10f, 10f, 110f, 40f), RecognitionScript.LATIN)
        val chinesePass = line("Hello wor1d", FloatRect(12f, 11f, 112f, 41f), RecognitionScript.CHINESE)

        assertEquals(listOf(latin), RecognitionMerger.merge(listOf(latin), listOf(chinesePass)))
    }

    @Test
    fun overlappingCjkTextPrefersChineseRecognizer() {
        val latinPass = line("Huawei", FloatRect(10f, 10f, 110f, 40f), RecognitionScript.LATIN)
        val chinese = line("华为 Huawei", FloatRect(11f, 10f, 111f, 40f), RecognitionScript.CHINESE)

        assertEquals(listOf(chinese), RecognitionMerger.merge(listOf(latinPass), listOf(chinese)))
    }

    @Test
    fun textSelectionUsesLineCentersAndReadingOrder() {
        val lines = listOf(
            line("Second", FloatRect(10f, 60f, 100f, 90f), RecognitionScript.LATIN),
            line("Outside", FloatRect(200f, 20f, 280f, 50f), RecognitionScript.LATIN),
            line("First", FloatRect(10f, 20f, 100f, 50f), RecognitionScript.LATIN),
        )

        assertEquals(
            "First\nSecond",
            TextSelection.textInside(FloatRect(0f, 0f, 150f, 120f), lines),
        )
    }

    @Test
    fun actionPolicyKeepsLensAvailableWithoutText() {
        val empty = EditorActionPolicy.availability("", isActing = false)
        assertFalse(empty.copy)
        assertFalse(empty.search)
        assertFalse(empty.translate)
        assertTrue(empty.lens)

        val busy = EditorActionPolicy.availability("selected", isActing = true)
        assertFalse(busy.copy)
        assertFalse(busy.lens)
    }

    private fun line(
        text: String,
        bounds: FloatRect,
        script: RecognitionScript,
    ) = RecognizedTextLine(text, bounds, script, sourceOrder = 0)
}
