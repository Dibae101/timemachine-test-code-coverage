package com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord.customView.MultitaskRecordView

data class MultitaskRecordViewData(
    val ids: List<Long>,
    val data: MultitaskRecordView.ViewData,
) : ViewHolderType {

    override fun getUniqueId(): Long = ids.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is MultitaskRecordViewData
}