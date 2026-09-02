package com.example.util.simpletimetracker.domain.backup.repo

import com.example.util.simpletimetracker.domain.backup.model.BackupOptionsData
import com.example.util.simpletimetracker.domain.backup.model.ResultCode

interface BackupRepo {

    suspend fun saveBackupFile(
        uriString: String,
        params: BackupOptionsData.Save,
    ): ResultCode

    suspend fun restoreBackupFile(
        uriString: String,
        params: BackupOptionsData.Restore,
    ): ResultCode
}