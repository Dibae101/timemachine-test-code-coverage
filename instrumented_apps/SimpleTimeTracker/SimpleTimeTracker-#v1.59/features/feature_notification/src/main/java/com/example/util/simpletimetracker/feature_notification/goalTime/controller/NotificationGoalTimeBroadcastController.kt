package com.example.util.simpletimetracker.feature_notification.goalTime.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.notifications.interactor.NotificationGoalTimeInteractor
import com.example.util.simpletimetracker.domain.notifications.interactor.UpdateExternalViewsInteractor
import com.example.util.simpletimetracker.domain.recordType.model.RecordTypeGoal
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationGoalTimeBroadcastController @Inject constructor(
    private val notificationGoalTimeInteractor: NotificationGoalTimeInteractor,
    private val externalViewsInteractor: UpdateExternalViewsInteractor,
) {

    fun onGoalTimeReminder(
        idData: RecordTypeGoal.IdData,
        goalRange: RecordTypeGoal.Range,
    ) {
        allowDiskRead { MainScope() }.launch {
            notificationGoalTimeInteractor.show(idData, goalRange)
            if (idData is RecordTypeGoal.IdData.Type) {
                externalViewsInteractor.onGoalTimeReached(idData.value)
            }
        }
    }

    fun onRangeEndReminder() {
        allowDiskRead { MainScope() }.launch {
            reschedule()
            externalViewsInteractor.onGoalRangeEnd()
        }
    }

    fun onBootCompleted() {
        allowDiskRead { MainScope() }.launch {
            reschedule()
        }
    }

    fun onExactAlarmPermissionStateChanged() {
        allowDiskRead { MainScope() }.launch {
            reschedule()
        }
    }

    private suspend fun reschedule() {
        notificationGoalTimeInteractor.checkAndReschedule()
    }
}