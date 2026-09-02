package com.example.util.simpletimetracker.domain.pomodoro.interactor

import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleSettings
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class GetPomodoroSettingsInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
) {

    suspend fun execute(): PomodoroCycleSettings {
        return PomodoroCycleSettings(
            focusTimeMs = prefsInteractor.getPomodoroFocusTime() * 1000L,
            breakTimeMs = prefsInteractor.getPomodoroBreakTime() * 1000L,
            longBreakTimeMs = prefsInteractor.getPomodoroLongBreakTime() * 1000L,
            periodsUntilLongBreak = prefsInteractor.getPomodoroPeriodsUntilLongBreak(),
        )
    }
}