package com.example.util.simpletimetracker.feature_base_adapter.statistics

import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemStatisticsSelectableLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsSelectableViewData as ViewData

fun createStatisticsSelectableAdapterDelegate(
    onItemClick: ((StatisticsViewData, Map<Any, String>) -> Unit)?,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        viewStatisticsSelectableItemCheck.isVisible = item.isSelected
        viewStatisticsItem.bind(item.data, onItemClick)
    }
}

data class StatisticsSelectableViewData(
    val data: StatisticsViewData,
    val isSelected: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = data.id

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}
