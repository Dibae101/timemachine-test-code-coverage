package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartLength

data class StatisticsDetailChartLengthViewData(
    val chartLength: ChartLength,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = chartLength.ordinal.toLong()
}