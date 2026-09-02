package com.example.util.simpletimetracker.domain.model

data class WearCurrentState(
    val currentActivities: List<WearCurrentActivity>,
    val lastRecords: List<WearLastRecord>,
    val suggestionIds: List<Long>,
)