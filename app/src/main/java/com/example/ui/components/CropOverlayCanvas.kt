package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatio
import com.example.data.model.CropHandle
import com.example.data.model.CropRect
import com.example.data.model.GridDisplayMode
import com.example.ui.theme.CropScrim
import com.example.ui.theme.NeonCyan
import kotlin.math.hypot
import kotlin.math.min

/**
 * High-performance hardware-accelerated interactive crop workstation.
 * Ultra-smooth 60+ FPS touch dragging with zero stutter or gesture cancellations.
 */
@Composable
fun CropOverlayCanvas(
    bitmap: Bitmap?,
    cropRect: CropRect,
    aspectRatio: AspectRatio,
    rotationDegrees: Int,
    isFlippedHorizontally: Boolean,
    gridMode: GridDisplayMode = GridDisplayMode.ALWAYS,
    onCropRectChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 48.dp.toPx() } // Generous 48dp hit-test zone for instant grabs

    // Pre-cache ImageBitmap so it is not re-wrapped on every frame
    val cachedImageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    // Fast local state for instantaneous drag updates without recomposition stutter
    var activeCropRect by remember { mutableStateOf(cropRect) }
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

    // Sync from outside changes when not actively dragging
    LaunchedEffect(cropRect) {
        if (activeHandle == CropHandle.NONE) {
            activeCropRect = cropRect
        }
    }

    val currentRatio by rememberUpdatedState(aspectRatio.ratio)
    val currentOnCropRectChange by rememberUpdatedState(onCropRectChange)

    BoxWithConstraints(modifier = modifier.testTag("crop_overlay_workstation")) {
        val containerWidth = maxWidth.value * density.density
        val containerHeight = maxHeight.value * density.density

        // Compute aspect fit rectangle for the image inside container
        val imageBounds = remember(bitmap, containerWidth, containerHeight, rotationDegrees) {
            calculateImageDisplayBounds(
                bitmap = bitmap,
                containerW = containerWidth,
                containerH = containerHeight,
                rotationDegrees = rotationDegrees
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // IMPORTANT: Do NOT key on cropRect! Only key on layout dimensions so gestures are never cancelled.
                .pointerInput(imageBounds.left, imageBounds.top, imageBounds.width, imageBounds.height) {
                    if (imageBounds.width <= 0 || imageBounds.height <= 0) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            activeHandle = hitTestHandle(
                                touchPoint = offset,
                                imageBounds = imageBounds,
                                cropRect = activeCropRect,
                                touchRadius = touchRadiusPx
                            )
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle != CropHandle.NONE) {
                                val normDx = dragAmount.x / imageBounds.width
                                val normDy = dragAmount.y / imageBounds.height

                                val updatedRect = processDrag(
                                    currentRect = activeCropRect,
                                    handle = activeHandle,
                                    normDx = normDx,
                                    normDy = normDy,
                                    aspectRatio = currentRatio,
                                    imageAspect = imageBounds.width / imageBounds.height
                                )
                                // Local state update only: 120 FPS buttery smooth canvas redraw
                                activeCropRect = updatedRect
                            }
                        },
                        onDragEnd = {
                            activeHandle = CropHandle.NONE
                            // Dispatch final rect once drag is completed
                            currentOnCropRectChange(activeCropRect)
                        },
                        onDragCancel = {
                            activeHandle = CropHandle.NONE
                            currentOnCropRectChange(activeCropRect)
                        }
                    )
                }
        ) {
            val imgBitmap = cachedImageBitmap ?: return@Canvas
            if (imageBounds.width <= 0 || imageBounds.height <= 0) return@Canvas

            // 1. Draw source image within calculated display bounds
            drawSourceImage(
                imageBitmap = imgBitmap,
                imageBounds = imageBounds
            )

            // 2. Absolute crop rect on canvas using activeCropRect
            val current = activeCropRect
            val absLeft = imageBounds.left + current.left * imageBounds.width
            val absTop = imageBounds.top + current.top * imageBounds.height
            val absRight = imageBounds.left + current.right * imageBounds.width
            val absBottom = imageBounds.top + current.bottom * imageBounds.height
            val absCrop = Rect(absLeft, absTop, absRight, absBottom)

            // 3. Draw semi-transparent scrim outside crop box
            drawScrim(imageBounds = imageBounds, cropRect = absCrop)

            // 4. Draw rule-of-thirds grid if enabled
            val shouldDrawGrid = when (gridMode) {
                GridDisplayMode.ALWAYS -> true
                GridDisplayMode.DURING_DRAG -> activeHandle != CropHandle.NONE
                GridDisplayMode.HIDDEN -> false
            }
            if (shouldDrawGrid) {
                drawGrid(absCrop)
            }

            // 5. Draw active crop outline border
            drawRect(
                color = NeonCyan,
                topLeft = absCrop.topLeft,
                size = absCrop.size,
                style = Stroke(width = 2.dp.toPx())
            )

            // 6. Draw 8 interactive handles
            drawHandles(absCrop, activeHandle)
        }
    }
}

