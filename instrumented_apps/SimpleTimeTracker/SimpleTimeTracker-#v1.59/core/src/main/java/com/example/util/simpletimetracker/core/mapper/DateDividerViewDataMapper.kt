package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.recordsDateDivider.RecordsDateDividerViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordsDateDivider.RecordsDaysBetweenDividerViewData
import java.util.Calendar
import javax.inject.Inject

class DateDividerViewDataMapper @Inject constructor(
    private val timeMapper: TimeMapper,
    private val resourceRepo: ResourceRepo,
) {

    fun addDateViewData(
        viewData: List<Pair<Long, ViewHolderType>>,
        addDaysBetween: DaysBetween = DaysBetween.Hidden,
    ): List<ViewHolderType> {
        val calendar = Calendar.getInstance()
        val newViewData = mutableListOf<ViewHolderType>()
        var previousTimeStarted = 0L

        viewData.forEach { (timeStarted, recordViewData) ->
            newViewData += getDaysBetweenData(
                previousTimeStarted = previousTimeStarted,
                timeStarted = timeStarted,
                calendar = calendar,
                addDaysBetween = addDaysBetween,
            )

            if (!timeMapper.sameDay(timeStarted, previousTimeStarted, calendar)) {
                newViewData += RecordsDateDividerViewData(
                    message = timeMapper.formatDayDateYear(timeStarted),
                )
            }
            previousTimeStarted = timeStarted
            newViewData.add(recordViewData)
        }

        return newViewData
    }

    private fun getDaysBetweenData(
        previousTimeStarted: Long,
        timeStarted: Long,
        calendar: Calendar,
        addDaysBetween: DaysBetween,
    ): ViewHolderType? {
        if (previousTimeStarted == 0L) return null
        if (addDaysBetween !is DaysBetween.Shown) return null

        val daysCount = timeMapper.toTimestampShift(
            fromTime = timeStarted,
            toTime = previousTimeStarted,
            range = RangeLength.Day,
            firstDayOfWeek = addDaysBetween.firstDayOfWeek,
            calendar = calendar,
        ) - 1
        if (daysCount <= 0) return null

        return RecordsDaysBetweenDividerViewData(
            id = timeStarted,
            message = resourceRepo.getString(
                R.string.records_all_days_without_records,
                daysCount,
            ),
        )
    }

    sealed interface DaysBetween {
        data object Hidden : DaysBetween
        data class Shown(val firstDayOfWeek: DayOfWeek) : DaysBetween
    }
}
