package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_statistics_detail.model.DataDistributionGraph

data class StatisticsDetailDataDistributionGraphViewData(
    val graph: DataDistributionGraph,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = graph.ordinal.toLong()
}