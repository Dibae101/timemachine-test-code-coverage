package com.example.util.simpletimetracker.feature_base_adapter.category

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemCategoryLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryAddViewData as ViewData

fun createCategoryAddAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.viewCategoryItem) {
        item as ViewData

        itemColor = item.color
        itemName = item.name
        if (item.icon != null) {
            itemIcon = item.icon
            itemIconVisible = true
        } else {
            itemIconVisible = false
        }
        setOnClickWith(item, onItemClick)
    }
}

class CategoryAddViewData(
    val type: Type,
    val name: String,
    @ColorInt val color: Int,
    val icon: RecordTypeIcon?,
) : ViewHolderType {

    // Only one add item on screen
    override fun getUniqueId(): Long = 1L

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData && other.type == type

    interface Type {
        data object AddTag : Type
        data object AddCategory : Type
        data object ShowAll : Type
        data object EnableSearch : Type
    }
}