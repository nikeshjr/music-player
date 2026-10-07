package com.example.core.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AuraMotion {
    const val DurationShort = 200
    const val DurationMedium = 350
    const val DurationLong = 500
    const val DurationCrossfade = 600

    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing: Easing = FastOutSlowInEasing
    val DecelerateEasing: Easing = LinearOutSlowInEasing

    val GentleSpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 200.0f)
    val SnappySpring: SpringSpec<Float> = spring(dampingRatio = 1.0f, stiffness = 1500.0f)
    val IslandMorphSpring: SpringSpec<Float> = spring(dampingRatio = 0.68f, stiffness = 380.0f)
    val BouncySpring: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 400.0f)

    fun adjustedDuration(baseMs: Int, speedMultiplier: Float, reduceMotion: Boolean): Int {
        if (reduceMotion || speedMultiplier <= 0f) return 0
        return (baseMs / speedMultiplier).toInt().coerceAtLeast(1)
    }

    fun <T> dynamicSpring(
        dampingRatio: Float = 0.5f,
        stiffness: Float = 1500.0f,
        reduceMotion: Boolean = false
    ): SpringSpec<T> {
        return if (reduceMotion) {
            spring(dampingRatio = 1.0f, stiffness = 10000.0f)
        } else {
            spring(dampingRatio = dampingRatio, stiffness = stiffness)
        }
    }

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
