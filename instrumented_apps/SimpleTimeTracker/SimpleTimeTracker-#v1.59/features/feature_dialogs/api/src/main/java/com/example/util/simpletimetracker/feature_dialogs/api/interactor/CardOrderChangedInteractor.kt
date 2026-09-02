package com.example.util.simpletimetracker.feature_dialogs.api.interactor

import kotlinx.coroutines.flow.SharedFlow

interface CardOrderChangedInteractor {
    val update: SharedFlow<Unit>
    suspend fun sendUpdate()
}