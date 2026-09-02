package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType

interface ChartFilterDialogListener {

    fun onChartFilterDataSelected(
        chartFilterType: ChartFilterType,
        dataIds: List<Long>,
    )

    fun onChartFilterDialogDismissed()

    fun onChartFilterDialogOpened() = Unit
}