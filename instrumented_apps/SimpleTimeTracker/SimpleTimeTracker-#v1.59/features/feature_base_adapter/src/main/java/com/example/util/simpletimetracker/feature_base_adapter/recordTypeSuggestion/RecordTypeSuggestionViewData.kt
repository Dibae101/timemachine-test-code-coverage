package com.example.util.simpletimetracker.feature_base_adapter.recordTypeSuggestion

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.recordType.RecordTypeViewData

data class RecordTypeSuggestionViewData(
    val data: RecordTypeViewData,
    val type: Type,
) : ViewHolderType {

    override fun getUniqueId(): Long = data.id

    override fun isValidType(other: ViewHolderType): Boolean =
        other is RecordTypeSuggestionViewData && other.type == type

    interface Type

    companion object {
        const val TEST_TAG = "RecordTypeSuggestionViewData"
    }
}