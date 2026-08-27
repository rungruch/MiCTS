package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {
    @Test
    fun fitCenterLetterboxesWideImage() {
        val transform = CropGeometry.fitCenter(
            FloatSize(1000f, 500f),
            FloatSize(500f, 500f),
        )
        assertEquals(0.5f, transform.scale)
        assertEquals(0f, transform.offsetX)
        assertEquals(125f, transform.offsetY)
        assertEquals(500f, transform.displayedWidth)
        assertEquals(250f, transform.displayedHeight)
    }

    @Test
    fun initialCropIsCenteredInsideDisplayedImage() {
        val crop = CropGeometry.initialCrop(FloatRect(0f, 100f, 500f, 400f), 0.8f)
        assertEquals(FloatRect(50f, 130f, 450f, 370f), crop)
    }

    @Test
    fun movingCropClampsToImageBounds() {
        val moved = CropGeometry.drag(
            FloatRect(100f, 100f, 300f, 300f),
            CropHandle.MOVE,
            FloatPoint(-500f, 500f),
            FloatRect(0f, 0f, 400f, 400f),
            80f,
        )
        assertEquals(FloatRect(0f, 200f, 200f, 400f), moved)
    }

    @Test
    fun resizingCropHonorsMinimumSize() {
        val resized = CropGeometry.drag(
            FloatRect(100f, 100f, 300f, 300f),
            CropHandle.TOP_LEFT,
            FloatPoint(500f, 500f),
            FloatRect(0f, 0f, 400f, 400f),
            96f,
        )
        assertEquals(96f, resized.width)
        assertEquals(96f, resized.height)
    }

    @Test
    fun viewSelectionMapsBackToBitmapPixels() {
        val transform = CropGeometry.fitCenter(
            FloatSize(1000f, 500f),
            FloatSize(500f, 500f),
        )
        val imageRect = CropGeometry.toImageRect(
            FloatRect(50f, 150f, 450f, 350f),
            transform,
            imageWidth = 1000,
            imageHeight = 500,
        )
        assertEquals(IntCropRect(100, 50, 900, 450), imageRect)
        assertTrue(imageRect.width > 0)
        assertTrue(imageRect.height > 0)
    }

    @Test
    fun portraitImageWithHorizontalLetterboxingMapsToPixels() {
        val transform = CropGeometry.fitCenter(
            FloatSize(500f, 1000f),
            FloatSize(500f, 500f),
        )
        assertEquals(0.5f, transform.scale)
        assertEquals(125f, transform.offsetX)
        assertEquals(0f, transform.offsetY)

        assertEquals(
            IntCropRect(50, 100, 450, 900),
            CropGeometry.toImageRect(
                FloatRect(150f, 50f, 350f, 450f),
                transform,
                imageWidth = 500,
                imageHeight = 1000,
            ),
        )
    }

    @Test
    fun everyCornerHandleClampsToBounds() {
        val bounds = FloatRect(0f, 0f, 400f, 400f)
        val rect = FloatRect(100f, 100f, 300f, 300f)
        assertEquals(
            FloatRect(0f, 0f, 300f, 300f),
            CropGeometry.drag(
                rect,
                CropHandle.TOP_LEFT,
                FloatPoint(-500f, -500f),
                bounds,
                80f,
            ),
        )
        assertEquals(
            FloatRect(100f, 0f, 400f, 300f),
            CropGeometry.drag(
                rect,
                CropHandle.TOP_RIGHT,
                FloatPoint(500f, -500f),
                bounds,
                80f,
            ),
        )
        assertEquals(
            FloatRect(0f, 100f, 300f, 400f),
            CropGeometry.drag(
                rect,
                CropHandle.BOTTOM_LEFT,
                FloatPoint(-500f, 500f),
                bounds,
                80f,
            ),
        )
        assertEquals(
            FloatRect(100f, 100f, 400f, 400f),
            CropGeometry.drag(
                rect,
                CropHandle.BOTTOM_RIGHT,
                FloatPoint(500f, 500f),
                bounds,
                80f,
            ),
        )
    }

    @Test
    fun hitTestingFindsCornersMoveAndOutside() {
        val rect = FloatRect(100f, 100f, 300f, 300f)
        assertEquals(
            CropHandle.TOP_LEFT,
            CropGeometry.hitTest(rect, FloatPoint(102f, 102f), 24f),
        )
        assertEquals(
            CropHandle.BOTTOM_RIGHT,
            CropGeometry.hitTest(rect, FloatPoint(298f, 298f), 24f),
        )
        assertEquals(
            CropHandle.MOVE,
            CropGeometry.hitTest(rect, FloatPoint(200f, 200f), 24f),
        )
        assertEquals(
            CropHandle.NONE,
            CropGeometry.hitTest(rect, FloatPoint(20f, 20f), 24f),
        )
    }
}
