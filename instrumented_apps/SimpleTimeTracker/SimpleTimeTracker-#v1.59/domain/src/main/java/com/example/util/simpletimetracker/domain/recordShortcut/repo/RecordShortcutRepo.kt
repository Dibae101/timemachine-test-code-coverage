package com.example.util.simpletimetracker.domain.recordShortcut.repo

import com.example.util.simpletimetracker.domain.recordShortcut.model.RecordShortcut

interface RecordShortcutRepo {

    suspend fun getAll(): List<RecordShortcut>

    suspend fun getByType(typeIds: List<Long>): List<RecordShortcut>

    suspend fun get(id: Long): RecordShortcut?

    suspend fun add(recordShortcut: RecordShortcut): Long

    suspend fun update(recordShortcut: RecordShortcut)

    suspend fun remove(id: Long)

    suspend fun removeByType(typeId: Long)

    suspend fun clear()
}