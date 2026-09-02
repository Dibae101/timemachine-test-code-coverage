package com.example.util.simpletimetracker.domain.durationSuggestion.repo

import com.example.util.simpletimetracker.domain.durationSuggestion.model.DurationSuggestion

interface DurationSuggestionRepo {

    suspend fun getAll(): List<DurationSuggestion>

    suspend fun add(data: DurationSuggestion): Long

    suspend fun remove(id: Long)

    suspend fun clear()
}