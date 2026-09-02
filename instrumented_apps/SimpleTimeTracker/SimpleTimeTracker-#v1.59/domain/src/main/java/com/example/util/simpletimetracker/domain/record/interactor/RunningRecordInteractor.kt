package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.model.RunningRecord
import com.example.util.simpletimetracker.domain.record.repo.RunningRecordRepo
import com.example.util.simpletimetracker.domain.recordTag.repo.RunningRecordToRecordTagRepo
import javax.inject.Inject

class RunningRecordInteractor @Inject constructor(
    private val runningRecordRepo: RunningRecordRepo,
    private val runningRecordToRecordTagRepo: RunningRecordToRecordTagRepo,
) {

    suspend fun isEmpty(): Boolean {
        return runningRecordRepo.isEmpty()
    }

    suspend fun getAll(): List<RunningRecord> {
        return runningRecordRepo.getAll()
    }

    suspend fun get(id: Long): RunningRecord? {
        return runningRecordRepo.get(id)
    }

    suspend fun has(id: Long): Boolean {
        return runningRecordRepo.has(id)
    }

    suspend fun add(runningRecord: RunningRecord) {
        val recordId = runningRecordRepo.add(runningRecord)
        updateTags(recordId, runningRecord.tags)
    }

    suspend fun remove(id: Long) {
        runningRecordToRecordTagRepo.removeAllByRunningRecordId(id)
        runningRecordRepo.remove(id)
    }

    private suspend fun updateTags(
        recordId: Long,
        tags: List<RecordBase.Tag>,
    ) {
        runningRecordToRecordTagRepo.removeAllByRunningRecordId(recordId)
        runningRecordToRecordTagRepo.addRunningRecordTags(recordId, tags)
    }
}