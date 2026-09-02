package com.example.util.simpletimetracker.data_local.recordsFilter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FavouriteRecordsFilterDao {

    @Transaction
    @Query("SELECT * FROM favouriteRecordFilters")
    suspend fun getAll(): List<FavouriteRecordsFilterDBO>

    @Transaction
    @Query("SELECT * FROM favouriteRecordFilters WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): FavouriteRecordsFilterDBO?

    @Transaction
    suspend fun insert(data: FavouriteRecordsFilterDBO) {
        val mainId = insertMain(data.main)

        data.filters.forEach { filter ->
            insertFilter(filter.copy(ownerId = mainId))
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMain(data: FavouriteRecordsFilterDBO.MainDBO): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(data: FavouriteRecordsFilterDBO.FilterDBO)

    @Delete
    suspend fun delete(data: FavouriteRecordsFilterDBO.MainDBO)

    @Query("DELETE FROM favouriteRecordFilters")
    suspend fun clear()
}