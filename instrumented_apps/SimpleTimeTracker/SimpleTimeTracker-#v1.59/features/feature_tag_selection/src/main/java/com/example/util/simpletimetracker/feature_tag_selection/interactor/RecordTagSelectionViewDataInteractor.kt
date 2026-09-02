package com.example.util.simpletimetracker.feature_tag_selection.interactor

import com.example.util.simpletimetracker.core.interactor.RecordCommentSearchViewDataInteractor
import com.example.util.simpletimetracker.core.interactor.RecordTagViewDataInteractor
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.EmptySpaceViewData
import com.example.util.simpletimetracker.feature_tag_selection.R
import com.example.util.simpletimetracker.feature_tag_selection.adapter.RecordTagSelectionTextViewData
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagSelectionParams
import javax.inject.Inject

class RecordTagSelectionViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val recordTagViewDataInteractor: RecordTagViewDataInteractor,
    private val recordCommentSearchViewDataInteractor: RecordCommentSearchViewDataInteractor,
) {

    suspend fun getViewData(
        extra: RecordTagSelectionParams,
        selectedTags: List<RecordBase.Tag>,
        multipleChoiceAvailable: Boolean,
        comment: String,
        tagSearch: String,
        fromCommentChange: Boolean,
        fromSearchChange: Boolean,
    ): List<ViewHolderType> {
        val typeId = extra.typeId
        val shouldShowCommentSelection = RecordTagSelectionParams.FieldParam.Comment in extra.fields
        val shouldShowTagSelection = RecordTagSelectionParams.FieldParam.Tags in extra.fields

        val result: MutableList<ViewHolderType> = mutableListOf()

        if (shouldShowCommentSelection) {
            result += RecordTagSelectionTextViewData(
                text = resourceRepo.getString(R.string.change_record_comment_field),
            )

            result += CommentFieldViewData(
                id = 1L, // Only one at the time.
                text = if (fromCommentChange) null else comment,
                marginTopDp = 0,
                marginHorizontal = resourceRepo.getDimenInDp(R.dimen.edit_screen_margin_horizontal),
                hint = resourceRepo.getString(R.string.change_record_comment_hint),
                valueType = CommentFieldViewData.ValueType.TextMultiLine,
            )

            result += recordCommentSearchViewDataInteractor.getViewData(
                comment = comment,
                typeId = typeId,
                isSettingsAvailable = false,
            )
        }

        if (shouldShowTagSelection) {
            if (shouldShowCommentSelection) {
                result += EmptySpaceViewData(
                    id = "record_tag_selection_divider_space".hashCode().toLong(),
                    width = EmptySpaceViewData.ViewDimension.MatchParent,
                    height = EmptySpaceViewData.ViewDimension.ExactSizeDp(16),
                )
            }

            result += RecordTagSelectionTextViewData(
                text = resourceRepo.getString(R.string.record_tag_selection_hint),
            )

            result += EmptySpaceViewData(
                id = "record_tag_selection_tags_list_divider_space".hashCode().toLong(),
                width = EmptySpaceViewData.ViewDimension.MatchParent,
                height = EmptySpaceViewData.ViewDimension.ExactSizeDp(8),
            )

            result += recordTagViewDataInteractor.getViewData(
                selectedTags = selectedTags,
                typeIds = listOf(typeId),
                multipleChoiceAvailable = multipleChoiceAvailable,
                showBigEmptyHint = false,
                showHint = false,
                showArchived = false,
                searchText = tagSearch,
                fromSearchChange = fromSearchChange,
                buttons = listOf(
                    RecordTagViewDataInteractor.Button.UNTAGGED,
                    RecordTagViewDataInteractor.Button.SEARCH,
                    RecordTagViewDataInteractor.Button.ALL_TAGS,
                ),
            ).data
        }

        result += EmptySpaceViewData(
            id = "record_tag_selection_end_space".hashCode().toLong(),
            width = EmptySpaceViewData.ViewDimension.MatchParent,
            height = EmptySpaceViewData.ViewDimension.ExactSizeDp(8),
        )

        return result
    }
}