package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_statistics_detail.model.StreaksGoal

data class StatisticsDetailStreaksGoalViewData(
    val type: StreaksGoal,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = type.ordinal.toLong()
}