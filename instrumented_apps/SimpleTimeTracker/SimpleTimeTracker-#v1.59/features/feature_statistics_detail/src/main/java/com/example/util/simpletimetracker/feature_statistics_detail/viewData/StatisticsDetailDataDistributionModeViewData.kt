package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_statistics_detail.model.DataDistributionMode

data class StatisticsDetailDataDistributionModeViewData(
    val mode: DataDistributionMode,
    override val name: String,
    override val isSelected: Boolean,
    override val textSizeSp: Int?,
) : ButtonsRowViewData() {

    override val id: Long = mode.ordinal.toLong()
}