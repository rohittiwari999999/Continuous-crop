package com.example.data.model

import android.graphics.Bitmap

enum class OutputImageFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val compressFormat: Bitmap.CompressFormat
) {
    JPEG("JPEG", "jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("PNG", "png", "image/png", Bitmap.CompressFormat.PNG),
    WEBP("WEBP", "webp", "image/webp", Bitmap.CompressFormat.WEBP)
}
