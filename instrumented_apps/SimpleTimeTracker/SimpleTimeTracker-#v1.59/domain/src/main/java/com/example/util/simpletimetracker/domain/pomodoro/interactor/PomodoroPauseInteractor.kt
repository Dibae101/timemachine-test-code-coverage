package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class PomodoroPauseInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val pomodoroCycleNotificationInteractor: PomodoroCycleNotificationInteractor,
    private val getPomodoroStateInteractor: GetPomodoroStateInteractor,
) {

    suspend fun pause() {
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Running) return

        val current = System.currentTimeMillis()
        prefsInteractor.setPomodoroModePausedTimestampMs(current)
        pomodoroCycleNotificationInteractor.cancel()
    }

    suspend fun startAfterPause() {
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Paused) return

        val current = System.currentTimeMillis()
        val timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val timePausedMs = prefsInteractor.getPomodoroModePausedTimestampMs()
        val newTimeStarted = timeStartedMs + current - timePausedMs
        prefsInteractor.setPomodoroModeStartedTimestampMs(newTimeStarted)
        prefsInteractor.setPomodoroModePausedTimestampMs(0)
        pomodoroCycleNotificationInteractor.checkAndReschedule()
    }
}