package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.viewData.StatisticsDataHolder
import com.example.util.simpletimetracker.domain.base.DurationFormat
import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.domain.statistics.model.Statistics
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData
import com.example.util.simpletimetracker.feature_views.TransitionNames
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import javax.inject.Inject

class StatisticsViewDataMapper @Inject constructor(
    private val iconMapper: IconMapper,
    private val colorMapper: ColorMapper,
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
    private val statisticsMapper: StatisticsMapper,
) {

    fun mapItemsList(
        shift: Int,
        statistics: List<Statistics>,
        data: Map<Long, StatisticsDataHolder>,
        filterType: ChartFilterType,
        filteredIds: List<Long>,
        showDuration: Boolean,
        isDarkTheme: Boolean,
        durationFormat: DurationFormat,
        showSeconds: Boolean,
        hasTransitions: Boolean,
    ): List<StatisticsViewData> {
        val statisticsFiltered = statistics.filterNot { it.id in filteredIds }
        val sumDuration = statisticsFiltered.sumOf { it.data.duration }
        val statisticsSize = statisticsFiltered.size

        return statisticsFiltered
            .mapNotNull { statistic ->
                val item = mapItem(
                    shift = shift,
                    filterType = filterType,
                    statistics = statistic,
                    sumDuration = sumDuration,
                    dataHolder = data[statistic.id],
                    showDuration = showDuration,
                    isDarkTheme = isDarkTheme,
                    statisticsSize = statisticsSize,
                    durationFormat = durationFormat,
                    showSeconds = showSeconds,
                    hasTransitions = hasTransitions,
                ) ?: return@mapNotNull null

                item to statistic.data.duration
            }
            .sortedByDescending { (_, duration) -> duration }
            .map { (statistics, _) -> statistics }
    }

    private fun mapItem(
        shift: Int,
        filterType: ChartFilterType,
        statistics: Statistics,
        sumDuration: Long,
        dataHolder: StatisticsDataHolder?,
        showDuration: Boolean,
        isDarkTheme: Boolean,
        statisticsSize: Int,
        durationFormat: DurationFormat,
        showSeconds: Boolean,
        hasTransitions: Boolean,
    ): StatisticsViewData? {
        val durationPercent = statisticsMapper.getDurationPercentString(
            sumDuration = sumDuration,
            duration = statistics.data.duration,
            statisticsSize = statisticsSize,
        )
        val transitionName = if (hasTransitions) {
            "${TransitionNames.STATISTICS_DETAIL}_shift${shift}_id${statistics.id}"
        } else {
            null
        }
        val duration = mapDuration(
            statistics = statistics,
            showDuration = showDuration,
            showSeconds = showSeconds,
            durationFormat = durationFormat,
        )

        return when {
            statistics.id == UNTRACKED_ITEM_ID -> {
                StatisticsViewData(
                    id = statistics.id,
                    name = R.string.untracked_time_name
                        .let(resourceRepo::getString),
                    duration = duration,
                    percent = durationPercent,
                    icon = RecordTypeIcon.Image(R.drawable.unknown),
                    color = colorMapper.toUntrackedColor(isDarkTheme),
                    transitionName = transitionName,
                )
            }
            statistics.id == UNCATEGORIZED_ITEM_ID -> {
                StatisticsViewData(
                    id = statistics.id,
                    name = if (filterType == ChartFilterType.RECORD_TAG) {
                        R.string.change_record_untagged
                    } else {
                        R.string.uncategorized_time_name
                    }.let(resourceRepo::getString),
                    duration = duration,
                    percent = durationPercent,
                    icon = RecordTypeIcon.Image(R.drawable.untagged),
                    color = colorMapper.toUntrackedColor(isDarkTheme),
                    transitionName = transitionName,
                )
            }
            dataHolder != null -> {
                StatisticsViewData(
                    id = statistics.id,
                    name = dataHolder.name,
                    duration = duration,
                    percent = durationPercent,
                    icon = dataHolder.icon
                        ?.let(iconMapper::mapIcon),
                    color = dataHolder.color
                        .let { colorMapper.mapToColorInt(it, isDarkTheme) },
                    transitionName = transitionName,
                )
            }
            else -> null
        }
    }

    private fun mapDuration(
        statistics: Statistics,
        showDuration: Boolean,
        showSeconds: Boolean,
        durationFormat: DurationFormat,
    ): String {
        return if (showDuration) {
            timeMapper.formatInterval(
                interval = statistics.data.duration,
                forceSeconds = showSeconds,
                durationFormat = durationFormat,
            )
        } else {
            ""
        }
    }
}