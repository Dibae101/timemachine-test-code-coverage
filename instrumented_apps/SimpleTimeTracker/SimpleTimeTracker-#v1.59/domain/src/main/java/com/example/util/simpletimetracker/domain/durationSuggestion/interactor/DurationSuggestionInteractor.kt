package com.example.util.simpletimetracker.domain.durationSuggestion.interactor

import com.example.util.simpletimetracker.domain.durationSuggestion.model.DurationSuggestion
import com.example.util.simpletimetracker.domain.durationSuggestion.repo.DurationSuggestionRepo
import javax.inject.Inject

class DurationSuggestionInteractor @Inject constructor(
    private val repo: DurationSuggestionRepo,
) {

    suspend fun getAll(): List<DurationSuggestion> {
        return repo.getAll().let(::sort)
    }

    suspend fun add(data: DurationSuggestion): Long {
        return repo.add(data)
    }

    suspend fun remove(id: Long) {
        repo.remove(id)
    }

    fun sort(
        data: List<DurationSuggestion>,
    ): List<DurationSuggestion> {
        return data.sortedBy { it.valueSeconds }
    }
}