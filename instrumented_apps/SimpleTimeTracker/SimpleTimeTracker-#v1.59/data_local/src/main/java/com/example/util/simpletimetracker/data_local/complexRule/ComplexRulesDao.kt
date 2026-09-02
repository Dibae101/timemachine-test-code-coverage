package com.example.util.simpletimetracker.data_local.complexRule

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ComplexRulesDao {

    @Transaction
    @Query("select exists(select 1 from complexRules)")
    suspend fun isEmpty(): Long

    @Query("SELECT * FROM complexRules")
    suspend fun getAll(): List<ComplexRuleDBO>

    @Query("SELECT * FROM complexRules WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): ComplexRuleDBO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: ComplexRuleDBO): Long

    @Query("UPDATE complexRules SET disabled = 1 WHERE id = :id")
    suspend fun disable(id: Long)

    @Query("UPDATE complexRules SET disabled = 0 WHERE id = :id")
    suspend fun enable(id: Long)

    @Query("DELETE FROM complexRules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM complexRules")
    suspend fun clear()
}