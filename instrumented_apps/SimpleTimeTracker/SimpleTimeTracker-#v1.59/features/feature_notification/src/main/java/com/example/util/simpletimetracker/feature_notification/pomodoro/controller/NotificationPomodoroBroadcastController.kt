package com.example.util.simpletimetracker.feature_notification.pomodoro.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroCycleNotificationInteractor
import com.example.util.simpletimetracker.feature_notification.pomodoro.interactor.ShowPomodoroNotificationInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationPomodoroBroadcastController @Inject constructor(
    private val showPomodoroNotificationInteractor: ShowPomodoroNotificationInteractor,
    private val pomodoroCycleNotificationInteractor: PomodoroCycleNotificationInteractor,
) {

    fun onReminder(
        cycleType: Long,
    ) = allowDiskRead { MainScope() }.launch {
        showPomodoroNotificationInteractor.show(cycleType)
        checkAndSchedule()
    }

    fun onBootCompleted() = allowDiskRead { MainScope() }.launch {
        checkAndSchedule()
    }

    fun onExactAlarmPermissionStateChanged() = allowDiskRead { MainScope() }.launch {
        checkAndSchedule()
    }

    private suspend fun checkAndSchedule() {
        pomodoroCycleNotificationInteractor.checkAndReschedule()
    }
}