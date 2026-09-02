package com.example.util.simpletimetracker.feature_records.model

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DaysInCalendar

data class RecordsContainerPosition(
    val position: Int,
    val isCalendar: Boolean,
    val daysInCalendar: DaysInCalendar,
    val animate: Boolean,
)