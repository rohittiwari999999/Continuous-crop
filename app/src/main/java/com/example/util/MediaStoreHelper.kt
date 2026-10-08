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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreHelper {

    data class SaveResult(
        val uri: Uri?,
        val filePath: String?,
        val bytesWritten: Long
    )

    /**
     * Saves a cropped Bitmap to the device MediaStore / Pictures directory asynchronously.
     */
    suspend fun saveCroppedBitmap(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 92,
        prefix: String = "CROP_",
        albumName: String = "ContinuousCrop"
    ): SaveResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
            else -> "jpg"
        }
        val mimeType = when (extension) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val safePrefix = if (prefix.isNotBlank()) prefix else "CROP_"
        val safeAlbum = if (albumName.isNotBlank()) albumName else "ContinuousCrop"
        val displayName = "${safePrefix}${timestamp}.$extension"

        val resolver: ContentResolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$safeAlbum")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            var bytesWritten = 0L

            if (imageUri != null) {
                try {
                    resolver.openOutputStream(imageUri)?.use { outStream: OutputStream ->
                        bitmap.compress(format, quality, outStream)
                        outStream.flush()
                    }

                    // Retrieve actual size
                    resolver.openFileDescriptor(imageUri, "r")?.use { pfd ->
                        bytesWritten = pfd.statSize
                    }

                    // Mark as no longer pending
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)

                    return@withContext SaveResult(
                        uri = imageUri,
                        filePath = "Pictures/$safeAlbum/$displayName",
                        bytesWritten = bytesWritten
                    )
                } catch (e: Exception) {
                    resolver.delete(imageUri, null, null)
                    throw e
                }
            } else {
                throw IllegalStateException("Failed to create MediaStore entry for cropped image")
            }
        } else {
            // Android 9 and below
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val albumDir = File(picturesDir, safeAlbum)
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }
            val destinationFile = File(albumDir, displayName)
            var bytesWritten = 0L

            FileOutputStream(destinationFile).use { outStream ->
                bitmap.compress(format, quality, outStream)
                outStream.flush()
                bytesWritten = destinationFile.length()
            }

            // Also insert into MediaStore so it appears in the device gallery immediately
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, displayName)
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.DESCRIPTION, "Continuous crop image")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATA, destinationFile.absolutePath)
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            return@withContext SaveResult(
                uri = imageUri ?: Uri.fromFile(destinationFile),
                filePath = destinationFile.absolutePath,
                bytesWritten = bytesWritten
            )
        }
    }
}
