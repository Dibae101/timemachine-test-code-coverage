package com.example.util.simpletimetracker.feature_base_adapter.recordSelected

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.record.bindState
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.setOnLongClickWith
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemRecordSelectedLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.recordSelected.RecordSelectedViewData as ViewData

fun createRecordSelectedAdapterDelegate(
    onItemClick: ((RecordViewData) -> Unit),
    onItemLongClick: ((RecordViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.viewRecordItem) {
        item as ViewData

        bindState(item.record)
        setOnClickWith(item.record, onItemClick)
        setOnLongClickWith(item.record, onItemLongClick)
    }
}

data class RecordSelectedViewData(
    val record: RecordViewData,
) : ViewHolderType {

    override fun getUniqueId(): Long = record.getUniqueId()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData
}