private fun calculateImageDisplayBounds(
    bitmap: Bitmap?,
    containerW: Float,
    containerH: Float,
    rotationDegrees: Int
): Rect {
    if (bitmap == null || containerW <= 0f || containerH <= 0f) return Rect.Zero

    val isSwapped = rotationDegrees % 180 != 0
    val bmpW = if (isSwapped) bitmap.height.toFloat() else bitmap.width.toFloat()
    val bmpH = if (isSwapped) bitmap.width.toFloat() else bitmap.height.toFloat()

    val scale = min(containerW / bmpW, containerH / bmpH)
    val displayW = bmpW * scale
    val displayH = bmpH * scale

    val left = (containerW - displayW) / 2f
    val top = (containerH - displayH) / 2f

    return Rect(left, top, left + displayW, top + displayH)
}

private fun DrawScope.drawSourceImage(
    imageBitmap: ImageBitmap,
    imageBounds: Rect
) {
    clipRect(
        left = imageBounds.left,
        top = imageBounds.top,
        right = imageBounds.right,
        bottom = imageBounds.bottom
    ) {
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(imageBounds.left.toInt(), imageBounds.top.toInt()),
            dstSize = IntSize(imageBounds.width.toInt(), imageBounds.height.toInt())
        )
    }
}

private fun DrawScope.drawScrim(imageBounds: Rect, cropRect: Rect) {
    // Top
    if (cropRect.top > imageBounds.top) {
        drawRect(
            color = CropScrim,
            topLeft = Offset(imageBounds.left, imageBounds.top),
            size = Size(imageBounds.width, cropRect.top - imageBounds.top)
        )
    }
    // Bottom
    if (cropRect.bottom < imageBounds.bottom) {
        drawRect(
            color = CropScrim,
            topLeft = Offset(imageBounds.left, cropRect.bottom),
            size = Size(imageBounds.width, imageBounds.bottom - cropRect.bottom)
        )
    }
    // Left
    if (cropRect.left > imageBounds.left) {
        drawRect(
            color = CropScrim,
            topLeft = Offset(imageBounds.left, cropRect.top),
            size = Size(cropRect.left - imageBounds.left, cropRect.height)
        )
    }
    // Right
    if (cropRect.right < imageBounds.right) {
        drawRect(
            color = CropScrim,
            topLeft = Offset(cropRect.right, cropRect.top),
            size = Size(imageBounds.right - cropRect.right, cropRect.height)
        )
    }
}

private fun DrawScope.drawGrid(crop: Rect) {
    val thirdW = crop.width / 3f
    val thirdH = crop.height / 3f
    val gridColor = Color.White.copy(alpha = 0.4f)
    val strokeW = 1.dp.toPx()

    // Vertical lines
    drawLine(
        color = gridColor,
        start = Offset(crop.left + thirdW, crop.top),
        end = Offset(crop.left + thirdW, crop.bottom),
        strokeWidth = strokeW
    )
    drawLine(
        color = gridColor,
        start = Offset(crop.left + thirdW * 2f, crop.top),
        end = Offset(crop.left + thirdW * 2f, crop.bottom),
        strokeWidth = strokeW
    )

    // Horizontal lines
    drawLine(
        color = gridColor,
        start = Offset(crop.left, crop.top + thirdH),
        end = Offset(crop.right, crop.top + thirdH),
        strokeWidth = strokeW
    )
    drawLine(
        color = gridColor,
        start = Offset(crop.left, crop.top + thirdH * 2f),
        end = Offset(crop.right, crop.top + thirdH * 2f),
        strokeWidth = strokeW
    )
}

