package com.example.util.simpletimetracker.data_local.favourite

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favouriteComments")
data class FavouriteCommentDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "comment")
    val comment: String,
)