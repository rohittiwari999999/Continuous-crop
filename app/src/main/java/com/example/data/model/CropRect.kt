package com.example.data.model

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0.01f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.01f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun clamped(): CropRect {
        val cLeft = left.coerceIn(0f, 0.99f)
        val cTop = top.coerceIn(0f, 0.99f)
        val cRight = right.coerceIn(cLeft + 0.01f, 1f)
        val cBottom = bottom.coerceIn(cTop + 0.01f, 1f)
        return CropRect(cLeft, cTop, cRight, cBottom)
    }

    companion object {
        val DEFAULT = CropRect(0.1f, 0.1f, 0.9f, 0.9f)
    }
}
