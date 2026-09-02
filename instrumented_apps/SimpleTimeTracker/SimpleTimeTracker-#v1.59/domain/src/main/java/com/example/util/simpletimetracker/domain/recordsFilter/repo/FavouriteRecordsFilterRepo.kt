package com.example.util.simpletimetracker.domain.recordsFilter.repo

import com.example.util.simpletimetracker.domain.record.model.FavouriteRecordsFilter

interface FavouriteRecordsFilterRepo {

    suspend fun getAll(): List<FavouriteRecordsFilter>

    suspend fun get(id: Long): FavouriteRecordsFilter?

    suspend fun add(data: FavouriteRecordsFilter)

    suspend fun remove(id: Long)

    suspend fun clear()
}