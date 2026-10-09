package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
    onSelectDetectedBox: (Int) -> Unit = {},
    onCropRectChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return

    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("crop_overlay_canvas")
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

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
                    .pointerInput(cropRect, aspectRatio, transformedBitmap) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val canvasW = size.width.toFloat()
                                val canvasH = size.height.toFloat()
                                val imgRect = calculateImageBounds(
                                    canvasW, canvasH,
                                    transformedBitmap.width.toFloat(),
                                    transformedBitmap.height.toFloat()
                                )

                                val normX = ((startOffset.x - imgRect.left) / imgRect.width).coerceIn(0f, 1f)
                                val normY = ((startOffset.y - imgRect.top) / imgRect.height).coerceIn(0f, 1f)

                                val handle = hitTestHandle(normX, normY, cropRect, imgRect.width, imgRect.height)
                                if (handle == CropHandle.NONE && detectedPassportBoxes.isNotEmpty()) {
                                    val tappedBox = detectedPassportBoxes.firstOrNull { box ->
                                        normX in box.cropRect.left..box.cropRect.right &&
                                        normY in box.cropRect.top..box.cropRect.bottom
                                    }
                                    if (tappedBox != null) {
                                        onSelectDetectedBox(tappedBox.index)
                                        activeHandle = CropHandle.NONE
                                    } else {
                                        activeHandle = CropHandle.NONE
                                    }
                                } else {
                                    activeHandle = handle
                                }
                            },
                            onDragEnd = {
                                activeHandle = CropHandle.NONE
                            },
                            onDragCancel = {
                                activeHandle = CropHandle.NONE
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val canvasW = size.width.toFloat()
                                val canvasH = size.height.toFloat()
                                val imgRect = calculateImageBounds(
                                    canvasW, canvasH,
                                    transformedBitmap.width.toFloat(),
                                    transformedBitmap.height.toFloat()
                                )

                                val deltaNormX = dragAmount.x / imgRect.width
                                val deltaNormY = dragAmount.y / imgRect.height

                                val updated = applyDrag(
                                    activeHandle,
                                    cropRect,
                                    deltaNormX,
                                    deltaNormY,
                                    aspectRatio,
                                    imgRect.width / imgRect.height
                                )
                                onCropRectChange(updated)
                            }
                        )
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
    val thresholdPx = 48f
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
        }
        CropHandle.TOP_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            t = (t + dy).coerceIn(0f, b - minSize)
        }
        CropHandle.BOTTOM_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }
        CropHandle.BOTTOM_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }
        CropHandle.TOP -> {
            t = (t + dy).coerceIn(0f, b - minSize)
        }
        CropHandle.BOTTOM -> {
            b = (b + dy).coerceIn(t + minSize, 1f)
        }
        CropHandle.LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
        }
        CropHandle.RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
        }
        CropHandle.NONE -> return rect
    }

    // Aspect ratio enforcement if locked
    val ratio = aspectRatio.ratio
    if (ratio != null) {
        val currW = (r - l)
        val targetH = (currW * imgAspect / ratio).coerceIn(minSize, 1f - t)
        b = (t + targetH).coerceAtMost(1f)
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
    val cornerLen = 22f
    val cornerThickness = 4f
    val handleColor = NeonCyan

    // Corner brackets
    // Top-Left
    drawLine(handleColor, Offset(left, top), Offset(left + cornerLen, top), cornerThickness)
    drawLine(handleColor, Offset(left, top), Offset(left, top + cornerLen), cornerThickness)

    // Top-Right
    drawLine(handleColor, Offset(right, top), Offset(right - cornerLen, top), cornerThickness)
    drawLine(handleColor, Offset(right, top), Offset(right, top + cornerLen), cornerThickness)

    // Bottom-Left
    drawLine(handleColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerThickness)
    drawLine(handleColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerThickness)

    // Bottom-Right
    drawLine(handleColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerThickness)
    drawLine(handleColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerThickness)

    // Edge middle pills
    val pillLen = 14f
    val midX = (left + right) / 2f
    val midY = (top + bottom) / 2f

    // Top & Bottom
    drawLine(handleColor, Offset(midX - pillLen, top), Offset(midX + pillLen, top), cornerThickness)
    drawLine(handleColor, Offset(midX - pillLen, bottom), Offset(midX + pillLen, bottom), cornerThickness)

    // Left & Right
    drawLine(handleColor, Offset(left, midY - pillLen), Offset(left, midY + pillLen), cornerThickness)
    drawLine(handleColor, Offset(right, midY - pillLen), Offset(right, midY + pillLen), cornerThickness)
}
