package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.example.data.model.CropRect
import com.example.data.model.ResizeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {

    /**
     * Decode a bitmap from Uri with bounds downsampling to prevent OutOfMemoryError.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 2560,
        reqHeight: Int = 2560
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Executes the crop operation from a normalized crop rectangle and applies rotation/flip/resize.
     * Guaranteed thread-safe by taking coordinate snapshots.
     */
    suspend fun cropBitmap(
        source: Bitmap,
        cropRect: CropRect,
        rotationDegrees: Int = 0,
        isFlippedHorizontally: Boolean = false,
        resizeOption: ResizeOption = ResizeOption.ORIGINAL
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Apply rotation / flip to source if needed
        val orientedBitmap: Bitmap = if (rotationDegrees != 0 || isFlippedHorizontally) {
            val matrix = Matrix().apply {
                if (isFlippedHorizontally) {
                    postScale(-1f, 1f)
                }
                if (rotationDegrees != 0) {
                    postRotate(rotationDegrees.toFloat())
                }
            }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val srcW = orientedBitmap.width
        val srcH = orientedBitmap.height

        // Step 2: Compute pixel bounds from normalized rect
        val clamped = cropRect.clamped()
        val x = (clamped.left * srcW).toInt().coerceIn(0, srcW - 1)
        val y = (clamped.top * srcH).toInt().coerceIn(0, srcH - 1)
        val width = ((clamped.right - clamped.left) * srcW).toInt().coerceIn(1, srcW - x)
        val height = ((clamped.bottom - clamped.top) * srcH).toInt().coerceIn(1, srcH - y)

        val cropped = Bitmap.createBitmap(orientedBitmap, x, y, width, height)

        // Step 3: Apply resize option if required
        val (targetW, targetH) = resizeOption.calculateTargetDimensions(cropped.width, cropped.height)
        val finalBitmap = if (targetW != cropped.width || targetH != cropped.height) {
            Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        } else {
            cropped
        }

        // Clean up temporary rotated bitmap if created
        if (orientedBitmap !== source && orientedBitmap !== cropped && orientedBitmap !== finalBitmap) {
            orientedBitmap.recycle()
        }

        finalBitmap
    }

    /**
     * Generates a compact thumbnail (e.g. 140x140) for ribbon rendering.
     */
    suspend fun createThumbnail(bitmap: Bitmap, size: Int = 140): Bitmap = withContext(Dispatchers.Default) {
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (targetW, targetH) = if (aspect > 1f) {
            (size * aspect).toInt() to size
        } else {
            size to (size / aspect).toInt()
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, max(1, targetW), max(1, targetH), true)
        // Crop center square
        val startX = max(0, (scaled.width - size) / 2)
        val startY = max(0, (scaled.height - size) / 2)
        val squareW = min(size, scaled.width)
        val squareH = min(size, scaled.height)

        val thumb = Bitmap.createBitmap(scaled, startX, startY, squareW, squareH)
        if (scaled !== thumb && scaled !== bitmap) {
            scaled.recycle()
        }
        thumb
    }

    /**
     * Generates a high-resolution, vivid multi-item design board for immediate testing.
     * Contains multiple visual elements: cards, infographics, badges, diagrams, and portraits.
     */
    fun createSampleWorkstationImage(width: Int = 1600, height: Int = 1200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background dark gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Grid dots
        val dotPaint = Paint().apply {
            color = Color.parseColor("#25314C")
            isAntiAlias = true
        }
        val step = 40f
        var gx = 20f
        while (gx < width) {
            var gy = 20f
            while (gy < height) {
                canvas.drawCircle(gx, gy, 2f, dotPaint)
                gy += step
            }
            gx += step
        }

        // Draw Section 1: Vibrant Card "Telemetry & Metrics"
        drawCard(
            canvas, 80f, 80f, 440f, 320f,
            "#1E293B", "#38BDF8", "METRICS & ANALYTICS",
            "Continuous Crop Speed: 60 FPS\nBackground I/O: Non-blocking\nScoped Storage: Direct Gallery Sync",
            Color.parseColor("#38BDF8")
        )

        // Draw Section 2: Colorful Circular Badge "Pro Palette"
        drawBadge(
            canvas, 750f, 240f, 130f,
            "#8B5CF6", "#EC4899", "CONTINUOUS", "CROP PRO"
        )

        // Draw Section 3: Landscape / Sunset Illustration Card
        drawLandscapeCard(canvas, 960f, 80f, 560f, 320f)

        // Draw Section 4: Multi-Avatar / Portrait Cluster (Bottom Left)
        drawAvatarCluster(canvas, 80f, 460f, 440f, 320f)

        // Draw Section 5: Tech Diagram / Flowchart (Center Bottom)
        drawFlowchartCard(canvas, 580f, 460f, 440f, 320f)

        // Draw Section 6: Architecture Blueprint (Bottom Right)
        drawBlueprintCard(canvas, 1080f, 460f, 440f, 320f)

        // Draw Section 7: Bottom Banner "Extract Any Element Instantly"
        drawBanner(canvas, 80f, 840f, 1440f, 280f)

        return bitmap
    }

    private fun drawCard(
        canvas: Canvas,
        x: Float, y: Float, w: Float, h: Float,
        bgColorHex: String, borderColorHex: String,
        title: String, body: String, accentDotColor: Int
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            color = Color.parseColor(bgColorHex)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor(borderColorHex)
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        // Accent dot
        val dotPaint = Paint().apply {
            color = accentDotColor
            isAntiAlias = true
        }
        canvas.drawCircle(x + 36f, y + 44f, 10f, dotPaint)

        // Title
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(title, x + 60f, y + 52f, textPaint)

        // Body lines
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val lines = body.split("\n")
        var lineY = y + 110f
        for (line in lines) {
            canvas.drawText(line, x + 36f, lineY, bodyPaint)
            lineY += 34f
        }
    }

    private fun drawBadge(
        canvas: Canvas,
        cx: Float, cy: Float, radius: Float,
        colorStart: String, colorEnd: String,
        line1: String, line2: String
    ) {
        val paint = Paint().apply {
            shader = LinearGradient(
                cx - radius, cy - radius, cx + radius, cy + radius,
                Color.parseColor(colorStart), Color.parseColor(colorEnd),
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius, paint)

        val border = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius - 8f, border)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(line1, cx, cy - 8f, textPaint)
        canvas.drawText(line2, cx, cy + 28f, textPaint)
    }

    private fun drawLandscapeCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            shader = LinearGradient(
                x, y, x, y + h,
                Color.parseColor("#F59E0B"), Color.parseColor("#EF4444"),
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)

        // Sun
        val sunPaint = Paint().apply {
            color = Color.parseColor("#FEF08A")
            isAntiAlias = true
        }
        canvas.drawCircle(x + w * 0.3f, y + h * 0.45f, 48f, sunPaint)

        // Mountain silhouettes
        val mountainPaint = Paint().apply {
            color = Color.parseColor("#450A0A")
            isAntiAlias = true
        }
        val path = android.graphics.Path().apply {
            moveTo(x, y + h)
            lineTo(x + w * 0.25f, y + h * 0.5f)
            lineTo(x + w * 0.5f, y + h * 0.8f)
            lineTo(x + w * 0.75f, y + h * 0.4f)
            lineTo(x + w, y + h)
            close()
        }
        canvas.drawPath(path, mountainPaint)

        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("SUNSET HORIZON (16:9)", x + 24f, y + 42f, labelPaint)
    }

    private fun drawAvatarCluster(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        drawCard(
            canvas, x, y, w, h,
            "#1E293B", "#10B981", "CHARACTER ROSTER",
            "Multi-subject face extraction\nIdeal for profile crops\nBatch avatars rapidly",
            Color.parseColor("#10B981")
        )

        val colors = listOf("#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B")
        var ax = x + 60f
        for ((idx, col) in colors.withIndex()) {
            val p = Paint().apply {
                color = Color.parseColor(col)
                isAntiAlias = true
            }
            canvas.drawCircle(ax, y + 240f, 34f, p)

            val textP = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("P${idx + 1}", ax, y + 248f, textP)
            ax += 86f
        }
    }

    private fun drawFlowchartCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        drawCard(
            canvas, x, y, w, h,
            "#1E293B", "#6366F1", "WORKFLOW PIPELINE",
            "Touch & Drag Bounding Box\n-> Tap 'Crop & Save'\n-> Asynchronous Coroutine\n-> MediaStore Scoped Storage\n-> Live Ribbon Preview",
            Color.parseColor("#6366F1")
        )
    }

    private fun drawBlueprintCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            color = Color.parseColor("#0369A1")
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        // Draw geometric grid inside
        var ly = y + 20f
        while (ly < y + h) {
            canvas.drawLine(x + 20f, ly, x + w - 20f, ly, linePaint)
            ly += 30f
        }
        var lx = x + 20f
        while (lx < x + w) {
            canvas.drawLine(lx, y + 20f, lx, y + h - 20f, linePaint)
            lx += 30f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("SCHEMATIC GRID (1:1)", x + 28f, y + 50f, textPaint)
    }

    private fun drawBanner(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        val fillPaint = Paint().apply {
            shader = LinearGradient(
                x, y, x + w, y,
                Color.parseColor("#1E1B4B"), Color.parseColor("#0F766E"),
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("CONTINUOUS CROP WORKSTATION", x + 40f, y + 70f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 20f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText(
            "1. Drag handles or move box to frame any region above.",
            x + 40f, y + 120f, subPaint
        )
        canvas.drawText(
            "2. Tap 'Crop & Save' — notice saving is instant & non-blocking!",
            x + 40f, y + 160f, subPaint
        )
        canvas.drawText(
            "3. Reposition immediately & crop again! Thumbnails stack at the top ribbon in real-time.",
            x + 40f, y + 200f, subPaint
        )
        canvas.drawText(
            "4. Tap any saved thumbnail in the ribbon to view, share, or inspect metadata.",
            x + 40f, y + 240f, subPaint
        )
    }
}
