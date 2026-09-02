package com.example.util.simpletimetracker.domain.recordsFilter.interactor

import com.example.util.simpletimetracker.domain.record.model.FavouriteRecordsFilter
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordsFilter.repo.FavouriteRecordsFilterRepo
import javax.inject.Inject

class FavouriteRecordsFilterInteractor @Inject constructor(
    private val repo: FavouriteRecordsFilterRepo,
) {

    suspend fun getAll(): List<FavouriteRecordsFilter> {
        return repo.getAll()
    }

    suspend fun get(id: Long): FavouriteRecordsFilter? {
        return repo.get(id)
    }

    suspend fun add(data: FavouriteRecordsFilter) {
        val normalizedData = data.copy(
            filter = data.filter.map {
                if (it is RecordsFilter.Date) it.copy(position = 0) else it
            },
        )
        repo.add(normalizedData)
    }

    suspend fun remove(id: Long) {
        repo.remove(id)
    }
}