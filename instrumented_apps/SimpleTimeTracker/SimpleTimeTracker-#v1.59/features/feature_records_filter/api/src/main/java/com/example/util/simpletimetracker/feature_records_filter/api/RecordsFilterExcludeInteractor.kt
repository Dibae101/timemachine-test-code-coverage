package com.example.util.simpletimetracker.feature_records_filter.api

import com.example.util.simpletimetracker.domain.record.model.RecordsFilter

interface RecordsFilterExcludeInteractor {

    suspend fun exclude(
        id: Long,
        type: ExcludeType,
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter>

    suspend fun excludeOther(
        id: Long,
        type: ExcludeType,
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter>

    sealed interface ExcludeType {
        data object Activity : ExcludeType
        data object Category : ExcludeType
        data object Tag : ExcludeType
    }
}