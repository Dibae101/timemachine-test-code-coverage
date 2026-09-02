package com.example.util.simpletimetracker.domain.backup.repo

import com.example.util.simpletimetracker.domain.backup.model.ResultCode
import com.example.util.simpletimetracker.domain.fileExport.ExportDateTimeFormat
import com.example.util.simpletimetracker.domain.record.model.Range

interface CsvRepo {

    suspend fun saveCsvFile(
        uriString: String,
        range: Range?,
        dateTimeFormat: ExportDateTimeFormat,
    ): ResultCode

    suspend fun importCsvFile(
        uriString: String,
    ): ResultCode
}