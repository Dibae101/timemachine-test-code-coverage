package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChartFilterDialogParams(
    val chartFilterType: ChartFilterType,
    val filteredTypeIds: List<Long>,
    val filteredCategoryIds: List<Long>,
    val filteredTagIds: List<Long>,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = ChartFilterDialogParams(
            chartFilterType = ChartFilterType.ACTIVITY,
            filteredTypeIds = emptyList(),
            filteredCategoryIds = emptyList(),
            filteredTagIds = emptyList(),
        )
    }
}