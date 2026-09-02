package com.example.util.simpletimetracker.data_local.recordTag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecordToRecordTagDao {

    @Query("SELECT * FROM recordToRecordTag")
    suspend fun getAll(): List<RecordToRecordTagDBO>

    @Query("SELECT record_id FROM recordToRecordTag WHERE record_tag_id = :tagId")
    suspend fun getRecordIdsByTagId(tagId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordToRecordTagDBO: List<RecordToRecordTagDBO>)

    @Query("DELETE FROM recordToRecordTag WHERE record_tag_id = :tagId")
    suspend fun deleteAllByTagId(tagId: Long)

    @Query("DELETE FROM recordToRecordTag WHERE record_id = :recordId")
    suspend fun deleteAllByRecordId(recordId: Long)

    @Query("DELETE FROM recordToRecordTag")
    suspend fun clear()
}