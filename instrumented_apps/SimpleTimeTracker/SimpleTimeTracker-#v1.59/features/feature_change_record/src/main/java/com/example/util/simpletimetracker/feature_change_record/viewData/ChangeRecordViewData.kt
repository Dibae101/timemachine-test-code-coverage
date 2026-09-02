package com.example.util.simpletimetracker.feature_change_record.viewData

import com.example.util.simpletimetracker.core.viewData.ChangeRecordDateTimeState
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData

data class ChangeRecordViewData(
    val recordPreview: RecordViewData,
    val dateTimeStarted: ChangeRecordDateTimeState,
    val dateTimeFinished: ChangeRecordDateTimeState,
)