package com.example.util.simpletimetracker.data_local.recordTag

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "recordShortcutToRecordTag", primaryKeys = ["shortcut_id", "record_tag_id"])
data class RecordShortcutToRecordTagDBO(
    @ColumnInfo(name = "shortcut_id")
    val shortcutId: Long,

    @ColumnInfo(name = "record_tag_id")
    val recordTagId: Long,

    @ColumnInfo(name = "record_tag_numeric_value")
    val recordTagNumericValue: Double?,
)