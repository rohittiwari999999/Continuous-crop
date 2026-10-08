package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatio
import com.example.data.model.ResizeOption
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface

/**
 * Bottom Workstation Control Deck.
 * Visible only after an image is loaded onto the screen.
 * Features: Instant "Crop & Save" primary action, aspect ratio toggle, transforms, resolution selector.
 */
@Composable
fun CropControlBar(
    selectedRatio: AspectRatio,
    selectedResizeOption: ResizeOption,
    inFlightSavesCount: Int,
    onRatioSelected: (AspectRatio) -> Unit,
    onResizeSelected: (ResizeOption) -> Unit,
    onRotateClick: () -> Unit,
    onFlipClick: () -> Unit,
    onResetClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCloseImageClick: () -> Unit,
    onCropAndSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var resizeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("crop_control_bar"),
        color = WorkspaceSurface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Row 1: Aspect Ratio Quick Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RATIO:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )

                AspectRatio.entries.forEach { ratio ->
                    val isSelected = ratio == selectedRatio
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) NeonCyan else WorkspaceBackground,
                        label = "ratioBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) WorkspaceBackground else TextSecondary,
                        label = "ratioText"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonCyan else WorkspaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onRatioSelected(ratio) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("ratio_button_${ratio.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Secondary Tool Actions + Big "Crop & Save" Primary Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Utility Group: Gallery, Camera, Rotate, Flip, Reset, Resize, Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pick from Gallery
                    IconButton(
                        onClick = onPickImageClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Select photo from gallery",
                            tint = NeonCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Click from Camera
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("camera_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Capture photo from camera",
                            tint = NeonCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Rotate
                    IconButton(
                        onClick = onRotateClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("rotate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90 degrees",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Flip
                    IconButton(
                        onClick = onFlipClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip horizontally",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = onResetClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Crop Box",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Resize Options Menu
                    Box {
                        IconButton(
                            onClick = { resizeMenuExpanded = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WorkspaceBackground)
                                .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                                .testTag("resize_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "Resize Output Scale",
                                tint = if (selectedResizeOption != ResizeOption.ORIGINAL) NeonCyan else TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = resizeMenuExpanded,
                            onDismissRequest = { resizeMenuExpanded = false }
                        ) {
                            ResizeOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == selectedResizeOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == selectedResizeOption) NeonCyan else TextPrimary
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onResizeSelected(option)
                                        resizeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Change Photo / Close current image
                    IconButton(
                        onClick = onCloseImageClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceBackground)
                            .border(1.dp, WorkspaceBorder, RoundedCornerShape(8.dp))
                            .testTag("close_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Change photo",
                            tint = TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Primary Continuous Action: "CROP & SAVE"
                Button(
                    onClick = onCropAndSaveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = WorkspaceBackground
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan)
                        .testTag("crop_and_save_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = "Crop & Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        if (inFlightSavesCount > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(WorkspaceBackground)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "+$inFlightSavesCount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
