package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.model.AspectRatio
import com.example.data.model.CropHandle
import com.example.data.model.CropRect
import com.example.data.model.GridDisplayMode
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.CropScrim
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.viewmodel.DetectedPassportBox
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CropOverlayCanvas(
    bitmap: Bitmap?,
    cropRect: CropRect,
    rotation: Int,
    isFlipped: Boolean,
    aspectRatio: AspectRatio,
    gridMode: GridDisplayMode,
    showDimensionBadge: Boolean,
    pixelDimensions: Pair<Int, Int>,
    detectedPassportBoxes: List<DetectedPassportBox> = emptyList(),
    selectedBoxIndex: Int = -1,
    currentScannedNumber: String? = null,
    isSampleImage: Boolean = false,
    onScreenClickToPickImage: () -> Unit = {},
    onSelectDetectedBox: (Int) -> Unit = {},
    onCropRectChange: (CropRect) -> Unit,
    onCropGestureStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return

    val currentCropRect by rememberUpdatedState(cropRect)
    val currentAspectRatio by rememberUpdatedState(aspectRatio)
    val currentOnCropRectChange by rememberUpdatedState(onCropRectChange)
    val currentOnCropGestureStart by rememberUpdatedState(onCropGestureStart)
    val currentOnSelectDetectedBox by rememberUpdatedState(onSelectDetectedBox)
    val currentDetectedPassportBoxes by rememberUpdatedState(detectedPassportBoxes)
    val currentIsSampleImage by rememberUpdatedState(isSampleImage)
    val currentOnScreenClickToPickImage by rememberUpdatedState(onScreenClickToPickImage)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("crop_overlay_canvas")
    ) {
        val orientedW = if (rotation % 180 != 0) bitmap.height else bitmap.width
        val orientedH = if (rotation % 180 != 0) bitmap.width else bitmap.height

        val transformedBitmap = remember(bitmap, rotation, isFlipped) {
            if (rotation != 0 || isFlipped) {
                val matrix = Matrix()
                if (isFlipped) matrix.postScale(-1f, 1f)
                if (rotation != 0) matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(transformedBitmap) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val imgRect = calculateImageBounds(
                                canvasW, canvasH,
                                transformedBitmap.width.toFloat(),
                                transformedBitmap.height.toFloat()
                            )

                            val normX = ((down.position.x - imgRect.left) / imgRect.width).coerceIn(0f, 1f)
                            val normY = ((down.position.y - imgRect.top) / imgRect.height).coerceIn(0f, 1f)

                            var liveRect = currentCropRect
                            val initialHandle = hitTestHandle(normX, normY, liveRect, imgRect.width, imgRect.height)
                            var activeHandle = initialHandle

                            var prevCentroid = down.position
                            var prevSpan = 0f
                            var hasMoved = false
                            var gestureSnapshotTriggered = false
                            val touchSlop = viewConfiguration.touchSlop
                            var totalDragX = 0f
                            var totalDragY = 0f

                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }

                                if (pressed.size >= 2) {
                                    // Pinch-to-zoom and 2-finger pan: smooth scaling of crop selector
                                    val p1 = pressed[0].position
                                    val p2 = pressed[1].position
                                    val currSpan = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                                    val currCentroid = (p1 + p2) / 2f

                                    if (prevSpan > 10f && currSpan > 10f) {
                                        val zoomFactor = currSpan / prevSpan
                                        val pan = currCentroid - prevCentroid
                                        val panNormX = pan.x / imgRect.width
                                        val panNormY = pan.y / imgRect.height

                                        val imgAspect = imgRect.width / imgRect.height
                                        val updated = applyZoomAndPan(
                                            rect = liveRect,
                                            zoom = zoomFactor,
                                            panX = panNormX,
                                            panY = panNormY,
                                            aspectRatio = currentAspectRatio,
                                            imgAspect = imgAspect
                                        )
                                        liveRect = updated
                                        if (!gestureSnapshotTriggered) {
                                            gestureSnapshotTriggered = true
                                            currentOnCropGestureStart()
                                        }
                                        currentOnCropRectChange(updated)
                                        hasMoved = true
                                    }
                                    prevSpan = currSpan
                                    prevCentroid = currCentroid
                                    event.changes.forEach { it.consume() }

                                } else if (pressed.size == 1) {
                                    val pointer = pressed[0]
                                    prevSpan = 0f
                                    val delta = pointer.position - pointer.previousPosition
                                    totalDragX += delta.x
                                    totalDragY += delta.y

                                    if (!hasMoved) {
                                        if (kotlin.math.hypot(totalDragX, totalDragY) > touchSlop) {
                                            hasMoved = true
                                            if (!gestureSnapshotTriggered) {
                                                gestureSnapshotTriggered = true
                                                currentOnCropGestureStart()
                                            }
                                        }
                                    }

                                    if (hasMoved) {
                                        val panNormX = delta.x / imgRect.width
                                        val panNormY = delta.y / imgRect.height
                                        val imgAspect = imgRect.width / imgRect.height

                                        val effectiveHandle = if (activeHandle == CropHandle.NONE) CropHandle.INSIDE else activeHandle
                                        val updated = applyDrag(
                                            effectiveHandle,
                                            liveRect,
                                            panNormX,
                                            panNormY,
                                            currentAspectRatio,
                                            imgAspect
                                        )
                                        liveRect = updated
                                        currentOnCropRectChange(updated)
                                        pointer.consume()
                                    }
                                    prevCentroid = pointer.position
                                }
                            } while (event.changes.any { it.pressed })

                            // When finger is released without dragging: Quick Tap
                            if (!hasMoved) {
                                if (currentIsSampleImage) {
                                    // Clicking screen on default sample image opens phone gallery directly
                                    currentOnScreenClickToPickImage()
                                } else {
                                    val tappedBox = currentDetectedPassportBoxes.firstOrNull { box ->
                                        normX in box.cropRect.left..box.cropRect.right &&
                                        normY in box.cropRect.top..box.cropRect.bottom
                                    }
                                    if (tappedBox != null) {
                                        currentOnSelectDetectedBox(tappedBox.index)
                                    } else if (activeHandle == CropHandle.NONE) {
                                        // Reposition/center crop on tapped person or area
                                        val w = liveRect.width
                                        val h = liveRect.height
                                        val newL = (normX - w / 2f).coerceIn(0f, 1f - w)
                                        val newT = (normY - h / 2f).coerceIn(0f, 1f - h)
                                        val repositioned = CropRect(newL, newT, newL + w, newT + h)
                                        currentOnCropRectChange(repositioned)
                                    }
                                }
                            }
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                val imgRect = calculateImageBounds(
                    canvasW, canvasH,
                    transformedBitmap.width.toFloat(),
                    transformedBitmap.height.toFloat()
                )

                // 1. Draw transformed image
                drawImage(
                    image = transformedBitmap.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(transformedBitmap.width, transformedBitmap.height),
                    dstOffset = IntOffset(imgRect.left.roundToInt(), imgRect.top.roundToInt()),
                    dstSize = IntSize(imgRect.width.roundToInt(), imgRect.height.roundToInt())
                )

                // 2. Draw all detected passport photo boxes on the sheet
                detectedPassportBoxes.forEach { box ->
                    val bCrop = if (box.index == selectedBoxIndex) cropRect else box.cropRect
                    val bPxL = imgRect.left + bCrop.left * imgRect.width
                    val bPxT = imgRect.top + bCrop.top * imgRect.height
                    val bPxR = imgRect.left + bCrop.right * imgRect.width
                    val bPxB = imgRect.top + bCrop.bottom * imgRect.height

                    if (box.index != selectedBoxIndex) {
                        drawRect(
                            color = AccentTeal.copy(alpha = 0.85f),
                            topLeft = Offset(bPxL, bPxT),
                            size = Size(bPxR - bPxL, bPxB - bPxT),
                            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                        )
                    }

                    val label = box.detectedName ?: "#${box.index + 1}"
                    drawContext.canvas.nativeCanvas.apply {
                        val isSelected = box.index == selectedBoxIndex
                        val textPaint = AndroidPaint().apply {
                            color = if (isSelected) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            textSize = 26f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        val textWidth = textPaint.measureText(label)
                        val pillH = 34f
                        val pillW = textWidth + 22f
                        val pillT = if (bPxT - pillH >= imgRect.top) bPxT - pillH - 3f else bPxT + 3f
                        val pillB = pillT + pillH

                        val bgPaint = AndroidPaint().apply {
                            color = if (isSelected) android.graphics.Color.rgb(6, 182, 212) else android.graphics.Color.rgb(15, 118, 110)
                            isAntiAlias = true
                        }
                        drawRoundRect(
                            bPxL, pillT, bPxL + pillW, pillB,
                            8f, 8f, bgPaint
                        )
                        drawText(label, bPxL + 11f, pillB - 9f, textPaint)
                    }
                }

                // 3. Compute absolute pixel bounds of active crop rect on canvas
                val cropPxLeft = imgRect.left + cropRect.left * imgRect.width
                val cropPxTop = imgRect.top + cropRect.top * imgRect.height
                val cropPxRight = imgRect.left + cropRect.right * imgRect.width
                val cropPxBottom = imgRect.top + cropRect.bottom * imgRect.height
                val cropWidthPx = cropPxRight - cropPxLeft
                val cropHeightPx = cropPxBottom - cropPxTop

                // 4. Draw Scrim outside active crop (only if no multi-selection sheet active)
                if (detectedPassportBoxes.isEmpty()) {
                    drawScrim(imgRect, cropPxLeft, cropPxTop, cropPxRight, cropPxBottom)
                }

                // 5. Draw Active Crop border
                drawRect(
                    color = NeonCyan,
                    topLeft = Offset(cropPxLeft, cropPxTop),
                    size = Size(cropWidthPx, cropHeightPx),
                    style = Stroke(width = 3.5f)
                )

                // 6. Draw Grid lines
                drawGrid(gridMode, cropPxLeft, cropPxTop, cropWidthPx, cropHeightPx)

                // 7. Draw 8 Handles & Corner brackets
                drawHandles(cropPxLeft, cropPxTop, cropPxRight, cropPxBottom)

                // 8. Draw detected number pill badge if single crop
                if (detectedPassportBoxes.isEmpty() && !currentScannedNumber.isNullOrBlank()) {
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = AndroidPaint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 26f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        val textWidth = textPaint.measureText(currentScannedNumber)
                        val pillH = 34f
                        val pillW = textWidth + 22f
                        val pillT = if (cropPxTop - pillH >= imgRect.top) cropPxTop - pillH - 3f else cropPxTop + 3f
                        val pillB = pillT + pillH

                        val bgPaint = AndroidPaint().apply {
                            color = android.graphics.Color.rgb(6, 182, 212)
                            isAntiAlias = true
                        }
                        drawRoundRect(
                            cropPxLeft, pillT, cropPxLeft + pillW, pillB,
                            8f, 8f, bgPaint
                        )
                        drawText(currentScannedNumber, cropPxLeft + 11f, pillB - 9f, textPaint)
                    }
                }
            }
        }
    }
}

