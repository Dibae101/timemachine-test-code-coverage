package com.example.util.simpletimetracker.domain.recordTag.interactor

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTagValueType
import javax.inject.Inject

class NeedTagValueSelectionInteractor @Inject constructor(
    private val recordTagInteractor: RecordTagInteractor,
) {

    fun execute(
        selectedTagIds: List<Long>,
        clickedTag: RecordTag?,
    ): Boolean {
        clickedTag ?: return false
        val isSelected = clickedTag.id in selectedTagIds

        return !isSelected && clickedTag.valueType == RecordTagValueType.NUMERIC
    }

    suspend fun execute(
        selectedTagIds: List<Long>,
        clickedTagId: Long,
    ): Boolean {
        return execute(
            selectedTagIds = selectedTagIds,
            clickedTag = recordTagInteractor.get(clickedTagId),
        )
    }
}