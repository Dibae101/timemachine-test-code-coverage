package com.example.util.simpletimetracker.domain.darkMode.interactor

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeChangedInteractor @Inject constructor() {

    val themeChanged: SharedFlow<Unit> get() = _themeChanged.asSharedFlow()

    private val _themeChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun send() {
        _themeChanged.emit(Unit)
    }
}