package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Top-left sliding Navigation Drawer for CropFlow.
 * Provides quick access to all key app features, tools, gallery actions, and settings.
 */
@Composable
fun AppNavigationDrawerContent(
    savedItemsCount: Int,
    onNavigateWorkstation: () -> Unit,
    onPickGallery: () -> Unit,
    onLaunchCamera: () -> Unit,
    onLoadDemoImage: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearAllCrops: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight()
            .testTag("app_navigation_drawer"),
        drawerContainerColor = WorkspaceSurface,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            // Header Section
            DrawerHeader(savedItemsCount = savedItemsCount)

            HorizontalDivider(
                color = WorkspaceBorder.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Primary Navigation Items
            DrawerMenuItem(
                title = "Workstation",
                subtitle = "Active crop canvas & tools",
                icon = Icons.Default.Crop,
                iconTint = NeonCyan,
                onClick = onNavigateWorkstation
            )

            DrawerMenuItem(
                title = "Select from Gallery",
                subtitle = "Choose photo to crop",
                icon = Icons.Default.AddPhotoAlternate,
                iconTint = NeonCyan,
                onClick = onPickGallery
            )

            DrawerMenuItem(
                title = "Capture from Camera",
                subtitle = "Take a new photo directly",
                icon = Icons.Default.PhotoCamera,
                iconTint = NeonCyan,
                onClick = onLaunchCamera
            )

            DrawerMenuItem(
                title = "Load Sample Graphic",
                subtitle = "Try multi-asset design board",
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFFF59E0B),
                onClick = onLoadDemoImage
            )

            HorizontalDivider(
                color = WorkspaceBorder.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            DrawerMenuItem(
                title = "Settings",
                subtitle = "Format, quality, grid & storage",
                icon = Icons.Default.Settings,
                iconTint = TextPrimary,
                onClick = onOpenSettings
            )

            DrawerMenuItem(
                title = "Share CropFlow App",
                subtitle = "Recommend to colleagues & friends",
                icon = Icons.Default.Share,
                iconTint = TextSecondary,
                onClick = {
                    shareApp(context)
                }
            )

            if (savedItemsCount > 0) {
                DrawerMenuItem(
                    title = "Clear Saved Ribbon",
                    subtitle = "Remove $savedItemsCount items from ribbon",
                    icon = Icons.Default.CleaningServices,
                    iconTint = ErrorRose,
                    onClick = onClearAllCrops
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Section
            DrawerFooter()
        }
    }
}

@Composable
private fun DrawerHeader(savedItemsCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(12.dp))
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "CropFlow",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Continuous Workstation",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Badge Status Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (savedItemsCount > 0) SuccessGreen.copy(alpha = 0.15f)
                    else WorkspaceSurfaceVariant
                )
                .border(
                    1.dp,
                    if (savedItemsCount > 0) SuccessGreen.copy(alpha = 0.4f)
                    else WorkspaceBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (savedItemsCount > 0) SuccessGreen else TextMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (savedItemsCount > 0) "$savedItemsCount crops saved in Gallery" else "Ready to extract crops",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (savedItemsCount > 0) SuccessGreen else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WorkspaceBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun DrawerFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% Offline & Private • Scoped Storage",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "CropFlow v1.0.0 Pro Edition",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextMuted
        )
    }
}

private fun shareApp(context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out CropFlow — High-speed continuous image cropping workstation for Android!"
        )
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share CropFlow"))
}
