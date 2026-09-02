package com.example.util.simpletimetracker.feature_base_adapter.optionsList

import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.GoalCheckmarkView
import com.example.util.simpletimetracker.feature_views.extension.setMargins
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemOptionsListLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.optionsList.OptionsListViewData as ViewData

fun createOptionsListAdapterDelegate(
    onClick: (ViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        tvItemOptionsList.text = item.text
        val textMargin = if (item.icon != null || item.isChecked) 50 else 4
        tvItemOptionsList.setMargins(start = textMargin, end = textMargin)

        if (item.icon != null) {
            ivItemOptionsListIcon.setImageResource(item.icon)
            cardItemOptionsListIcon.isVisible = true
        } else {
            cardItemOptionsListIcon.isVisible = false
        }
        viewItemOptionsListCheckmark.itemCheckState = if (item.isIconCheckVisible) {
            GoalCheckmarkView.CheckState.GOAL_NOT_REACHED
        } else {
            GoalCheckmarkView.CheckState.HIDDEN
        }
        ivItemOptionsListCheck.isVisible = item.isChecked
        viewItemOptionsListSelectedBackground.isVisible = item.isSelected

        root.setOnClickWith(item, onClick)
    }
}

data class OptionsListViewData(
    val id: Id,
    val text: String,
    @DrawableRes val icon: Int?,
    val isIconCheckVisible: Boolean,
    val isChecked: Boolean,
    val isSelected: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = id.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData

    interface Id
}