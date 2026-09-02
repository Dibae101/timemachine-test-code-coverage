package com.example.util.simpletimetracker.feature_pomodoro.timer.model

import androidx.annotation.DrawableRes

data class PomodoroButtonState(
    @DrawableRes val iconResId: Int,
    val buttonsOrder: List<Button>,
    val additionalButtonsVisible: Boolean,
) {

    sealed interface Button {
        data object Start : Button
        data object Restart : Button
        data object Pause : Button
        data object Prev : Button
        data object Next : Button
    }
}