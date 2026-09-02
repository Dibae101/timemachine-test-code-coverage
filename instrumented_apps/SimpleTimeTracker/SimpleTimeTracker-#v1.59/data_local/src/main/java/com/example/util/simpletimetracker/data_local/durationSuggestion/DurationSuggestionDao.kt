package com.example.util.simpletimetracker.data_local.durationSuggestion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DurationSuggestionDao {

    @Query("SELECT * FROM durationSuggestions")
    suspend fun getAll(): List<DurationSuggestionDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: DurationSuggestionDBO): Long

    @Query("DELETE FROM durationSuggestions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM durationSuggestions")
    suspend fun clear()
}