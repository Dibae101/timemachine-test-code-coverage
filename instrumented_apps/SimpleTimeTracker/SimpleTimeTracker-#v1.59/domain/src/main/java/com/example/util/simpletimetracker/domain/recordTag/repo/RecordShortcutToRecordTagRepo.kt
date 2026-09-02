package com.example.util.simpletimetracker.domain.recordTag.repo

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordShortcutToRecordTag

interface RecordShortcutToRecordTagRepo {

    suspend fun getAll(): List<RecordShortcutToRecordTag>

    suspend fun add(recordShortcutToRecordTag: RecordShortcutToRecordTag)

    suspend fun addRecordTags(shortcutId: Long, tags: List<RecordBase.Tag>)

    suspend fun removeAllByTagId(tagId: Long)

    suspend fun removeAllByShortcutId(shortcutId: Long)

    suspend fun clear()
}