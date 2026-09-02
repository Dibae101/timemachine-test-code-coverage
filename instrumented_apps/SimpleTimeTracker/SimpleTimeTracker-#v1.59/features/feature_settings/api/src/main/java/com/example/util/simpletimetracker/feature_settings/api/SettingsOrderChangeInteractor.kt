package com.example.util.simpletimetracker.feature_settings.api

import com.example.util.simpletimetracker.navigation.params.screen.CardOrderDialogParams

interface SettingsOrderChangeInteractor {
    suspend fun onOrderSelected(type: CardOrderDialogParams.Type)
    fun openOrderDialog(type: CardOrderDialogParams.Type)
}