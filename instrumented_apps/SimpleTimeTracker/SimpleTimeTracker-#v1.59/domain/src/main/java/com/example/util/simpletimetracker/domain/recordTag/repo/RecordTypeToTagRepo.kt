package com.example.util.simpletimetracker.domain.recordTag.repo

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag

interface RecordTypeToTagRepo {

    suspend fun getAll(): List<RecordTypeToTag>

    suspend fun getTagIdsByType(typeId: Long): Set<Long>

    suspend fun getTypeIdsByTag(tagId: Long): Set<Long>

    suspend fun add(recordTypeToTag: RecordTypeToTag)

    suspend fun addTypes(tagId: Long, typeIds: List<Long>)

    suspend fun addTags(typeId: Long, tagIds: List<Long>)

    suspend fun removeTypes(tagId: Long, typeIds: List<Long>)

    suspend fun removeAll(tagId: Long)

    suspend fun removeAllByType(typeId: Long)

    suspend fun clear()
}