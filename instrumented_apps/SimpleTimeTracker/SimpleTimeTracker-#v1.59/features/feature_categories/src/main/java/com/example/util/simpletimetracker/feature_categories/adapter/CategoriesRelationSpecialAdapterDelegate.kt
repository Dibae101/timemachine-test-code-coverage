package com.example.util.simpletimetracker.feature_categories.adapter

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation.ActivitySuggestionListViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation.activitySuggestionListAdapterBindDelegate
import com.example.util.simpletimetracker.feature_categories.adapter.CategoriesRelationSpecialViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemActivitySuggestionListLayoutBinding as Binding

fun createCategoriesRelationSpecialAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.tag = item.id.forTypeId
        activitySuggestionListAdapterBindDelegate(
            item = item.data,
            binding = this,
        )
    }
}

data class CategoriesRelationSpecialViewData(
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
        data object CategoriesEmpty : Type
        data object TagCommon : Type
        data object TagDefaultHint : Type
        data class TagDefault(val id: Long) : Type
        data object TagRecordsCount : Type
    }
}