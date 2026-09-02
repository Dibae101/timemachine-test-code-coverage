package com.example.util.simpletimetracker.feature_base_adapter.activityFilter

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.domain.activityFilter.model.ActivityFilterType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class ActivityFilterViewData(
    val id: Long,
    val name: String,
    @ColorInt val color: Int,
    @ColorInt val backgroundColor: Int,
    val selected: Boolean,
    val type: ActivityFilterType,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ActivityFilterViewData && other.type == type
}