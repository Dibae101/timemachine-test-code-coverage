package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import javax.inject.Inject

class CalculateAdjacentActivitiesInteractor @Inject constructor(
    private val getMultitaskRecordsInteractor: GetMultitaskRecordsInteractor,
) {

    // TODO count multitask also
    // Doesn't count multitask activities.
    // Only those that started after current ended.
    fun calculateNextActivities(
        typeIds: List<Long>,
        records: List<Record>,
        maxCount: Int,
    ): Map<Long, List<CalculationResult>> {
        val counts = mutableMapOf<Long, MutableMap<Long, Long>>()
        val recordsSorted = records.sortedBy { it.timeStarted }
        val currentRecords: MutableMap<Long, Record> = mutableMapOf()

        recordsSorted.forEach { record ->
            val typeId = record.typeId
            val idsToRemoveFromCurrents = mutableListOf<Long>()

            currentRecords.values.forEach { currentRecord ->
                val currentTimeEnded = currentRecord.timeEnded
                if (currentTimeEnded <= record.timeStarted) {
                    counts[currentRecord.typeId] = counts[currentRecord.typeId]
                        .orEmpty().toMutableMap()
                        .also { it[typeId] = it[typeId].orZero() + 1 }
                    idsToRemoveFromCurrents += currentRecord.typeId
                }
            }

            idsToRemoveFromCurrents.forEach(currentRecords::remove)
            if (typeId !in currentRecords && typeId in typeIds) {
                currentRecords[typeId] = record
            }
        }

        return counts.mapValues { (_, counts) ->
            counts.keys
                .sortedByDescending { counts[it].orZero() }
                .take(maxCount)
                .map { CalculationResult(it, counts[it].orZero()) }
        }
    }

    fun calculateMultitasking(
        typeId: Long,
        records: List<RecordBase>,
        maxCount: Int,
    ): List<CalculationResult> {
        val counts = mutableMapOf<Long, Long>()

        getMultitaskRecordsInteractor.get(records).forEach { record ->
            if (typeId !in record.typeIds) return@forEach
            if (record.duration <= SHORT_MULTITASK_CUTOFF_MS) return@forEach

            record.typeIds.forEach { id ->
                if (id != typeId) {
                    counts[id] = counts[id].orZero() + 1
                }
            }
        }

        return counts.keys
            .sortedByDescending { counts[it].orZero() }
            .take(maxCount)
            .map { CalculationResult(it, counts[it].orZero()) }
    }

    data class CalculationResult(
        val typeId: Long,
        val count: Long,
    )

    companion object {
        private const val SHORT_MULTITASK_CUTOFF_MS = 1_000L
    }
}