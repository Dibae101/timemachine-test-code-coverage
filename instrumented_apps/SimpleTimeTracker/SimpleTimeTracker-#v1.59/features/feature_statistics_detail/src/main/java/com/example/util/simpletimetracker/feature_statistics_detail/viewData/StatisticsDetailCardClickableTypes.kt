package com.example.util.simpletimetracker.feature_statistics_detail.viewData

object StatisticsDetailClickableTracked : StatisticsDetailCardInternalViewData.ClickableType

data class StatisticsDetailClickablePopup(
    val message: String,
) : StatisticsDetailCardInternalViewData.ClickableType