private fun calculateImageBounds(canvasW: Float, canvasH: Float, imgW: Float, imgH: Float): Rect {
    val scale = min(canvasW / imgW, canvasH / imgH)
    val scaledW = imgW * scale
    val scaledH = imgH * scale
    val left = (canvasW - scaledW) / 2f
    val top = (canvasH - scaledH) / 2f
    return Rect(left, top, left + scaledW, top + scaledH)
}

private fun hitTestHandle(
    normX: Float,
    normY: Float,
    rect: CropRect,
    imgWidthPx: Float,
    imgHeightPx: Float
): CropHandle {
    val thresholdPx = 64f
    val threshX = thresholdPx / imgWidthPx
    val threshY = thresholdPx / imgHeightPx

    val nearLeft = abs(normX - rect.left) < threshX
    val nearRight = abs(normX - rect.right) < threshX
    val nearTop = abs(normY - rect.top) < threshY
    val nearBottom = abs(normY - rect.bottom) < threshY

    val midX = abs(normX - rect.centerX) < threshX
    val midY = abs(normY - rect.centerY) < threshY

    return when {
        nearLeft && nearTop -> CropHandle.TOP_LEFT
        nearRight && nearTop -> CropHandle.TOP_RIGHT
        nearLeft && nearBottom -> CropHandle.BOTTOM_LEFT
        nearRight && nearBottom -> CropHandle.BOTTOM_RIGHT
        nearTop && midX -> CropHandle.TOP
        nearBottom && midX -> CropHandle.BOTTOM
        nearLeft && midY -> CropHandle.LEFT
        nearRight && midY -> CropHandle.RIGHT
        normX in rect.left..rect.right && normY in rect.top..rect.bottom -> CropHandle.INSIDE
        else -> CropHandle.NONE
    }
}

