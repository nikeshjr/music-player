package com.example.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * ContrastGuard: Ensures strict compliance with WCAG AA accessibility standards
 * (minimum 4.5:1 contrast ratio for normal text and 3:1 for large UI components)
 * across user-customized glass surfaces, dark/light themes, and artwork ambient glows.
 */
object ContrastGuard {

    private const val MIN_NORMAL_TEXT_CONTRAST = 4.5
    private const val MIN_LARGE_TEXT_CONTRAST = 3.0

    /**
     * Calculates the WCAG contrast ratio between foreground and background colors.
     * Returns a value between 1.0 (no contrast) and 21.0 (black on white).
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val fgArgb = foreground.toArgb()
        val bgArgb = background.toArgb()
        return ColorUtils.calculateContrast(fgArgb, bgArgb)
    }

    /**
     * Checks if a color combination meets WCAG AA standards.
     */
    fun isWcagCompliant(
        foreground: Color,
        background: Color,
        isLargeText: Boolean = false
    ): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        val threshold = if (isLargeText) MIN_LARGE_TEXT_CONTRAST else MIN_NORMAL_TEXT_CONTRAST
        return ratio >= threshold
    }

    /**
     * Automatically adjusts text color to ensure at least 4.5:1 contrast against the background.
     * If the requested text color is insufficient, it smoothly shifts towards pure white or deep black.
     */
    fun ensureContrast(
        textColor: Color,
        backgroundColor: Color,
        isLargeText: Boolean = false
    ): Color {
        if (isWcagCompliant(textColor, backgroundColor, isLargeText)) {
            return textColor
        }

        // Test contrast against pure white vs pure black
        val whiteContrast = calculateContrastRatio(Color.White, backgroundColor)
        val blackContrast = calculateContrastRatio(Color(0xFF0F172A), backgroundColor)

        return if (whiteContrast >= blackContrast) {
            Color(0xFFF8FAFC)
        } else {
            Color(0xFF0F172A)
        }
    }

    /**
     * Returns human-readable contrast analysis report for the theme editor.
     */
    fun getContrastStatus(
        textColor: Color,
        backgroundColor: Color
    ): ContrastStatus {
        val ratio = calculateContrastRatio(textColor, backgroundColor)
        return when {
            ratio >= 7.0 -> ContrastStatus.AAA(ratio)
            ratio >= 4.5 -> ContrastStatus.AA(ratio)
            ratio >= 3.0 -> ContrastStatus.AA_LARGE_ONLY(ratio)
            else -> ContrastStatus.FAIL(ratio)
        }
    }
}

sealed class ContrastStatus(val ratio: Double) {
    class AAA(ratio: Double) : ContrastStatus(ratio)
    class AA(ratio: Double) : ContrastStatus(ratio)
    class AA_LARGE_ONLY(ratio: Double) : ContrastStatus(ratio)
    class FAIL(ratio: Double) : ContrastStatus(ratio)

    val label: String
        get() = when (this) {
            is AAA -> "Excellent (AAA • %.1f:1)".format(ratio)
            is AA -> "Good (AA • %.1f:1)".format(ratio)
            is AA_LARGE_ONLY -> "Passes for Large Text Only (%.1f:1)".format(ratio)
            is FAIL -> "Low Contrast Warning (%.1f:1)".format(ratio)
        }

    val isPassing: Boolean
        get() = this !is FAIL
}
