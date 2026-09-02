package com.example.util.simpletimetracker.domain.activitySuggestion.repo

import com.example.util.simpletimetracker.domain.activitySuggestion.model.ActivitySuggestion

interface ActivitySuggestionRepo {

    suspend fun getAll(): List<ActivitySuggestion>

    suspend fun get(id: Long): ActivitySuggestion?

    suspend fun getByTypeId(typeId: Long): List<ActivitySuggestion>

    suspend fun add(activityFilters: List<ActivitySuggestion>)

    suspend fun remove(ids: List<Long>)

    suspend fun clear()
}