package com.example.util.simpletimetracker.domain.favourite.interactor

import com.example.util.simpletimetracker.domain.favourite.repo.FavouriteIconRepo
import com.example.util.simpletimetracker.domain.favourite.model.FavouriteIcon
import javax.inject.Inject

class FavouriteIconInteractor @Inject constructor(
    private val repo: FavouriteIconRepo,
) {

    suspend fun getAll(): List<FavouriteIcon> {
        return repo.getAll()
    }

    suspend fun get(icon: String): FavouriteIcon? {
        return repo.get(icon)
    }

    suspend fun add(icon: FavouriteIcon): Long {
        return repo.add(icon)
    }

    suspend fun remove(id: Long) {
        repo.remove(id)
    }
}