package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

object StatisticsDetailPreviewCompareViewData : ViewHolderType, StatisticsDetailPreview {

    // Only one item in recycler
    override fun getUniqueId(): Long = 1L

    override fun isValidType(other: ViewHolderType): Boolean =
        other is StatisticsDetailPreviewCompareViewData
}