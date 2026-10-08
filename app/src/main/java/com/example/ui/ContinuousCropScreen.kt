package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.CropControlBar
import com.example.ui.components.CropDetailDialog
import com.example.ui.components.CropOverlayCanvas
import com.example.ui.components.SavedRibbonBar
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.theme.WorkspaceSurfaceVariant
import com.example.ui.viewmodel.ContinuousCropViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ContinuousCropScreen(
    viewModel: ContinuousCropViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // If Settings Screen is open, show Settings UI
    if (uiState.isSettingsOpen) {
        SettingsScreen(
            settings = uiState.settings,
            onSettingsChange = { viewModel.updateSettings(it) },
            onResetDefaults = { viewModel.resetSettingsToDefault() },
            onNavigateBack = { viewModel.closeSettings() },
            modifier = modifier
        )
        return
    }

    // Android Standard Photo Picker (Zero-permission MediaStore contract)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFromUri(context, uri)
        }
    }

    // Camera Capture with FileProvider
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            viewModel.loadFromUri(context, uri)
        }
    }

    // Fallback direct preview capture if needed
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.loadDirectBitmap(bitmap)
        }
    }

    fun launchCamera() {
        try {
            val photosDir = File(context.cacheDir, "camera_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val tempFile = File(photosDir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            takePicturePreviewLauncher.launch(null)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    // Left Navigation Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = WorkspaceSurface,
                drawerContentColor = TextPrimary
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WorkspaceSurfaceVariant)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WorkspaceSurface)
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "SnapCrop",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Continuous Crop Workstation",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }

                HorizontalDivider(color = WorkspaceBorder, thickness = 1.dp)

                // Drawer Navigation Items
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Item: Settings
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openSettings()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("drawer_menu_settings")
                    )

                    // Item: Gallery
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Text(text = "Open Gallery", color = TextPrimary)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("drawer_menu_gallery")
                    )

                    // Item: Camera
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Text(text = "Take Photo", color = TextPrimary)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            launchCamera()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("drawer_menu_camera")
                    )

                    // Item: Demo Image
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Text(text = "Try Demo Image", color = TextPrimary)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.loadSampleImage()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("drawer_menu_sample")
                    )

                    if (uiState.sourceBitmap != null) {
                        // Item: Close current image
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            },
                            label = {
                                Text(text = "Close Current Photo", color = TextPrimary)
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.clearImage()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = WorkspaceBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Item: Storage Info
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    text = "Saved Folder",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pictures/${uiState.settings.storageAlbumName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                snackbarHostState.showSnackbar("All crops save to Pictures/${uiState.settings.storageAlbumName}")
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    // Item: How to Use
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Text(text = "How to Use", color = TextPrimary)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showHelpDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    // Item: About
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        label = {
                            Text(text = "About SnapCrop", color = TextPrimary)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = WorkspaceBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // Top Sticky "Saved Items" Ribbon (Mini-Map) with hamburger menu
                Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    SavedRibbonBar(
                        savedItems = uiState.savedItems,
                        inFlightCount = uiState.inFlightSavesCount,
                        lastSavedId = uiState.lastSavedItemId,
                        autoScroll = uiState.settings.autoScrollRibbon,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onItemClick = { item -> viewModel.openItemDetail(item) },
                        onClearAllClick = { viewModel.clearAllSavedCrops() },
                        onSettingsClick = { viewModel.openSettings() }
                    )
                }
            },
            bottomBar = {
                // ONLY show the workstation bottom controls if an image is loaded onto the screen!
                // "jab foto select ho jaye aur photo aa jaye screen par tab neeche wale jo sare button hai wo aaye"
                if (uiState.sourceBitmap != null) {
                    CropControlBar(
                        selectedRatio = uiState.selectedAspectRatio,
                        selectedResizeOption = uiState.selectedResizeOption,
                        inFlightSavesCount = uiState.inFlightSavesCount,
                        onRatioSelected = { viewModel.setAspectRatio(it) },
                        onResizeSelected = { viewModel.setResizeOption(it) },
                        onRotateClick = { viewModel.rotate90() },
                        onFlipClick = { viewModel.flipHorizontal() },
                        onResetClick = { viewModel.resetCropRect() },
                        onPickImageClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCameraClick = { launchCamera() },
                        onCloseImageClick = { viewModel.clearImage() },
                        onCropAndSaveClick = { viewModel.cropAndSave(context) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.sourceBitmap == null) {
                    // INITIAL SCREEN: strictly Gallery & Camera buttons side-by-side in one row at the bottom
                    InitialImageSelectionScreen(
                        onGalleryClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCameraClick = { launchCamera() },
                        onSampleClick = { viewModel.loadSampleImage() }
                    )
                } else {
                    // WORKSTATION CANVAS (Smooth 120 FPS dragging & live bounding box)
                    CropOverlayCanvas(
                        bitmap = uiState.sourceBitmap,
                        cropRect = uiState.cropRect,
                        aspectRatio = uiState.selectedAspectRatio,
                        rotationDegrees = uiState.rotationDegrees,
                        isFlippedHorizontally = uiState.isFlippedHorizontally,
                        gridMode = uiState.settings.showRuleOfThirdsGrid,
                        onCropRectChange = { viewModel.updateCropRect(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live Pixel Dimension & Ratio Indicator Floating Pill
                    val sourceBitmap = uiState.sourceBitmap
                    if (uiState.settings.showDimensionBadge && sourceBitmap != null) {
                        val isSwapped = uiState.rotationDegrees % 180 != 0
                        val activeW = if (isSwapped) sourceBitmap.height else sourceBitmap.width
                        val activeH = if (isSwapped) sourceBitmap.width else sourceBitmap.height
                        val pixelW = (uiState.cropRect.width * activeW).toInt()
                        val pixelH = (uiState.cropRect.height * activeH).toInt()

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, WorkspaceBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("crop_dimensions_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$pixelW × $pixelH px",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${uiState.selectedAspectRatio.label})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                // Image loading spinner
                AnimatedVisibility(
                    visible = uiState.isLoadingImage,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            }
        }
    }

    // Detail Dialog for saved items
    val selectedItem = uiState.selectedItemForDetail
    if (selectedItem != null) {
        CropDetailDialog(
            item = selectedItem,
            onDismiss = { viewModel.closeItemDetail() },
            onDelete = { viewModel.deleteSavedCrop(it) }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "About SnapCrop", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SnapCrop v1.0",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fast-workflow continuous image cropping workstation. Designed for rapid multi-region extraction with non-blocking background saves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Storage: Pictures/${uiState.settings.storageAlbumName}\n• Output: MediaStore Scoped Storage\n• Play Console: Target SDK 36 Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = "Close", color = NeonCyan)
                }
            },
            containerColor = WorkspaceSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    // How to Use Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Continuous Crop Guide", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Pick an image from Gallery or capture using Camera.\n2. Drag the handles or box to define the crop area.\n3. Tap 'Crop & Save' — it saves in the background without closing the screen!\n4. Drag to a new area and tap 'Crop & Save' again immediately.\n5. View saved crops anytime in the top ribbon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(text = "Got It", color = NeonCyan)
                }
            },
            containerColor = WorkspaceSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

/**
 * Initial view shown before an image is selected.
 * Strict user requirement:
 * Two buttons in one row side-by-side: strictly labeled "Gallery" and "Camera".
 * When photo is selected, all workstation buttons appear.
 */
@Composable
private fun InitialImageSelectionScreen(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSampleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Center Hero Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(WorkspaceSurface)
                    .border(2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = NeonCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SnapCrop Workstation",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Rapid continuous image cropping with non-blocking background gallery saves",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Demo Image pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(WorkspaceSurface.copy(alpha = 0.8f))
                    .border(1.dp, WorkspaceBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onSampleClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("try_sample_board_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Demo Image",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
            }
        }

        // Bottom Row: strictly "Gallery" and "Camera" buttons side-by-side in ONE ROW
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BUTTON 1: Gallery
                Button(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = WorkspaceBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("select_from_gallery_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gallery",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // BUTTON 2: Camera
                Button(
                    onClick = onCameraClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkspaceSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, WorkspaceBorder, RoundedCornerShape(14.dp))
                        .testTag("capture_from_camera_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Camera",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
