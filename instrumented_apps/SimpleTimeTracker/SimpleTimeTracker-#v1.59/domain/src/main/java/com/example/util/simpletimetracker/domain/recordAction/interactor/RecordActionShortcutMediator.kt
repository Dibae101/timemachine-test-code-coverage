package com.example.util.simpletimetracker.domain.recordAction.interactor

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordShortcut.interactor.RecordShortcutInteractor
import com.example.util.simpletimetracker.domain.recordShortcut.model.RecordShortcut
import javax.inject.Inject

class RecordActionShortcutMediator @Inject constructor(
    private val recordShortcutInteractor: RecordShortcutInteractor,
) {

    suspend fun execute(
        typeId: Long,
        comment: String,
        tagIds: List<RecordBase.Tag>,
    ) {
        RecordShortcut(
            id = 0L, // Creates new record.
            target = RecordShortcut.Target.Record(
                typeId = typeId,
                comment = comment,
                tags = tagIds,
            ),
        ).let {
            recordShortcutInteractor.add(it)
        }
    }

    suspend fun execute(
        target: RecordShortcut.Target,
    ) {
        RecordShortcut(
            id = 0L, // Creates new record.
            target = target,
        ).let {
            recordShortcutInteractor.add(it)
        }
    }
}