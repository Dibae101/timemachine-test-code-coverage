package com.example.util.simpletimetracker.domain.backup.repo

import com.example.util.simpletimetracker.domain.backup.model.BackupOptionsData
import com.example.util.simpletimetracker.domain.backup.model.PartialBackupRestoreData
import com.example.util.simpletimetracker.domain.backup.model.ResultCode

interface BackupPartialRepo {

    suspend fun partialRestoreBackupFile(
        params: BackupOptionsData.Custom,
    ): ResultCode

    suspend fun readBackupFile(
        uriString: String,
    ): Pair<ResultCode, PartialBackupRestoreData?>
}