package com.example.util.simpletimetracker.feature_base_adapter.activityFilter

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_base_adapter.activityFilter.ActivityFilterAddViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemActivityFilterLayoutBinding as Binding

fun createActivityFilterAddAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.viewActivityFilterItem) {
        item as ViewData

        itemColor = item.color
        itemName = item.name
        itemBackgroundVisible = false
        setOnClickWith(item, onItemClick)
    }
}