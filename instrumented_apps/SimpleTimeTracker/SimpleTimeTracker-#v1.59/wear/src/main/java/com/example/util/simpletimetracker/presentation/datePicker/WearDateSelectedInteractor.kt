/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.datePicker

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearDateSelectedInteractor @Inject constructor() {

    val data: SharedFlow<LocalDate> get() = _data.asSharedFlow()

    private val _data = MutableSharedFlow<LocalDate>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun send(date: LocalDate) {
        _data.emit(date)
    }
}