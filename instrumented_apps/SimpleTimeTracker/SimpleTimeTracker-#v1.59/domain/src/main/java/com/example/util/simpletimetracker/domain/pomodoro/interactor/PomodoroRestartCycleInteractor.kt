package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.pomodoro.mapper.PomodoroCycleDurationsMapper
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class PomodoroRestartCycleInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val pomodoroCycleDurationsMapper: PomodoroCycleDurationsMapper,
    private val getPomodoroStateInteractor: GetPomodoroStateInteractor,
    private val getPomodoroSettingsInteractor: GetPomodoroSettingsInteractor,
    private val pomodoroCycleNotificationInteractor: PomodoroCycleNotificationInteractor,
) {

    suspend fun execute() {
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Running) return

        val timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val result = pomodoroCycleDurationsMapper.map(
            timeStartedMs = timeStartedMs,
            settings = getPomodoroSettingsInteractor.execute(),
        )

        val newTimeStarted = timeStartedMs + result.currentCycleDurationMs
        prefsInteractor.setPomodoroModeStartedTimestampMs(newTimeStarted)

        pomodoroCycleNotificationInteractor.checkAndReschedule()
    }
}