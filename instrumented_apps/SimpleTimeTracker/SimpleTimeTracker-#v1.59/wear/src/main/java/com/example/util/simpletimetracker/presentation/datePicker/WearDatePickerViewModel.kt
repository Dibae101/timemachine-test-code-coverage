/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.datePicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WearDatePickerViewModel @Inject constructor(
    private val wearDateSelectedInteractor: WearDateSelectedInteractor,
) : ViewModel() {

    val effects: SharedFlow<Effect> get() = _effects.asSharedFlow()
    private val _effects = MutableSharedFlow<Effect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun onDateConfirm(date: LocalDate) = viewModelScope.launch {
        wearDateSelectedInteractor.send(date)
        _effects.emit(Effect.OnComplete)
    }

    sealed interface Effect {
        data object OnComplete : Effect
    }
}