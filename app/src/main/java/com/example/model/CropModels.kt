package com.example.model

import android.graphics.RectF
import android.net.Uri

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1.0f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    STORY_9_16("9:16", 9f / 16f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    LANDSCAPE_16_9("16:9", 16f / 9f)
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}

data class SavedCrop(
    val id: String,
    val fileUri: Uri,
    val filePath: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val timestamp: Long
)

data class DetectedFeature(
    val id: String,
    val label: String,
    val normalizedBounds: RectF
)
