package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.domain.record.model.MultiSelectedRecordId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordsContainerMultiselectInteractor @Inject constructor() {

    var isEnabled: Boolean = false
        private set
    var selectedRecordIds: List<MultiSelectedRecordId> = emptyList()
        private set

    val stateChanged: SharedFlow<Unit> get() = _stateChanged.asSharedFlow()
    private val _stateChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun enable(id: MultiSelectedRecordId) {
        onRecordClick(id)
        onNewState(true)
    }

    suspend fun disable() {
        if (!isEnabled) return
        selectedRecordIds = emptyList()
        onNewState(false)
    }

    fun onRecordClick(id: MultiSelectedRecordId) {
        selectedRecordIds = selectedRecordIds.toMutableList()
            .apply { addOrRemove(id) }
    }

    private suspend fun onNewState(newState: Boolean) {
        isEnabled = newState
        _stateChanged.emit(Unit)
    }
}