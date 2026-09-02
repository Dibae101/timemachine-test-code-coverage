package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class GetPomodoroStateInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
) {

    suspend fun execute(): State {
        val timeStarted = prefsInteractor.getPomodoroModeStartedTimestampMs()
        val timePaused = prefsInteractor.getPomodoroModePausedTimestampMs()

        return when {
            timeStarted == 0L -> State.Stopped
            timePaused != 0L -> State.Paused
            else -> State.Running
        }
    }

    sealed interface State {
        data object Stopped : State
        data object Paused : State
        data object Running : State
    }
}