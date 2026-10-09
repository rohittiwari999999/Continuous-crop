package com.example.data.model

enum class AspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    PASSPORT("Passport", 35f / 45f), // Standard 3.5cm x 4.5cm ~ 0.778
    RATIO_1_1("1:1", 1.0f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_3_2("3:2", 3f / 2f),
    RATIO_2_3("2:3", 2f / 3f);

    val isFree: Boolean get() = ratio == null
}
