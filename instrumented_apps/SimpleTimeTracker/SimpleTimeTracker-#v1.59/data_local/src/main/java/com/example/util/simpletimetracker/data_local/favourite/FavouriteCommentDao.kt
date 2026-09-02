package com.example.util.simpletimetracker.data_local.favourite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteCommentDao {

    @Query("SELECT * FROM favouriteComments")
    suspend fun getAll(): List<FavouriteCommentDBO>

    @Query("SELECT * FROM favouriteComments WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): FavouriteCommentDBO?

    @Query("SELECT * FROM favouriteComments WHERE comment = :text LIMIT 1")
    suspend fun get(text: String): FavouriteCommentDBO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: FavouriteCommentDBO): Long

    @Query("DELETE FROM favouriteComments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM favouriteComments")
    suspend fun clear()
}