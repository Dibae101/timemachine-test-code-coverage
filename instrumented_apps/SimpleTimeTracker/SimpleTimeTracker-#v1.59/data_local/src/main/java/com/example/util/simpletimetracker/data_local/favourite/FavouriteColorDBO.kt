package com.example.util.simpletimetracker.data_local.favourite

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favouriteColors")
data class FavouriteColorDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "color_int")
    val colorInt: String,
)