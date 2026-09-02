package com.example.util.simpletimetracker.feature_records_filter.model

import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData

sealed interface RecordFilterSelectionType : FilterViewData.Type {
    data object Select : RecordFilterSelectionType
    data object Filter : RecordFilterSelectionType
}