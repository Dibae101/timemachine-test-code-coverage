package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartGrouping
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartLength

data class StatisticsDetailGoalsCompositeViewData(
    val viewData: List<ViewHolderType>,
    val appliedChartGrouping: ChartGrouping,
    val appliedChartLength: ChartLength,
)