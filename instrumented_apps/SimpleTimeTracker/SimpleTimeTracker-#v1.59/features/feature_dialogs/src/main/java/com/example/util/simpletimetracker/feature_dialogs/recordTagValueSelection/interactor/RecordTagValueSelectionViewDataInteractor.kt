package com.example.util.simpletimetracker.feature_dialogs.recordTagValueSelection.interactor

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_dialogs.R
import javax.inject.Inject

class RecordTagValueSelectionViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
) {

    fun loadViewData(
        value: Double?,
        fromCommentChange: Boolean,
    ): List<ViewHolderType> {
        val result: MutableList<ViewHolderType> = mutableListOf()

        result += CommentFieldViewData(
            id = 1L, // Only one at the time.
            text = if (fromCommentChange) null else value?.toString().orEmpty(),
            marginTopDp = 0,
            marginHorizontal = resourceRepo.getDimenInDp(R.dimen.edit_screen_margin_horizontal),
            hint = resourceRepo.getString(R.string.change_record_type_value_selection_hint),
            valueType = CommentFieldViewData.ValueType.NumberDecimal,
        )

        return result
    }
}