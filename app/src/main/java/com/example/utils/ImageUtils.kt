package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ExportFormat
import com.example.model.SavedCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ImageUtils {

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // Determine sample size
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@withContext null

                var sampleSize = 1
                var width = options.outWidth
                var height = options.outHeight

                while (width > maxDimension || height > maxDimension) {
                    width /= 2
                    height /= 2
                    sampleSize *= 2
                }

                // Decode with inSampleSize
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@withContext null

                // Check Exif orientation
                val orientation = getExifOrientation(context, uri)
                if (orientation != 0) {
                    val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                } else {
                    decoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun cropAndTransform(
        source: Bitmap,
        normalizedRect: RectF,
        rotationDegrees: Int,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap {
        // Step 1: Apply global rotation and flipping to source if needed
        var baseBitmap = source
        if (rotationDegrees != 0 || flipH || flipV) {
            val matrix = Matrix().apply {
                if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
                val sx = if (flipH) -1f else 1f
                val sy = if (flipV) -1f else 1f
                if (flipH || flipV) postScale(sx, sy)
            }
            baseBitmap = Bitmap.createBitmap(
                source, 0, 0, source.width, source.height, matrix, true
            )
        }

        // Clamp normalized rect to [0, 1]
        val leftNorm = normalizedRect.left.coerceIn(0f, 1f)
        val topNorm = normalizedRect.top.coerceIn(0f, 1f)
        val rightNorm = normalizedRect.right.coerceIn(leftNorm + 0.01f, 1f)
        val bottomNorm = normalizedRect.bottom.coerceIn(topNorm + 0.01f, 1f)

        val cropX = (leftNorm * baseBitmap.width).toInt().coerceIn(0, baseBitmap.width - 1)
        val cropY = (topNorm * baseBitmap.height).toInt().coerceIn(0, baseBitmap.height - 1)
        val cropW = ((rightNorm - leftNorm) * baseBitmap.width).toInt().coerceIn(1, baseBitmap.width - cropX)
        val cropH = ((bottomNorm - topNorm) * baseBitmap.height).toInt().coerceIn(1, baseBitmap.height - cropY)

        val cropped = Bitmap.createBitmap(baseBitmap, cropX, cropY, cropW, cropH)
        if (baseBitmap != source && baseBitmap != cropped) {
            baseBitmap.recycle()
        }
        return cropped
    }

    suspend fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int = 92
    ): SavedCrop = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "SnapCrop_${timeStamp}.${format.extension}"
        val storageDir = File(context.filesDir, "saved_crops").apply { mkdirs() }
        val file = File(storageDir, fileName)

        val compressFormat = when (format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(compressFormat, quality, out)
        }

        val fileUri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        // Also export to MediaStore gallery if possible
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SnapCrop")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val mediaUri = context.contentResolver.insert(collection, contentValues)
            if (mediaUri != null) {
                context.contentResolver.openOutputStream(mediaUri)?.use { outStream ->
                    bitmap.compress(compressFormat, quality, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(mediaUri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SavedCrop(
            id = UUID.randomUUID().toString(),
            fileUri = fileUri,
            filePath = file.absolutePath,
            fileName = fileName,
            width = bitmap.width,
            height = bitmap.height,
            sizeBytes = file.length(),
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun getSavedCrops(context: Context): List<SavedCrop> = withContext(Dispatchers.IO) {
        val storageDir = File(context.filesDir, "saved_crops")
        if (!storageDir.exists()) return@withContext emptyList()

        val files = storageDir.listFiles { f ->
            f.isFile && (f.name.endsWith(".jpg") || f.name.endsWith(".png") || f.name.endsWith(".webp"))
        } ?: return@withContext emptyList()

        files.sortedByDescending { it.lastModified() }.map { file ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            SavedCrop(
                id = file.name,
                fileUri = uri,
                filePath = file.absolutePath,
                fileName = file.name,
                width = options.outWidth,
                height = options.outHeight,
                sizeBytes = file.length(),
                timestamp = file.lastModified()
            )
        }
    }

    suspend fun deleteSavedCrop(context: Context, crop: SavedCrop): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(crop.filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun createDefaultSampleBitmap(): Bitmap {
        val width = 1200
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background (Sunset over mountains)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#FF1A237E"), Color.parseColor("#FFD81B60"), Color.parseColor("#FFFFB300")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glowing sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFEE58")
        }
        canvas.drawCircle(width * 0.7f, height * 0.35f, 90f, sunPaint)

        // Mountain ridge 1 (Distant)
        val mtn1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#55281E5D")
        }
        val path1 = Path().apply {
            moveTo(0f, height * 0.65f)
            lineTo(width * 0.25f, height * 0.42f)
            lineTo(width * 0.55f, height * 0.58f)
            lineTo(width * 0.82f, height * 0.38f)
            lineTo(width.toFloat(), height * 0.62f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(path1, mtn1Paint)

        // Mountain ridge 2 (Foreground)
        val mtn2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DD0D1117")
        }
        val path2 = Path().apply {
            moveTo(0f, height * 0.78f)
            lineTo(width * 0.35f, height * 0.54f)
            lineTo(width * 0.68f, height * 0.72f)
            lineTo(width.toFloat(), height * 0.60f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(path2, mtn2Paint)

        // Sample text badge
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SnapCrop Workstation", width / 2f, 120f, textPaint)

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Drag handles to crop • Tap Open to load your photo", width / 2f, 175f, subTextPaint)

        return bitmap
    }
}
