package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.navigation.params.screen.DataExportSettingsResult

interface DataExportSettingsDialogListener {

    fun onDataExportSettingsSelected(data: DataExportSettingsResult)
}