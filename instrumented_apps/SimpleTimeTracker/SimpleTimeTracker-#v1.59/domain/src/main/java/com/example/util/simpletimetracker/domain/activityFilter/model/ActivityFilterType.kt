package com.example.util.simpletimetracker.domain.activityFilter.model

sealed interface ActivityFilterType {
    data object Default : ActivityFilterType
    data object Predefined : ActivityFilterType
}