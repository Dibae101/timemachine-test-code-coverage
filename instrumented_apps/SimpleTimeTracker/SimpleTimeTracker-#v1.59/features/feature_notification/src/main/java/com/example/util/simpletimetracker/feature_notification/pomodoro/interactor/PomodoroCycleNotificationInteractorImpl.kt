package com.example.util.simpletimetracker.feature_notification.pomodoro.interactor

import com.example.util.simpletimetracker.domain.pomodoro.interactor.GetPomodoroSettingsInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.GetPomodoroStateInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroCycleNotificationInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.pomodoro.mapper.PomodoroCycleDurationsMapper
import com.example.util.simpletimetracker.feature_notification.pomodoro.manager.NotificationPomodoroManager
import com.example.util.simpletimetracker.feature_notification.pomodoro.scheduler.NotificationPomodoroScheduler
import javax.inject.Inject

class PomodoroCycleNotificationInteractorImpl @Inject constructor(
    private val manager: NotificationPomodoroManager,
    private val scheduler: NotificationPomodoroScheduler,
    private val pomodoroCycleDurationsMapper: PomodoroCycleDurationsMapper,
    private val getPomodoroStateInteractor: GetPomodoroStateInteractor,
    private val getPomodoroSettingsInteractor: GetPomodoroSettingsInteractor,
    private val prefsInteractor: PrefsInteractor,
) : PomodoroCycleNotificationInteractor {

    override suspend fun checkAndReschedule() {
        scheduler.cancelSchedule()
        if (!prefsInteractor.getEnablePomodoroMode()) return
        val state = getPomodoroStateInteractor.execute()
        if (state !is GetPomodoroStateInteractor.State.Running) return

        val timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val settings = getPomodoroSettingsInteractor.execute()
        val result = pomodoroCycleDurationsMapper.map(
            timeStartedMs = timeStartedMs,
            settings = settings,
        )
        val cycleDurationMs = pomodoroCycleDurationsMapper.mapToCycleTime(
            cycleType = result.cycleType,
            settings = settings,
        )
        val timeLeft = cycleDurationMs - result.currentCycleDurationMs
        scheduler.schedule(
            timestamp = System.currentTimeMillis() + timeLeft,
            cycleType = result.nextCycleType,
        )
    }

    override fun cancel() {
        scheduler.cancelSchedule()
        manager.hide()
    }
}