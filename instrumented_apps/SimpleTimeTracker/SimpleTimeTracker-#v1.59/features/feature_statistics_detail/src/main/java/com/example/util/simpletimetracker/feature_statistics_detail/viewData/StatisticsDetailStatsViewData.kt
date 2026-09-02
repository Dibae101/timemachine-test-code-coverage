package com.example.util.simpletimetracker.feature_statistics_detail.viewData

data class StatisticsDetailStatsViewData(
    val totalDuration: List<StatisticsDetailCardInternalViewData>,
    val timesTracked: List<StatisticsDetailCardInternalViewData>,
    val averageRecord: List<StatisticsDetailCardInternalViewData>,
    val datesTracked: List<StatisticsDetailCardInternalViewData>,
)