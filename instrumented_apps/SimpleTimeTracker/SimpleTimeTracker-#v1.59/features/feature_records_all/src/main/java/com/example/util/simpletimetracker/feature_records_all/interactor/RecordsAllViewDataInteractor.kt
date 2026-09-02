package com.example.util.simpletimetracker.feature_records_all.interactor

import com.example.util.simpletimetracker.core.interactor.GetRunningRecordViewDataMediator
import com.example.util.simpletimetracker.core.interactor.RecordFilterInteractor
import com.example.util.simpletimetracker.core.mapper.DateDividerViewDataMapper
import com.example.util.simpletimetracker.core.mapper.MultitaskRecordViewDataMapper
import com.example.util.simpletimetracker.core.mapper.RecordViewDataMapper
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeGoalInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.record.model.MultitaskRecord
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.record.model.RunningRecord
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_records_all.model.RecordsAllSortOrder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordsAllViewDataInteractor @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordTypeGoalInteractor: RecordTypeGoalInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val recordFilterInteractor: RecordFilterInteractor,
    private val recordViewDataMapper: RecordViewDataMapper,
    private val getRunningRecordViewDataMediator: GetRunningRecordViewDataMediator,
    private val multitaskRecordViewDataMapper: MultitaskRecordViewDataMapper,
    private val dateDividerViewDataMapper: DateDividerViewDataMapper,
) {

    suspend fun getViewData(
        filter: List<RecordsFilter>,
        sortOrder: RecordsAllSortOrder,
    ): List<ViewHolderType> = withContext(Dispatchers.Default) {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val durationFormat = prefsInteractor.getDurationFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val recordTypes = recordTypeInteractor.getAll().associateBy(RecordType::id)
        val recordTags = recordTagInteractor.getAll()
        val goals = recordTypeGoalInteractor.getAllTypeGoals().groupBy { it.idData.value }

        // Show empty records if no filters other than date.
        val records = filter
            .takeUnless { it.none { filter -> filter !is RecordsFilter.Date } }
            .orEmpty()
            .let { recordFilterInteractor.getByFilter(it) }

        return@withContext records
            .mapNotNull { record ->
                val viewData = when (record) {
                    is Record -> if (record.typeId != UNTRACKED_ITEM_ID) {
                        recordViewDataMapper.map(
                            record = record,
                            recordType = recordTypes[record.typeId] ?: return@mapNotNull null,
                            recordTags = recordTags,
                            isDarkTheme = isDarkTheme,
                            useMilitaryTime = useMilitaryTime,
                            durationFormat = durationFormat,
                            showSeconds = showSeconds,
                        )
                    } else {
                        recordViewDataMapper.mapToUntracked(
                            timeStarted = record.timeStarted,
                            timeEnded = record.timeEnded,
                            isDarkTheme = isDarkTheme,
                            useMilitaryTime = useMilitaryTime,
                            durationFormat = durationFormat,
                            showSeconds = showSeconds,
                        )
                    }
                    is RunningRecord -> getRunningRecordViewDataMediator.execute(
                        type = recordTypes[record.id] ?: return@mapNotNull null,
                        tags = recordTags,
                        goals = goals[record.id].orEmpty(),
                        record = record,
                        nowIconVisible = true,
                        goalsVisible = false,
                        totalDurationVisible = false,
                        isDarkTheme = isDarkTheme,
                        useMilitaryTime = useMilitaryTime,
                        durationFormat = durationFormat,
                        showSeconds = showSeconds,
                    )
                    is MultitaskRecord -> multitaskRecordViewDataMapper.map(
                        multitaskRecord = record,
                        recordTypes = recordTypes,
                        recordTags = recordTags,
                        isDarkTheme = isDarkTheme,
                        useMilitaryTime = useMilitaryTime,
                        durationFormat = durationFormat,
                        showSeconds = showSeconds,
                    )
                }

                Triple(
                    record.timeStarted,
                    record.duration,
                    viewData,
                )
            }
            .sortedByDescending { (timeStarted, duration, _) ->
                when (sortOrder) {
                    RecordsAllSortOrder.TIME_STARTED -> timeStarted
                    RecordsAllSortOrder.DURATION -> duration
                }
            }
            .map { (timeStarted, _, record) -> timeStarted to record }
            .let { viewData ->
                if (sortOrder == RecordsAllSortOrder.TIME_STARTED) {
                    dateDividerViewDataMapper.addDateViewData(
                        viewData = viewData,
                        addDaysBetween = DateDividerViewDataMapper.DaysBetween.Shown(firstDayOfWeek),
                    )
                } else {
                    viewData.map { it.second }
                }
            }
            .ifEmpty { listOf(recordViewDataMapper.mapToEmpty()) }
    }
}