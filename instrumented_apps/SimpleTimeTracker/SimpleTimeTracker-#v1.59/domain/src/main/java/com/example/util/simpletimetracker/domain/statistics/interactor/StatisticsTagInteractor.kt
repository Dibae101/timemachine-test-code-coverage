package com.example.util.simpletimetracker.domain.statistics.interactor

import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.statistics.model.Statistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StatisticsTagInteractor @Inject constructor(
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
        val recordsMap = getTagRecords(records + untrackedRecords, addUncategorized)
        statisticsInteractor.getStatistics(
            range = range,
            records = recordsMap,
            showSeconds = showSeconds,
        )
    }

    fun getTagRecords(
        allRecords: List<RecordBase>,
        addUncategorized: Boolean,
    ): Map<Long, List<RecordBase>> {
        val tags: MutableMap<Long, MutableList<RecordBase>> = mutableMapOf()

        allRecords.forEach { record ->
            val isUntracked = record.typeIds.any { it == UNTRACKED_ITEM_ID }
            record.tags.forEach { tag ->
                tags.getOrPut(tag.tagId) { mutableListOf() }.add(record)
            }
            if (isUntracked) {
                tags.getOrPut(UNTRACKED_ITEM_ID) { mutableListOf() }.add(record)
            } else if (addUncategorized && record.tags.isEmpty()) {
                tags.getOrPut(UNCATEGORIZED_ITEM_ID) { mutableListOf() }.add(record)
            }
        }

        return tags
    }
}