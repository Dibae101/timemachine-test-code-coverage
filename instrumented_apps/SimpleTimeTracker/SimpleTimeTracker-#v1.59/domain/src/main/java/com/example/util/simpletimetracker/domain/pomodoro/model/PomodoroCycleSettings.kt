package com.example.util.simpletimetracker.domain.pomodoro.model

data class PomodoroCycleSettings(
    val focusTimeMs: Long,
    val breakTimeMs: Long,
    val longBreakTimeMs: Long,
    val periodsUntilLongBreak: Long,
)