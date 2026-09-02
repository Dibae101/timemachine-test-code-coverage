package com.example.util.simpletimetracker.feature_statistics_detail.model

data class ChartBarDataDuration(
    val rangeStart: Long,
    val legend: String,
    val durations: List<Pair<Long, Int>>,
) {

    val totalDuration: Long? = durations.takeUnless { it.isEmpty() }?.sumOf { it.first }
}