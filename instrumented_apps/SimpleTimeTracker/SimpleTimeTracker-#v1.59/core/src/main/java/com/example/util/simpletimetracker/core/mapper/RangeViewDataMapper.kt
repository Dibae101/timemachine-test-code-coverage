package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.extension.toParams
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.viewData.RangeSelectionOptionsListItem
import com.example.util.simpletimetracker.core.viewData.RangeViewData
import com.example.util.simpletimetracker.core.viewData.SelectDateViewData
import com.example.util.simpletimetracker.core.viewData.SelectLastDaysViewData
import com.example.util.simpletimetracker.core.viewData.SelectRangeViewData
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import javax.inject.Inject

class RangeViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
    private val rangeTitleMapper: RangeTitleMapper,
) {

    fun mapToRangesOptions(
        currentRange: RangeLength,
        addSelection: Boolean,
        lastDaysCount: Int,
    ): OptionsListParams {
        fun mapRange(
            text: String,
            range: RangeLength,
        ): OptionsListParams.Item {
            val isSelected = currentRange::class == range::class
            val id = when (range) {
                is RangeLength.Day,
                is RangeLength.Week,
                is RangeLength.Month,
                is RangeLength.Year,
                is RangeLength.All,
                -> RangeSelectionOptionsListItem.Simple(range.toParams())
                is RangeLength.Custom -> RangeSelectionOptionsListItem.Custom
                is RangeLength.Last -> RangeSelectionOptionsListItem.Last
            }
            val icon = when (range) {
                is RangeLength.Day -> R.drawable.date_day
                is RangeLength.Week -> R.drawable.date_week
                is RangeLength.Month -> R.drawable.date_month
                is RangeLength.Year -> R.drawable.date_year
                else -> null
            }
            return OptionsListParams.Item(
                id = id,
                text = text,
                icon = icon,
                isChecked = isSelected,
                isSelected = isSelected,
            )
        }

        val selectDateButton = mapToSelectDateName(currentRange)?.text?.let {
            OptionsListParams.Item(
                id = RangeSelectionOptionsListItem.SelectDate,
                text = it,
                icon = R.drawable.date,
            )
        }.takeIf { addSelection }?.let(::listOf) ?: emptyList()

        val selectRangeButton = mapRange(
            text = mapToSelectRange().text,
            range = RangeLength.Custom(Range(0, 0)), // doesn't matter.
        ).takeIf { addSelection }?.let(::listOf) ?: emptyList()

        val selectLastDaysButton = mapRange(
            text = mapToSelectLastDays(lastDaysCount).text,
            range = RangeLength.Last(0), // doesn't matter.
        ).let(::listOf)

        val rangeButtons = ranges.mapNotNull(::mapToRangeName).map {
            mapRange(
                text = it.text,
                range = it.range,
            )
        }

        val data = selectDateButton +
            selectLastDaysButton +
            selectRangeButton +
            rangeButtons

        return OptionsListParams(
            items = data,
        )
    }

    // TODO the same as rangeTitleMapper.mapToTitle but to avoid "Today" texts
    //  its use different mapping for day/week/month/year.
    fun mapToShareTitle(
        rangeLength: RangeLength,
        position: Int,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): String {
        return when (rangeLength) {
            is RangeLength.Day -> timeMapper.toDayDateTitle(position, startOfDayShift)
            is RangeLength.Week -> timeMapper.toWeekDateTitle(position, startOfDayShift, firstDayOfWeek)
            is RangeLength.Month -> timeMapper.toMonthDateTitle(position, startOfDayShift)
            is RangeLength.Year -> timeMapper.toYearDateTitle(position, startOfDayShift)
            is RangeLength.All,
            is RangeLength.Custom,
            is RangeLength.Last,
            -> rangeTitleMapper.mapToTitle(rangeLength, position, startOfDayShift, firstDayOfWeek)
        }
    }

    private fun mapToRangeName(rangeLength: RangeLength): RangeViewData? {
        val text = when (rangeLength) {
            is RangeLength.Day -> R.string.range_day
            is RangeLength.Week -> R.string.range_week
            is RangeLength.Month -> R.string.range_month
            is RangeLength.Year -> R.string.range_year
            is RangeLength.All -> R.string.range_overall
            // These ranges mapped separately
            is RangeLength.Custom -> return null
            is RangeLength.Last -> return null
        }.let(resourceRepo::getString)

        return RangeViewData(
            range = rangeLength,
            text = text,
        )
    }

    fun mapToSelectDateName(rangeLength: RangeLength): SelectDateViewData? {
        return when (rangeLength) {
            is RangeLength.Day -> R.string.range_select_day
            is RangeLength.Week -> R.string.range_select_week
            is RangeLength.Month -> R.string.range_select_month
            is RangeLength.Year -> R.string.range_select_year
            else -> null
        }
            ?.let(resourceRepo::getString)
            ?.let(::SelectDateViewData)
    }

    private fun mapToSelectRange(): SelectRangeViewData {
        val text = rangeTitleMapper.mapToSelectRangeName()
        return SelectRangeViewData(text)
    }

    private fun mapToSelectLastDays(days: Int): SelectLastDaysViewData {
        val text = rangeTitleMapper.mapToLastDaysTitle(days)
        return SelectLastDaysViewData(text)
    }

    companion object {
        private val ranges: List<RangeLength> = listOf(
            RangeLength.All,
            RangeLength.Year,
            RangeLength.Month,
            RangeLength.Week,
            RangeLength.Day,
        )
    }
}