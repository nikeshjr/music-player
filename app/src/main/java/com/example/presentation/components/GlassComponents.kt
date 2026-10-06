package com.example.presentation.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.motion.AuraMotion
import com.example.core.theme.AuraTheme
import com.example.presentation.components.liquidglass.GlassEffectConfig
import com.example.presentation.components.liquidglass.LocalGlassEffectConfig
import com.example.presentation.components.liquidglass.liquidGlass

/**
 * Modifier.liquidGlassmorphic: Echo Music Liquid Glass with AGSL Refraction,
 * Chromatic Dispersion, Specular Rim Lighting and adaptive fallback.
 */
@Composable
fun Modifier.liquidGlassmorphic(
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    tint: Color = AuraTheme.current.surfaceColor,
    tintAlpha: Float = AuraTheme.current.glassTintOpacity,
    borderBrightness: Float = AuraTheme.current.glassBorderBrightness,
    elevation: Dp = 8.dp,
    blurRadiusDp: Float = 10f,
    lensAmount: Float = 0.5f,
    chromaticAberration: Boolean = true
): Modifier {
    val config = LocalGlassEffectConfig.current.copy(
        globalEnabled = true,
        vibrancy = 1.3f,
        blurRadius = blurRadiusDp,
        lensHeight = 0.5f,
        lensAmount = lensAmount,
        chromaticAberration = chromaticAberration,
        depthEffect = true,
        surfaceTintColor = tint,
        surfaceOpacity = tintAlpha
    )

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.5f)
        )
        .liquidGlass(
            config = config,
            shape = shape,
            applyEdgeEffects = false,
            blurRadiusDp = blurRadiusDp
        )
        .clip(shape)
        .background(tint.copy(alpha = tintAlpha))
        .border(
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderBrightness),
                        Color.White.copy(alpha = borderBrightness * 0.15f),
                        Color.Transparent
                    )
                )
            ),
            shape = shape
        )
}

/**
 * Modifier.glassmorphic: Core glassmorphism modifier that applies specular linear gradient borders,
 * frosted background tinting, inner highlight, and elevation shadow.
 */
@Composable
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = AuraTheme.current.surfaceColor,
    tintAlpha: Float = AuraTheme.current.glassTintOpacity,
    borderBrightness: Float = AuraTheme.current.glassBorderBrightness,
    elevation: Dp = 8.dp
): Modifier {
    val isLight = AuraTheme.current.isLight
    val borderBrush = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.09f),
                Color.Black.copy(alpha = 0.03f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderBrightness),
                Color.White.copy(alpha = borderBrightness * 0.15f),
                Color.Transparent
            )
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.35f),
            spotColor = if (isLight) Color.Black.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.5f)
        )
        .clip(shape)
        .background(tint.copy(alpha = tintAlpha))
        .border(
            border = BorderStroke(width = 1.dp, brush = borderBrush),
            shape = shape
        )
}

/**
 * BackdropGlassLayer: Renders an actual blurred backdrop layer on API 31+ using RenderEffect.
 * Falls back to a high-density tinted frosted layer on API 26-30.
 */
@Composable
fun BackdropGlassLayer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AuraTheme.current.globalCornerRadius),
    blurRadius: Dp = AuraTheme.current.glassBlurRadius,
    reduceEffects: Boolean = AuraTheme.current.reduceMotion,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }

    Box(
        modifier = modifier
            .clip(shape)
            .graphicsLayer {
                if (!reduceEffects && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0f) {
                    renderEffect = RenderEffect
                        .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
    ) {
        content()
    }
}

/**
 * GlassSurface: Container surface implementing backdrop glass styling.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AuraTheme.current.globalCornerRadius),
    tint: Color = AuraTheme.current.surfaceColor,
    tintAlpha: Float = AuraTheme.current.glassTintOpacity,
    elevation: Dp = AuraTheme.current.glassShadowElevation,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassmorphic(
            shape = shape,
            tint = tint,
            tintAlpha = tintAlpha,
            borderBrightness = AuraTheme.current.glassBorderBrightness,
            elevation = elevation
        )
    ) {
        content()
    }
}

/**
 * GlassCard: Clickable glass card with responsive elevation, touch feedback, and crisp glassmorphic style.
 */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AuraTheme.current.globalCornerRadius),
    tint: Color = AuraTheme.current.surfaceColor,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassmorphic(
                shape = shape,
                tint = tint,
                tintAlpha = AuraTheme.current.glassTintOpacity,
                borderBrightness = AuraTheme.current.glassBorderBrightness,
                elevation = 4.dp
            )
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        content()
    }
}

