package com.example.util.simpletimetracker.domain.statistics.interactor

import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.statistics.model.Statistics
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatisticsCategoryInteractor @Inject constructor(
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val statisticsInteractor: StatisticsInteractor,
) {

    suspend fun getFromRange(
        range: Range,
        addUntracked: Boolean,
        addUncategorized: Boolean,
        showSeconds: Boolean,
    ): List<Statistics> = withContext(Dispatchers.IO) {
        val records = statisticsInteractor.getRecords(range)
        val untrackedRecords = statisticsInteractor.getUntracked(
            range = range,
            records = records,
            addUntracked = addUntracked,
            showSeconds = showSeconds,
        )
        val recordsMap = getCategoryRecords(records + untrackedRecords, addUncategorized)
        statisticsInteractor.getStatistics(
            range = range,
            records = recordsMap,
            showSeconds = showSeconds,
        )
    }

    suspend fun getCategoryRecords(
        allRecords: List<RecordBase>,
        addUncategorized: Boolean,
    ): Map<Long, List<RecordBase>> {
        val recordTypeCategories = recordTypeCategoryInteractor.getAll()
            .groupBy(RecordTypeCategory::recordTypeId)
            .mapValues { it.value.map(RecordTypeCategory::categoryId) }

        val categories: MutableMap<Long, MutableList<RecordBase>> = mutableMapOf()

        allRecords.forEach { record ->
            val isUntracked = record.typeIds.any { it == UNTRACKED_ITEM_ID }
            record.typeIds.forEach { typeId ->
                recordTypeCategories[typeId]?.forEach { category ->
                    categories.getOrPut(category) { mutableListOf() }.add(record)
                }
            }
            if (isUntracked) {
                categories.getOrPut(UNTRACKED_ITEM_ID) { mutableListOf() }.add(record)
            } else if (addUncategorized && record.typeIds.all { it !in recordTypeCategories }) {
                categories.getOrPut(UNCATEGORIZED_ITEM_ID) { mutableListOf() }.add(record)
            }
        }

        return categories
    }
}