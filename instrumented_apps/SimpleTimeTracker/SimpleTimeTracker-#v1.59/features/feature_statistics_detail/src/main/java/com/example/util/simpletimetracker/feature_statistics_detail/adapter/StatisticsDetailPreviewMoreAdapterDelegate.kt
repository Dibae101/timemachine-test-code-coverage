package com.example.util.simpletimetracker.feature_statistics_detail.adapter

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreview
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_statistics_detail.databinding.StatisticsDetailPreviewMoreItemBinding as Binding
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreviewMoreViewData as ViewData

fun createStatisticsPreviewMoreAdapterDelegate(
    onClick: (StatisticsDetailPreview) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.setOnClickWith(item, onClick)
    }
}