private fun applyZoomAndPan(
    rect: CropRect,
    zoom: Float,
    panX: Float,
    panY: Float,
    aspectRatio: AspectRatio,
    imgAspect: Float
): CropRect {
    val minSize = 0.05f
    val oldW = rect.width
    val oldH = rect.height

    val newW = (oldW * zoom).coerceIn(minSize, 1f)
    val ratio = aspectRatio.ratio

    val finalW: Float
    val finalH: Float
    if (ratio != null) {
        val targetH = (newW * imgAspect / ratio).coerceIn(minSize, 1f)
        finalH = targetH
        finalW = (targetH * ratio / imgAspect).coerceIn(minSize, 1f)
    } else {
        finalW = newW
        finalH = (oldH * zoom).coerceIn(minSize, 1f)
    }

    val cx = (rect.centerX + panX).coerceIn(finalW / 2f, 1f - finalW / 2f)
    val cy = (rect.centerY + panY).coerceIn(finalH / 2f, 1f - finalH / 2f)

    val l = (cx - finalW / 2f).coerceIn(0f, 1f - finalW)
    val t = (cy - finalH / 2f).coerceIn(0f, 1f - finalH)
    val r = (l + finalW).coerceAtMost(1f)
    val b = (t + finalH).coerceAtMost(1f)

    return CropRect(l, t, r, b).clamped()
}

