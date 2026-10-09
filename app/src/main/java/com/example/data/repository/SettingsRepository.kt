package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.GridDisplayMode
import com.example.data.model.OutputImageFormat
import com.example.data.model.ResizeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "snapcrop_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val OUTPUT_FORMAT = stringPreferencesKey("output_format")
        val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val DEFAULT_RESIZE_OPTION = stringPreferencesKey("default_resize_option")
        val GRID_MODE = stringPreferencesKey("grid_mode")
        val SHOW_DIMENSION_BADGE = booleanPreferencesKey("show_dimension_badge")
        val ENABLE_HAPTIC = booleanPreferencesKey("enable_haptic")
        val AUTO_SCROLL_RIBBON = booleanPreferencesKey("auto_scroll_ribbon")
        val FILENAME_PREFIX = stringPreferencesKey("filename_prefix")
        val ALBUM_NAME = stringPreferencesKey("album_name")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val formatStr = preferences[PreferencesKeys.OUTPUT_FORMAT] ?: OutputImageFormat.JPEG.name
        val format = try { OutputImageFormat.valueOf(formatStr) } catch (_: Exception) { OutputImageFormat.JPEG }

        val quality = preferences[PreferencesKeys.JPEG_QUALITY] ?: 95

        val aspectStr = preferences[PreferencesKeys.DEFAULT_ASPECT_RATIO] ?: AspectRatio.FREE.name
        val aspect = try { AspectRatio.valueOf(aspectStr) } catch (_: Exception) { AspectRatio.FREE }

        val resizeStr = preferences[PreferencesKeys.DEFAULT_RESIZE_OPTION] ?: ResizeOption.ORIGINAL.name
        val resize = try { ResizeOption.valueOf(resizeStr) } catch (_: Exception) { ResizeOption.ORIGINAL }

        val gridStr = preferences[PreferencesKeys.GRID_MODE] ?: GridDisplayMode.RULE_OF_THIRDS.name
        val grid = try { GridDisplayMode.valueOf(gridStr) } catch (_: Exception) { GridDisplayMode.RULE_OF_THIRDS }

        val showBadge = preferences[PreferencesKeys.SHOW_DIMENSION_BADGE] ?: true
        val haptic = preferences[PreferencesKeys.ENABLE_HAPTIC] ?: true
        val autoScroll = preferences[PreferencesKeys.AUTO_SCROLL_RIBBON] ?: true
        val prefix = preferences[PreferencesKeys.FILENAME_PREFIX] ?: "CROP_"
        val album = preferences[PreferencesKeys.ALBUM_NAME] ?: "SnapCrop"

        AppSettings(
            outputFormat = format,
            jpegQuality = quality,
            defaultAspectRatio = aspect,
            defaultResizeOption = resize,
            showRuleOfThirdsGrid = grid,
            showDimensionBadge = showBadge,
            enableHapticFeedback = haptic,
            autoScrollRibbon = autoScroll,
            filenamePrefix = prefix,
            storageAlbumName = album
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OUTPUT_FORMAT] = settings.outputFormat.name
            preferences[PreferencesKeys.JPEG_QUALITY] = settings.jpegQuality
            preferences[PreferencesKeys.DEFAULT_ASPECT_RATIO] = settings.defaultAspectRatio.name
            preferences[PreferencesKeys.DEFAULT_RESIZE_OPTION] = settings.defaultResizeOption.name
            preferences[PreferencesKeys.GRID_MODE] = settings.showRuleOfThirdsGrid.name
            preferences[PreferencesKeys.SHOW_DIMENSION_BADGE] = settings.showDimensionBadge
            preferences[PreferencesKeys.ENABLE_HAPTIC] = settings.enableHapticFeedback
            preferences[PreferencesKeys.AUTO_SCROLL_RIBBON] = settings.autoScrollRibbon
            preferences[PreferencesKeys.FILENAME_PREFIX] = settings.filenamePrefix
            preferences[PreferencesKeys.ALBUM_NAME] = settings.storageAlbumName
        }
    }
}
