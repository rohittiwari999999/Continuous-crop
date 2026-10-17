package com.example.model

import android.net.Uri

data class CropItem(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val aspectRatioName: String = "Free"
)
