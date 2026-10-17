package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import com.example.util.PassportDetectionHelper
import com.example.util.PhotoNumberOcrHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ContinuousCropViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ContinuousCropUiState())
    val uiState: StateFlow<ContinuousCropUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        appSettings = settings,
                        activeAspectRatio = if (current.activeAspectRatio == AspectRatio.FREE) settings.defaultAspectRatio else current.activeAspectRatio,
                        activeResizeOption = settings.defaultResizeOption
                    )
                }
            }
        }
        // Load default sample workstation image so user sees something rich immediately
        loadSampleImage()
    }

    fun loadSampleImage() {
        viewModelScope.launch {
            val sampleBitmap = BitmapUtils.createSampleWorkstationImage()
            _uiState.update {
                it.copy(
                    sourceBitmap = sampleBitmap,
                    sourceUri = null,
                    imageRotation = 0,
                    isFlippedHorizontal = false,
                    cropRect = CropRect.DEFAULT
                )
            }
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(inFlightCount = it.inFlightCount + 1) }
            val bitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
            if (bitmap != null) {
                _uiState.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        sourceUri = uri,
                        imageRotation = 0,
                        isFlippedHorizontal = false,
                        cropRect = CropRect.DEFAULT,
                        inFlightCount = (it.inFlightCount - 1).coerceAtLeast(0),
                        statusMessage = "Image loaded successfully"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        inFlightCount = (it.inFlightCount - 1).coerceAtLeast(0),
                        statusMessage = "Failed to load image"
                    )
                }
            }
        }
    }

    fun updateCropRect(newRect: CropRect) {
        val clamped = newRect.clamped()
        val state = _uiState.value
        val updatedBoxes = if (state.selectedDetectedBoxIndex in state.detectedPassportBoxes.indices) {
            state.detectedPassportBoxes.mapIndexed { idx, box ->
                if (idx == state.selectedDetectedBoxIndex) box.copy(cropRect = clamped) else box
            }
        } else {
            state.detectedPassportBoxes
        }
        _uiState.update { it.copy(cropRect = clamped, detectedPassportBoxes = updatedBoxes) }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        val current = _uiState.value
        val ratio = aspectRatio.ratio

        val updatedRect = if (ratio != null) {
            val bmp = current.sourceBitmap
            val imgRatio = if (bmp != null) {
                val w = if (current.imageRotation % 180 != 0) bmp.height else bmp.width
                val h = if (current.imageRotation % 180 != 0) bmp.width else bmp.height
                w.toFloat() / h.toFloat()
            } else 1f

            val currentRect = current.cropRect
            val cx = currentRect.centerX
            val cy = currentRect.centerY

            val targetWidthNorm = (currentRect.height * ratio) / imgRatio
            val finalW = targetWidthNorm.coerceIn(0.1f, 0.95f)
            val finalH = (finalW * imgRatio / ratio).coerceIn(0.1f, 0.95f)

            val left = (cx - finalW / 2f).coerceIn(0f, 1f - finalW)
            val top = (cy - finalH / 2f).coerceIn(0f, 1f - finalH)
            CropRect(left, top, left + finalW, top + finalH)
        } else {
            current.cropRect
        }

        _uiState.update {
            it.copy(
                activeAspectRatio = aspectRatio,
                cropRect = updatedRect
            )
        }
    }

    fun setResizeOption(option: ResizeOption) {
        _uiState.update { it.copy(activeResizeOption = option) }
    }

    fun rotateClockwise() {
        _uiState.update {
            it.copy(imageRotation = (it.imageRotation + 90) % 360)
        }
    }

    fun flipHorizontal() {
        _uiState.update {
            it.copy(isFlippedHorizontal = !it.isFlippedHorizontal)
        }
    }

    fun resetCrop() {
        _uiState.update {
            it.copy(
                cropRect = CropRect.DEFAULT,
                imageRotation = 0,
                isFlippedHorizontal = false,
                activeAspectRatio = AspectRatio.FREE
            )
        }
    }

    fun cropAndSave(context: Context) {
        val state = _uiState.value
        // If an existing item or box is currently selected for editing, update it instead of creating duplicate
        if (state.selectedSavedItemId != null || state.selectedDetectedBoxIndex in state.detectedPassportBoxes.indices) {
            saveOrUpdateSelectedCrop(context)
            return
        }

        val bmp = state.sourceBitmap ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(inFlightCount = it.inFlightCount + 1) }

            val cropped = BitmapUtils.cropBitmap(
                source = bmp,
                cropRect = state.cropRect,
                rotationDegrees = state.imageRotation,
                isFlipped = state.isFlippedHorizontal,
                resizeOption = state.activeResizeOption
            )

            // Use existing scanned number or detect OCR
            val detectedNum = state.detectedNumberForCurrentCrop ?: PhotoNumberOcrHelper.detectNumberAroundCrop(
                source = bmp,
                cropRect = state.cropRect,
                rotationDegrees = state.imageRotation,
                isFlipped = state.isFlippedHorizontal
            )

            val thumbnail = BitmapUtils.createThumbnail(cropped)

            val saveResult = MediaStoreHelper.saveCroppedBitmap(
                context = context,
                bitmap = cropped,
                format = state.appSettings.outputFormat.compressFormat,
                quality = state.appSettings.jpegQuality,
                prefix = state.appSettings.filenamePrefix,
                albumName = state.appSettings.storageAlbumName,
                customName = detectedNum
            )

            val id = UUID.randomUUID().toString()
            val newItem = SavedCropItem(
                id = id,
                uri = saveResult.uri,
                filePath = saveResult.filePath,
                thumbnailBitmap = thumbnail,
                cropWidth = cropped.width,
                cropHeight = cropped.height,
                aspectRatioLabel = state.activeAspectRatio.label,
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = saveResult.sizeBytes,
                detectedName = detectedNum,
                cropRect = state.cropRect
            )

            _uiState.update { current ->
                current.copy(
                    savedItems = listOf(newItem) + current.savedItems,
                    lastSavedId = id,
                    selectedSavedItemId = id,
                    inFlightCount = (current.inFlightCount - 1).coerceAtLeast(0),
                    statusMessage = "Photo saved to gallery & storage"
                )
            }
        }
    }

    /**
     * AUTO PASSPORT:
     * - Uses real ML Kit face detection taking rotation and flip into account.
     * - Accurately frames passport proportions (3.5x4.5 ratio, ~72% head size).
     * - Saves real crops and sets the interactive crop handles on the real photo.
     */
    fun autoDetectPassport(context: Context) {
        val state = _uiState.value
        val bmp = state.sourceBitmap ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAutoPassport = true,
                    autoPassportProgress = 0.05f
                )
            }

            // Detect faces respecting orientation
            val faceItems = PassportDetectionHelper.detectAllPassportFaces(
                source = bmp,
                rotationDegrees = state.imageRotation,
                isFlipped = state.isFlippedHorizontal
            )

            val total = faceItems.size
            val newSavedItems = mutableListOf<SavedCropItem>()
            val boxes = mutableListOf<DetectedPassportBox>()
            var firstSavedId: String? = null

            faceItems.forEachIndexed { index, item ->
                val progress = 0.05f + (index.toFloat() / total.coerceAtLeast(1)) * 0.9f
                _uiState.update { it.copy(autoPassportProgress = progress) }

                val detectedNum = PhotoNumberOcrHelper.detectNumberAroundCrop(
                    source = bmp,
                    cropRect = item.cropRect,
                    rotationDegrees = state.imageRotation,
                    isFlipped = state.isFlippedHorizontal
                ) ?: if (total > 1) "Passport ${index + 1}" else "Passport"

                val cropped = BitmapUtils.cropBitmap(
                    source = bmp,
                    cropRect = item.cropRect,
                    rotationDegrees = state.imageRotation,
                    isFlipped = state.isFlippedHorizontal,
                    resizeOption = state.appSettings.defaultResizeOption
                )

                val thumb = BitmapUtils.createThumbnail(cropped)

                val saveResult = MediaStoreHelper.saveCroppedBitmap(
                    context = context.applicationContext,
                    bitmap = cropped,
                    format = state.appSettings.outputFormat.compressFormat,
                    quality = state.appSettings.jpegQuality,
                    prefix = "PASSPORT_",
                    albumName = state.appSettings.storageAlbumName,
                    customName = detectedNum
                )

                val itemId = UUID.randomUUID().toString()
                if (firstSavedId == null) {
                    firstSavedId = itemId
                }

                val savedItem = SavedCropItem(
                    id = itemId,
                    uri = saveResult.uri,
                    filePath = saveResult.filePath,
                    thumbnailBitmap = thumb,
                    cropWidth = cropped.width,
                    cropHeight = cropped.height,
                    aspectRatioLabel = "Passport",
                    timestamp = System.currentTimeMillis() + index,
                    fileSizeBytes = saveResult.sizeBytes,
                    detectedName = detectedNum,
                    cropRect = item.cropRect
                )
                newSavedItems.add(savedItem)

                val box = DetectedPassportBox(
                    id = UUID.randomUUID().toString(),
                    index = index,
                    cropRect = item.cropRect,
                    detectedName = detectedNum,
                    savedItemId = itemId
                )
                boxes.add(box)
            }

            val firstBox = boxes.firstOrNull()
            _uiState.update { current ->
                current.copy(
                    detectedPassportBoxes = boxes,
                    selectedDetectedBoxIndex = 0,
                    cropRect = firstBox?.cropRect ?: current.cropRect,
                    detectedNumberForCurrentCrop = firstBox?.detectedName,
                    activeAspectRatio = AspectRatio.PASSPORT,
                    savedItems = newSavedItems + current.savedItems,
                    lastSavedId = firstSavedId,
                    selectedSavedItemId = firstSavedId,
                    isProcessingAutoPassport = false,
                    autoPassportProgress = 1f,
                    detectedFaceBoxCount = boxes.size,
                    statusMessage = if (boxes.size > 1) {
                        "Detected ${boxes.size} photos! Tap any thumbnail to adjust & update."
                    } else {
                        "Passport photo detected! Adjust handles if needed, then tap Update."
                    }
                )
            }
        }
    }

    /**
     * Updates/re-saves the currently adjusted photo crop:
     * - Re-crops from source image using the exact user-adjusted crop box
     * - Updates the corresponding photo in the top slider ribbon in-place!
     * - Overwrites the saved file in storage so the gallery stays clean
     */
    fun saveOrUpdateSelectedCrop(context: Context) {
        val state = _uiState.value
        val bmp = state.sourceBitmap ?: return

        val targetItemId = state.selectedSavedItemId
            ?: state.selectedDetectedBox?.savedItemId
            ?: state.lastSavedId
            ?: UUID.randomUUID().toString()

        viewModelScope.launch {
            _uiState.update { it.copy(inFlightCount = it.inFlightCount + 1) }

            val cropped = BitmapUtils.cropBitmap(
                source = bmp,
                cropRect = state.cropRect,
                rotationDegrees = state.imageRotation,
                isFlipped = state.isFlippedHorizontal,
                resizeOption = state.activeResizeOption
            )

            val thumb = BitmapUtils.createThumbnail(cropped)
            val currentBox = state.selectedDetectedBox
            val existingItem = state.savedItems.firstOrNull { it.id == targetItemId }
            val rollName = state.detectedNumberForCurrentCrop
                ?: currentBox?.detectedName
                ?: existingItem?.detectedName
                ?: "Passport"

            val saveResult = MediaStoreHelper.saveCroppedBitmap(
                context = context.applicationContext,
                bitmap = cropped,
                format = state.appSettings.outputFormat.compressFormat,
                quality = state.appSettings.jpegQuality,
                prefix = "PASSPORT_",
                albumName = state.appSettings.storageAlbumName,
                customName = rollName
            )

            val updatedSavedItem = SavedCropItem(
                id = targetItemId,
                uri = saveResult.uri,
                filePath = saveResult.filePath,
                thumbnailBitmap = thumb,
                cropWidth = cropped.width,
                cropHeight = cropped.height,
                aspectRatioLabel = state.activeAspectRatio.label,
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = saveResult.sizeBytes,
                detectedName = rollName,
                cropRect = state.cropRect
            )

            // Update top slider ribbon: Replace existing item in-place by exact ID
            val currentSaved = state.savedItems.toMutableList()
            val existingIndex = currentSaved.indexOfFirst { it.id == targetItemId }
            if (existingIndex != -1) {
                currentSaved[existingIndex] = updatedSavedItem
            } else {
                currentSaved.add(0, updatedSavedItem)
            }

            // Update detectedPassportBoxes with new cropRect and rollName
            val updatedBoxes = if (state.selectedDetectedBoxIndex in state.detectedPassportBoxes.indices) {
                state.detectedPassportBoxes.mapIndexed { idx, box ->
                    if (idx == state.selectedDetectedBoxIndex) {
                        box.copy(cropRect = state.cropRect, detectedName = rollName, savedItemId = targetItemId)
                    } else box
                }
            } else {
                state.detectedPassportBoxes
            }

            _uiState.update { current ->
                current.copy(
                    savedItems = currentSaved,
                    detectedPassportBoxes = updatedBoxes,
                    lastSavedId = targetItemId,
                    selectedSavedItemId = targetItemId,
                    inFlightCount = (current.inFlightCount - 1).coerceAtLeast(0),
                    statusMessage = "Updated photo '$rollName' in slider & storage"
                )
            }
        }
    }

    /**
     * Called when tapping any thumbnail in the ribbon:
     * - Selects that item for editing
     * - Moves the crop box on the canvas directly to this photo's crop rect
     * - Sets active aspect ratio to match
     */
    fun onRibbonItemClick(item: SavedCropItem) {
        selectSavedItemForEditing(item)
    }

    fun selectSavedItemForEditing(item: SavedCropItem) {
        val state = _uiState.value
        val boxIndex = state.detectedPassportBoxes.indexOfFirst {
            it.savedItemId == item.id
        }

        val targetRect = item.cropRect
            ?: (if (boxIndex != -1) state.detectedPassportBoxes[boxIndex].cropRect else null)
            ?: state.cropRect

        _uiState.update {
            it.copy(
                selectedSavedItemId = item.id,
                selectedDetectedBoxIndex = boxIndex,
                cropRect = targetRect,
                detectedNumberForCurrentCrop = item.detectedName,
                lastSavedId = item.id,
                statusMessage = "Editing '${item.detectedName ?: "photo"}'. Adjust handles, then tap Update."
            )
        }
    }

    fun deselectActiveEdit() {
        _uiState.update {
            it.copy(
                selectedSavedItemId = null,
                selectedDetectedBoxIndex = -1,
                statusMessage = "Edit deselected. Ready for new crop."
            )
        }
    }

    fun selectDetectedBox(index: Int) {
        val state = _uiState.value
        val boxes = state.detectedPassportBoxes
        if (index in boxes.indices) {
            val box = boxes[index]
            _uiState.update {
                it.copy(
                    selectedDetectedBoxIndex = index,
                    selectedSavedItemId = box.savedItemId,
                    cropRect = box.cropRect,
                    detectedNumberForCurrentCrop = box.detectedName,
                    lastSavedId = box.savedItemId ?: it.lastSavedId,
                    statusMessage = "Selected ${box.detectedName ?: "Photo #${index + 1}"}. Adjust crop, then tap Update."
                )
            }
        }
    }

    fun nextDetectedBox() {
        val state = _uiState.value
        val boxes = state.detectedPassportBoxes
        if (boxes.isNotEmpty()) {
            val nextIndex = (state.selectedDetectedBoxIndex + 1) % boxes.size
            selectDetectedBox(nextIndex)
        }
    }

    fun prevDetectedBox() {
        val state = _uiState.value
        val boxes = state.detectedPassportBoxes
        if (boxes.isNotEmpty()) {
            val prevIndex = if (state.selectedDetectedBoxIndex <= 0) boxes.size - 1 else state.selectedDetectedBoxIndex - 1
            selectDetectedBox(prevIndex)
        }
    }

    fun scanNumberForCurrentCrop() {
        val state = _uiState.value
        val bmp = state.sourceBitmap ?: return

        viewModelScope.launch {
            val detected = PhotoNumberOcrHelper.detectNumberAroundCrop(
                source = bmp,
                cropRect = state.cropRect,
                rotationDegrees = state.imageRotation,
                isFlipped = state.isFlippedHorizontal
            ) ?: "#${101 + state.selectedDetectedBoxIndex.coerceAtLeast(0)}"

            _uiState.update {
                it.copy(detectedNumberForCurrentCrop = detected)
            }
            if (state.selectedDetectedBoxIndex in state.detectedPassportBoxes.indices) {
                val updated = state.detectedPassportBoxes.mapIndexed { idx, box ->
                    if (idx == state.selectedDetectedBoxIndex) box.copy(detectedName = detected) else box
                }
                _uiState.update { it.copy(detectedPassportBoxes = updated) }
            }
        }
    }

    fun updateSelectedRollNumber(newRoll: String) {
        val state = _uiState.value
        val updatedBoxes = state.detectedPassportBoxes.mapIndexed { idx, box ->
            if (idx == state.selectedDetectedBoxIndex) box.copy(detectedName = newRoll) else box
        }
        _uiState.update {
            it.copy(
                detectedNumberForCurrentCrop = newRoll,
                detectedPassportBoxes = updatedBoxes
            )
        }
    }

    fun extractAllSheet(context: Context) {
        autoDetectPassport(context)
    }

    fun selectDetailItem(item: SavedCropItem?) {
        _uiState.update { it.copy(selectedDetailItem = item) }
    }

    fun clearAllSavedItems() {
        _uiState.update {
            it.copy(
                savedItems = emptyList(),
                lastSavedId = null,
                selectedDetailItem = null,
                detectedPassportBoxes = emptyList(),
                selectedDetectedBoxIndex = -1,
                statusMessage = "Ribbon gallery cleared"
            )
        }
    }

    fun toggleDrawer(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
            _uiState.update {
                it.copy(
                    appSettings = settings,
                    statusMessage = "Settings updated"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
