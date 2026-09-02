package com.example.util.simpletimetracker.feature_statistics_detail.adapter

import com.example.util.simpletimetracker.domain.extension.orFalse
import com.example.util.simpletimetracker.domain.base.OneShotValue
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.pieChart.PiePortion
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.StatisticsDetailPieChartViewData as ViewData
import com.example.util.simpletimetracker.feature_statistics_detail.databinding.StatisticsDetailPieChartItemBinding as Binding

fun createStatisticsDetailPieChartAdapterDelegate(
    onPieClick: (StatisticsDetailBlock, Long?) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.root) {
        item as ViewData

        tag = item.block
        resetAnimation()
        setSegments(
            data = item.data,
            selectedPiePosition = item.selectedPiePosition,
            piesAreClickable = true,
            animateOpen = item.animate.getValue().orFalse(),
        )
        setOnPieClickListener { onPieClick(item.block, it) }
    }
}

data class StatisticsDetailPieChartViewData(
    val block: StatisticsDetailBlock,
    val data: List<PiePortion>,
    val selectedPiePosition: Int?,
    val animate: OneShotValue<Boolean>,
) : ViewHolderType {

    override fun getUniqueId(): Long = block.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}