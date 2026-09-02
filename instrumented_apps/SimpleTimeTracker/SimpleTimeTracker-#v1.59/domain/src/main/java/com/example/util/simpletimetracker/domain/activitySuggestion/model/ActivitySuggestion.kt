package com.example.util.simpletimetracker.domain.activitySuggestion.model

data class ActivitySuggestion(
    val id: Long = 0,
    val forTypeId: Long,
    val suggestionIds: Set<Long>,
)