package com.example.util.simpletimetracker.domain.recordTag.interactor

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag
import javax.inject.Inject

class FilterSelectableTagsInteractor @Inject constructor() {

    fun execute(
        tagIds: List<Long>,
        typesToTags: List<RecordTypeToTag>,
        typeIds: List<Long>,
        byAllTypeIds: List<Long> = emptyList(),
    ): List<Long> {
        val tagsToAssignedTypes = typesToTags
            .groupBy { it.tagId }
            .mapValues { (_, typeToTag) ->
                typeToTag.map { it.recordTypeId }
            }
        val tagIdsAssignedToAnyType = tagsToAssignedTypes
            .map { it.key }
            .toSet()

        return tagIds.filter { tagId ->
            val assignedTypes = tagsToAssignedTypes[tagId].orEmpty()
            // Tag can be assigned to any type.
            typeIds.any { it in assignedTypes } ||
                (byAllTypeIds.isNotEmpty() && byAllTypeIds.all { it in assignedTypes }) ||
                // Tag can be assigned to any type.
                tagId !in tagIdsAssignedToAnyType
        }
    }
}