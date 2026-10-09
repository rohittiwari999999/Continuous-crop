package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.CropHandle
import com.example.data.model.CropRect
import com.example.data.model.ResizeOption
import com.example.data.model.SavedCropItem

data class DetectedPassportBox(
    val id: String,
    val index: Int,
    val cropRect: CropRect,
    val detectedName: String?,
    val savedItemId: String? = null
)

data class ContinuousCropUiState(
    val sourceBitmap: Bitmap? = null,
    val sourceUri: Uri? = null,
    val imageRotation: Int = 0,
    val isFlippedHorizontal: Boolean = false,
    val cropRect: CropRect = CropRect.DEFAULT,
    val activeAspectRatio: AspectRatio = AspectRatio.FREE,
    val activeResizeOption: ResizeOption = ResizeOption.ORIGINAL,
    val activeHandle: CropHandle = CropHandle.NONE,
    val savedItems: List<SavedCropItem> = emptyList(),
    val lastSavedId: String? = null,
    val inFlightCount: Int = 0,
    val selectedDetailItem: SavedCropItem? = null,
    val isProcessingAutoPassport: Boolean = false,
    val autoPassportProgress: Float = 0f,
    val statusMessage: String? = null,
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val detectedPassportBoxes: List<DetectedPassportBox> = emptyList(),
    val selectedDetectedBoxIndex: Int = -1,
    val detectedNumberForCurrentCrop: String? = null,
    val detectedFaceBoxCount: Int = 0,
    val appSettings: AppSettings = AppSettings()
) {
    val hasImage: Boolean get() = sourceBitmap != null

    val currentCropPixelDimensions: Pair<Int, Int>
        get() {
            val bmp = sourceBitmap ?: return Pair(0, 0)
            val w = if (imageRotation % 180 != 0) bmp.height else bmp.width
            val h = if (imageRotation % 180 != 0) bmp.width else bmp.height
            val pxW = (cropRect.width * w).toInt().coerceAtLeast(1)
            val pxH = (cropRect.height * h).toInt().coerceAtLeast(1)
            return activeResizeOption.calculateTargetDimensions(pxW, pxH)
        }

    val selectedDetectedBox: DetectedPassportBox?
        get() = detectedPassportBoxes.getOrNull(selectedDetectedBoxIndex)
}
