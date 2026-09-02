package com.example.util.simpletimetracker.domain.backup.interactor

import com.example.util.simpletimetracker.domain.backup.model.ResultCode
import com.example.util.simpletimetracker.domain.backup.repo.CsvRepo
import com.example.util.simpletimetracker.domain.fileExport.ExportDateTimeFormat
import com.example.util.simpletimetracker.domain.notifications.interactor.UpdateExternalViewsInteractor
import com.example.util.simpletimetracker.domain.record.model.Range
import javax.inject.Inject

class CsvExportInteractor @Inject constructor(
    private val csvRepo: CsvRepo,
    private val externalViewsInteractor: UpdateExternalViewsInteractor,
) {

    suspend fun saveCsvFile(
        uriString: String,
        range: Range?,
        dateTimeFormat: ExportDateTimeFormat,
    ): ResultCode {
        return csvRepo.saveCsvFile(
            uriString = uriString,
            range = range,
            dateTimeFormat = dateTimeFormat,
        )
    }

    suspend fun importCsvFile(uriString: String): ResultCode {
        val resultCode = csvRepo.importCsvFile(uriString)
        externalViewsInteractor.onCsvImport()
        return resultCode
    }
}