package com.example.util.simpletimetracker.feature_notification.activity.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.notifications.interactor.NotificationActivityInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationActivityBroadcastController @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val notificationActivityInteractor: NotificationActivityInteractor,
) {

    fun onActivityReminder() = allowDiskRead { MainScope() }.launch {
        notificationActivityInteractor.show()
        checkAndSchedule()
    }

    fun onBootCompleted() = allowDiskRead { MainScope() }.launch {
        checkAndSchedule()
    }

    private suspend fun checkAndSchedule() {
        if (prefsInteractor.getActivityReminderRecurrent()) {
            notificationActivityInteractor.checkAndSchedule()
        }
    }
}