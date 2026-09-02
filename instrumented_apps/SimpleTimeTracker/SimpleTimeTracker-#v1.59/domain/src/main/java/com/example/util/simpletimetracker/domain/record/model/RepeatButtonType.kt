package com.example.util.simpletimetracker.domain.record.model

sealed interface RepeatButtonType {
    object RepeatLast : RepeatButtonType
    object RepeatBeforeLast : RepeatButtonType
}