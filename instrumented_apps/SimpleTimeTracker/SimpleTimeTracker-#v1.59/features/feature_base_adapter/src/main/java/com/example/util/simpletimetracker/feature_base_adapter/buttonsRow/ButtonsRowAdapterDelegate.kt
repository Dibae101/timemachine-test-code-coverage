package com.example.util.simpletimetracker.feature_base_adapter.buttonsRow

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.ButtonsRowItemViewData.ButtonsRowId
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setMargins
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.ButtonsRowItemViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.StatisticsDetailButtonsRowItemBinding as Binding

fun createButtonsRowAdapterDelegate(
    onClick: (ButtonsRowId, ButtonsRowViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.root) {
        item as ViewData

        setMargins(top = item.marginTopDp)
        // Replace without animation on reuse for different buttons.
        replace(item.data, isFast = tag != item.block)
        listener = { onClick(item.block, it) }
        tag = item.block
    }
}

data class ButtonsRowItemViewData(
    val block: ButtonsRowId,
    val marginTopDp: Int,
    val data: List<ViewHolderType>,
) : ViewHolderType {

    override fun getUniqueId(): Long = block.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData

    interface ButtonsRowId
}