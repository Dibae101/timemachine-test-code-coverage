package com.example.util.simpletimetracker.domain.recordTag.interactor

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag
import com.example.util.simpletimetracker.domain.recordTag.repo.RecordTypeToTagRepo
import javax.inject.Inject

class RecordTypeToTagInteractor @Inject constructor(
    private val repo: RecordTypeToTagRepo,
) {

    suspend fun getAll(): List<RecordTypeToTag> {
        return repo.getAll()
    }

    suspend fun getTags(typeId: Long): Set<Long> {
        return repo.getTagIdsByType(typeId)
    }

    suspend fun getTypes(tagId: Long): Set<Long> {
        return repo.getTypeIdsByTag(tagId)
    }

    suspend fun addTypes(tagId: Long, typeIds: List<Long>) {
        repo.addTypes(tagId, typeIds)
    }

    suspend fun addTags(typeId: Long, tagIds: List<Long>) {
        repo.addTags(typeId, tagIds)
    }

    suspend fun removeTypes(categoryId: Long, typeIds: List<Long>) {
        repo.removeTypes(categoryId, typeIds)
    }
}