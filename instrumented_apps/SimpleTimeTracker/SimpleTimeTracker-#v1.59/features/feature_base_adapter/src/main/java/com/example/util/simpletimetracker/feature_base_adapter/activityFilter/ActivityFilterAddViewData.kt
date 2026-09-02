package com.example.util.simpletimetracker.feature_base_adapter.activityFilter

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class ActivityFilterAddViewData(
    val type: Type,
    val name: String,
    @ColorInt val color: Int,
) : ViewHolderType {

    override fun getUniqueId(): Long = type.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ActivityFilterAddViewData

    enum class Type {
        ADD,
        TOGGLE_VISIBILITY,
    }
}