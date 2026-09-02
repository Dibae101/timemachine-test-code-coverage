package com.example.util.simpletimetracker.feature_notification.inactivity.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.notifications.interactor.NotificationInactivityInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationInactivityBroadcastController @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val notificationInactivityInteractor: NotificationInactivityInteractor,
) {

    fun onInactivityReminder() = allowDiskRead { MainScope() }.launch {
        notificationInactivityInteractor.show()
        checkAndSchedule()
    }

    fun onBootCompleted() = allowDiskRead { MainScope() }.launch {
        checkAndSchedule()
    }

    private suspend fun checkAndSchedule() {
        if (prefsInteractor.getInactivityReminderRecurrent()) {
            notificationInactivityInteractor.checkAndSchedule()
        }
    }
}