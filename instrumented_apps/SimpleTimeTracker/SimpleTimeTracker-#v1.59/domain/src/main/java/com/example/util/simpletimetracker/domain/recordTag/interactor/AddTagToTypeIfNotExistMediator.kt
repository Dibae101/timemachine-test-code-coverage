package com.example.util.simpletimetracker.domain.recordTag.interactor

import javax.inject.Inject

class AddTagToTypeIfNotExistMediator @Inject constructor(
    private val recordTypeToTagInteractor: RecordTypeToTagInteractor,
) {

    suspend fun execute(
        typeId: Long,
        tagIds: List<Long>,
    ) {
        val assignedTags = recordTypeToTagInteractor.getTags(typeId)
        val needToAdd = tagIds.filter { tagId ->
            // Already assignable.
            if (tagId in assignedTags) return@filter false
            val assignedToTypes = recordTypeToTagInteractor.getTypes(tagId)
            // General tag - no need to assign.
            return@filter assignedToTypes.isNotEmpty()
        }
        recordTypeToTagInteractor.addTags(typeId, needToAdd)
    }
}