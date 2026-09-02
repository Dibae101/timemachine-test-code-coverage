package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams

interface OptionsListDialogListener {

    fun onOptionsItemClick(id: OptionsListParams.Item.Id)

    fun onOptionsDialogOpened() = Unit

    fun onOptionsDialogClosed() = Unit
}