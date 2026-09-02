package com.example.util.simpletimetracker.feature_running_records.api

import com.example.util.simpletimetracker.core.interactor.ActivityFilterViewDataInteractor
import com.example.util.simpletimetracker.domain.record.model.RunningRecord
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.recordShortcut.RecordShortcutViewData

interface RecordsShortcutsViewDataInteractor {
    suspend fun getShortcutsViewData(
        filter: ActivityFilterViewDataInteractor.Filter,
        recordTypesMap: Map<Long, RecordType>,
        recordTags: List<RecordTag>,
        runningRecords: List<RunningRecord>,
        searchText: String,
        isDarkTheme: Boolean,
    ): List<RecordShortcutViewData>
}