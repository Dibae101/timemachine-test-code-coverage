package com.example.util.simpletimetracker.feature_base_adapter.recordsDateDivider

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemRecordsDaysBetweenDividerLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.recordsDateDivider.RecordsDaysBetweenDividerViewData as ViewData

fun createRecordsDaysBetweenDividerAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        tvItemRecordsDateDivider.text = item.message
    }
}

data class RecordsDaysBetweenDividerViewData(
    val id: Long,
    val message: String,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}