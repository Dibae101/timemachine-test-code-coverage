package com.example.util.simpletimetracker.domain.record.interactor

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordsContainerUpdateInteractor @Inject constructor() {

    val showCalendarUpdated: SharedFlow<Unit> get() = _showCalendarUpdated.asSharedFlow()
    private val _showCalendarUpdated = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val calendarDaysUpdated: SharedFlow<Unit> get() = _calendarDaysUpdated.asSharedFlow()
    private val _calendarDaysUpdated = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val dateSelectorUpdate: SharedFlow<Unit> get() = _dateSelectorUpdate.asSharedFlow()
    private val _dateSelectorUpdate = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun sendShowCalendarUpdated() {
        _showCalendarUpdated.emit(Unit)
    }

    suspend fun sendCalendarDaysUpdated() {
        _calendarDaysUpdated.emit(Unit)
    }

    suspend fun sendDateSelectorUpdate() {
        _dateSelectorUpdate.emit(Unit)
    }
}