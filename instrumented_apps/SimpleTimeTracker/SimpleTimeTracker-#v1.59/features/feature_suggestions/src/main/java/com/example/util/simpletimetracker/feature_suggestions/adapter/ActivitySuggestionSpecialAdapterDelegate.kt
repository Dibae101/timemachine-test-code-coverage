package com.example.util.simpletimetracker.feature_suggestions.adapter

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation.ActivitySuggestionListViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation.activitySuggestionListAdapterBindDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_suggestions.adapter.ActivitySuggestionSpecialViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemActivitySuggestionListLayoutBinding as Binding

fun createActivitySuggestionSpecialAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.tag = item.id.forTypeId
        activitySuggestionListAdapterBindDelegate(
            item = item.data,
            binding = this,
        )

        root.setOnClickWith(item, onItemClick)
    }
}

data class ActivitySuggestionSpecialViewData(
    val id: Id,
    val data: ActivitySuggestionListViewData,
) : ViewHolderType {

    override fun getUniqueId(): Long = id.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData

    data class Id(
        val forTypeId: Long,
        val type: Type,
    )

    sealed interface Type {
        data object Add : Type
        data object Calculate : Type
    }
}