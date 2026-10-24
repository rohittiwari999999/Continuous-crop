package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.CropControlBar
import com.example.ui.components.CropDetailDialog
import com.example.ui.components.CropOverlayCanvas
import com.example.ui.components.PassportModeSelectionDialog
import com.example.ui.components.SavedRibbonBar
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.viewmodel.ContinuousCropViewModel
import com.example.util.ShareHelper
import kotlinx.coroutines.launch

@Composable
fun ContinuousCropScreen(
    viewModel: ContinuousCropViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showPassportModeDialog by remember { mutableStateOf(false) }

    // Visual media picker (zero permission Android photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            showPassportModeDialog = false
            viewModel.loadFromUri(context, uri)
        }
    }

    // Handle status messages
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Back handling
    val isEditing = uiState.isEditingExistingCrop
    BackHandler(enabled = drawerState.isOpen || uiState.isSettingsOpen || uiState.selectedDetailItem != null || uiState.canUndo || isEditing || uiState.detectedPassportBoxes.isNotEmpty()) {
        when {
            uiState.selectedDetailItem != null -> viewModel.selectDetailItem(null)
            uiState.isSettingsOpen -> viewModel.toggleSettings(false)
            drawerState.isOpen -> scope.launch { drawerState.close() }
            uiState.canUndo -> viewModel.undo()
            uiState.detectedPassportBoxes.isNotEmpty() -> viewModel.clearDetectedBoxes()
            isEditing -> viewModel.deselectActiveEdit()
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsScreen(
            currentSettings = uiState.appSettings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onBackClick = { viewModel.toggleSettings(false) }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                onLoadSampleClick = { viewModel.loadSampleImage() },
                onClearRibbonClick = { viewModel.clearAllSavedItems() },
                onSettingsClick = { viewModel.toggleSettings(true) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        },
        modifier = modifier.fillMaxSize().testTag("main_crop_screen")
    ) {
        Scaffold(
            topBar = {
                SavedRibbonBar(
                    savedItems = uiState.savedItems,
                    inFlightCount = uiState.inFlightCount,
                    isProcessingAutoPassport = uiState.isProcessingAutoPassport,
                    autoPassportProgress = uiState.autoPassportProgress,
                    lastSavedId = uiState.lastSavedId,
                    selectedSavedItemId = uiState.selectedSavedItemId,
                    autoScroll = uiState.appSettings.autoScrollRibbon,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onBackClick = {
                        when {
                            uiState.selectedDetailItem != null -> viewModel.selectDetailItem(null)
                            uiState.isSettingsOpen -> viewModel.toggleSettings(false)
                            drawerState.isOpen -> scope.launch { drawerState.close() }
                            uiState.canUndo -> viewModel.undo()
                            uiState.detectedPassportBoxes.isNotEmpty() -> viewModel.clearDetectedBoxes()
                            isEditing -> viewModel.deselectActiveEdit()
                            else -> viewModel.resetCrop()
                        }
                    },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onItemClick = { item -> viewModel.onRibbonItemClick(item) },
                    onClearAllClick = { viewModel.clearAllSavedItems() },
                    onSettingsClick = { viewModel.toggleSettings(true) },
                    onDeselectEdit = { viewModel.deselectActiveEdit() }
                )
            },
            bottomBar = {
                CropControlBar(
                    hasImage = uiState.hasImage,
                    activeAspectRatio = uiState.activeAspectRatio,
                    isProcessingAutoPassport = uiState.isProcessingAutoPassport,
                    isEditing = isEditing,
                    isAlreadySaved = uiState.isCurrentCropAlreadySaved,
                    detectedBoxCount = uiState.detectedPassportBoxes.size,
                    selectedBoxIndex = uiState.selectedDetectedBoxIndex,
                    currentScannedNumber = uiState.detectedNumberForCurrentCrop,
                    onPickImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAspectRatioSelect = { viewModel.setAspectRatio(it) },
                    onAutoPassportClick = {
                        showPassportModeDialog = true
                    },
                    onCropAndSaveClick = {
                        if (isEditing) {
                            viewModel.saveOrUpdateSelectedCrop(context)
                        } else {
                            viewModel.cropAndSave(context)
                        }
                    },
                    onRotateClick = { viewModel.rotateClockwise() },
                    onFlipClick = { viewModel.flipHorizontal() },
                    onResetClick = { viewModel.resetCrop() },
                    onNextBoxClick = { viewModel.nextDetectedBox() },
                    onPrevBoxClick = { viewModel.prevDetectedBox() },
                    onScanNumberClick = { viewModel.scanNumberForCurrentCrop() },
                    onExtractSheetClick = { viewModel.extractAllSheet(context) },
                    onUpdateNumber = { viewModel.updateSelectedRollNumber(it) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WorkspaceBackground,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val isSampleImage = uiState.sourceUri == null
                CropOverlayCanvas(
                    bitmap = uiState.sourceBitmap,
                    cropRect = uiState.cropRect,
                    rotation = uiState.imageRotation,
                    isFlipped = uiState.isFlippedHorizontal,
                    aspectRatio = uiState.activeAspectRatio,
                    gridMode = uiState.appSettings.showRuleOfThirdsGrid,
                    showDimensionBadge = uiState.appSettings.showDimensionBadge,
                    pixelDimensions = uiState.currentCropPixelDimensions,
                    detectedPassportBoxes = uiState.detectedPassportBoxes,
                    selectedBoxIndex = uiState.selectedDetectedBoxIndex,
                    currentScannedNumber = uiState.detectedNumberForCurrentCrop,
                    isSampleImage = isSampleImage,
                    onScreenClickToPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectDetectedBox = { viewModel.selectDetectedBox(it) },
                    onCropRectChange = { viewModel.updateCropRect(it) },
                    onCropGestureStart = { viewModel.onCropGestureStart() }
                )

                // If initial sample student ID sheet is showing, clicking screen or banner opens gallery
                if (isSampleImage) {
                    Surface(
                        color = WorkspaceSurface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, NeonCyan),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("tap_screen_open_gallery_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tap screen to select photo from Gallery",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail dialog when tapping any thumbnail in the ribbon
    uiState.selectedDetailItem?.let { detailItem ->
        CropDetailDialog(
            item = detailItem,
            onDismiss = { viewModel.selectDetailItem(null) },
            onShareClick = { item ->
                item.uri?.let { uri ->
                    ShareHelper.shareImage(context, uri, "Share Passport Photo (${item.detectedName ?: "Crop"})")
                } ?: Toast.makeText(context, "Image file not accessible", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Passport Mode Selection Dialog (Photo Only vs With Signature)
    if (showPassportModeDialog) {
        PassportModeSelectionDialog(
            onDismiss = { showPassportModeDialog = false },
            onSelectMode = { mode ->
                viewModel.autoDetectPassport(context, mode)
            }
        )
    }
}
