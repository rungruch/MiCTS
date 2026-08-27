package com.parallelc.micts.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min

data class FloatPoint(val x: Float, val y: Float)

data class FloatSize(val width: Float, val height: Float)

data class FloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(point: FloatPoint): Boolean =
        point.x in left..right && point.y in top..bottom
}

data class FitCenterTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val displayedWidth: Float,
    val displayedHeight: Float,
) {
    val displayedBounds: FloatRect
        get() = FloatRect(
            left = offsetX,
            top = offsetY,
            right = offsetX + displayedWidth,
            bottom = offsetY + displayedHeight,
        )
}

data class IntCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

enum class CropHandle {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

object CropGeometry {
    fun fitCenter(image: FloatSize, container: FloatSize): FitCenterTransform {
        require(image.width > 0 && image.height > 0)
        require(container.width > 0 && container.height > 0)
        val scale = min(container.width / image.width, container.height / image.height)
        val displayedWidth = image.width * scale
        val displayedHeight = image.height * scale
        return FitCenterTransform(
            scale = scale,
            offsetX = (container.width - displayedWidth) / 2f,
            offsetY = (container.height - displayedHeight) / 2f,
            displayedWidth = displayedWidth,
            displayedHeight = displayedHeight,
        )
    }

    fun initialCrop(bounds: FloatRect, fraction: Float = 0.72f): FloatRect {
        val safeFraction = fraction.coerceIn(0.2f, 1f)
        val width = bounds.width * safeFraction
        val height = bounds.height * safeFraction
        val left = bounds.left + (bounds.width - width) / 2f
        val top = bounds.top + (bounds.height - height) / 2f
        return FloatRect(left, top, left + width, top + height)
    }

    fun hitTest(rect: FloatRect, point: FloatPoint, handleRadius: Float): CropHandle {
        val corners = listOf(
            CropHandle.TOP_LEFT to FloatPoint(rect.left, rect.top),
            CropHandle.TOP_RIGHT to FloatPoint(rect.right, rect.top),
            CropHandle.BOTTOM_LEFT to FloatPoint(rect.left, rect.bottom),
            CropHandle.BOTTOM_RIGHT to FloatPoint(rect.right, rect.bottom),
        )
        corners.firstOrNull { (_, corner) ->
            hypot(point.x - corner.x, point.y - corner.y) <= handleRadius
        }?.let { return it.first }
        return if (rect.contains(point)) CropHandle.MOVE else CropHandle.NONE
    }

    fun drag(
        rect: FloatRect,
        handle: CropHandle,
        delta: FloatPoint,
        bounds: FloatRect,
        minSize: Float,
    ): FloatRect {
        val safeMinSize = minSize.coerceAtMost(min(bounds.width, bounds.height))
        return when (handle) {
            CropHandle.NONE -> rect
            CropHandle.MOVE -> {
                val dx = delta.x.coerceIn(bounds.left - rect.left, bounds.right - rect.right)
                val dy = delta.y.coerceIn(bounds.top - rect.top, bounds.bottom - rect.bottom)
                FloatRect(
                    rect.left + dx,
                    rect.top + dy,
                    rect.right + dx,
                    rect.bottom + dy,
                )
            }
            CropHandle.TOP_LEFT -> FloatRect(
                (rect.left + delta.x).coerceIn(bounds.left, rect.right - safeMinSize),
                (rect.top + delta.y).coerceIn(bounds.top, rect.bottom - safeMinSize),
                rect.right,
                rect.bottom,
            )
            CropHandle.TOP_RIGHT -> FloatRect(
                rect.left,
                (rect.top + delta.y).coerceIn(bounds.top, rect.bottom - safeMinSize),
                (rect.right + delta.x).coerceIn(rect.left + safeMinSize, bounds.right),
                rect.bottom,
            )
            CropHandle.BOTTOM_LEFT -> FloatRect(
                (rect.left + delta.x).coerceIn(bounds.left, rect.right - safeMinSize),
                rect.top,
                rect.right,
                (rect.bottom + delta.y).coerceIn(rect.top + safeMinSize, bounds.bottom),
            )
            CropHandle.BOTTOM_RIGHT -> FloatRect(
                rect.left,
                rect.top,
                (rect.right + delta.x).coerceIn(rect.left + safeMinSize, bounds.right),
                (rect.bottom + delta.y).coerceIn(rect.top + safeMinSize, bounds.bottom),
            )
        }
    }

    fun toImageRect(
        crop: FloatRect,
        transform: FitCenterTransform,
        imageWidth: Int,
        imageHeight: Int,
    ): IntCropRect {
        val left = floor((crop.left - transform.offsetX) / transform.scale)
            .toInt().coerceIn(0, imageWidth - 1)
        val top = floor((crop.top - transform.offsetY) / transform.scale)
            .toInt().coerceIn(0, imageHeight - 1)
        val right = ceil((crop.right - transform.offsetX) / transform.scale)
            .toInt().coerceIn(left + 1, imageWidth)
        val bottom = ceil((crop.bottom - transform.offsetY) / transform.scale)
            .toInt().coerceIn(top + 1, imageHeight)
        return IntCropRect(left, top, right, bottom)
    }
}
