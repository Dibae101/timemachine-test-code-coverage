package com.example.util.simpletimetracker.domain.recordTag.interactor

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import javax.inject.Inject

class GetSelectableTagsInteractor @Inject constructor(
    private val recordTypeToTagInteractor: RecordTypeToTagInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val filterSelectableTagsInteractor: FilterSelectableTagsInteractor,
) {

    suspend fun execute(vararg typeId: Long): List<RecordTag> {
        val tags = recordTagInteractor.getAll()
        val typesToTags = recordTypeToTagInteractor.getAll()
        val typeIds = typeId.filter { it != 0L }
        val selectableTagIds = filterSelectableTagsInteractor.execute(
            tagIds = tags.map { it.id },
            typesToTags = typesToTags,
            typeIds = emptyList(),
            byAllTypeIds = typeIds,
        )

        return tags.filter { it.id in selectableTagIds }
    }
}