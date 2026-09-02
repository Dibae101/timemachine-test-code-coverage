package com.example.util.simpletimetracker.feature_records_filter.model

import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData

sealed interface RecordFilterCommentType : FilterViewData.Type {
    data object NoComment : RecordFilterCommentType
    data object AnyComment : RecordFilterCommentType
}