package com.example.util.simpletimetracker.feature_views

import androidx.annotation.FloatRange
import kotlin.math.sin

object ParticlesAnimationUtils {

    @FloatRange(from = 0.0, to = 1.0)
    fun pseudoRandom(seed: Double): Double {
        return sin(seed) / 2.0 + 0.5
    }

    @Suppress("SameParameterValue")
    fun interpolate(a: Float, b: Float, f: Double): Double {
        return a + f * (b - a)
    }
}