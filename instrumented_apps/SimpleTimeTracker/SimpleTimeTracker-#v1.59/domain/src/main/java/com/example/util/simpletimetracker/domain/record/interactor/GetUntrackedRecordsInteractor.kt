package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.Record

interface GetUntrackedRecordsInteractor {

    suspend fun get(
        range: Range,
        records: List<Range>,
    ): List<Record>
}