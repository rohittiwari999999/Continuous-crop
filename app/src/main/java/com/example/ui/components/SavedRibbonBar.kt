package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedCropItem
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.theme.WorkspaceSurfaceVariant

@Composable
fun SavedRibbonBar(
    savedItems: List<SavedCropItem>,
    inFlightCount: Int,
    isProcessingAutoPassport: Boolean = false,
    autoPassportProgress: Float = 0f,
    lastSavedId: String?,
    selectedSavedItemId: String? = null,
    autoScroll: Boolean = true,
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit,
    onItemClick: (SavedCropItem) -> Unit,
    onClearAllClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(savedItems.size) {
        if (autoScroll && savedItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
            .padding(top = 10.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, WorkspaceBorder, RoundedCornerShape(16.dp))
            .testTag("saved_items_ribbon"),
        color = WorkspaceSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row with Back, Menu, stats & action icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(42.dp).testTag("top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(42.dp).testTag("menu_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Drawer",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "SnapCrop",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = if (savedItems.isNotEmpty()) NeonCyan else WorkspaceBorder,
                                contentColor = WorkspaceSurface
                            ) {
                                Text(
                                    text = "${savedItems.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Saved Count",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // If editing a photo, show Editing Pill
                    if (!selectedSavedItemId.isNullOrBlank()) {
                        val activeItem = savedItems.firstOrNull { it.id == selectedSavedItemId }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Editing: ${activeItem?.detectedName ?: "Photo"}",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (inFlightCount > 0 || isProcessingAutoPassport) {
                        Spacer(modifier = Modifier.width(10.dp))
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (savedItems.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAllClick,
                            modifier = Modifier.size(42.dp).testTag("clear_all_ribbon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Saved",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(42.dp).testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Processing progress bar
            if (isProcessingAutoPassport) {
                LinearProgressIndicator(
                    progress = { autoPassportProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentTeal,
                    trackColor = WorkspaceSurfaceVariant
                )
            }

            // Thumbnail Gallery Ribbon
            if (savedItems.isEmpty()) {
                RibbonEmptyState()
            } else {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                ) {
                    items(
                        items = savedItems,
                        key = { it.id }
                    ) { item ->
                        val isSelected = item.id == selectedSavedItemId
                        SavedCropThumbnailCard(
                            item = item,
                            isSelected = isSelected,
                            isLatest = item.id == lastSavedId,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCropThumbnailCard(
    item: SavedCropItem,
    isSelected: Boolean,
    isLatest: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> NeonCyan
        isLatest -> AccentTeal
        else -> WorkspaceBorder
    }
    val borderWidth = if (isSelected) 2.5.dp else if (isLatest) 1.5.dp else 1.dp

    Box(
        modifier = Modifier
            .width(82.dp)
            .height(98.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WorkspaceSurfaceVariant)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("saved_crop_card_${item.id.take(4)}")
    ) {
        Image(
            bitmap = item.thumbnailBitmap.asImageBitmap(),
            contentDescription = "Saved Crop ${item.detectedName ?: item.id}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(68.dp)
        )

        // Detected Roll Number / Name Badge on top-left of photo
        if (!item.detectedName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan.copy(alpha = 0.92f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = item.detectedName,
                    color = WorkspaceSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom metadata pill (dimensions)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(26.dp)
                .background(WorkspaceSurface)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.detectedName ?: item.formattedDimensions,
                color = if (!item.detectedName.isNullOrBlank()) NeonCyan else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Latest indicator dot
        if (isLatest) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            )
        }
    }
}

@Composable
private fun RibbonEmptyState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Saved photos appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "Click 'Auto Passport' to detect and extract all photos instantly",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
