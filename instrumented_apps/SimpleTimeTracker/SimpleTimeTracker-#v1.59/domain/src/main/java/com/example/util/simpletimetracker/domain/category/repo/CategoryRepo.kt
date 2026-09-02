package com.example.util.simpletimetracker.domain.category.repo

import com.example.util.simpletimetracker.domain.category.model.Category

interface CategoryRepo {

    suspend fun getAll(): List<Category>

    suspend fun get(id: Long): Category?

    suspend fun get(name: String): List<Category>

    suspend fun add(category: Category): Long

    suspend fun remove(id: Long)

    suspend fun clear()
}