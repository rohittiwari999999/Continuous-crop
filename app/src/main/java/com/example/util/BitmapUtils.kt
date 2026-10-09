package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.example.data.model.CropRect
import com.example.data.model.ResizeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtils {

    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val (height: Int, width: Int) = options.outHeight to options.outWidth
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun cropBitmap(
        source: Bitmap,
        cropRect: CropRect,
        rotationDegrees: Int = 0,
        isFlipped: Boolean = false,
        resizeOption: ResizeOption = ResizeOption.ORIGINAL
    ): Bitmap = withContext(Dispatchers.Default) {
        val srcW = source.width
        val srcH = source.height

        val leftPx = (cropRect.left * srcW).roundToInt().coerceIn(0, srcW - 1)
        val topPx = (cropRect.top * srcH).roundToInt().coerceIn(0, srcH - 1)
        val widthPx = ((cropRect.right - cropRect.left) * srcW).roundToInt().coerceIn(1, srcW - leftPx)
        val heightPx = ((cropRect.bottom - cropRect.top) * srcH).roundToInt().coerceIn(1, srcH - topPx)

        val cropped = Bitmap.createBitmap(source, leftPx, topPx, widthPx, heightPx)

        val matrix = Matrix()
        if (isFlipped) {
            matrix.postScale(-1f, 1f, cropped.width / 2f, cropped.height / 2f)
        }
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }

        val transformed = if (rotationDegrees != 0 || isFlipped) {
            val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
            if (rotated != cropped) {
                cropped.recycle()
            }
            rotated
        } else {
            cropped
        }

        val (targetW, targetH) = resizeOption.calculateTargetDimensions(transformed.width, transformed.height)
        if (targetW != transformed.width || targetH != transformed.height) {
            val resized = Bitmap.createScaledBitmap(transformed, targetW, targetH, true)
            if (resized != transformed) {
                transformed.recycle()
            }
            resized
        } else {
            transformed
        }
    }

    suspend fun createThumbnail(
        source: Bitmap,
        maxDimension: Int = 180
    ): Bitmap = withContext(Dispatchers.Default) {
        val scale = maxDimension.toFloat() / max(source.width, source.height)
        val targetW = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * scale).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    fun createSampleWorkstationImage(): Bitmap {
        val width = 1600
        val height = 2000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.rgb(245, 247, 250),
                Color.rgb(226, 232, 240),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("STUDENT ID PASSPORT SHEET", width / 2f, 120f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ACADEMIC BATCH 2026 • AUTO PASSPORT CROP TEST", width / 2f, 175f, subPaint)

        val cols = 3
        val rows = 3
        val startX = 140f
        val startY = 240f
        val photoW = 380f
        val photoH = 490f
        val gapX = 80f
        val gapY = 85f

        val photoBgPaint = Paint().apply {
            color = Color.rgb(220, 235, 252)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 191, 160)
        }
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 30, 20)
        }
        val eyesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
        }
        val shirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
        }
        val labelBgPaint = Paint().apply {
            color = Color.rgb(255, 255, 255)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        var studentId = 101
        val names = listOf(
            "AARAV SHARMA", "PRIYA VERMA", "ROHAN GUPTA",
            "ANANYA SINGH", "VIVEK PATEL", "ISHA REDDY",
            "KARAN JOSHI", "NEHA MALHOTRA", "AMIT KUMAR"
        )

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val px = startX + c * (photoW + gapX)
                val py = startY + r * (photoH + gapY)

                canvas.drawRect(px, py, px + photoW, py + photoH, photoBgPaint)
                canvas.drawRect(px, py, px + photoW, py + photoH, borderPaint)

                val cx = px + photoW / 2f
                val cy = py + photoH * 0.44f

                canvas.drawArc(
                    cx - photoW * 0.42f, py + photoH * 0.65f,
                    cx + photoW * 0.42f, py + photoH * 1.25f,
                    180f, 180f, true, shirtPaint
                )

                canvas.drawCircle(cx, cy - 35f, 95f, hairPaint)
                canvas.drawOval(
                    cx - 75f, cy - 80f,
                    cx + 75f, cy + 90f,
                    facePaint
                )

                canvas.drawCircle(cx - 28f, cy - 10f, 9f, eyesPaint)
                canvas.drawCircle(cx + 28f, cy - 10f, 9f, eyesPaint)

                val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(217, 119, 90)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawArc(cx - 22f, cy + 30f, cx + 22f, cy + 50f, 0f, 180f, false, mouthPaint)

                val labelY = py + photoH - 60f
                canvas.drawRect(px + 10f, labelY, px + photoW - 10f, py + photoH - 10f, labelBgPaint)
                canvas.drawRect(px + 10f, labelY, px + photoW - 10f, py + photoH - 10f, borderPaint)

                val rollNoText = "ROLL NO: $studentId"
                canvas.drawText(rollNoText, cx, labelY + 36f, textPaint)

                studentId++
            }
        }

        return bitmap
    }
}
