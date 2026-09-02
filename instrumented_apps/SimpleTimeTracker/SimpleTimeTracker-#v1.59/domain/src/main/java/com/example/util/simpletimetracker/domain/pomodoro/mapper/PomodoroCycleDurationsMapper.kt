package com.example.util.simpletimetracker.domain.pomodoro.mapper

import com.example.util.simpletimetracker.domain.base.CurrentTimestampProvider
import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleSettings
import com.example.util.simpletimetracker.domain.pomodoro.model.PomodoroCycleType
import javax.inject.Inject

class PomodoroCycleDurationsMapper @Inject constructor(
    private val currentTimestampProvider: CurrentTimestampProvider,
) {

    /**
     * Period is for example focus-break-focus-break-focus-longBreak.
     * Cycle is focus/break/longBreak.
     */
    fun map(
        timeStartedMs: Long,
        settings: PomodoroCycleSettings,
    ): Result {
        val focusTime = settings.focusTimeMs
        val breakTime = settings.breakTimeMs
        val longBreakTime = settings.longBreakTimeMs
        val periodsUntilLongBreak = settings.periodsUntilLongBreak

        val currentTime = currentTimestampProvider.get()
        // Time since pomodoro was started.
        val currentDuration = currentTime - timeStartedMs
        val periodDuration = mapFullPeriodDuration(settings)
        if (periodDuration == 0L) {
            // Avoid divide by zero just in case.
            return Result(
                cycleType = PomodoroCycleType.Focus,
                prevCycleType = PomodoroCycleType.Focus,
                nextCycleType = PomodoroCycleType.Focus,
                currentCycleDurationMs = 0L,
            )
        }
        val currentPeriodDuration = currentDuration % periodDuration
        val currentShortPeriodDuration = currentPeriodDuration % (focusTime + breakTime)

        return if (periodsUntilLongBreak > 0) {
            when {
                // This is long break.
                currentPeriodDuration >= periodDuration - longBreakTime -> Result(
                    cycleType = PomodoroCycleType.LongBreak,
                    prevCycleType = PomodoroCycleType.Focus,
                    nextCycleType = PomodoroCycleType.Focus,
                    currentCycleDurationMs = currentPeriodDuration - (periodDuration - longBreakTime),
                )
                // Focus.
                currentShortPeriodDuration < focusTime -> Result(
                    cycleType = PomodoroCycleType.Focus,
                    prevCycleType = if (currentPeriodDuration <= focusTime) {
                        PomodoroCycleType.LongBreak.takeIf { longBreakTime > 0L }
                    } else {
                        PomodoroCycleType.Break.takeIf { breakTime > 0L }
                    } ?: PomodoroCycleType.Focus,
                    nextCycleType = if (currentPeriodDuration >=
                        periodDuration - longBreakTime - focusTime
                    ) {
                        PomodoroCycleType.LongBreak.takeIf { longBreakTime > 0L }
                    } else {
                        PomodoroCycleType.Break.takeIf { breakTime > 0L }
                    } ?: PomodoroCycleType.Focus,
                    currentCycleDurationMs = currentShortPeriodDuration,
                )
                // Break.
                else -> Result(
                    cycleType = PomodoroCycleType.Break,
                    prevCycleType = PomodoroCycleType.Focus,
                    nextCycleType = PomodoroCycleType.Focus,
                    currentCycleDurationMs = currentShortPeriodDuration - focusTime,
                )
            }
        } else {
            if (currentPeriodDuration < focusTime) {
                // Long breaks are disabled, so prev or next is break if it is enabled.
                val prevOrNextCycle = PomodoroCycleType.Break
                    .takeIf { breakTime > 0L }
                    ?: PomodoroCycleType.Focus
                Result(
                    cycleType = PomodoroCycleType.Focus,
                    prevCycleType = prevOrNextCycle,
                    nextCycleType = prevOrNextCycle,
                    currentCycleDurationMs = currentPeriodDuration,
                )
            } else {
                Result(
                    cycleType = PomodoroCycleType.Break,
                    prevCycleType = PomodoroCycleType.Focus,
                    nextCycleType = PomodoroCycleType.Focus,
                    currentCycleDurationMs = currentPeriodDuration - focusTime,
                )
            }
        }
    }

    fun mapToCycleTime(
        cycleType: PomodoroCycleType,
        settings: PomodoroCycleSettings,
    ): Long {
        return when (cycleType) {
            PomodoroCycleType.Focus -> settings.focusTimeMs
            PomodoroCycleType.Break -> settings.breakTimeMs
            PomodoroCycleType.LongBreak -> settings.longBreakTimeMs
        }
    }

    fun mapFullPeriodDuration(
        settings: PomodoroCycleSettings,
    ): Long {
        return if (settings.periodsUntilLongBreak > 0) {
            settings.focusTimeMs * settings.periodsUntilLongBreak +
                settings.breakTimeMs * (settings.periodsUntilLongBreak - 1) +
                settings.longBreakTimeMs
        } else {
            settings.focusTimeMs + settings.breakTimeMs
        }
    }

    data class Result(
        val cycleType: PomodoroCycleType,
        val prevCycleType: PomodoroCycleType,
        val nextCycleType: PomodoroCycleType,
        val currentCycleDurationMs: Long, // Time spent in current cycle.
    )
}