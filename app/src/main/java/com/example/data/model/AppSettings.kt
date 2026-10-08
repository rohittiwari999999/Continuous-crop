package com.example.data.model

import android.graphics.Bitmap

enum class OutputImageFormat(val displayName: String, val extension: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("JPEG (Recommended)", "jpg", Bitmap.CompressFormat.JPEG),
    PNG("PNG (Lossless)", "png", Bitmap.CompressFormat.PNG),
    WEBP("WEBP (Modern Compact)", "webp", Bitmap.CompressFormat.WEBP_LOSSY)
}

enum class GridDisplayMode(val label: String) {
    ALWAYS("Always Visible"),
    DURING_DRAG("Only When Dragging"),
    HIDDEN("Hidden")
}

data class AppSettings(
    val outputFormat: OutputImageFormat = OutputImageFormat.JPEG,
    val jpegQuality: Int = 92, // 50 to 100
    val defaultAspectRatio: AspectRatio = AspectRatio.FREE,
    val defaultResizeOption: ResizeOption = ResizeOption.ORIGINAL,
    val showRuleOfThirdsGrid: GridDisplayMode = GridDisplayMode.ALWAYS,
    val showDimensionBadge: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val autoScrollRibbon: Boolean = true,
    val filenamePrefix: String = "SNAP_",
    val storageAlbumName: String = "SnapCrop"
)
