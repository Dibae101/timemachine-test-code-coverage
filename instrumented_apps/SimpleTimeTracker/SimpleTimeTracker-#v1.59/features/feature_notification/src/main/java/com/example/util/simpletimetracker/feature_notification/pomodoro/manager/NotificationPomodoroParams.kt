package com.example.util.simpletimetracker.feature_notification.pomodoro.manager

import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleType

data class NotificationPomodoroParams(
    val title: String,
    val subtitle: String,
    val cycleType: PomodoroCycleType,
    val isDarkTheme: Boolean,
)