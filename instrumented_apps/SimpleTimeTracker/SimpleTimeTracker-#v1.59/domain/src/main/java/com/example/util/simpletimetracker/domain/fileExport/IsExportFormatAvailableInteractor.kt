package com.example.util.simpletimetracker.domain.fileExport

interface IsExportFormatAvailableInteractor {

    fun execute(format: ExportDateTimeFormat): Boolean
}