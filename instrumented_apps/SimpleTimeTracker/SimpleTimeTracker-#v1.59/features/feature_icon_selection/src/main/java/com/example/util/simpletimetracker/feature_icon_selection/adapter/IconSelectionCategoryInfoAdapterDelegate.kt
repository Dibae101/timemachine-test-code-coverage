package com.example.util.simpletimetracker.feature_icon_selection.adapter

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.databinding.ItemIconSelectionCategoryInfoLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_icon_selection.viewData.IconSelectionCategoryInfoViewData as ViewData

fun createIconSelectionCategoryInfoAdapterDelegate() =
    createRecyclerBindingAdapterDelegate<ViewData, Binding>(
        Binding::inflate,
    ) { binding, item, _ ->

        with(binding) {
            item as ViewData

            tvIconSelectionCategoryInfoItem.text = item.text
        }
    }