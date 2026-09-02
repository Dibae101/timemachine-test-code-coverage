package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartBarDataDuration
import javax.inject.Inject

class FillEmptyBarsWithPreviousValueInteractor @Inject constructor() {

    fun invoke(
        data: List<ChartBarDataDuration>,
        previousRangeData: List<ChartBarDataDuration>?,
    ): List<ChartBarDataDuration> {
        if (data.isEmpty()) return data

        var previousNonEmptyDurations = previousRangeData
            ?.lastOrNull { it.durations.isNotEmpty() }?.durations.orEmpty()

        val now = System.currentTimeMillis()

        return data.map { bar ->
            when {
                bar.durations.isNotEmpty() -> {
                    previousNonEmptyDurations = bar.durations
                    bar
                }
                bar.rangeStart >= now -> {
                    bar
                }
                previousNonEmptyDurations.isNotEmpty() -> {
                    bar.copy(durations = previousNonEmptyDurations)
                }
                else -> bar
            }
        }
    }
}
