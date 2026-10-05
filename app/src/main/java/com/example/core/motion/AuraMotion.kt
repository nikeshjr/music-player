package com.example.core.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * AuraMotion: Premium motion token system designed for 60/120fps fluid physics.
 * Includes user-configurable animation speed multipliers and reduced-motion fallbacks.
 */
@Immutable
object AuraMotion {

    // Easing curves matching Material 3 Emphasized & Decelerate
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing: Easing = FastOutSlowInEasing
    val DecelerateEasing: Easing = LinearOutSlowInEasing

    // Durations in milliseconds
    const val DurationShort = 200
    const val DurationMedium = 350
    const val DurationLong = 500
    const val DurationCrossfade = 600

    /**
     * Physics spring specifications for fluid organic morphing (e.g. Dynamic Island and cards).
     */
    val GentleSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SnappySpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Dynamic Island signature spring: responsive with subtle organic overshoot
    val IslandMorphSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.68f,
        stiffness = 380f
    )

    val BouncySpring: SpringSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 400f
    )

    /**
     * Resolves duration taking into account user animation speed multiplier and reduced motion.
     * Speed multipliers: 0.0f (Off), 0.5f (Slow), 1.0f (Normal), 1.5f (Fast).
     */
    fun adjustedDuration(baseMs: Int, speedMultiplier: Float, reduceMotion: Boolean): Int {
        if (reduceMotion || speedMultiplier <= 0f) return 0
        return (baseMs / speedMultiplier).toInt().coerceAtLeast(1)
    }

    /**
     * Dynamic spring or instant transition helper based on user preferences.
     */
    fun <T> dynamicSpring(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessMedium,
        reduceMotion: Boolean = false
    ): SpringSpec<T> {
        return if (reduceMotion) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = dampingRatio, stiffness = stiffness)
        }
    }

    /**
     * Dynamic tween helper taking user settings into account.
     */
    fun <T> dynamicTween(
        baseMs: Int,
        easing: Easing = EmphasizedEasing,
        speedMultiplier: Float = 1.0f,
        reduceMotion: Boolean = false
    ): TweenSpec<T> {
        val duration = adjustedDuration(baseMs, speedMultiplier, reduceMotion)
        return tween(durationMillis = duration, easing = easing)
    }
}
