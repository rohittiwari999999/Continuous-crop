package com.example.model

import android.graphics.RectF

sealed class DetectionType {
    object Face : DetectionType()
    object Text : DetectionType()
}

data class SmartDetection(
    val bounds: RectF,
    val type: DetectionType,
    val label: String
)
