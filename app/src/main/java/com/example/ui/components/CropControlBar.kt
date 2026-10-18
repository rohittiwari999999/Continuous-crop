package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatio
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.theme.WorkspaceSurfaceVariant

@Composable
fun CropControlBar(
    hasImage: Boolean,
    activeAspectRatio: AspectRatio,
    isProcessingAutoPassport: Boolean,
    isEditing: Boolean = false,
    isAlreadySaved: Boolean = false,
    detectedBoxCount: Int = 0,
    selectedBoxIndex: Int = -1,
    currentScannedNumber: String? = null,
    onPickImageClick: () -> Unit,
    onAspectRatioSelect: (AspectRatio) -> Unit,
    onAutoPassportClick: () -> Unit,
    onCropAndSaveClick: () -> Unit,
    onRotateClick: () -> Unit,
    onFlipClick: () -> Unit,
    onResetClick: () -> Unit,
    onNextBoxClick: () -> Unit = {},
    onPrevBoxClick: () -> Unit = {},
    onScanNumberClick: () -> Unit = {},
    onExtractSheetClick: () -> Unit = {},
    onUpdateNumber: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isEditingNumber by remember { mutableStateOf(false) }
    var textFieldValue by remember(currentScannedNumber) {
        val initialText = currentScannedNumber ?: ""
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditingNumber) {
        if (isEditingNumber) {
            val text = currentScannedNumber ?: ""
            textFieldValue = TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(bottom = 14.dp, top = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, WorkspaceBorder, RoundedCornerShape(20.dp))
            .testTag("crop_control_bar"),
        color = WorkspaceSurface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Row 1: Sheet Navigation (if multiple photos) + Aspect Ratio Chips (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If sheet photos detected, show compact inline photo navigator
                if (detectedBoxCount > 1) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WorkspaceSurfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevBoxClick,
                            modifier = Modifier.size(28.dp).testTag("prev_detected_box")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Prev",
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = "${selectedBoxIndex + 1}/$detectedBoxCount",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = onNextBoxClick,
                            modifier = Modifier.size(28.dp).testTag("next_detected_box")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Detected handwriting number / file name badge (clickable to edit)
                if (hasImage && (!currentScannedNumber.isNullOrBlank() || detectedBoxCount > 0)) {
                    if (isEditingNumber) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WorkspaceSurfaceVariant)
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
                                .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                .testTag("photo_number_editing_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No: ",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val newNum = textFieldValue.text.trim()
                                        if (newNum.isNotEmpty()) {
                                            onUpdateNumber(newNum)
                                        }
                                        isEditingNumber = false
                                    }
                                ),
                                modifier = Modifier
                                    .widthIn(min = 45.dp, max = 95.dp)
                                    .focusRequester(focusRequester)
                                    .testTag("photo_number_input"),
                                singleLine = true,
                                cursorBrush = SolidColor(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    val newNum = textFieldValue.text.trim()
                                    if (newNum.isNotEmpty()) {
                                        onUpdateNumber(newNum)
                                    }
                                    isEditingNumber = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("save_number_ok_button")
                            ) {
                                Text(
                                    text = "OK",
                                    color = WorkspaceSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            IconButton(
                                onClick = { isEditingNumber = false },
                                modifier = Modifier.size(26.dp).testTag("cancel_number_edit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.16f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { isEditingNumber = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("photo_number_tag"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No: ${currentScannedNumber ?: "1"}",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Number",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Aspect Ratio Selector Chips
                AspectRatio.values().forEach { ratio ->
                    val isSelected = activeAspectRatio == ratio
                    FilterChip(
                        selected = isSelected,
                        onClick = { onAspectRatioSelect(ratio) },
                        label = {
                            Text(
                                text = ratio.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = WorkspaceSurface,
                            containerColor = WorkspaceSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = WorkspaceBorder,
                            selectedBorderColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("aspect_ratio_chip_${ratio.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Tool Icons (Left) + Auto Passport & Save/Update Actions (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Transformation & Image Picker Tools
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPickImageClick,
                        modifier = Modifier.size(42.dp).testTag("pick_image_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Open Image",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onRotateClick,
                        enabled = hasImage,
                        modifier = Modifier.size(42.dp).testTag("rotate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = if (hasImage) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onFlipClick,
                        enabled = hasImage,
                        modifier = Modifier.size(42.dp).testTag("flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip Horizontal",
                            tint = if (hasImage) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onResetClick,
                        enabled = hasImage,
                        modifier = Modifier.size(42.dp).testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Crop",
                            tint = if (hasImage) TextSecondary else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Right: Primary Actions (Auto Passport & Save/Update)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AUTO PASSPORT BUTTON
                    Button(
                        onClick = onAutoPassportClick,
                        enabled = hasImage && !isProcessingAutoPassport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentTeal,
                            contentColor = WorkspaceSurface,
                            disabledContainerColor = WorkspaceSurfaceVariant,
                            disabledContentColor = TextSecondary.copy(alpha = 0.35f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(42.dp).testTag("auto_passport_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isProcessingAutoPassport) "Detecting..." else "Auto Passport",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // SAVE / UPDATE BUTTON
                    Button(
                        onClick = onCropAndSaveClick,
                        enabled = hasImage && !isProcessingAutoPassport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = WorkspaceSurface,
                            disabledContainerColor = WorkspaceSurfaceVariant,
                            disabledContentColor = TextSecondary.copy(alpha = 0.35f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(42.dp).testTag("save_crop_button")
                    ) {
                        Icon(
                            imageVector = if (isEditing && isAlreadySaved) Icons.Default.DownloadDone else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditing && isAlreadySaved) "Update" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
