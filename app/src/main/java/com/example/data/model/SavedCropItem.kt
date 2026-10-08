package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri

data class SavedCropItem(
    val id: String,
    val uri: Uri?,
    val filePath: String?,
    val thumbnailBitmap: Bitmap,
    val cropWidth: Int,
    val cropHeight: Int,
    val aspectRatioLabel: String,
    val timestamp: Long,
    val fileSizeBytes: Long = 0L
) {
    val formattedDimensions: String
        get() = "${cropWidth} × ${cropHeight}"

    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return ""
            val kb = fileSizeBytes / 1024f
            return if (kb > 1024) {
                String.format("%.1f MB", kb / 1024f)
            } else {
                String.format("%.0f KB", kb)
            }
        }
}
