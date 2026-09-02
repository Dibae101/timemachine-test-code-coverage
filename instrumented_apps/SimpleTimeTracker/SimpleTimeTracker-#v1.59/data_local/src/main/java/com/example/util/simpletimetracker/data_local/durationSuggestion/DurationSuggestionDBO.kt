package com.example.util.simpletimetracker.data_local.durationSuggestion

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "durationSuggestions")
data class DurationSuggestionDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "valueSeconds")
    val valueSeconds: Long,
)