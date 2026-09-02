package com.example.util.simpletimetracker.domain.recordAction.interactor

import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.interactor.AddRecordMediator
import javax.inject.Inject

class RecordActionMergeMediator @Inject constructor(
    private val addRecordMediator: AddRecordMediator,
) {

    suspend fun execute(
        prevRecord: Record?,
        newTimeEnded: Long,
        onMergeComplete: () -> Unit,
    ) {
        // If merge would be available but only for untracked - add removal of current record
        prevRecord?.copy(
            timeEnded = newTimeEnded,
        )?.let {
            addRecordMediator.add(it)
            onMergeComplete()
        }
    }
}