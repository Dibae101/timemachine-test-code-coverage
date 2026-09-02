package com.example.util.simpletimetracker.domain.favourite.repo

import com.example.util.simpletimetracker.domain.favourite.model.FavouriteColor

interface FavouriteColorRepo {

    suspend fun getAll(): List<FavouriteColor>

    suspend fun get(id: Long): FavouriteColor?

    suspend fun get(text: String): FavouriteColor?

    suspend fun add(comment: FavouriteColor): Long

    suspend fun remove(id: Long)

    suspend fun clear()
}