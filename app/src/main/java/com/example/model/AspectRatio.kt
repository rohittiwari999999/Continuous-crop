package com.example.model

enum class AspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE_1_1("1:1", 1.0f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    PORTRAIT_9_16("9:16", 9f / 16f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    LANDSCAPE_16_9("16:9", 16f / 9f)
}
