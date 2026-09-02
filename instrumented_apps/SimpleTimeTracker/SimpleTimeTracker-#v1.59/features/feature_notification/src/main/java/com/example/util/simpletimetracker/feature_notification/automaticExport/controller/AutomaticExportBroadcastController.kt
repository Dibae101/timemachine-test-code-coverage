package com.example.util.simpletimetracker.feature_notification.automaticExport.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.backup.interactor.AutomaticExportInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class AutomaticExportBroadcastController @Inject constructor(
    private val automaticExportInteractor: AutomaticExportInteractor,
) {

    suspend fun onReminder() {
        automaticExportInteractor.export()
    }

    fun onFinished() {
        automaticExportInteractor.onFinished()
    }

    fun onBootCompleted() = allowDiskRead { MainScope() }.launch {
        automaticExportInteractor.schedule()
    }
}