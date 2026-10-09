package com.example.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object MediaStoreHelper {

    data class SaveResult(
        val uri: Uri?,
        val filePath: String?,
        val sizeBytes: Long
    )

    suspend fun saveCroppedBitmap(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
        prefix: String = "CROP_",
        albumName: String = "SnapCrop",
        customName: String? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val timestamp = System.currentTimeMillis()

        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
            else -> "jpg"
        }

        val mimeType = when (format) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
            else -> "image/jpeg"
        }

        val sanitizedCustom = customName?.replace(Regex("[^a-zA-Z0-9_-]"), "_")?.take(30)
        val filename = if (!sanitizedCustom.isNullOrBlank()) {
            "${sanitizedCustom}_${timestamp}.$extension"
        } else {
            "${prefix}${timestamp}.$extension"
        }

        var savedUri: Uri? = null
        var writtenBytes = 0L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$albumName")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                savedUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                        bitmap.compress(format, quality, outputStream)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        writtenBytes = pfd.statSize
                    }
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, albumName)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val destFile = File(appDir, filename)
                FileOutputStream(destFile).use { outputStream ->
                    bitmap.compress(format, quality, outputStream)
                }
                writtenBytes = destFile.length()

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SaveResult(
            uri = savedUri,
            filePath = filename,
            sizeBytes = writtenBytes
        )
    }
}
