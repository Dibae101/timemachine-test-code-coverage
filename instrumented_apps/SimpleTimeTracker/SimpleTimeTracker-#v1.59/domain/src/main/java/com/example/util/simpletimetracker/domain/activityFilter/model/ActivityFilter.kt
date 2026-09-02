package com.example.util.simpletimetracker.domain.activityFilter.model

import com.example.util.simpletimetracker.domain.color.model.AppColor

data class ActivityFilter(
    val id: Long = 0,
    val selectedIds: Set<Long>,
    val type: Type,
    val name: String,
    val color: AppColor,
    val selected: Boolean,
) {

    sealed interface Type {
        object Category : Type
        object Activity : Type
    }
}