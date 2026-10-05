package com.example.service.island

import androidx.compose.runtime.Immutable

/**
 * IslandLayoutConfig: Coordinates, dimensions, and behavior preferences
 * for the Universal Floating Dynamic Island overlay.
 */
@Immutable
data class IslandLayoutConfig(
    val isEnabled: Boolean = true,
    val offsetX: Int = 0,
    val offsetY: Int = 12, // Default dp below top status bar / camera cutout
    val scaleMultiplier: Float = 1.0f, // 0.8f .. 1.3f
    val cornerRadiusDp: Int = 28,
    val opacity: Float = 0.95f,
    val snapToCutout: Boolean = true,
    val cutoutAlignment: CutoutAlignment = CutoutAlignment.CENTER,
    val freePositioning: Boolean = false,
    val autoHidePausedSeconds: Int = 15, // 0 = never hide, 5, 15, 30, 60
    val visualizerStyle: VisualizerStyle = VisualizerStyle.SYMMETRIC_BARS,
    val enableGestures: Boolean = true,
    val hapticFeedback: Boolean = true
)

enum class CutoutAlignment {
    CENTER, LEFT, RIGHT
}

enum class VisualizerStyle(val displayName: String) {
    SYMMETRIC_BARS("Symmetric Bars"),
    FLUID_WAVE("Fluid Wave"),
    CIRCULAR_RING("Circular Ring")
}

enum class IslandState {
    IDLE_PILL,
    COMPACT,
    LARGE_CARD
}
