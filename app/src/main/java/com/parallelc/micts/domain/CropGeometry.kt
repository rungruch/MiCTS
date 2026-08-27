package com.parallelc.micts.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
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
    val center: FloatPoint get() = FloatPoint((left + right) / 2f, (top + bottom) / 2f)

    fun contains(point: FloatPoint): Boolean =
        point.x in left..right && point.y in top..bottom

    fun intersectionArea(other: FloatRect): Float {
        val overlapWidth = (min(right, other.right) - max(left, other.left)).coerceAtLeast(0f)
        val overlapHeight = (min(bottom, other.bottom) - max(top, other.top)).coerceAtLeast(0f)
        return overlapWidth * overlapHeight
    }
}

data class ViewportState(
    val zoom: Float = 1f,
    val panXFraction: Float = 0f,
    val panYFraction: Float = 0f,
)

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

    fun viewport(
        image: FloatSize,
        container: FloatSize,
        state: ViewportState,
    ): FitCenterTransform {
        val base = fitCenter(image, container)
        val zoom = state.zoom.coerceIn(1f, 5f)
        val scale = base.scale * zoom
        val displayedWidth = image.width * scale
        val displayedHeight = image.height * scale
        val centeredX = (container.width - displayedWidth) / 2f
        val centeredY = (container.height - displayedHeight) / 2f
        val desiredX = centeredX + state.panXFraction * container.width
        val desiredY = centeredY + state.panYFraction * container.height
        return FitCenterTransform(
            scale = scale,
            offsetX = clampViewportOffset(desiredX, displayedWidth, container.width),
            offsetY = clampViewportOffset(desiredY, displayedHeight, container.height),
            displayedWidth = displayedWidth,
            displayedHeight = displayedHeight,
        )
    }

    fun transformViewport(
        state: ViewportState,
        centroid: FloatPoint,
        pan: FloatPoint,
        zoomChange: Float,
        image: FloatSize,
        container: FloatSize,
    ): ViewportState {
        val old = viewport(image, container, state)
        val anchor = viewToImage(centroid, old)
        val newZoom = (state.zoom * zoomChange).coerceIn(1f, 5f)
        val newScale = fitCenter(image, container).scale * newZoom
        val displayedWidth = image.width * newScale
        val displayedHeight = image.height * newScale
        val centeredX = (container.width - displayedWidth) / 2f
        val centeredY = (container.height - displayedHeight) / 2f
        val desiredX = centroid.x + pan.x - anchor.x * newScale
        val desiredY = centroid.y + pan.y - anchor.y * newScale
        val offsetX = clampViewportOffset(desiredX, displayedWidth, container.width)
        val offsetY = clampViewportOffset(desiredY, displayedHeight, container.height)
        return ViewportState(
            zoom = newZoom,
            panXFraction = if (container.width == 0f) 0f else (offsetX - centeredX) / container.width,
            panYFraction = if (container.height == 0f) 0f else (offsetY - centeredY) / container.height,
        )
    }

    fun imageToView(point: FloatPoint, transform: FitCenterTransform): FloatPoint = FloatPoint(
        x = point.x * transform.scale + transform.offsetX,
        y = point.y * transform.scale + transform.offsetY,
    )

    fun viewToImage(point: FloatPoint, transform: FitCenterTransform): FloatPoint = FloatPoint(
        x = (point.x - transform.offsetX) / transform.scale,
        y = (point.y - transform.offsetY) / transform.scale,
    )

    fun imageToView(rect: FloatRect, transform: FitCenterTransform): FloatRect {
        val topLeft = imageToView(FloatPoint(rect.left, rect.top), transform)
        val bottomRight = imageToView(FloatPoint(rect.right, rect.bottom), transform)
        return FloatRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    fun clampToBounds(rect: FloatRect, bounds: FloatRect, minSize: Float): FloatRect {
        val safeMin = minSize.coerceAtMost(min(bounds.width, bounds.height))
        val width = rect.width.coerceIn(safeMin, bounds.width)
        val height = rect.height.coerceIn(safeMin, bounds.height)
        val left = rect.left.coerceIn(bounds.left, bounds.right - width)
        val top = rect.top.coerceIn(bounds.top, bounds.bottom - height)
        return FloatRect(left, top, left + width, top + height)
    }

    fun rectFromPoints(
        start: FloatPoint,
        end: FloatPoint,
        bounds: FloatRect,
        minSize: Float,
    ): FloatRect {
        val raw = FloatRect(
            left = min(start.x, end.x),
            top = min(start.y, end.y),
            right = max(start.x, end.x),
            bottom = max(start.y, end.y),
        )
        val expanded = FloatRect(
            raw.left,
            raw.top,
            max(raw.right, raw.left + minSize),
            max(raw.bottom, raw.top + minSize),
        )
        return clampToBounds(expanded, bounds, minSize)
    }

    fun toIntRect(rect: FloatRect, imageWidth: Int, imageHeight: Int): IntCropRect {
        val left = floor(rect.left).toInt().coerceIn(0, imageWidth - 1)
        val top = floor(rect.top).toInt().coerceIn(0, imageHeight - 1)
        val right = ceil(rect.right).toInt().coerceIn(left + 1, imageWidth)
        val bottom = ceil(rect.bottom).toInt().coerceIn(top + 1, imageHeight)
        return IntCropRect(left, top, right, bottom)
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

    private fun clampViewportOffset(offset: Float, displayedSize: Float, containerSize: Float): Float =
        if (displayedSize <= containerSize) {
            (containerSize - displayedSize) / 2f
        } else {
            offset.coerceIn(containerSize - displayedSize, 0f)
        }
}
