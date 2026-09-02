package com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.visible
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeRelation.ActivitySuggestionListViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemActivitySuggestionListLayoutBinding as Binding

fun createActivitySuggestionListAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.tag = item.id.forTypeId
        activitySuggestionListAdapterBindDelegate(
            item = item,
            binding = this,
        )
    }
}

fun activitySuggestionListAdapterBindDelegate(
    item: ViewData,
    binding: Binding,
) = with(binding) {
    tvActivitySuggestionListItemContent.text = item.text
    cvActivitySuggestionListItemContent.setCardBackgroundColor(item.color)
    if (item.icon != null) {
        ivActivitySuggestionListItemContent.visible = true
        ivActivitySuggestionListItemContent.itemIcon = item.icon
    } else {
        ivActivitySuggestionListItemContent.visible = false
    }
}

data class ActivitySuggestionListViewData(
    val id: Id,
    val text: String,
    val icon: RecordTypeIcon?,
    @ColorInt val color: Int,
) : ViewHolderType {

    override fun getUniqueId(): Long = id.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData

    data class Id(
        val suggestionTypeId: Long,
        val forTypeId: Long,
    )
}