package com.example.util.simpletimetracker.domain.recordAction.interactor

import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import javax.inject.Inject

// Repeats concrete record.
class RecordActionRepeatMediator @Inject constructor(
    private val runningRecordInteractor: RunningRecordInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
) {

    suspend fun execute(
        typeId: Long,
        comment: String,
        tags: List<RecordBase.Tag>,
    ) {
        val currentTime = System.currentTimeMillis()
        // Stop same type running record if exist (only one of the same type can run at once).
        // Widgets will update on adding.
        runningRecordInteractor.get(typeId)?.let {
            removeRunningRecordMediator.removeWithRecordAdd(
                runningRecord = it,
                updateWidgets = false,
                timeEnded = currentTime,
            )
        }
        // Add new running record.
        addRunningRecordMediator.startTimer(
            typeId = typeId,
            comment = comment,
            tags = tags,
            timeStarted = AddRunningRecordMediator.StartTime.Current(currentTime),
        )
    }
}