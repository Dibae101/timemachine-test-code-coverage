package com.example.util.simpletimetracker.domain.activityFilter.model

import com.example.util.simpletimetracker.domain.color.model.AppColor

data class PredefinedFilter(
    val categoryId: Long,
    val name: String,
    val color: AppColor,
    val selected: Boolean,
)