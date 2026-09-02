package com.example.util.simpletimetracker.feature_base_adapter.runningRecordSelected

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.bindState
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.setOnLongClickWith
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemRunningRecordSelectedLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.runningRecordSelected.RunningRecordSelectedViewData as ViewData

fun createRunningRecordSelectedAdapterDelegate(
    onItemClick: ((RunningRecordViewData) -> Unit),
    onItemLongClick: ((RunningRecordViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding.viewRunningRecordItem) {
        item as ViewData

        bindState(item.record, rebind = true, updates = emptyList())
        setOnClickWith(item.record, onItemClick)
        setOnLongClickWith(item.record, onItemLongClick)
    }
}

data class RunningRecordSelectedViewData(
    val record: RunningRecordViewData,
) : ViewHolderType {

    override fun getUniqueId(): Long = record.getUniqueId()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData
}