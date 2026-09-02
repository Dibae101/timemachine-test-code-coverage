package com.example.util.simpletimetracker.feature_statistics.adapter

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics.databinding.ItemStatisticsChartLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_statistics.viewData.StatisticsChartViewData as ViewData

fun createStatisticsChartAdapterDelegate(
    onChartAttached: (Boolean) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        chartStatisticsItem.setSegments(
            data = item.data,
            selectedPiePosition = null,
            piesAreClickable = false,
            animateOpen = item.animatedOpen,
        )
        chartStatisticsItem.setAttachedListener(onChartAttached)
    }
}