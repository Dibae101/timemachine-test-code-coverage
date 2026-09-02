package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.mapper.FileExportDateTimeFormatMapper
import com.example.util.simpletimetracker.domain.fileExport.ExportDateTimeFormat
import com.example.util.simpletimetracker.domain.fileExport.IsExportFormatAvailableInteractor
import javax.inject.Inject

class IsExportFormatAvailableInteractorImpl @Inject constructor(
    private val exportDateTimeFormatMapper: FileExportDateTimeFormatMapper,
) : IsExportFormatAvailableInteractor {

    override fun execute(format: ExportDateTimeFormat): Boolean {
        return exportDateTimeFormatMapper.isAvailable(format)
    }
}