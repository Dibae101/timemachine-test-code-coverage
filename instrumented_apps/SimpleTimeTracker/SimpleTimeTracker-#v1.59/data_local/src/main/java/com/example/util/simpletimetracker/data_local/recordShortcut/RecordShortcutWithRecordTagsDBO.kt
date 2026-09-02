package com.example.util.simpletimetracker.data_local.recordShortcut

import androidx.room.Embedded
import androidx.room.Relation
import com.example.util.simpletimetracker.data_local.recordTag.RecordShortcutToRecordTagDBO

data class RecordShortcutWithRecordTagsDBO(
    @Embedded
    val shortcut: RecordShortcutDBO,
    @Relation(
        parentColumn = "id",
        entityColumn = "shortcut_id",
    )
    val recordTags: List<RecordShortcutToRecordTagDBO>,
)