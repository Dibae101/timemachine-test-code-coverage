package com.example.util.simpletimetracker.feature_icon_selection.adapter

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_icon_selection.databinding.ItemIconSelectionLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionViewData as ViewData

fun createIconSelectionAdapterDelegate(
    onIconItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        layoutIconSelectionItem.setCardBackgroundColor(item.colorInt)
        ivIconSelectionItem.setBackgroundResource(item.iconResId)
        ivIconSelectionItem.tag = item.iconResId
        layoutIconSelectionItem.setOnClickWith(item, onIconItemClick)
    }
}