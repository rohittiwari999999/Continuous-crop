package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppSettings
import com.example.data.model.AspectRatio
import com.example.data.model.CropRect
import com.example.data.model.GridDisplayMode
import com.example.data.model.OutputImageFormat
import com.example.data.model.ResizeOption
import com.example.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SnapCrop", appName)
    }

    @Test
    fun `cropRect clamped respects normalized bounds`() {
        val invalidRect = CropRect(left = -0.2f, top = -0.5f, right = 1.4f, bottom = 1.8f)
        val clamped = invalidRect.clamped()
        assertTrue(clamped.left >= 0f)
        assertTrue(clamped.top >= 0f)
        assertTrue(clamped.right <= 1f)
        assertTrue(clamped.bottom <= 1f)
        assertTrue(clamped.width > 0f)
        assertTrue(clamped.height > 0f)
    }

    @Test
    fun `resizeOption calculates correct scaled dimensions`() {
        val (w, h) = ResizeOption.SCALE_50.calculateTargetDimensions(1000, 800)
        assertEquals(500, w)
        assertEquals(400, h)

        val (origW, origH) = ResizeOption.ORIGINAL.calculateTargetDimensions(1000, 800)
        assertEquals(1000, origW)
        assertEquals(800, origH)
    }

    @Test
    fun `settingsRepository persists and restores settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        val custom = AppSettings(
            outputFormat = OutputImageFormat.PNG,
            jpegQuality = 98,
            defaultAspectRatio = AspectRatio.RATIO_1_1,
            defaultResizeOption = ResizeOption.MAX_1080P,
            showRuleOfThirdsGrid = GridDisplayMode.DURING_DRAG,
            showDimensionBadge = false,
            enableHapticFeedback = false,
            autoScrollRibbon = false,
            filenamePrefix = "SNAP_",
            storageAlbumName = "CustomAlbum"
        )
        repo.updateSettings(custom)

        val restored = repo.settingsFlow.value
        assertEquals(OutputImageFormat.PNG, restored.outputFormat)
        assertEquals(98, restored.jpegQuality)
        assertEquals(AspectRatio.RATIO_1_1, restored.defaultAspectRatio)
        assertEquals(ResizeOption.MAX_1080P, restored.defaultResizeOption)
        assertEquals(GridDisplayMode.DURING_DRAG, restored.showRuleOfThirdsGrid)
        assertEquals(false, restored.showDimensionBadge)
        assertEquals(false, restored.enableHapticFeedback)
        assertEquals("SNAP_", restored.filenamePrefix)
        assertEquals("CustomAlbum", restored.storageAlbumName)

        // Test reset to defaults
        repo.resetToDefaults()
        val resetSettings = repo.settingsFlow.value
        assertEquals(OutputImageFormat.JPEG, resetSettings.outputFormat)
        assertEquals(92, resetSettings.jpegQuality)
        assertEquals(true, resetSettings.showDimensionBadge)
    }
}
