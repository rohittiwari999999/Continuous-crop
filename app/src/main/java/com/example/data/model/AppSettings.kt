package com.example.data.model

data class AppSettings(
    val outputFormat: OutputImageFormat = OutputImageFormat.JPEG,
    val jpegQuality: Int = 95,
    val defaultAspectRatio: AspectRatio = AspectRatio.FREE,
    val defaultResizeOption: ResizeOption = ResizeOption.ORIGINAL,
    val showRuleOfThirdsGrid: GridDisplayMode = GridDisplayMode.RULE_OF_THIRDS,
    val showDimensionBadge: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val autoScrollRibbon: Boolean = true,
    val filenamePrefix: String = "CROP_",
    val storageAlbumName: String = "SnapCrop"
)
