package com.example.util.simpletimetracker.feature_base_adapter.buttonDouble

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_base_adapter.buttonDouble.DoubleButtonsViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemDoubleButtonsLayoutBinding as Binding

fun createDoubleButtonsAdapterDelegate(
    onClick: (ViewData.Type) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        btnDoubleButtonsFirst.text = item.first.name
        btnDoubleButtonsFirst.tag = item.first.type

        btnDoubleButtonsSecond.text = item.second.name
        btnDoubleButtonsSecond.tag = item.second.type

        btnDoubleButtonsFirst.setOnClickWith(item.first.type, onClick)
        btnDoubleButtonsSecond.setOnClickWith(item.second.type, onClick)
    }
}

data class DoubleButtonsViewData(
    val first: Button,
    val second: Button,
) : ViewHolderType {

    override fun getUniqueId(): Long = first.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData

    data class Button(
        val type: Type,
        val name: String,
    )

    interface Type
}