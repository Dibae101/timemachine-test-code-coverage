package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.base.CurrentTimestampProvider
import com.example.util.simpletimetracker.domain.daysOfWeek.mapper.DaysInCalendarMapper
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DaysInCalendar
import com.example.util.simpletimetracker.domain.extension.orZero
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.ceil

class CalendarToListShiftMapper @Inject constructor(
    private val timeMapper: TimeMapper,
    private val daysInCalendarMapper: DaysInCalendarMapper,
    private val currentTimestampProvider: CurrentTimestampProvider,
) {

    fun mapCalendarToListShift(
        calendarShift: Int,
        daysInCalendar: DaysInCalendar,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): CalendarRange {
        val calendarDayCount = daysInCalendarMapper.mapDaysCount(daysInCalendar)
        if (calendarDayCount == 0) return CalendarRange(0, 0)

        val weekShift = getWeekShift(
            daysInCalendar = daysInCalendar,
            startOfDayShift = startOfDayShift,
            firstDayOfWeek = firstDayOfWeek,
        )

        val end = calendarShift * calendarDayCount + weekShift
        val start = end - (calendarDayCount - 1)

        return CalendarRange(start, end)
    }

    fun mapListToCalendarShift(
        listShift: Int,
        daysInCalendar: DaysInCalendar,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): Int {
        val calendarDayCount = daysInCalendarMapper.mapDaysCount(daysInCalendar)
        if (calendarDayCount == 0) return 0

        val weekShift = getWeekShift(
            daysInCalendar = daysInCalendar,
            startOfDayShift = startOfDayShift,
            firstDayOfWeek = firstDayOfWeek,
        )
        val actualListShift = listShift - weekShift

        return ceil(actualListShift.toFloat() / calendarDayCount).toInt()
    }

    fun recalculateRangeOnCalendarViewSwitched(
        currentPosition: Int,
        lastListPosition: Int,
        showCalendar: Boolean,
        daysInCalendar: DaysInCalendar,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): Int {
        return if (showCalendar) {
            mapListToCalendarShift(
                listShift = currentPosition,
                daysInCalendar = daysInCalendar,
                startOfDayShift = startOfDayShift,
                firstDayOfWeek = firstDayOfWeek,
            )
        } else {
            val calendarRange = mapCalendarToListShift(
                calendarShift = currentPosition,
                daysInCalendar = daysInCalendar,
                startOfDayShift = startOfDayShift,
                firstDayOfWeek = firstDayOfWeek,
            )
            if (lastListPosition in (calendarRange.start..calendarRange.end)) {
                lastListPosition
            } else if (0 in (calendarRange.start..calendarRange.end)) {
                0
            } else {
                calendarRange.end
            }
        }
    }

    fun recalculateRangeOnCalendarDaysChanged(
        currentPosition: Int,
        currentDaysInCalendar: DaysInCalendar,
        newDaysInCalendar: DaysInCalendar,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): Int {
        // Find another range that contains last day of current range.
        // Or today if range contains today.
        val listPosition = if (currentPosition == 0) {
            0
        } else {
            mapCalendarToListShift(
                calendarShift = currentPosition,
                daysInCalendar = currentDaysInCalendar,
                startOfDayShift = startOfDayShift,
                firstDayOfWeek = firstDayOfWeek,
            ).end
        }
        return mapListToCalendarShift(
            listShift = listPosition,
            daysInCalendar = newDaysInCalendar,
            startOfDayShift = startOfDayShift,
            firstDayOfWeek = firstDayOfWeek,
        )
    }

    // How many days until week end.
    // Week starts on Monday, today Tuesday, result 5.
    // Week starts on Monday, today Monday, result 6.
    // Week starts on Monday, today Sunday, result 0.
    private fun getWeekShift(
        daysInCalendar: DaysInCalendar,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): Int {
        return if (daysInCalendar == DaysInCalendar.WEEK) {
            val weekOrder = timeMapper.getWeekOrder(firstDayOfWeek)
            val currentDayOfWeek = timeMapper.getDayOfWeek(
                timestamp = currentTimestampProvider.get(),
                calendar = Calendar.getInstance(),
                startOfDayShift = startOfDayShift,
            )
            weekOrder.size - weekOrder.indexOf(currentDayOfWeek).takeUnless { it == -1 }.orZero() - 1
        } else {
            0
        }
    }

    // Values are days from today.
    // -6:0 last 7 days, 1:7 next 7 days.
    data class CalendarRange(
        val start: Int,
        val end: Int,
    )
}