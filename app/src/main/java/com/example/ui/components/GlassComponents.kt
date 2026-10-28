package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.LiquidGlassTheme

/**
 * Liquid Glass dynamic mesh background with glowing specular ambient orbs.
 */
@Composable
fun LiquidGlassBackground(
    theme: LiquidGlassTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val topColor by animateColorAsState(Color(theme.topColor), label = "top_bg")
    val midColor by animateColorAsState(Color(theme.midColor), label = "mid_bg")
    val bottomColor by animateColorAsState(Color(theme.bottomColor), label = "bot_bg")
    val orb1 by animateColorAsState(Color(theme.orbColor1), label = "orb1")
    val orb2 by animateColorAsState(Color(theme.orbColor2), label = "orb2")

    Box(modifier = modifier.fillMaxSize()) {
        // Ambient background gradient + luminous glowing liquid orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base vertical atmospheric gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, midColor, bottomColor)
                )
            )

            // Luminous Liquid Orb 1 (Top-right accent glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1, orb1.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.18f),
                    radius = w * 0.7f
                )
            )

            // Luminous Liquid Orb 2 (Center-left secondary glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2, orb2.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.55f),
                    radius = w * 0.65f
                )
            )

            // Luminous Liquid Orb 3 (Bottom-right tertiary ambient glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(w * 0.7f, h * 0.85f),
                    radius = w * 0.55f
                )
            )
        }

        // Foreground content on top of glowing liquid mesh
        content()
    }
}

/**
 * iOS Liquid Glass container with translucent frosted blur effect,
 * soft inner reflection, and specular gradient rim.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0xFF131C31).copy(alpha = 0.62f),
    borderColor: Color = Color.White,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor.copy(alpha = 0.38f),
            borderColor.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, brush = specularBrush, shape = shape),
        content = content
    )
}

/**
 * Liquid Glass Pill/Badge with sleek specular shine
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.40f),
            accentColor.copy(alpha = 0.10f)
        )
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(accentColor.copy(alpha = 0.18f))
            .border(1.dp, brush, shape),
        content = content
    )
}

/**
 * Interactive Liquid Glass Button with iOS haptic-like ripple and active sheen
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.10f),
    activeAccent: Color? = null,
    isActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = if (isActive && activeAccent != null) {
        activeAccent.copy(alpha = 0.35f)
    } else {
        backgroundColor
    }

    val borderBrush = if (isActive && activeAccent != null) {
        Brush.verticalGradient(
            listOf(
                activeAccent.copy(alpha = 0.8f),
                activeAccent.copy(alpha = 0.3f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderBrush, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = activeAccent ?: Color.White),
                onClick = onClick
            ),
        content = content
    )
}
