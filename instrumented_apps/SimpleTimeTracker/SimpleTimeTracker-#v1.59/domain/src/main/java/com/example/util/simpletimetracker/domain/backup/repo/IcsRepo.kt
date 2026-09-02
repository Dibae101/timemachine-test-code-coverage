package com.example.util.simpletimetracker.domain.backup.repo

import com.example.util.simpletimetracker.domain.backup.model.ResultCode
import com.example.util.simpletimetracker.domain.record.model.Range

interface IcsRepo {

    suspend fun saveIcsFile(
        uriString: String,
        range: Range?,
    ): ResultCode
}