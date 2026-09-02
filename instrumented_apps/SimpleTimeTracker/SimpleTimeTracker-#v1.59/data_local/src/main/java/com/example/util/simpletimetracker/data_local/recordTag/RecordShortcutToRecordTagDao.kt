package com.example.util.simpletimetracker.data_local.recordTag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecordShortcutToRecordTagDao {

    @Query("SELECT * FROM recordShortcutToRecordTag")
    suspend fun getAll(): List<RecordShortcutToRecordTagDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordShortcutToRecordTagDBO: List<RecordShortcutToRecordTagDBO>)

    @Query("DELETE FROM recordShortcutToRecordTag WHERE record_tag_id = :tagId")
    suspend fun deleteAllByTagId(tagId: Long)

    @Query("DELETE FROM recordShortcutToRecordTag WHERE shortcut_id = :shortcutId")
    suspend fun deleteAllByShortcutId(shortcutId: Long)

    @Query("DELETE FROM recordShortcutToRecordTag")
    suspend fun clear()
}