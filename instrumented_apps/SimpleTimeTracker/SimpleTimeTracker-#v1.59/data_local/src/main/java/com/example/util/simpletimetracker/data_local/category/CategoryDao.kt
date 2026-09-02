package com.example.util.simpletimetracker.data_local.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryDBO>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): CategoryDBO?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun get(name: String): List<CategoryDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryDBO): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun clear()
}