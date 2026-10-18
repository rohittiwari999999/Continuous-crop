package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Animated SnapCrop Logo with camera viewfinder crop breathing brackets,
 * neon shimmer sweep across the typography, and a tactile tap-to-snap animation.
 */
@Composable
fun AnimatedSnapCropLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    textSize: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.4.sp
    ),
    showSubtitle: Boolean = false,
    subtitleText: String = "Passport AI Studio",
    interactive: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val snapScale = remember { Animatable(1f) }

    // Infinite transitions for smooth continuous animation
    val infiniteTransition = rememberInfiniteTransition(label = "snapCropLogoInfinite")

    // 1. Shimmer gradient sweep across text
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    // 2. Viewfinder corner brackets breathing / autofocus lock effect
    val bracketPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracketPulse"
    )

    // 3. Center lens/spark glow pulsation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 4. Subtle slow angle tilt for the viewfinder reticle
    val reticleRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticleRotation"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            NeonCyan,
            Color.White,
            AccentTeal,
            NeonCyan
        ),
        start = Offset(shimmerTranslate - 200f, 0f),
        end = Offset(shimmerTranslate, 100f)
    )

    Row(
        modifier = modifier
            .scale(snapScale.value)
            .clickable(
                enabled = interactive,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                coroutineScope.launch {
                    snapScale.animateTo(0.85f, spring(dampingRatio = 0.4f, stiffness = 800f))
                    snapScale.animateTo(1.08f, spring(dampingRatio = 0.5f, stiffness = 600f))
                    snapScale.animateTo(1f, spring(dampingRatio = 0.6f))
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Animated Crop Viewfinder Icon
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    rotationZ = reticleRotation
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height

                // Draw pulsing viewfinder corner brackets
                val pad = (w * 0.12f) * (2f - bracketPulse)
                val cornerLen = w * 0.28f
                val strokeW = (w * 0.10f).coerceAtLeast(2f)

                val left = pad
                val top = pad
                val right = w - pad
                val bottom = h - pad

                val bracketColor = NeonCyan.copy(alpha = glowAlpha)

                // Top-Left corner
                drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)

                // Top-Right corner
                drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)

                // Bottom-Left corner
                drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom + cornerLen), strokeW, StrokeCap.Round)

                // Bottom-Right corner
                drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW, StrokeCap.Round)

                // Center aperture shutter/crosshair dot
                val centerRadius = (w * 0.12f) * bracketPulse
                drawCircle(
                    color = AccentTeal.copy(alpha = glowAlpha * 0.9f),
                    radius = centerRadius,
                    center = Offset(w / 2f, h / 2f)
                )

                // Outer aperture thin ring
                drawCircle(
                    color = Color.White.copy(alpha = glowAlpha * 0.7f),
                    radius = centerRadius * 0.5f,
                    center = Offset(w / 2f, h / 2f)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Animated Typography
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // "Snap" in shimmer gradient
                Text(
                    text = "Snap",
                    style = textSize.copy(
                        brush = shimmerBrush
                    )
                )
                // "Crop" in glowing AccentTeal
                Text(
                    text = "Crop",
                    style = textSize.copy(
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Pulsing camera active indicator dot
                Canvas(modifier = Modifier.size(5.dp)) {
                    drawCircle(
                        color = AccentTeal.copy(alpha = glowAlpha),
                        radius = size.width / 2f * bracketPulse
                    )
                }
            }

            if (showSubtitle) {
                Text(
                    text = subtitleText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
