package com.example.util.simpletimetracker.feature_dialogs.chartFilter.model

import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType

data class ChartFilterDataSelectionResult(
    val chartFilterType: ChartFilterType,
    val dataIds: List<Long>,
)