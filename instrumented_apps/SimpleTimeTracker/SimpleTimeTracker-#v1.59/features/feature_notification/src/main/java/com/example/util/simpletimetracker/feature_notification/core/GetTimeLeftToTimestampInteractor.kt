package com.example.util.simpletimetracker.feature_notification.core

import com.example.util.simpletimetracker.core.extension.shiftTimeStamp
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import java.util.Calendar
import javax.inject.Inject

class GetTimeLeftToTimestampInteractor @Inject constructor(
    private val timeMapper: TimeMapper,
) {

    fun execute(
        dayTimestamp: Long,
    ): Long {
        val calendar = Calendar.getInstance()
        val current = System.currentTimeMillis()

        val triggerTime = calendar.shiftTimeStamp(
            timestamp = timeMapper.getStartOfDayTimeStamp(),
            shift = dayTimestamp,
        ).let {
            if (it > current) {
                it
            } else {
                calendar.timeInMillis = it
                calendar.apply { add(Calendar.DATE, 1) }
                calendar.timeInMillis
            }
        }

        return triggerTime - current
    }
}