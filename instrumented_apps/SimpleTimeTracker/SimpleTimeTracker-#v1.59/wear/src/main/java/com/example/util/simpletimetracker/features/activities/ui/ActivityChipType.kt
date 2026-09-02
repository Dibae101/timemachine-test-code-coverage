package com.example.util.simpletimetracker.features.activities.ui

sealed interface ActivityChipType {
    data object Base : ActivityChipType
    data class Suggestion(val isLast: Boolean) : ActivityChipType
    data object Repeat : ActivityChipType
    data object Untracked : ActivityChipType
}