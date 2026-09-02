package com.example.util.simpletimetracker.feature_records_filter.model

import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData

sealed interface RecordFilterDuplicationsType : FilterViewData.Type {
    object SameActivity : RecordFilterDuplicationsType
    object SameTimes : RecordFilterDuplicationsType
}