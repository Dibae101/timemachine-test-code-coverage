package com.example.util.simpletimetracker.feature_base_adapter.statistics

import androidx.core.view.ViewCompat
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.StatisticsView
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemStatisticsLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData as ViewData

fun createStatisticsAdapterDelegate(
    onItemClick: ((ViewData, Map<Any, String>) -> Unit)?,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.viewStatisticsItem) {
        item as ViewData

        bind(item, onItemClick)
    }
}

fun StatisticsView.bind(
    item: ViewData,
    onItemClick: ((ViewData, Map<Any, String>) -> Unit)?,
) {
    itemColor = item.color
    itemName = item.name
    itemDuration = item.duration
    itemPercent = item.percent

    if (item.icon != null) {
        itemIconVisible = true
        itemIcon = item.icon
    } else {
        itemIconVisible = false
    }

    if (onItemClick != null) {
        val transitionName = item.transitionName
        setOnClick { onItemClick(item, mapOf(this to transitionName.orEmpty())) }
        if (!transitionName.isNullOrEmpty()) {
            ViewCompat.setTransitionName(this, transitionName)
        }
    }
}
