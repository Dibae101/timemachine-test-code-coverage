package com.example.util.simpletimetracker.feature_statistics_detail.adapter

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreview
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.StatisticsDetailPreviewsViewData as ViewData
import com.example.util.simpletimetracker.feature_statistics_detail.databinding.StatisticsDetailPreviewsItemBinding as Binding

fun createStatisticsDetailPreviewsAdapterDelegate(
    onClick: (StatisticsDetailPreview) -> Unit,
    onLongClick: (StatisticsDetailPreview) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.adapter.replace(item.data)
        root.setClickListener(onClick)
        root.setLongClickListener(onLongClick)
    }
}

data class StatisticsDetailPreviewsViewData(
    val block: StatisticsDetailBlock,
    val data: List<ViewHolderType>,
) : ViewHolderType {

    override fun getUniqueId(): Long = block.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}