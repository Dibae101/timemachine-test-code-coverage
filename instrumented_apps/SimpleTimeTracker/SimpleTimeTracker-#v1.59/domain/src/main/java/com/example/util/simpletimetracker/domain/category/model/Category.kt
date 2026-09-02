package com.example.util.simpletimetracker.domain.category.model

import com.example.util.simpletimetracker.domain.color.model.AppColor

data class Category(
    val id: Long = 0,
    val name: String,
    val color: AppColor,
    val note: String,
)