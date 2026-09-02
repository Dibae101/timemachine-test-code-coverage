package com.example.util.simpletimetracker.feature_records_filter.model

import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData

sealed interface RecordFilterActivitiesType : FilterViewData.Type {
    data object Activities : RecordFilterActivitiesType
    data object Categories : RecordFilterActivitiesType
}