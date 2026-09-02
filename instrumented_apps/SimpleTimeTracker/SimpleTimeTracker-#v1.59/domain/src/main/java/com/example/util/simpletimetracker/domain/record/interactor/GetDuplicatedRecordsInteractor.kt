package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.record.extension.hasSameActivity
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import javax.inject.Inject

class GetDuplicatedRecordsInteractor @Inject constructor() {

    fun execute(
        filters: List<RecordsFilter.DuplicationsItem>,
        records: List<RecordBase>,
    ): Result {
        if (filters.isEmpty()) {
            return Result(
                original = emptyList(),
                duplications = emptyList(),
            )
        }
        val hasSameActivity = filters.hasSameActivity()

        data class Id(
            val typeId: Long,
            val timeStarted: Long,
            val timeEnded: Long,
        )

        val data = mutableMapOf<Id, MutableList<Record>>()
        val result = mutableListOf<Long>()
        val resultDuplications = mutableListOf<Long>()

        // Check duplications by adding to map with data class as key.
        records.forEach { record ->
            if (record !is Record) return@forEach
            val id = Id(
                typeId = if (hasSameActivity) {
                    record.typeIds.firstOrNull() ?: return@forEach
                } else {
                    0L
                },
                // Times are always checked and should be in the list.
                timeStarted = record.timeStarted,
                timeEnded = record.timeEnded,
            )
            data[id] = data.getOrElse(key = id, defaultValue = { mutableListOf() })
                .apply { add(record) }
        }

        data.forEach { (_, duplications) ->
            if (duplications.size < 2) return@forEach
            // This record will not be counted as duplication.
            val originalRecord = duplications.firstOrNull {
                it.tags.isNotEmpty() ||
                    it.comment.isNotEmpty()
            } ?: duplications.firstOrNull()
            duplications.forEach { record ->
                if (record.id == originalRecord?.id) {
                    result += record.id
                } else {
                    resultDuplications += record.id
                }
            }
        }

        return Result(
            original = result,
            duplications = resultDuplications,
        )
    }

    data class Result(
        val original: List<Long>,
        val duplications: List<Long>,
    )
}