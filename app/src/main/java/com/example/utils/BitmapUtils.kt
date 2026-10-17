package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {

    suspend fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int = 2048, reqHeight: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun cropBitmap(source: Bitmap, normLeft: Float, normTop: Float, normRight: Float, normBottom: Float): Bitmap? {
        val l = (normLeft.coerceIn(0f, 1f) * source.width).toInt()
        val t = (normTop.coerceIn(0f, 1f) * source.height).toInt()
        val r = (normRight.coerceIn(0f, 1f) * source.width).toInt()
        val b = (normBottom.coerceIn(0f, 1f) * source.height).toInt()

        val cropX = min(l, r).coerceIn(0, source.width - 1)
        val cropY = min(t, b).coerceIn(0, source.height - 1)
        val cropW = max(1, (max(l, r) - cropX)).coerceAtMost(source.width - cropX)
        val cropH = max(1, (max(t, b) - cropY)).coerceAtMost(source.height - cropY)

        return try {
            Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            else postScale(1f, -1f, source.width / 2f, source.height / 2f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, quality: Int = 92): Pair<Uri, File>? = withContext(Dispatchers.IO) {
        try {
            val outputDir = File(context.cacheDir, "shares").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(outputDir, "SNAP_CROP_$timeStamp.jpg")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )
            Pair(uri, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createCameraUri(context: Context): Pair<Uri, File> {
        val outputDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(outputDir, "CAMERA_$timeStamp.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        return Pair(uri, file)
    }

    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Cropped Image"))
    }
}
