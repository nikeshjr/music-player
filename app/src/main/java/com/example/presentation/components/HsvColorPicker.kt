package com.example.presentation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.AuraTheme
import com.example.core.theme.ContrastGuard

/**
 * HsvColorPicker: Built-in glassmorphic color customization interface.
 * Features Hue slider, Saturation/Value adjustments, Alpha slider, Hex input,
 * and a live WCAG AA contrast guard indicator against the active surface background.
 */
@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    title: String = "Select Color",
    modifier: Modifier = Modifier
) {
    val theme = AuraTheme.current

    // Convert initial color to HSV
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(
            AndroidColor.argb(
                (initialColor.alpha * 255).toInt(),
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val currentColor = remember(hue, saturation, value, alpha) {
        val colorInt = AndroidColor.HSVToColor((alpha * 255).toInt(), floatArrayOf(hue, saturation, value))
        Color(colorInt)
    }

    val contrastStatus = remember(currentColor, theme.backgroundColor) {
        ContrastGuard.getContrastStatus(currentColor, theme.backgroundColor)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.textColorPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Color Swatch & Contrast Guard Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(currentColor)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val hexString = "#%02X%02X%02X%02X".format(
                    (alpha * 255).toInt(),
                    (currentColor.red * 255).toInt(),
                    (currentColor.green * 255).toInt(),
                    (currentColor.blue * 255).toInt()
                )
                Text(
                    text = hexString,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = theme.textColorPrimary
                )
                Text(
                    text = contrastStatus.label,
                    fontSize = 11.sp,
                    color = if (contrastStatus.isPassing) Color(0xFF10B981) else Color(0xFFFFB703),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hue Gradient Slider
        Text(
            text = "Hue",
            fontSize = 12.sp,
            color = theme.textColorSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val position = (change.position.x / size.width).coerceIn(0f, 1f)
                        hue = position * 360f
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Saturation Slider
        Text(
            text = "Saturation",
            fontSize = 12.sp,
            color = theme.textColorSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        GlassSlider(
            value = saturation,
            onValueChange = { saturation = it },
            accentColor = currentColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Brightness / Value Slider
        Text(
            text = "Brightness",
            fontSize = 12.sp,
            color = theme.textColorSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        GlassSlider(
            value = value,
            onValueChange = { value = it },
            accentColor = currentColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Preset Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val swatches = listOf(
                Color(0xFF8C65FF), Color(0xFF00E5FF), Color(0xFFFF5E7E),
                Color(0xFF10B981), Color(0xFFFF9900), Color(0xFFC084FC)
            )
            swatches.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(swatch.value.toLong().toInt(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlassButton(
            onClick = { onColorSelected(currentColor) },
            accentColor = currentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Apply Color",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
