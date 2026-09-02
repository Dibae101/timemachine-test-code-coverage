package com.example.util.simpletimetracker.domain.activitySuggestion.interactor

import com.example.util.simpletimetracker.domain.activitySuggestion.model.ActivitySuggestion
import com.example.util.simpletimetracker.domain.activitySuggestion.repo.ActivitySuggestionRepo
import javax.inject.Inject

class ActivitySuggestionInteractor @Inject constructor(
    private val activitySuggestionRepo: ActivitySuggestionRepo,
) {

    suspend fun getAll(): List<ActivitySuggestion> {
        return activitySuggestionRepo.getAll()
    }

    suspend fun get(id: Long): ActivitySuggestion? {
        return activitySuggestionRepo.get(id)
    }

    suspend fun getByTypeId(id: Long): List<ActivitySuggestion> {
        return activitySuggestionRepo.getByTypeId(id)
    }

    suspend fun add(data: List<ActivitySuggestion>) {
        activitySuggestionRepo.add(data)
    }

    suspend fun remove(ids: List<Long>) {
        activitySuggestionRepo.remove(ids)
    }

    suspend fun removeTypeId(id: Long) {
        val idsToRemove = mutableListOf<Long>()
        val suggestionsToChange = mutableListOf<ActivitySuggestion>()

        getAll().forEach { suggestion ->
            if (suggestion.forTypeId == id) {
                idsToRemove += suggestion.id
                return@forEach
            }
            if (id in suggestion.suggestionIds) {
                val newSuggestions = suggestion.suggestionIds
                    .toMutableSet()
                    .apply { removeAll { it == id } }
                suggestionsToChange += suggestion.copy(
                    suggestionIds = newSuggestions,
                )
            }
        }

        remove(idsToRemove)
        add(suggestionsToChange)
    }
}