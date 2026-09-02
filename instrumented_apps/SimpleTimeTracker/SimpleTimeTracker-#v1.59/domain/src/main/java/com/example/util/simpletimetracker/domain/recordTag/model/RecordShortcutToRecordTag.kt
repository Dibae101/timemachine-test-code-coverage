package com.example.util.simpletimetracker.domain.recordTag.model

data class RecordShortcutToRecordTag(
    val shortcutId: Long,
    val recordTagId: Long,
    val recordTagNumericValue: Double?,
)