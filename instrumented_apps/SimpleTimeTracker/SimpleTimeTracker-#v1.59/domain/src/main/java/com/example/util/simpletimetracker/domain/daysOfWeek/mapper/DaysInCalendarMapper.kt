package com.example.util.simpletimetracker.domain.daysOfWeek.mapper

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DaysInCalendar
import javax.inject.Inject

class DaysInCalendarMapper @Inject constructor() {

    fun mapDaysCount(daysInCalendar: DaysInCalendar): Int {
        return when (daysInCalendar) {
            DaysInCalendar.ONE -> 1
            DaysInCalendar.THREE -> 3
            DaysInCalendar.FIVE -> 5
            DaysInCalendar.SEVEN -> 7
            DaysInCalendar.WEEK -> 7
        }
    }
}