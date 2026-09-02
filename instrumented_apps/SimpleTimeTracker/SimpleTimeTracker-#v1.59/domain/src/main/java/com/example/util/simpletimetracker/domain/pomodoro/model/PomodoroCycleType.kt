package com.example.util.simpletimetracker.domain.pomodoro.model

sealed interface PomodoroCycleType {
    data object Focus : PomodoroCycleType
    data object Break : PomodoroCycleType
    data object LongBreak : PomodoroCycleType
}