private fun DrawScope.drawHandles(crop: Rect, activeHandle: CropHandle) {
    val bracketLen = min(crop.width, crop.height) * 0.18f.coerceIn(0.12f, 0.25f)
    val bracketStroke = 4.dp.toPx()
    val handleColor = Color.White
    val midPillColor = NeonCyan

    // Top-Left corner bracket
    drawLine(handleColor, Offset(crop.left - 1, crop.top), Offset(crop.left + bracketLen, crop.top), bracketStroke)
    drawLine(handleColor, Offset(crop.left, crop.top - 1), Offset(crop.left, crop.top + bracketLen), bracketStroke)

    // Top-Right corner bracket
    drawLine(handleColor, Offset(crop.right + 1, crop.top), Offset(crop.right - bracketLen, crop.top), bracketStroke)
    drawLine(handleColor, Offset(crop.right, crop.top - 1), Offset(crop.right, crop.top + bracketLen), bracketStroke)

    // Bottom-Left corner bracket
    drawLine(handleColor, Offset(crop.left - 1, crop.bottom), Offset(crop.left + bracketLen, crop.bottom), bracketStroke)
    drawLine(handleColor, Offset(crop.left, crop.bottom + 1), Offset(crop.left, crop.bottom - bracketLen), bracketStroke)

    // Bottom-Right corner bracket
    drawLine(handleColor, Offset(crop.right + 1, crop.bottom), Offset(crop.right - bracketLen, crop.bottom), bracketStroke)
    drawLine(handleColor, Offset(crop.right, crop.bottom + 1), Offset(crop.right, crop.bottom - bracketLen), bracketStroke)

    // Edge center grab bars (Top, Bottom, Left, Right)
    val midLen = 22.dp.toPx()
    val midStroke = 3.5.dp.toPx()
    // Top
    drawLine(midPillColor, Offset(crop.center.x - midLen / 2, crop.top), Offset(crop.center.x + midLen / 2, crop.top), midStroke)
    // Bottom
    drawLine(midPillColor, Offset(crop.center.x - midLen / 2, crop.bottom), Offset(crop.center.x + midLen / 2, crop.bottom), midStroke)
    // Left
    drawLine(midPillColor, Offset(crop.left, crop.center.y - midLen / 2), Offset(crop.left, crop.center.y + midLen / 2), midStroke)
    // Right
    drawLine(midPillColor, Offset(crop.right, crop.center.y - midLen / 2), Offset(crop.right, crop.center.y + midLen / 2), midStroke)
}

private fun hitTestHandle(
    touchPoint: Offset,
    imageBounds: Rect,
    cropRect: CropRect,
    touchRadius: Float
): CropHandle {
    val absLeft = imageBounds.left + cropRect.left * imageBounds.width
    val absTop = imageBounds.top + cropRect.top * imageBounds.height
    val absRight = imageBounds.left + cropRect.right * imageBounds.width
    val absBottom = imageBounds.top + cropRect.bottom * imageBounds.height

    fun dist(x: Float, y: Float) = hypot(touchPoint.x - x, touchPoint.y - y)

    // Test 4 corners first (highest priority)
    if (dist(absLeft, absTop) <= touchRadius) return CropHandle.TOP_LEFT
    if (dist(absRight, absTop) <= touchRadius) return CropHandle.TOP_RIGHT
    if (dist(absLeft, absBottom) <= touchRadius) return CropHandle.BOTTOM_LEFT
    if (dist(absRight, absBottom) <= touchRadius) return CropHandle.BOTTOM_RIGHT

    val edgeMargin = touchRadius * 0.7f

    // Test 4 edges (allows dragging anywhere along the perimeter border)
    if (touchPoint.x in (absLeft - edgeMargin)..(absRight + edgeMargin) &&
        kotlin.math.abs(touchPoint.y - absTop) <= edgeMargin
    ) return CropHandle.TOP

    if (touchPoint.x in (absLeft - edgeMargin)..(absRight + edgeMargin) &&
        kotlin.math.abs(touchPoint.y - absBottom) <= edgeMargin
    ) return CropHandle.BOTTOM

    if (touchPoint.y in (absTop - edgeMargin)..(absBottom + edgeMargin) &&
        kotlin.math.abs(touchPoint.x - absLeft) <= edgeMargin
    ) return CropHandle.LEFT

    if (touchPoint.y in (absTop - edgeMargin)..(absBottom + edgeMargin) &&
        kotlin.math.abs(touchPoint.x - absRight) <= edgeMargin
    ) return CropHandle.RIGHT

    // Test inside (smooth whole-box translation)
    if (touchPoint.x in absLeft..absRight && touchPoint.y in absTop..absBottom) {
        return CropHandle.INSIDE
    }

    return CropHandle.NONE
}

