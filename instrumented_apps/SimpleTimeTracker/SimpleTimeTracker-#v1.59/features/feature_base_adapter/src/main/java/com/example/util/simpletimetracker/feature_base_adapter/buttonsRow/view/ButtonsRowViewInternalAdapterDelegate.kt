package com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view

import android.util.TypedValue
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ButtonsRowItemLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData as ViewData

internal fun createButtonsRowViewInternalAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        btnButtonsRowView.tag = if (item.isSelected) ViewData.SELECTED_BUTTON_TEST_TAG else ""
        btnButtonsRowView.text = item.name
        btnButtonsRowView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            item.textSizeSp?.toFloat() ?: 14f,
        )
        btnButtonsRowView.setOnClickWith(item, onItemClick)
    }
}