private fun applyDrag(
    handle: CropHandle,
    rect: CropRect,
    dx: Float,
    dy: Float,
    aspectRatio: AspectRatio,
    imgAspect: Float
): CropRect {
    var l = rect.left
    var t = rect.top
    var r = rect.right
    var b = rect.bottom

    val minSize = 0.05f
    val ratio = aspectRatio.ratio

    when (handle) {
        CropHandle.INSIDE -> {
            val w = rect.width
            val h = rect.height
            val newL = (l + dx).coerceIn(0f, 1f - w)
            val newT = (t + dy).coerceIn(0f, 1f - h)
            return CropRect(newL, newT, newL + w, newT + h)
        }
        CropHandle.TOP_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            t = (t + dy).coerceIn(0f, b - minSize)
            if (ratio != null) {
                val currW = (r - l)
                val targetH = (currW * imgAspect / ratio).coerceIn(minSize, 1f)
                t = (b - targetH).coerceAtLeast(0f)
                l = (r - (targetH * ratio / imgAspect)).coerceAtLeast(0f)
            }
        }
        CropHandle.TOP_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            t = (t + dy).coerceIn(0f, b - minSize)
            if (ratio != null) {
                val currW = (r - l)
                val targetH = (currW * imgAspect / ratio).coerceIn(minSize, 1f)
                t = (b - targetH).coerceAtLeast(0f)
                r = (l + (targetH * ratio / imgAspect)).coerceAtMost(1f)
            }
        }
        CropHandle.BOTTOM_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            b = (b + dy).coerceIn(t + minSize, 1f)
            if (ratio != null) {
                val currW = (r - l)
                val targetH = (currW * imgAspect / ratio).coerceIn(minSize, 1f - t)
                b = (t + targetH).coerceAtMost(1f)
                l = (r - (targetH * ratio / imgAspect)).coerceAtLeast(0f)
            }
        }
        CropHandle.BOTTOM_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            b = (b + dy).coerceIn(t + minSize, 1f)
            if (ratio != null) {
                val currW = (r - l)
                val targetH = (currW * imgAspect / ratio).coerceIn(minSize, 1f - t)
                b = (t + targetH).coerceAtMost(1f)
                r = (l + (targetH * ratio / imgAspect)).coerceAtMost(1f)
            }
        }
        CropHandle.TOP -> {
            t = (t + dy).coerceIn(0f, b - minSize)
            if (ratio != null) {
                val targetW = ((b - t) * ratio / imgAspect).coerceIn(minSize, 1f)
                val cx = (l + r) / 2f
                l = (cx - targetW / 2f).coerceIn(0f, 1f - targetW)
                r = (l + targetW).coerceAtMost(1f)
            }
        }
        CropHandle.BOTTOM -> {
            b = (b + dy).coerceIn(t + minSize, 1f)
            if (ratio != null) {
                val targetW = ((b - t) * ratio / imgAspect).coerceIn(minSize, 1f)
                val cx = (l + r) / 2f
                l = (cx - targetW / 2f).coerceIn(0f, 1f - targetW)
                r = (l + targetW).coerceAtMost(1f)
            }
        }
        CropHandle.LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            if (ratio != null) {
                val targetH = ((r - l) * imgAspect / ratio).coerceIn(minSize, 1f)
                val cy = (t + b) / 2f
                t = (cy - targetH / 2f).coerceIn(0f, 1f - targetH)
                b = (t + targetH).coerceAtMost(1f)
            }
        }
        CropHandle.RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            if (ratio != null) {
                val targetH = ((r - l) * imgAspect / ratio).coerceIn(minSize, 1f)
                val cy = (t + b) / 2f
                t = (cy - targetH / 2f).coerceIn(0f, 1f - targetH)
                b = (t + targetH).coerceAtMost(1f)
            }
        }
        CropHandle.NONE -> return rect
    }

    return CropRect(l, t, r, b).clamped()
}

