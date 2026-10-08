package com.example.data.model

enum class ResizeOption(val label: String, val description: String) {
    ORIGINAL("Original", "100% full pixel resolution"),
    SCALE_75("75%", "Downscale to 75% resolution"),
    SCALE_50("50%", "Downscale to 50% for fast sharing"),
    MAX_1080P("Max 1080p", "Fit within 1920x1080"),
    MAX_720P("Max 720p", "Fit within 1280x720");

    fun calculateTargetDimensions(srcWidth: Int, srcHeight: Int): Pair<Int, Int> {
        return when (this) {
            ORIGINAL -> Pair(srcWidth, srcHeight)
            SCALE_75 -> Pair((srcWidth * 0.75f).toInt().coerceAtLeast(1), (srcHeight * 0.75f).toInt().coerceAtLeast(1))
            SCALE_50 -> Pair((srcWidth * 0.50f).toInt().coerceAtLeast(1), (srcHeight * 0.50f).toInt().coerceAtLeast(1))
            MAX_1080P -> {
                val maxDim = 1920
                if (srcWidth <= maxDim && srcHeight <= maxDim) {
                    Pair(srcWidth, srcHeight)
                } else {
                    val scale = maxDim.toFloat() / maxOf(srcWidth, srcHeight)
                    Pair((srcWidth * scale).toInt().coerceAtLeast(1), (srcHeight * scale).toInt().coerceAtLeast(1))
                }
            }
            MAX_720P -> {
                val maxDim = 1280
                if (srcWidth <= maxDim && srcHeight <= maxDim) {
                    Pair(srcWidth, srcHeight)
                } else {
                    val scale = maxDim.toFloat() / maxOf(srcWidth, srcHeight)
                    Pair((srcWidth * scale).toInt().coerceAtLeast(1), (srcHeight * scale).toInt().coerceAtLeast(1))
                }
            }
        }
    }
}
