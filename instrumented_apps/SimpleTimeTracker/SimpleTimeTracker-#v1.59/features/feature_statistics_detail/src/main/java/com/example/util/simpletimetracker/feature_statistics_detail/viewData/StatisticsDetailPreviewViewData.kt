package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

data class StatisticsDetailPreviewViewData(
    val id: Long,
    val type: Type,
    val dataType: DataType,
    val name: String,
    val iconId: RecordTypeIcon? = null,
    @ColorInt val iconColor: Int?,
    val iconAlpha: Float = 1.0f,
    @ColorInt val color: Int,
    val isFiltered: Boolean,
) : ViewHolderType, StatisticsDetailPreview {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean =
        other is StatisticsDetailPreviewViewData && other.type == type

    enum class Type {
        FILTER,
        COMPARISON,
    }

    enum class DataType {
        ACTIVITY,
        CATEGORY,
        TAG,
        OTHER,
    }
}