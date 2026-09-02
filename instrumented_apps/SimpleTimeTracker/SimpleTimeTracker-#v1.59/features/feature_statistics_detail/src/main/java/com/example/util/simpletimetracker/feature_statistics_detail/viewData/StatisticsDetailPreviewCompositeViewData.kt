package com.example.util.simpletimetracker.feature_statistics_detail.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class StatisticsDetailPreviewCompositeViewData(
    val previewColor: Int?,
    val comparisonPreviewColor: Int?,
    val mainPreview: StatisticsDetailPreviewViewData?,
    val additionalData: List<ViewHolderType>,
    val comparisonData: List<ViewHolderType>,
)