/**
 * GlassButton: Premium glassmorphic button with customizable accent colors.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color = AuraTheme.current.primaryColor,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassmorphic(
                shape = shape,
                tint = accentColor,
                tintAlpha = 0.28f,
                borderBrightness = 0.4f,
                elevation = 4.dp
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * GlassChip: Filter / Category chip with glass styling.
 */
@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AuraTheme.current
    val tint = if (isSelected) theme.primaryColor else theme.surfaceColor
    val textCol = if (isSelected) Color.White else theme.textColorPrimary
    val alpha = if (isSelected) 0.90f else (if (theme.isLight) 0.85f else 0.25f)

    Box(
        modifier = modifier
            .glassmorphic(
                shape = RoundedCornerShape(14.dp),
                tint = tint,
                tintAlpha = alpha,
                borderBrightness = if (isSelected) 0.4f else 0.15f,
                elevation = 2.dp
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textCol
        )
    }
}

/**
 * GlassSlider: Modern audio seekbar / equalizer slider styled for Aura Music.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    accentColor: Color = AuraTheme.current.primaryColor
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = accentColor.copy(alpha = 0.24f)
        )
    )
}

/**
 * GlassSwitch: Animated toggle switch with glassmorphic track and sliding thumb.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AuraTheme.current
    val density = LocalDensity.current
    // Total width is 52.dp, padding is 2.dp on both sides, inner width is 48.dp.
    // Thumb is 24.dp. Travel distance = 48.dp - 24.dp = 24.dp.
    val travelDistancePx = with(density) { 24.dp.toPx() }

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) travelDistancePx else 0f,
        animationSpec = AuraMotion.SnappySpring,
        label = "GlassSwitchThumbOffset"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .glassmorphic(
                shape = CircleShape,
                tint = if (checked) theme.primaryColor else theme.surfaceColor,
                tintAlpha = if (checked) 0.90f else (if (theme.isLight) 0.85f else 0.35f),
                borderBrightness = if (checked) 0.5f else 0.2f,
                elevation = 2.dp
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = thumbOffset }
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (checked) Color.White
                    else if (theme.isLight) Color(0xFF64748B)
                    else theme.textColorSecondary
                )
        )
    }
}

/**
 * GlassBottomBar: Floating pill or docked navigation bar with glass backdrop.
 */
@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val theme = AuraTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .glassmorphic(
                shape = RoundedCornerShape(26.dp),
                tint = theme.surfaceColor,
                tintAlpha = 0.35f,
                borderBrightness = 0.3f,
                elevation = 16.dp
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * GlassDialog: Modal popup dialog surrounded by frosted glass styling.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .glassmorphic(
                    shape = RoundedCornerShape(24.dp),
                    tint = AuraTheme.current.surfaceColor,
                    tintAlpha = 0.45f,
                    borderBrightness = 0.35f,
                    elevation = 24.dp
                )
                .padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * GlassTextField: Form/Search input styled with glass borders.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val theme = AuraTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(16.dp),
                tint = theme.surfaceColor,
                tintAlpha = 0.25f,
                borderBrightness = 0.25f,
                elevation = 2.dp
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Box(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = theme.textColorSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = theme.textColorPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(theme.primaryColor),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
