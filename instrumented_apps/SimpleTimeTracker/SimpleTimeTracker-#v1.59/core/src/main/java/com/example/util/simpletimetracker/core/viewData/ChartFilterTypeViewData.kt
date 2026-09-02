package com.example.util.simpletimetracker.core.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType

data class ChartFilterTypeViewData(
    val filterType: ChartFilterType,
    override val name: String,
    override val isSelected: Boolean,
    override val textSizeSp: Int?,
) : ButtonsRowViewData() {

    override val id: Long = filterType.ordinal.toLong()
}