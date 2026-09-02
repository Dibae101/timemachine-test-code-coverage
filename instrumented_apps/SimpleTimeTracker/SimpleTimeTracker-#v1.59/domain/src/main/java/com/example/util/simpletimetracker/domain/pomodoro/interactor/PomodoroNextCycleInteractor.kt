package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.base.CurrentTimestampProvider
import com.example.util.simpletimetracker.domain.pomodoro.mapper.PomodoroCycleDurationsMapper
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class PomodoroNextCycleInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val currentTimestampProvider: CurrentTimestampProvider,
    private val pomodoroCycleDurationsMapper: PomodoroCycleDurationsMapper,
    private val getPomodoroStateInteractor: GetPomodoroStateInteractor,
    private val getPomodoroSettingsInteractor: GetPomodoroSettingsInteractor,
    private val pomodoroCycleNotificationInteractor: PomodoroCycleNotificationInteractor,
) {

    suspend fun executePrev() {
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Running) return

        val timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val settings = getPomodoroSettingsInteractor.execute()
        val result = pomodoroCycleDurationsMapper.map(
            timeStartedMs = timeStartedMs,
            settings = settings,
        )
        val currentTime = currentTimestampProvider.get()
        val prevCycleDurationMs = pomodoroCycleDurationsMapper.mapToCycleTime(
            cycleType = result.prevCycleType,
            settings = settings,
        )
        var newTimeStarted = timeStartedMs +
            result.currentCycleDurationMs +
            prevCycleDurationMs + 1 // Just in case.
        if (newTimeStarted > currentTime) {
            // Compensate one period if went to future.
            newTimeStarted -= pomodoroCycleDurationsMapper.mapFullPeriodDuration(settings)
        }

        prefsInteractor.setPomodoroModeStartedTimestampMs(newTimeStarted)

        pomodoroCycleNotificationInteractor.checkAndReschedule()
    }

    suspend fun executeNext() {
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Running) return

        val timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val settings = getPomodoroSettingsInteractor.execute()
        val result = pomodoroCycleDurationsMapper.map(
            timeStartedMs = timeStartedMs,
            settings = getPomodoroSettingsInteractor.execute(),
        )
        val cycleDurationMs = pomodoroCycleDurationsMapper.mapToCycleTime(
            cycleType = result.cycleType,
            settings = settings,
        )
        val newTimeStarted = timeStartedMs +
            result.currentCycleDurationMs -
            cycleDurationMs + 1 // Just in case.

        prefsInteractor.setPomodoroModeStartedTimestampMs(newTimeStarted)

        pomodoroCycleNotificationInteractor.checkAndReschedule()
    }
}