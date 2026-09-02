package com.example.util.simpletimetracker.feature_base_adapter.listElement

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.visible
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemListElementLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.listElement.ListElementViewData as ViewData

fun createListElementAdapter() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->
    with(binding) {
        item as ViewData

        tvItemListElement.text = item.text
        cvItemListElement.setCardBackgroundColor(item.color)
        if (item.icon != null) {
            ivItemListElement.visible = true
            ivItemListElement.itemIcon = item.icon
        } else {
            ivItemListElement.visible = false
        }
    }
}

data class ListElementViewData(
    val text: String,
    val icon: RecordTypeIcon?,
    @ColorInt val color: Int,
) : ViewHolderType {

    override fun getUniqueId(): Long = text.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}
