package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import com.example.util.simpletimetracker.domain.statistics.model.ChartValueMode
import kotlinx.parcelize.Parcelize

@Parcelize
data class StatisticsTagValuesSettingsParams(
    val tagId: Long,
    val chartValueMode: ChartValueMode,
    val multiplyDuration: Boolean,
    val fillEmptyPeriods: Boolean,
    val yAxisZoomed: Boolean,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = StatisticsTagValuesSettingsParams(
            tagId = 0,
            chartValueMode = ChartValueMode.TOTAL,
            multiplyDuration = false,
            fillEmptyPeriods = false,
            yAxisZoomed = false,
        )
    }
}
