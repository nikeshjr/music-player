package com.example.service.island

enum class CutoutAlignment {
    CENTER,
    LEFT,
    RIGHT
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

data class IslandLayoutConfig(
    val isEnabled: Boolean = true,
    val offsetX: Int = 0,
    val offsetY: Int = 12,
    val scaleMultiplier: Float = 1.0f,
    val cornerRadiusDp: Int = 28,
    val opacity: Float = 0.95f,
    val snapToCutout: Boolean = true,
    val cutoutAlignment: CutoutAlignment = CutoutAlignment.CENTER,
    val freePositioning: Boolean = false,
    val autoHidePausedSeconds: Int = 15,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.SYMMETRIC_BARS,
    val enableGestures: Boolean = true,
    val hapticFeedback: Boolean = true
)
