package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCameraBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.GridDisplayMode
import com.example.data.model.OutputImageFormat
import com.example.data.model.ResizeOption
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.theme.WorkspaceSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onResetDefaults: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = WorkspaceBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Workstation Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Continuous Crop & Processing Preferences",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Workstation",
                            tint = NeonCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Settings",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WorkspaceSurface),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: OUTPUT FORMAT & QUALITY
            SettingsSectionCard(
                title = "IMAGE OUTPUT & QUALITY",
                icon = Icons.Default.HighQuality
            ) {
                // Format Selector
                Text(
                    text = "Compression Format",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutputImageFormat.entries.forEach { format ->
                        val isSelected = format == settings.outputFormat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else WorkspaceBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else WorkspaceBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSettingsChange(settings.copy(outputFormat = format))
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) WorkspaceBackground else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // JPEG / WEBP Quality Slider
                val isLossless = settings.outputFormat == OutputImageFormat.PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLossless) "PNG Quality (Lossless 100%)" else "Compression Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLossless) TextMuted else TextPrimary
                    )
                    Text(
                        text = if (isLossless) "100%" else "${settings.jpegQuality}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isLossless) TextMuted else NeonCyan
                    )
                }

                Slider(
                    value = settings.jpegQuality.toFloat(),
                    onValueChange = { newQ ->
                        onSettingsChange(settings.copy(jpegQuality = newQ.toInt()))
                    },
                    valueRange = 50f..100f,
                    steps = 10,
                    enabled = !isLossless,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = WorkspaceSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Default Output Scale
                Text(
                    text = "Default Output Scale",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ResizeOption.entries.forEach { option ->
                        val isSelected = option == settings.defaultResizeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else WorkspaceBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else WorkspaceBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onSettingsChange(settings.copy(defaultResizeOption = option))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // SECTION 2: CROP WORKSTATION & CANVAS
            SettingsSectionCard(
                title = "WORKSTATION & CANVAS",
                icon = Icons.Default.Tune
            ) {
                // Rule of Thirds Grid Mode
                Text(
                    text = "Rule-of-Thirds Grid Display",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GridDisplayMode.entries.forEach { mode ->
                        val isSelected = mode == settings.showRuleOfThirdsGrid
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else WorkspaceBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else WorkspaceBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSettingsChange(settings.copy(showRuleOfThirdsGrid = mode))
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) WorkspaceBackground else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Show Live Pixel Dimensions Badge Toggle
                SettingSwitchRow(
                    title = "Live Dimension Badge",
                    subtitle = "Display realtime pixel width & height above crop box",
                    checked = settings.showDimensionBadge,
                    icon = Icons.Default.Straighten,
                    onCheckedChange = { onSettingsChange(settings.copy(showDimensionBadge = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Default Aspect Ratio
                Text(
                    text = "Default Initial Aspect Ratio",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AspectRatio.FREE,
                        AspectRatio.RATIO_1_1,
                        AspectRatio.RATIO_4_3,
                        AspectRatio.RATIO_16_9
                    ).forEach { ratio ->
                        val isSelected = ratio == settings.defaultAspectRatio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else WorkspaceBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else WorkspaceBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onSettingsChange(settings.copy(defaultAspectRatio = ratio))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // SECTION 3: WORKFLOW & FEEDBACK
            SettingsSectionCard(
                title = "WORKFLOW & FEEDBACK",
                icon = Icons.Default.Vibration
            ) {
                SettingSwitchRow(
                    title = "Tactile Haptic Feedback",
                    subtitle = "Vibrate softly upon pressing 'Crop & Save'",
                    checked = settings.enableHapticFeedback,
                    icon = Icons.Default.Vibration,
                    onCheckedChange = { onSettingsChange(settings.copy(enableHapticFeedback = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRow(
                    title = "Auto-Scroll Saved Ribbon",
                    subtitle = "Smoothly scroll to newly saved thumbnails in real-time",
                    checked = settings.autoScrollRibbon,
                    icon = Icons.Default.ViewCarousel,
                    onCheckedChange = { onSettingsChange(settings.copy(autoScrollRibbon = it)) }
                )
            }

            // SECTION 4: STORAGE & MEDIASTORE
            SettingsSectionCard(
                title = "STORAGE & MEDIASTORE",
                icon = Icons.Default.FolderSpecial
            ) {
                // Storage Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Scoped Storage Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreen
                            )
                            Text(
                                text = "Crops save directly to device Gallery without full disk permissions.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = settings.storageAlbumName,
                    onValueChange = {
                        onSettingsChange(settings.copy(storageAlbumName = it))
                    },
                    label = { Text("Gallery Album Folder") },
                    supportingText = { Text("Saved inside Pictures/${settings.storageAlbumName}") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = WorkspaceBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.filenamePrefix,
                    onValueChange = {
                        onSettingsChange(settings.copy(filenamePrefix = it))
                    },
                    label = { Text("Filename Prefix") },
                    supportingText = { Text("e.g. ${settings.filenamePrefix}20261007_120000.jpg") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = WorkspaceBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // SECTION 5: APP INFORMATION
            SettingsSectionCard(
                title = "APP & SYSTEM INFORMATION",
                icon = Icons.Default.Info
            ) {
                InfoItemRow(label = "Application", value = "CropFlow Pro Studio")
                InfoItemRow(label = "Version", value = "1.0.0 (Production)")
                InfoItemRow(label = "Architecture", value = "Jetpack Compose M3 + Kotlin Coroutines")
                InfoItemRow(label = "Processing Engine", value = "Hardware Bitmap Matrix & Region Decoder")
                InfoItemRow(label = "Workflow Policy", value = "Zero-Interrupt Continuous State Retention")

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ErrorRose.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset All Settings to Defaults",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = "Reset Settings?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "All preferences will return to default values (JPEG 92%, Free aspect ratio, vibration enabled).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = WorkspaceBackground)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = WorkspaceSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkspaceSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(WorkspaceBorder.copy(alpha = 0.6f))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = NeonCyan
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WorkspaceBackground,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = WorkspaceSurfaceVariant
            )
        )
    }
}

@Composable
private fun InfoItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}
