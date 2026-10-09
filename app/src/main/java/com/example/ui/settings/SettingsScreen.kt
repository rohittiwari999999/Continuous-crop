package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.GridDisplayMode
import com.example.data.model.OutputImageFormat
import com.example.data.model.ResizeOption
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.theme.WorkspaceBorder
import com.example.ui.theme.WorkspaceSurface
import com.example.ui.theme.WorkspaceSurfaceVariant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onBackClick: () -> Unit
) {
    var format by remember { mutableStateOf(currentSettings.outputFormat) }
    var quality by remember { mutableFloatStateOf(currentSettings.jpegQuality.toFloat()) }
    var resizeOption by remember { mutableStateOf(currentSettings.defaultResizeOption) }
    var gridMode by remember { mutableStateOf(currentSettings.showRuleOfThirdsGrid) }
    var showDimensionBadge by remember { mutableStateOf(currentSettings.showDimensionBadge) }
    var autoScrollRibbon by remember { mutableStateOf(currentSettings.autoScrollRibbon) }
    var albumName by remember { mutableStateOf(currentSettings.storageAlbumName) }

    fun emitUpdate() {
        onSaveSettings(
            currentSettings.copy(
                outputFormat = format,
                jpegQuality = quality.roundToInt(),
                defaultResizeOption = resizeOption,
                showRuleOfThirdsGrid = gridMode,
                showDimensionBadge = showDimensionBadge,
                autoScrollRibbon = autoScrollRibbon,
                storageAlbumName = albumName
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workstation Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WorkspaceSurface
                )
            )
        },
        containerColor = WorkspaceBackground,
        modifier = Modifier.testTag("settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Output Format & Quality
            SettingsSectionCard(title = "EXPORT FORMAT & QUALITY") {
                Text(
                    text = "File Format",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutputImageFormat.values().forEach { fmt ->
                        val isSelected = format == fmt
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                format = fmt
                                emitUpdate()
                            },
                            label = { Text(fmt.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = WorkspaceSurface,
                                containerColor = WorkspaceSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                if (format == OutputImageFormat.JPEG || format == OutputImageFormat.WEBP) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Compression Quality", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "${quality.roundToInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = quality,
                        onValueChange = {
                            quality = it
                            emitUpdate()
                        },
                        valueRange = 60f..100f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = WorkspaceSurfaceVariant
                        )
                    )
                }
            }

            // Section 2: Default Resize Option
            SettingsSectionCard(title = "DEFAULT RESIZE PRESET") {
                ResizeOption.values().forEach { opt ->
                    val isSelected = resizeOption == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WorkspaceSurfaceVariant else WorkspaceSurface)
                            .clickable {
                                resizeOption = opt
                                emitUpdate()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = opt.label,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = opt.description,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Section 3: Storage & Album Name
            SettingsSectionCard(title = "STORAGE ALBUM") {
                OutlinedTextField(
                    value = albumName,
                    onValueChange = {
                        albumName = it
                        emitUpdate()
                    },
                    label = { Text("Gallery Album Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = WorkspaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("album_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saved photos are stored in: Pictures/$albumName",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Section 4: Display & Feedback
            SettingsSectionCard(title = "WORKSTATION DISPLAY") {
                SettingSwitchRow(
                    label = "Show Dimension Badge",
                    subtitle = "Displays pixel width × height over crop box",
                    checked = showDimensionBadge,
                    onCheckedChange = {
                        showDimensionBadge = it
                        emitUpdate()
                    }
                )

                SettingSwitchRow(
                    label = "Auto-Scroll Ribbon",
                    subtitle = "Scrolls to latest saved photo in top bar",
                    checked = autoScrollRibbon,
                    onCheckedChange = {
                        autoScrollRibbon = it
                        emitUpdate()
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkspaceSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WorkspaceSurface,
                checkedTrackColor = NeonCyan,
                uncheckedTrackColor = WorkspaceSurfaceVariant
            )
        )
    }
}
