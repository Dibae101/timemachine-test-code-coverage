package com.example.util.simpletimetracker.feature_notification.automaticBackup.controller

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import com.example.util.simpletimetracker.domain.backup.interactor.AutomaticBackupInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class AutomaticBackupBroadcastController @Inject constructor(
    private val automaticBackupInteractor: AutomaticBackupInteractor,
) {

    suspend fun onReminder() {
        automaticBackupInteractor.backup()
    }

    fun onFinished() {
        automaticBackupInteractor.onFinished()
    }

    fun onBootCompleted() = allowDiskRead { MainScope() }.launch {
        automaticBackupInteractor.schedule()
    }
}