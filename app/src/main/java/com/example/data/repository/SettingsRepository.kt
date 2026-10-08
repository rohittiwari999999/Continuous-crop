package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.GridDisplayMode
import com.example.data.model.OutputImageFormat
import com.example.data.model.ResizeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("continuous_crop_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        val formatName = prefs.getString("output_format", OutputImageFormat.JPEG.name) ?: OutputImageFormat.JPEG.name
        val outputFormat = try {
            OutputImageFormat.valueOf(formatName)
        } catch (_: Exception) {
            OutputImageFormat.JPEG
        }

        val quality = prefs.getInt("jpeg_quality", 92)

        val ratioName = prefs.getString("default_ratio", AspectRatio.FREE.name) ?: AspectRatio.FREE.name
        val defaultRatio = try {
            AspectRatio.valueOf(ratioName)
        } catch (_: Exception) {
            AspectRatio.FREE
        }

        val resizeName = prefs.getString("default_resize", ResizeOption.ORIGINAL.name) ?: ResizeOption.ORIGINAL.name
        val defaultResize = try {
            ResizeOption.valueOf(resizeName)
        } catch (_: Exception) {
            ResizeOption.ORIGINAL
        }

        val gridModeName = prefs.getString("grid_mode", GridDisplayMode.ALWAYS.name) ?: GridDisplayMode.ALWAYS.name
        val gridMode = try {
            GridDisplayMode.valueOf(gridModeName)
        } catch (_: Exception) {
            GridDisplayMode.ALWAYS
        }

        val showDimensionBadge = prefs.getBoolean("show_dimension_badge", true)
        val enableHaptics = prefs.getBoolean("enable_haptics", true)
        val autoScrollRibbon = prefs.getBoolean("auto_scroll_ribbon", true)
        val prefix = prefs.getString("filename_prefix", "CROP_") ?: "CROP_"
        val album = prefs.getString("storage_album", "ContinuousCrop") ?: "ContinuousCrop"

        return AppSettings(
            outputFormat = outputFormat,
            jpegQuality = quality,
            defaultAspectRatio = defaultRatio,
            defaultResizeOption = defaultResize,
            showRuleOfThirdsGrid = gridMode,
            showDimensionBadge = showDimensionBadge,
            enableHapticFeedback = enableHaptics,
            autoScrollRibbon = autoScrollRibbon,
            filenamePrefix = prefix,
            storageAlbumName = album
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString("output_format", newSettings.outputFormat.name)
            putInt("jpeg_quality", newSettings.jpegQuality)
            putString("default_ratio", newSettings.defaultAspectRatio.name)
            putString("default_resize", newSettings.defaultResizeOption.name)
            putString("grid_mode", newSettings.showRuleOfThirdsGrid.name)
            putBoolean("show_dimension_badge", newSettings.showDimensionBadge)
            putBoolean("enable_haptics", newSettings.enableHapticFeedback)
            putBoolean("auto_scroll_ribbon", newSettings.autoScrollRibbon)
            putString("filename_prefix", newSettings.filenamePrefix)
            putString("storage_album", newSettings.storageAlbumName)
            apply()
        }
        _settingsFlow.value = newSettings
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        updateSettings(defaults)
    }
}
