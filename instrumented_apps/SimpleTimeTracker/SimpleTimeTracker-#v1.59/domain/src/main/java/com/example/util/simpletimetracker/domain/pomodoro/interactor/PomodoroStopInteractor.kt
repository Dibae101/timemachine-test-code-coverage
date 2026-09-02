package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import javax.inject.Inject

class PomodoroStopInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val pomodoroCycleNotificationInteractor: PomodoroCycleNotificationInteractor,
) {

    suspend fun stop() {
        prefsInteractor.setPomodoroModeStartedTimestampMs(0)
        prefsInteractor.setPomodoroModePausedTimestampMs(0)
        pomodoroCycleNotificationInteractor.cancel()
    }

    suspend fun checkAndStop(typeId: Long) {
        if (!prefsInteractor.getEnablePomodoroMode()) return

        val currentRunningRecords = runningRecordInteractor.getAll().map { it.id }
        val typesWithAutoStart = prefsInteractor.getAutostartPomodoroActivities()

        // If was auto started and when stopped.
        if (typeId in typesWithAutoStart &&
            currentRunningRecords.none { it in typesWithAutoStart }
        ) {
            stop()
        }
    }
}