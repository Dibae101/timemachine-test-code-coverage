package com.example.util.simpletimetracker.feature_tag_selection.adapter

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_tag_selection.adapter.RecordTagSelectionTextViewData as ViewData
import com.example.util.simpletimetracker.feature_tag_selection.databinding.RecordTagSelectionItemTextLayoutBinding as Binding

fun createRecordTagSelectionTextAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        tvRecordTagSelectionItemText.text = item.text
    }
}

data class RecordTagSelectionTextViewData(
    val text: String,
) : ViewHolderType {

    override fun getUniqueId(): Long = text.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}