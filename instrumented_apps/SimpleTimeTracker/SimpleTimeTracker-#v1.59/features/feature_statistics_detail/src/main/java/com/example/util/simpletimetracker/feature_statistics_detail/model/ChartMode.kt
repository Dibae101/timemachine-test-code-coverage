package com.example.util.simpletimetracker.feature_statistics_detail.model

sealed interface ChartMode {

    data object DURATIONS : ChartMode

    data object COUNTS : ChartMode

    @Suppress("ClassName")
    data class TAG_VALUE(
        val tagId: Long,
        val multiplyDuration: Boolean,
    ) : ChartMode
}