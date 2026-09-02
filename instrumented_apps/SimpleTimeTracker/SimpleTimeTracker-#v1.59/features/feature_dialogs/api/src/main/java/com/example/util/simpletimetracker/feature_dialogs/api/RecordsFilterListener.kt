package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterResultParams

interface RecordsFilterListener {

    fun onFilterChanged(result: RecordsFilterResultParams)
    fun onFilterDismissed(tag: String)
}