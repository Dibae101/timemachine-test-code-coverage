package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.domain.statistics.model.ChartValueMode

data class StatisticsDetailChartValueModeViewData(
    val chartValueMode: ChartValueMode,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = chartValueMode.ordinal.toLong()
}