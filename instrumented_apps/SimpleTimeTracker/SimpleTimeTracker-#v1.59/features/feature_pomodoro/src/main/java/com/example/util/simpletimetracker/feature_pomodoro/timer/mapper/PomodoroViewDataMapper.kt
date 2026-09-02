package com.example.util.simpletimetracker.feature_pomodoro.timer.mapper

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.extension.toDuration
import com.example.util.simpletimetracker.domain.pomodoro.interactor.GetPomodoroStateInteractor
import com.example.util.simpletimetracker.domain.pomodoro.mapper.PomodoroCycleDurationsMapper
import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleSettings
import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleType
import com.example.util.simpletimetracker.feature_pomodoro.R
import com.example.util.simpletimetracker.feature_pomodoro.timer.model.PomodoroButtonState
import com.example.util.simpletimetracker.feature_pomodoro.timer.model.PomodoroTimerState
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PomodoroViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val pomodoroCycleDurationsMapper: PomodoroCycleDurationsMapper,
) {

    fun mapButtonState(
        showMoreControls: Boolean,
        state: GetPomodoroStateInteractor.State,
    ): PomodoroButtonState {
        val isStarted = state is GetPomodoroStateInteractor.State.Running
        val iconResId = if (isStarted) {
            R.drawable.button_stop
        } else {
            R.drawable.button_play
        }
        val buttonsOrder = if (showMoreControls) {
            listOf(
                PomodoroButtonState.Button.Prev,
                PomodoroButtonState.Button.Start,
                PomodoroButtonState.Button.Next,
                PomodoroButtonState.Button.Restart,
                PomodoroButtonState.Button.Pause,
            )
        } else {
            listOf(
                PomodoroButtonState.Button.Restart,
                PomodoroButtonState.Button.Start,
                PomodoroButtonState.Button.Next,
            )
        }

        return PomodoroButtonState(
            iconResId = iconResId,
            additionalButtonsVisible = isStarted,
            buttonsOrder = buttonsOrder,
        )
    }

    fun mapTimerState(
        state: GetPomodoroStateInteractor.State,
        timeStartedMs: Long,
        timePausedMs: Long,
        timerUpdateMs: Long,
        settings: PomodoroCycleSettings,
    ): PomodoroTimerState {
        // Min increment of one pixel.
        val maxProgress = 1024 * Math.PI
        val isStarted = state !is GetPomodoroStateInteractor.State.Stopped
        val isPaused = state is GetPomodoroStateInteractor.State.Paused

        return if (isStarted) {
            val timerUpdate = if (isPaused) 0 else timerUpdateMs
            val result = pomodoroCycleDurationsMapper.map(
                timeStartedMs = if (isPaused) {
                    timeStartedMs + System.currentTimeMillis() - timePausedMs
                } else {
                    timeStartedMs
                },
                settings = settings,
            )
            val currentCycle = result.cycleType
            val cycleDuration = pomodoroCycleDurationsMapper.mapToCycleTime(
                cycleType = result.cycleType,
                settings = settings,
            )
            val currentCycleDuration = result.currentCycleDurationMs

            val timeLeft = cycleDuration - currentCycleDuration
            val progression = (currentCycleDuration + timerUpdate) *
                maxProgress / cycleDuration
            val times = formatInterval(timeLeft)

            PomodoroTimerState(
                maxProgress = maxProgress.toInt(),
                progress = progression.toInt(),
                timerUpdateMs = timerUpdate,
                durationState = mapToDurationState(times),
                currentCycleHint = mapCurrentStateHint(currentCycle),
            )
        } else {
            val times = formatInterval(settings.focusTimeMs)
            val currentCycle = PomodoroCycleType.Focus

            PomodoroTimerState(
                maxProgress = maxProgress.toInt(),
                progress = 0,
                timerUpdateMs = timerUpdateMs,
                durationState = mapToDurationState(times),
                currentCycleHint = mapCurrentStateHint(currentCycle),
            )
        }
    }

    private fun mapToDurationState(
        data: TimeState,
    ): PomodoroTimerState.DurationState {
        return PomodoroTimerState.DurationState(
            textHours = data.hr.toDuration(),
            textMinutes = data.min.toDuration(),
            textSeconds = data.sec.toDuration(),
            hoursIsVisible = data.hr > 0,
        )
    }

    private fun formatInterval(interval: Long): TimeState {
        val hr: Long = TimeUnit.MILLISECONDS.toHours(
            interval,
        )
        val min: Long = TimeUnit.MILLISECONDS.toMinutes(
            interval - TimeUnit.HOURS.toMillis(hr),
        )
        val sec: Long = TimeUnit.MILLISECONDS.toSeconds(
            interval - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min),
        )
        return TimeState(
            hr = hr,
            min = min,
            sec = sec,
        )
    }

    private fun mapCurrentStateHint(
        cycleType: PomodoroCycleType,
    ): String {
        return when (cycleType) {
            is PomodoroCycleType.Focus -> R.string.pomodoro_state_focus
            is PomodoroCycleType.Break -> R.string.pomodoro_state_break
            is PomodoroCycleType.LongBreak -> R.string.pomodoro_state_long_break
        }.let(resourceRepo::getString)
    }

    private data class TimeState(
        val hr: Long,
        val min: Long,
        val sec: Long,
    )
}