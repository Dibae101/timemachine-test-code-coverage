/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagValueSelection.interactor

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagValueSelectedInteractor @Inject constructor() {

    val data: SharedFlow<Result> get() = _data.asSharedFlow()

    private val _data = MutableSharedFlow<Result>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun send(data: Result) {
        _data.emit(data)
    }

    data class Result(
        val tagId: Long,
        val value: Double?,
    )
}