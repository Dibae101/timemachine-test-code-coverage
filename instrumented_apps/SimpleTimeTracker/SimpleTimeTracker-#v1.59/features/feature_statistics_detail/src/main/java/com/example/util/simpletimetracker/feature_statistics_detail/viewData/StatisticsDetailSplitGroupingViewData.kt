package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_statistics_detail.model.SplitChartGrouping

data class StatisticsDetailSplitGroupingViewData(
    val splitChartGrouping: SplitChartGrouping,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = splitChartGrouping.ordinal.toLong()
}