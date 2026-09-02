package com.example.util.simpletimetracker.data_local.activitySuggestion

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activitySuggestion")
data class ActivitySuggestionDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "forTypeId")
    val forTypeId: Long,

    // Longs stored in string comma separated
    @ColumnInfo(name = "suggestionIds")
    val suggestionIds: String,
)