private fun DrawScope.drawScrim(
    imgRect: Rect,
    cropL: Float,
    cropT: Float,
    cropR: Float,
    cropB: Float
) {
    // Top
    drawRect(CropScrim, Offset(imgRect.left, imgRect.top), Size(imgRect.width, cropT - imgRect.top))
    // Bottom
    drawRect(CropScrim, Offset(imgRect.left, cropB), Size(imgRect.width, imgRect.bottom - cropB))
    // Left
    drawRect(CropScrim, Offset(imgRect.left, cropT), Size(cropL - imgRect.left, cropB - cropT))
    // Right
    drawRect(CropScrim, Offset(cropR, cropT), Size(imgRect.right - cropR, cropB - cropT))
}

private fun DrawScope.drawGrid(
    mode: GridDisplayMode,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    if (mode == GridDisplayMode.NONE) return

    val divisions = if (mode == GridDisplayMode.RULE_OF_THIRDS) 3 else 4
    val gridColor = Color.White.copy(alpha = 0.35f)

    for (i in 1 until divisions) {
        val x = left + (width / divisions) * i
        drawLine(gridColor, Offset(x, top), Offset(x, top + height), strokeWidth = 1f)

        val y = top + (height / divisions) * i
        drawLine(gridColor, Offset(left, y), Offset(left + width, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawHandles(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val cornerLen = 26f
    val cornerThickness = 4.5f
    val handleColor = NeonCyan

    // Corner brackets
    // Top-Left
    drawLine(handleColor, Offset(left, top), Offset(left + cornerLen, top), cornerThickness)
    drawLine(handleColor, Offset(left, top), Offset(left, top + cornerLen), cornerThickness)
    drawCircle(handleColor, radius = 4f, center = Offset(left, top))

    // Top-Right
    drawLine(handleColor, Offset(right, top), Offset(right - cornerLen, top), cornerThickness)
    drawLine(handleColor, Offset(right, top), Offset(right, top + cornerLen), cornerThickness)
    drawCircle(handleColor, radius = 4f, center = Offset(right, top))

    // Bottom-Left
    drawLine(handleColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerThickness)
    drawLine(handleColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerThickness)
    drawCircle(handleColor, radius = 4f, center = Offset(left, bottom))

    // Bottom-Right
    drawLine(handleColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerThickness)
    drawLine(handleColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerThickness)
    drawCircle(handleColor, radius = 4f, center = Offset(right, bottom))

    // Edge middle pills
    val pillLen = 16f
    val midX = (left + right) / 2f
    val midY = (top + bottom) / 2f

    // Top & Bottom
    drawLine(handleColor, Offset(midX - pillLen, top), Offset(midX + pillLen, top), cornerThickness)
    drawLine(handleColor, Offset(midX - pillLen, bottom), Offset(midX + pillLen, bottom), cornerThickness)

    // Left & Right
    drawLine(handleColor, Offset(left, midY - pillLen), Offset(left, midY + pillLen), cornerThickness)
    drawLine(handleColor, Offset(right, midY - pillLen), Offset(right, midY + pillLen), cornerThickness)
}
