package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.CropControlBar
import com.example.ui.components.CropDetailDialog
import com.example.ui.components.CropOverlayCanvas
import com.example.ui.components.SavedRibbonBar
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.WorkspaceBackground
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

    // Visual media picker (zero permission Android photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
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
    BackHandler(enabled = drawerState.isOpen || uiState.isSettingsOpen || uiState.selectedDetailItem != null) {
        when {
            uiState.selectedDetailItem != null -> viewModel.selectDetailItem(null)
            uiState.isSettingsOpen -> viewModel.toggleSettings(false)
            drawerState.isOpen -> scope.launch { drawerState.close() }
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
                    autoScroll = uiState.appSettings.autoScrollRibbon,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onItemClick = { item -> viewModel.onRibbonItemClick(item) },
                    onClearAllClick = { viewModel.clearAllSavedItems() },
                    onSettingsClick = { viewModel.toggleSettings(true) }
                )
            },
            bottomBar = {
                CropControlBar(
                    hasImage = uiState.hasImage,
                    activeAspectRatio = uiState.activeAspectRatio,
                    isProcessingAutoPassport = uiState.isProcessingAutoPassport,
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
                        viewModel.autoDetectPassport(context)
                    },
                    onCropAndSaveClick = {
                        if (uiState.detectedPassportBoxes.isNotEmpty() && uiState.selectedDetectedBoxIndex in uiState.detectedPassportBoxes.indices) {
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
                    onExtractSheetClick = { viewModel.extractAllSheet(context) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WorkspaceBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                    onSelectDetectedBox = { viewModel.selectDetectedBox(it) },
                    onCropRectChange = { viewModel.updateCropRect(it) }
                )
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
}
