package com.example.util.simpletimetracker.feature_statistics_detail.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.core.extension.shiftTimeStamp
import com.example.util.simpletimetracker.core.extension.toModel
import com.example.util.simpletimetracker.core.mapper.RangeViewDataMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.viewData.RangeSelectionOptionsListItem
import com.example.util.simpletimetracker.domain.daysOfWeek.interactor.GetProcessedLastDaysCountInteractor
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.CustomRangeSelectionParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogType
import com.example.util.simpletimetracker.navigation.params.screen.DurationDialogParams
import kotlinx.coroutines.launch
import javax.inject.Inject

class StatisticsDetailRangeViewModelDelegate @Inject constructor(
    private val router: Router,
    private val rangeViewDataMapper: RangeViewDataMapper,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
    private val getProcessedLastDaysCountInteractor: GetProcessedLastDaysCountInteractor,
) : StatisticsDetailViewModelDelegate, ViewModelDelegate() {

    private var parent: StatisticsDetailViewModelDelegate.Parent? = null
    private val dateFilter get() = parent?.filter?.filterIsInstance<RecordsFilter.Date>()?.firstOrNull()
    private val rangeLength: RangeLength get() = dateFilter?.range ?: RangeLength.All
    private val rangePosition: Int get() = dateFilter?.position.orZero()

    override fun attach(parent: StatisticsDetailViewModelDelegate.Parent) {
        this.parent = parent
    }

    fun onBackToTodayClick() {
        updatePosition(0)
    }

    fun onRangeSelected(id: RangeSelectionOptionsListItem) {
        when (id) {
            is RangeSelectionOptionsListItem.SelectDate -> {
                onSelectDateClick()
            }
            is RangeSelectionOptionsListItem.Custom -> {
                onSelectCustomRangeClick()
            }
            is RangeSelectionOptionsListItem.Last -> {
                onSelectLastDaysClick()
            }
            is RangeSelectionOptionsListItem.Simple -> {
                onRangeUpdated(id.rangeLengthParams.toModel())
            }
        }
    }

    fun onDateTimeSet(timestamp: Long, tag: String?) = delegateScope.launch {
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        when (tag) {
            DATE_TAG -> {
                timeMapper.toTimestampShift(
                    toTime = timestamp.shiftTimeStamp(startOfDayShift),
                    range = rangeLength,
                    firstDayOfWeek = prefsInteractor.getFirstDayOfWeek(),
                ).toInt().let(::updatePosition)
            }
        }
    }

    fun onCustomRangeSelected(range: Range) {
        onRangeUpdated(RangeLength.Custom(range))
    }

    fun onCountSet(count: Long, tag: String?) {
        if (tag != LAST_DAYS_COUNT_TAG) return

        val lastDaysCount = getProcessedLastDaysCountInteractor.execute(count)
        onRangeUpdated(RangeLength.Last(lastDaysCount))
    }

    fun onSelectDateClick() = delegateScope.launch {
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val current = timeMapper.toTimestampShifted(
            rangesFromToday = rangePosition,
            range = rangeLength,
            startOfDayShift = startOfDayShift,
        )

        router.navigate(
            DateTimeDialogParams(
                tag = DATE_TAG,
                type = DateTimeDialogType.DATE,
                timestamp = current,
                useMilitaryTime = useMilitaryTime,
                firstDayOfWeek = firstDayOfWeek,
            ),
        )
    }

    fun provideRangeLength(): RangeLength {
        return rangeLength
    }

    fun provideRangePosition(): Int {
        return rangePosition
    }

    private fun onSelectCustomRangeClick() = delegateScope.launch {
        val currentCustomRange = (rangeLength as? RangeLength.Custom)?.range

        CustomRangeSelectionParams(
            rangeStart = currentCustomRange?.timeStarted,
            rangeEnd = currentCustomRange?.timeEnded,
        ).let(router::navigate)
    }

    // TODO add custom range reopen same as last days?
    //  Currently if some last days number is selected, it is preselected on switching to other range and back.
    //  Custom range when switched to other range and back is always reset to current data.
    //  This could be a valid usecase though.
    private fun onSelectLastDaysClick() = delegateScope.launch {
        DurationDialogParams(
            tag = LAST_DAYS_COUNT_TAG,
            value = DurationDialogParams.Value.Count(
                getCurrentLastDaysCount().toLong(),
            ),
            hideDisableButton = true,
        ).let(router::navigate)
    }

    fun onSelectRangeClick() = delegateScope.launch {
        val data = rangeViewDataMapper.mapToRangesOptions(
            currentRange = rangeLength,
            addSelection = true,
            lastDaysCount = getCurrentLastDaysCount(),
        )
        router.navigate(data)
    }

    private suspend fun getCurrentLastDaysCount(): Int {
        return (rangeLength as? RangeLength.Last)?.days
            ?: prefsInteractor.getStatisticsDetailLastDays()
    }

    private fun onRangeUpdated(newRange: RangeLength) = delegateScope.launch {
        prefsInteractor.setStatisticsDetailRange(newRange)

        if (newRange != rangeLength) {
            parent?.onRangeChangedFromSelection(newRange)
            updatePosition(0)
        }
    }

    fun updatePosition(newPosition: Int) {
        parent?.onPositionChangedFromSelection(newPosition)
    }

    companion object {
        private const val LAST_DAYS_COUNT_TAG = "statistics_detail_last_days_count_tag"
        private const val DATE_TAG = "statistics_detail_date_tag"
    }
}