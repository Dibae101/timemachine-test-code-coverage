package com.example.util.simpletimetracker.feature_statistics.adapter

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData
import com.example.util.simpletimetracker.feature_base_adapter.statistics.bind
import com.example.util.simpletimetracker.feature_views.extension.getThemedAttr
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemStatisticsLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_statistics.adapter.StatisticsInfoViewData as ViewData

fun createStatisticsInfoAdapterDelegate(
    onItemClick: ((StatisticsViewData, Map<Any, String>) -> Unit)?,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        val textColor = root.context.getThemedAttr(R.attr.appTextPrimaryColor)
        val itemBinding = viewStatisticsItem.binding
        itemBinding.tvStatisticsItemName.setTextColor(textColor)
        itemBinding.tvStatisticsItemDuration.setTextColor(textColor)
        itemBinding.tvStatisticsItemPercent.setTextColor(textColor)

        viewStatisticsItem.bind(
            item = item.data,
            onItemClick = onItemClick,
        )
    }
}

data class StatisticsInfoViewData(
    val data: StatisticsViewData,
) : ViewHolderType {

    override fun getUniqueId(): Long = data.name.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}