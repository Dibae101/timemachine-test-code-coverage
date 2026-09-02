package com.example.util.simpletimetracker.domain.record.model

data class FavouriteRecordsFilter(
    val id: Long,
    val filter: List<RecordsFilter>,
)