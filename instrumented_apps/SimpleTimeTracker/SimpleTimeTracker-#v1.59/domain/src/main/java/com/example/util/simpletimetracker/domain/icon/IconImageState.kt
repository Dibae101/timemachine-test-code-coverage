package com.example.util.simpletimetracker.domain.icon

sealed interface IconImageState {
    object Chooser : IconImageState
    object Search : IconImageState
}