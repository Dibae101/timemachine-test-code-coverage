package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class StatisticsDetailPreviewMoreViewData(
    val type: StatisticsDetailPreviewViewData.Type,
) : ViewHolderType, StatisticsDetailPreview {

    override fun getUniqueId(): Long = type.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is StatisticsDetailPreviewMoreViewData
}