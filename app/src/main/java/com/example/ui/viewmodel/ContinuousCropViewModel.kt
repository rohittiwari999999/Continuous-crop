package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.CropRect
import com.example.data.model.ResizeOption
import com.example.data.model.SavedCropItem
import com.example.data.repository.SettingsRepository
import com.example.util.BitmapUtils
import com.example.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ContinuousCropUiState(
    val sourceBitmap: Bitmap? = null,
    val sourceUri: Uri? = null,
    val cropRect: CropRect = CropRect(0.1f, 0.1f, 0.9f, 0.9f),
    val selectedAspectRatio: AspectRatio = AspectRatio.FREE,
    val selectedResizeOption: ResizeOption = ResizeOption.ORIGINAL,
    val rotationDegrees: Int = 0,
    val isFlippedHorizontally: Boolean = false,
    val savedItems: List<SavedCropItem> = emptyList(),
    val inFlightSavesCount: Int = 0,
    val lastSavedItemId: String? = null,
    val selectedItemForDetail: SavedCropItem? = null,
    val isLoadingImage: Boolean = false,
    val errorMessage: String? = null,
    val settings: AppSettings = AppSettings(),
    val isSettingsOpen: Boolean = false
)

class ContinuousCropViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        ContinuousCropUiState(
            settings = settingsRepository.settingsFlow.value,
            selectedAspectRatio = settingsRepository.settingsFlow.value.defaultAspectRatio,
            selectedResizeOption = settingsRepository.settingsFlow.value.defaultResizeOption
        )
    )
    val uiState: StateFlow<ContinuousCropUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent settings changes
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { savedSettings ->
                _uiState.update { it.copy(settings = savedSettings) }
            }
        }
    }

    fun clearImage() {
        _uiState.update {
            it.copy(
                sourceBitmap = null,
                sourceUri = null,
                cropRect = CropRect.default(),
                rotationDegrees = 0,
                isFlippedHorizontally = false
            )
        }
    }

    fun loadDirectBitmap(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                sourceBitmap = bitmap,
                sourceUri = null,
                cropRect = CropRect(0.1f, 0.1f, 0.9f, 0.9f),
                selectedAspectRatio = it.settings.defaultAspectRatio,
                selectedResizeOption = it.settings.defaultResizeOption,
                rotationDegrees = 0,
                isFlippedHorizontally = false,
                isLoadingImage = false
            )
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.updateSettings(newSettings)
    }

    fun resetSettingsToDefault() {
        settingsRepository.resetToDefaults()
        _uiState.update {
            it.copy(
                selectedAspectRatio = AspectRatio.FREE,
                selectedResizeOption = ResizeOption.ORIGINAL
            )
        }
    }

    fun loadSampleImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImage = true, errorMessage = null) }
            val sampleBitmap = withContext(Dispatchers.Default) {
                BitmapUtils.createSampleWorkstationImage()
            }
            _uiState.update {
                it.copy(
                    sourceBitmap = sampleBitmap,
                    sourceUri = null,
                    cropRect = CropRect(0.08f, 0.08f, 0.92f, 0.92f),
                    selectedAspectRatio = it.settings.defaultAspectRatio,
                    selectedResizeOption = it.settings.defaultResizeOption,
                    rotationDegrees = 0,
                    isFlippedHorizontally = false,
                    isLoadingImage = false
                )
            }
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImage = true, errorMessage = null) }
            val bitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
            if (bitmap != null) {
                _uiState.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        sourceUri = uri,
                        cropRect = CropRect(0.1f, 0.1f, 0.9f, 0.9f),
                        selectedAspectRatio = it.settings.defaultAspectRatio,
                        rotationDegrees = 0,
                        isFlippedHorizontally = false,
                        isLoadingImage = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        errorMessage = "Could not load selected image."
                    )
                }
            }
        }
    }

    fun updateCropRect(newRect: CropRect) {
        _uiState.update { it.copy(cropRect = newRect) }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        val currentBitmap = _uiState.value.sourceBitmap ?: return
        val currentRotation = _uiState.value.rotationDegrees
        val isLandscape = currentRotation % 180 != 0
        val srcW = if (isLandscape) currentBitmap.height.toFloat() else currentBitmap.width.toFloat()
        val srcH = if (isLandscape) currentBitmap.width.toFloat() else currentBitmap.height.toFloat()
        val imageAspect = srcW / srcH

        val adjustedRect = if (aspectRatio.ratio != null) {
            _uiState.value.cropRect.withAspectRatio(aspectRatio.ratio, imageAspect)
        } else {
            _uiState.value.cropRect
        }

        _uiState.update {
            it.copy(
                selectedAspectRatio = aspectRatio,
                cropRect = adjustedRect
            )
        }
    }

    fun setResizeOption(resizeOption: ResizeOption) {
        _uiState.update { it.copy(selectedResizeOption = resizeOption) }
    }

    fun rotate90() {
        _uiState.update {
            val newAngle = (it.rotationDegrees + 90) % 360
            it.copy(rotationDegrees = newAngle)
        }
    }

    fun flipHorizontal() {
        _uiState.update { it.copy(isFlippedHorizontally = !it.isFlippedHorizontally) }
    }

    fun resetCropRect() {
        _uiState.update {
            it.copy(
                cropRect = CropRect.default(),
                selectedAspectRatio = it.settings.defaultAspectRatio
            )
        }
    }

    /**
     * Executes non-blocking background crop & save using configured settings.
     * Crucially preserves active workstation state and bounding box!
     */
    fun cropAndSave(context: Context) {
        val currentState = _uiState.value
        val bitmap = currentState.sourceBitmap ?: return

        // Take snapshot of immutable parameters so user can immediately move the box
        val snapshotCropRect = currentState.cropRect
        val snapshotRotation = currentState.rotationDegrees
        val snapshotFlip = currentState.isFlippedHorizontally
        val snapshotResize = currentState.selectedResizeOption
        val snapshotAspectLabel = currentState.selectedAspectRatio.label
        val activeSettings = currentState.settings

        // Increment in-flight saves counter
        _uiState.update { it.copy(inFlightSavesCount = it.inFlightSavesCount + 1) }

        // Vibrate if haptics enabled
        if (activeSettings.enableHapticFeedback) {
            triggerHaptic(context)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Background step 1: Crop and transform
                val croppedBitmap = BitmapUtils.cropBitmap(
                    source = bitmap,
                    cropRect = snapshotCropRect,
                    rotationDegrees = snapshotRotation,
                    isFlippedHorizontally = snapshotFlip,
                    resizeOption = snapshotResize
                )

                // Background step 2: Generate small thumbnail for instant ribbon display
                val thumbnail = BitmapUtils.createThumbnail(croppedBitmap, size = 160)

                // Background step 3: Save to MediaStore (Scoped Storage)
                val saveResult = MediaStoreHelper.saveCroppedBitmap(
                    context = context.applicationContext,
                    bitmap = croppedBitmap,
                    format = activeSettings.outputFormat.compressFormat,
                    quality = activeSettings.jpegQuality,
                    prefix = activeSettings.filenamePrefix,
                    albumName = activeSettings.storageAlbumName
                )

                val newItem = SavedCropItem(
                    id = UUID.randomUUID().toString(),
                    uri = saveResult.uri,
                    filePath = saveResult.filePath,
                    thumbnailBitmap = thumbnail,
                    cropWidth = croppedBitmap.width,
                    cropHeight = croppedBitmap.height,
                    aspectRatioLabel = snapshotAspectLabel,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = saveResult.bytesWritten
                )

                // Clean up full cropped bitmap from memory if not needed further
                if (croppedBitmap !== bitmap) {
                    croppedBitmap.recycle()
                }

                // Update UI State on main thread
                _uiState.update { state ->
                    state.copy(
                        inFlightSavesCount = (state.inFlightSavesCount - 1).coerceAtLeast(0),
                        savedItems = listOf(newItem) + state.savedItems, // Prepend newest
                        lastSavedItemId = newItem.id
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { state ->
                    state.copy(
                        inFlightSavesCount = (state.inFlightSavesCount - 1).coerceAtLeast(0),
                        errorMessage = "Save failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun openItemDetail(item: SavedCropItem) {
        _uiState.update { it.copy(selectedItemForDetail = item) }
    }

    fun closeItemDetail() {
        _uiState.update { it.copy(selectedItemForDetail = null) }
    }

    fun deleteSavedCrop(item: SavedCropItem) {
        _uiState.update { state ->
            val updated = state.savedItems.filter { it.id != item.id }
            state.copy(
                savedItems = updated,
                selectedItemForDetail = if (state.selectedItemForDetail?.id == item.id) null else state.selectedItemForDetail
            )
        }
    }

    fun clearAllSavedCrops() {
        _uiState.update { it.copy(savedItems = emptyList(), selectedItemForDetail = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun triggerHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }
}
