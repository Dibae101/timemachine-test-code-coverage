package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.RecordTagViewDataMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.view.dayCalendar.DayCalendarViewData
import com.example.util.simpletimetracker.domain.record.extension.getCategoryItems
import com.example.util.simpletimetracker.domain.record.extension.getSelectedTags
import com.example.util.simpletimetracker.domain.record.extension.hasUncategorizedItem
import com.example.util.simpletimetracker.domain.record.extension.hasUntaggedItem
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.statistics.interactor.StatisticsCategoryInteractor
import com.example.util.simpletimetracker.domain.statistics.interactor.StatisticsTagInteractor
import com.example.util.simpletimetracker.domain.record.mapper.RangeMapper
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_statistics_detail.mapper.StatisticsDetailDailyCalendarViewDataMapper
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailDayCalendarViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class StatisticsDetailDailyCalendarViewDataInteractor @Inject constructor(
    private val timeMapper: TimeMapper,
    private val rangeMapper: RangeMapper,
    private val colorMapper: ColorMapper,
    private val prefsInteractor: PrefsInteractor,
    private val statisticsCategoryInteractor: StatisticsCategoryInteractor,
    private val statisticsTagInteractor: StatisticsTagInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTagViewDataMapper: RecordTagViewDataMapper,
    private val statisticsDetailPreviewInteractor: StatisticsDetailPreviewInteractor,
    private val mapper: StatisticsDetailDailyCalendarViewDataMapper,
) {

    fun getEmptyChartViewData(
        rangeLength: RangeLength,
    ): List<ViewHolderType> {
        if (rangeLength != RangeLength.Day) return emptyList()
        return listOf(
            mapper.mapToHint(),
            mapper.mapToEmpty(),
        )
    }

    suspend fun getViewData(
        records: List<RecordBase>,
        compareRecords: List<RecordBase>,
        filter: List<RecordsFilter>,
        compare: List<RecordsFilter>,
        rangeLength: RangeLength,
        rangePosition: Int,
    ): List<ViewHolderType> = withContext(Dispatchers.Default) {
        if (rangeLength != RangeLength.Day) return@withContext emptyList()

        return@withContext listOfNotNull(
            mapper.mapToHint(),
            getViewData(
                records = records,
                filter = filter,
                rangeLength = rangeLength,
                rangePosition = rangePosition,
                isForComparison = false,
            ),
            getViewData(
                records = compareRecords,
                filter = compare,
                rangeLength = rangeLength,
                rangePosition = rangePosition,
                isForComparison = true,
            ),
        )
    }

    private suspend fun getViewData(
        records: List<RecordBase>,
        filter: List<RecordsFilter>,
        rangeLength: RangeLength,
        rangePosition: Int,
        isForComparison: Boolean,
    ): ViewHolderType? {
        if (isForComparison && filter.isEmpty()) return null

        val calendar = Calendar.getInstance()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()

        val range = timeMapper.getRangeStartAndEnd(
            rangeLength = rangeLength,
            shift = rangePosition,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift,
        )
        val recordsFromRange = rangeMapper.getRecordsFromRange(records, range)
            .map { rangeMapper.clampRecordToRange(it, range) }
        val previewType =
            statisticsDetailPreviewInteractor.getPreviewType(filter)
        val data = getData(
            previewType = previewType,
            records = recordsFromRange,
            filter = filter,
            isDarkTheme = isDarkTheme,
        ).map {
            mapper.mapToCalendarPoint(
                holder = it,
                calendar = calendar,
                startOfDayShift = startOfDayShift,
            )
        }

        return StatisticsDetailDayCalendarViewData(
            data = DayCalendarViewData(data),
        )
    }

    private suspend fun getData(
        previewType: StatisticsDetailPreviewInteractor.PreviewType,
        records: List<RecordBase>,
        filter: List<RecordsFilter>,
        isDarkTheme: Boolean,
    ): List<RecordHolder> {
        val untrackedColor = colorMapper.toUntrackedColor(isDarkTheme)

        return when (previewType) {
            is StatisticsDetailPreviewInteractor.PreviewType.Untracked,
            is StatisticsDetailPreviewInteractor.PreviewType.Multitask,
            -> getUndefinedData(
                records = records,
                untrackedColor = untrackedColor,
            )
            is StatisticsDetailPreviewInteractor.PreviewType.Activities,
            is StatisticsDetailPreviewInteractor.PreviewType.ActivitiesFromRecords,
            -> getActivitiesData(
                records = records,
                untrackedColor = untrackedColor,
                isDarkTheme = isDarkTheme,
            )
            is StatisticsDetailPreviewInteractor.PreviewType.Categories,
            -> getCategoriesData(
                records = records,
                filter = filter,
                untrackedColor = untrackedColor,
                isDarkTheme = isDarkTheme,
            )
            is StatisticsDetailPreviewInteractor.PreviewType.SelectedTags,
            -> getTagsData(
                records = records,
                filter = filter,
                untrackedColor = untrackedColor,
                isDarkTheme = isDarkTheme,
            )
        }
    }

    private fun getUndefinedData(
        records: List<RecordBase>,
        untrackedColor: Int,
    ): List<RecordHolder> {
        return records.map { record ->
            mapper.mapRecordHolder(record, untrackedColor)
        }
    }

    private suspend fun getActivitiesData(
        records: List<RecordBase>,
        untrackedColor: Int,
        isDarkTheme: Boolean,
    ): List<RecordHolder> {
        val types = recordTypeInteractor.getAll()
            .associateBy { it.id }

        return records.map { record ->
            val typeId = record.typeIds.firstOrNull().orZero()
            val color = types[typeId]?.color
                ?.let { colorMapper.mapToColorInt(it, isDarkTheme) }
                ?: untrackedColor
            mapper.mapRecordHolder(record, color)
        }
    }

    private suspend fun getCategoriesData(
        records: List<RecordBase>,
        filter: List<RecordsFilter>,
        untrackedColor: Int,
        isDarkTheme: Boolean,
    ): List<RecordHolder> {
        val categories = categoryInteractor.getAll()
            .associateBy(Category::id)

        return statisticsCategoryInteractor.getCategoryRecords(
            allRecords = records,
            addUncategorized = filter.getCategoryItems().hasUncategorizedItem(),
        ).flatMap { (categoryId, records) ->
            records.map { record ->
                val color = categories[categoryId]?.color
                    ?.let { colorMapper.mapToColorInt(it, isDarkTheme) }
                    ?: untrackedColor
                mapper.mapRecordHolder(record, color)
            }
        }
    }

    private suspend fun getTagsData(
        records: List<RecordBase>,
        filter: List<RecordsFilter>,
        untrackedColor: Int,
        isDarkTheme: Boolean,
    ): List<RecordHolder> {
        val tags = recordTagInteractor.getAll()
            .associateBy(RecordTag::id)
        val types = recordTypeInteractor.getAll()
            .associateBy(RecordType::id)

        return statisticsTagInteractor.getTagRecords(
            allRecords = records,
            addUncategorized = filter.getSelectedTags().hasUntaggedItem(),
        ).flatMap { (tagId, records) ->
            records.map { record ->
                val color = tags[tagId]
                    ?.let { recordTagViewDataMapper.mapColor(it, types) }
                    ?.let { colorMapper.mapToColorInt(it, isDarkTheme) }
                    ?: untrackedColor
                mapper.mapRecordHolder(record, color)
            }
        }
    }

    data class RecordHolder(
        val timeStartedTimestamp: Long,
        val timeEndedTimestamp: Long,
        @ColorInt val color: Int,
    )
}