private fun processDrag(
    currentRect: CropRect,
    handle: CropHandle,
    normDx: Float,
    normDy: Float,
    aspectRatio: Float?,
    imageAspect: Float
): CropRect {
    val minDim = 0.05f

    return when (handle) {
        CropHandle.INSIDE -> {
            val w = currentRect.width
            val h = currentRect.height
            var newL = currentRect.left + normDx
            var newT = currentRect.top + normDy

            // Clamp whole box inside [0, 1]
            newL = newL.coerceIn(0f, 1f - w)
            newT = newT.coerceIn(0f, 1f - h)

            CropRect(newL, newT, newL + w, newT + h)
        }

        CropHandle.TOP_LEFT -> {
            var newL = (currentRect.left + normDx).coerceIn(0f, currentRect.right - minDim)
            var newT = (currentRect.top + normDy).coerceIn(0f, currentRect.bottom - minDim)

            if (aspectRatio != null) {
                val targetNormRatio = aspectRatio / imageAspect
                val currentW = currentRect.right - newL
                val reqH = currentW / targetNormRatio
                newT = (currentRect.bottom - reqH).coerceIn(0f, currentRect.bottom - minDim)
            }
            CropRect(newL, newT, currentRect.right, currentRect.bottom)
        }

        CropHandle.TOP_RIGHT -> {
            var newR = (currentRect.right + normDx).coerceIn(currentRect.left + minDim, 1f)
            var newT = (currentRect.top + normDy).coerceIn(0f, currentRect.bottom - minDim)

            if (aspectRatio != null) {
                val targetNormRatio = aspectRatio / imageAspect
                val currentW = newR - currentRect.left
                val reqH = currentW / targetNormRatio
                newT = (currentRect.bottom - reqH).coerceIn(0f, currentRect.bottom - minDim)
            }
            CropRect(currentRect.left, newT, newR, currentRect.bottom)
        }

        CropHandle.BOTTOM_LEFT -> {
            var newL = (currentRect.left + normDx).coerceIn(0f, currentRect.right - minDim)
            var newB = (currentRect.bottom + normDy).coerceIn(currentRect.top + minDim, 1f)

            if (aspectRatio != null) {
                val targetNormRatio = aspectRatio / imageAspect
                val currentW = currentRect.right - newL
                val reqH = currentW / targetNormRatio
                newB = (currentRect.top + reqH).coerceIn(currentRect.top + minDim, 1f)
            }
            CropRect(newL, currentRect.top, currentRect.right, newB)
        }

        CropHandle.BOTTOM_RIGHT -> {
            var newR = (currentRect.right + normDx).coerceIn(currentRect.left + minDim, 1f)
            var newB = (currentRect.bottom + normDy).coerceIn(currentRect.top + minDim, 1f)

            if (aspectRatio != null) {
                val targetNormRatio = aspectRatio / imageAspect
                val currentW = newR - currentRect.left
                val reqH = currentW / targetNormRatio
                newB = (currentRect.top + reqH).coerceIn(currentRect.top + minDim, 1f)
            }
            CropRect(currentRect.left, currentRect.top, newR, newB)
        }

        CropHandle.TOP -> {
            val newT = (currentRect.top + normDy).coerceIn(0f, currentRect.bottom - minDim)
            CropRect(currentRect.left, newT, currentRect.right, currentRect.bottom)
        }

        CropHandle.BOTTOM -> {
            val newB = (currentRect.bottom + normDy).coerceIn(currentRect.top + minDim, 1f)
            CropRect(currentRect.left, currentRect.top, currentRect.right, newB)
        }

        CropHandle.LEFT -> {
            val newL = (currentRect.left + normDx).coerceIn(0f, currentRect.right - minDim)
            CropRect(newL, currentRect.top, currentRect.right, currentRect.bottom)
        }

        CropHandle.RIGHT -> {
            val newR = (currentRect.right + normDx).coerceIn(currentRect.left + minDim, 1f)
            CropRect(currentRect.left, currentRect.top, newR, currentRect.bottom)
        }

        CropHandle.NONE -> currentRect
    }
}
