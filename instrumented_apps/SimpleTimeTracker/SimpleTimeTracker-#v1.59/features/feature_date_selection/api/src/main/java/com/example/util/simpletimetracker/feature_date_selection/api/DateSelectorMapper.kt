package com.example.util.simpletimetracker.feature_date_selection.api

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DaysInCalendar
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.feature_base_adapter.InfiniteRecyclerAdapter

interface DateSelectorMapper : InfiniteRecyclerAdapter.DataProvider {

    var currentSelectedPosition: Int

    fun setup(setupData: SetupData)

    data class SetupData(
        val type: Type,
        val startOfDayShift: Long,
        val firstDayOfWeek: DayOfWeek,
    ) {
        sealed interface Type {
            val optionsButton: Button

            data class Records(
                override val optionsButton: Button,
                val isCalendarView: Boolean,
                val daysInCalendar: DaysInCalendar,
            ) : Type

            data class Statistics(
                override val optionsButton: Button,
                val rangeLength: RangeLength,
            ) : Type
        }

        sealed interface Button {
            data object Hidden : Button
            data class Visible(val iconResId: Int) : Button
        }

        companion object {
            val Empty = SetupData(
                type = Type.Statistics(
                    optionsButton = Button.Hidden,
                    rangeLength = RangeLength.Day,
                ),
                startOfDayShift = 0,
                firstDayOfWeek = DayOfWeek.SUNDAY,
            )
        }
    }
}