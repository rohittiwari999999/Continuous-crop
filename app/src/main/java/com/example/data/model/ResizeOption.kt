package com.example.data.model

import kotlin.math.roundToInt

enum class ResizeOption(val label: String, val description: String) {
    ORIGINAL("Original", "100% full pixel resolution"),
    SCALE_75("75%", "Downscale to 75% resolution"),
    SCALE_50("50%", "Downscale to 50% for fast sharing"),
    MAX_1080P("Max 1080p", "Fit within 1920x1080"),
    MAX_720P("Max 720p", "Fit within 1280x720");

    fun calculateTargetDimensions(srcWidth: Int, srcHeight: Int): Pair<Int, Int> {
        return when (this) {
            ORIGINAL -> Pair(srcWidth, srcHeight)
            SCALE_75 -> Pair((srcWidth * 0.75f).roundToInt().coerceAtLeast(1), (srcHeight * 0.75f).roundToInt().coerceAtLeast(1))
            SCALE_50 -> Pair((srcWidth * 0.50f).roundToInt().coerceAtLeast(1), (srcHeight * 0.50f).roundToInt().coerceAtLeast(1))
            MAX_1080P -> {
                val maxDim = 1920f
                val scale = if (srcWidth > maxDim || srcHeight > maxDim) {
                    (maxDim / maxOf(srcWidth, srcHeight))
                } else 1f
                Pair((srcWidth * scale).roundToInt().coerceAtLeast(1), (srcHeight * scale).roundToInt().coerceAtLeast(1))
            }
            MAX_720P -> {
                val maxDim = 1280f
                val scale = if (srcWidth > maxDim || srcHeight > maxDim) {
                    (maxDim / maxOf(srcWidth, srcHeight))
                } else 1f
                Pair((srcWidth * scale).roundToInt().coerceAtLeast(1), (srcHeight * scale).roundToInt().coerceAtLeast(1))
            }
        }
    }
}
