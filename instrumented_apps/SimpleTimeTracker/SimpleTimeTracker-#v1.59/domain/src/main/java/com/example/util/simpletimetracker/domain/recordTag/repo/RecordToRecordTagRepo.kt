package com.example.util.simpletimetracker.domain.recordTag.repo

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordToRecordTag

interface RecordToRecordTagRepo {

    suspend fun getAll(): List<RecordToRecordTag>

    suspend fun getRecordIdsByTagId(tagId: Long): List<Long>

    suspend fun add(recordToRecordTag: RecordToRecordTag)

    suspend fun addRecordTags(recordId: Long, tags: List<RecordBase.Tag>)

    suspend fun removeAllByTagId(tagId: Long)

    suspend fun removeAllByRecordId(recordId: Long)

    suspend fun clear()
}