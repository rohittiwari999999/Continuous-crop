package com.example.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Normalized coordinates for a crop rectangle within [0f, 1f].
 * left, top, right, bottom are relative to the image boundaries.
 */
data class CropRect(
    val left: Float = 0.1f,
    val top: Float = 0.1f,
    val right: Float = 0.9f,
    val bottom: Float = 0.9f
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0f)

    val centerX: Float
        get() = left + width / 2f

    val centerY: Float
        get() = top + height / 2f

    fun toAbsolute(imageWidth: Float, imageHeight: Float): Rect {
        return Rect(
            left = left * imageWidth,
            top = top * imageHeight,
            right = right * imageWidth,
            bottom = bottom * imageHeight
        )
    }

    /**
     * Clamps this crop rect to stay strictly within [0f, 1f] with a minimum normalized dimension.
     */
    fun clamped(minDimension: Float = 0.05f): CropRect {
        val safeLeft = left.coerceIn(0f, 1f - minDimension)
        val safeTop = top.coerceIn(0f, 1f - minDimension)
        val safeRight = right.coerceIn(safeLeft + minDimension, 1f)
        val safeBottom = bottom.coerceIn(safeTop + minDimension, 1f)
        return CropRect(safeLeft, safeTop, safeRight, safeBottom)
    }

    /**
     * Adjusts the rectangle to adhere to a specific aspect ratio (ratio = width / height),
     * centered around the current bounding box center or anchor.
     */
    fun withAspectRatio(aspectRatio: Float, imageAspect: Float): CropRect {
        // aspectRatio is target width / target height in image pixel coordinates
        // normalized width / normalized height = aspectRatio / imageAspect
        val targetNormRatio = aspectRatio / imageAspect
        val cX = centerX
        val cY = centerY

        var newW = width
        var newH = newW / targetNormRatio

        if (newH > 1f) {
            newH = 1f
            newW = newH * targetNormRatio
        }
        if (newW > 1f) {
            newW = 1f
            newH = newW / targetNormRatio
        }

        var newLeft = cX - newW / 2f
        var newRight = cX + newW / 2f
        var newTop = cY - newH / 2f
        var newBottom = cY + newH / 2f

        // Shift inside [0..1]
        if (newLeft < 0f) {
            newRight -= newLeft
            newLeft = 0f
        }
        if (newRight > 1f) {
            newLeft -= (newRight - 1f)
            newRight = 1f
        }
        if (newTop < 0f) {
            newBottom -= newTop
            newTop = 0f
        }
        if (newBottom > 1f) {
            newTop -= (newBottom - 1f)
            newBottom = 1f
        }

        return CropRect(
            left = newLeft.coerceIn(0f, 1f),
            top = newTop.coerceIn(0f, 1f),
            right = newRight.coerceIn(0f, 1f),
            bottom = newBottom.coerceIn(0f, 1f)
        ).clamped()
    }

    companion object {
        fun default(): CropRect = CropRect(0.15f, 0.15f, 0.85f, 0.85f)
        fun full(): CropRect = CropRect(0.02f, 0.02f, 0.98f, 0.98f)
    }
}

enum class CropHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    INSIDE
}
