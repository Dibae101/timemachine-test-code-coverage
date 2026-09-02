package com.example.util.simpletimetracker.feature_records_filter.adapter

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class RecordsFilterButtonViewData(
    val type: Type,
    val text: String,
    @ColorInt val backgroundColor: Int,
    val isEnabled: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = type.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is RecordsFilterButtonViewData

    enum class Type {
        INVERT_SELECTION,
        FILTER_DUPLICATES,
        SAVE_FAVOURITE,
        DELETE_FAVOURITE,
    }
}