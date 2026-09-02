package com.example.util.simpletimetracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun Int.scaledSp(): TextUnit {
    val value: Int = this
    return with(LocalDensity.current) {
        val fontScale = this.fontScale
        val textSize = value / fontScale
        textSize.sp
    }
}

@Composable
fun getCoercedFontScale(): Float {
    return LocalDensity.current.fontScale.coerceAtLeast(1f)
}