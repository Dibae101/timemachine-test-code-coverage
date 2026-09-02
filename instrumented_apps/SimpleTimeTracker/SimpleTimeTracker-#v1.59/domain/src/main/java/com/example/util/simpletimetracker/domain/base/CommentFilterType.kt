package com.example.util.simpletimetracker.domain.base

sealed interface CommentFilterType {
    data object Similar : CommentFilterType
    data object Favourite : CommentFilterType
    data object Last : CommentFilterType
}