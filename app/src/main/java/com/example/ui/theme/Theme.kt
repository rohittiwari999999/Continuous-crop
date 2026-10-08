package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WorkstationColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = WorkspaceBackground,
    primaryContainer = WorkspaceSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = AccentTeal,
    onSecondary = WorkspaceBackground,
    secondaryContainer = WorkspaceSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    background = WorkspaceBackground,
    onBackground = TextPrimary,
    surface = WorkspaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = WorkspaceSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = WorkspaceBorder,
    error = ErrorRose,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep dark workstation theme consistent for precision photo work
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = WorkspaceBackground.toArgb()
                window.navigationBarColor = WorkspaceBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = WorkstationColorScheme,
        typography = Typography,
        content = content
    )
}
