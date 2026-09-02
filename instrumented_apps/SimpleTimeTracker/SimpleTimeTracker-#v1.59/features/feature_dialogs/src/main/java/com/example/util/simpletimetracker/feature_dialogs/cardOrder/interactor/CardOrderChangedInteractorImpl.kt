package com.example.util.simpletimetracker.feature_dialogs.cardOrder.interactor

import com.example.util.simpletimetracker.feature_dialogs.api.interactor.CardOrderChangedInteractor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardOrderChangedInteractorImpl @Inject constructor() : CardOrderChangedInteractor {

    override val update: SharedFlow<Unit> get() = _update.asSharedFlow()
    private val _update = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override suspend fun sendUpdate() {
        _update.emit(Unit)
    }
}