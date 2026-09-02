package com.example.util.simpletimetracker.feature_comment_selection.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.interactor.RecordCommentSearchViewDataInteractor
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.favourite.interactor.IsCommentFavouriteInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.commentChangeField.ChangeRecordCommentFieldViewData
import javax.inject.Inject

class CommentSelectionDelegateViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val isCommentFavouriteInteractor: IsCommentFavouriteInteractor,
    private val resourceRepo: ResourceRepo,
    private val colorMapper: ColorMapper,
    private val recordCommentSearchViewDataInteractor: RecordCommentSearchViewDataInteractor,
) {

    // TODO replace fav to text button?
    suspend fun getCommentsViewData(
        comment: String,
        typeId: Long,
        fromCommentChange: Boolean,
    ): List<ViewHolderType> {
        val items = mutableListOf<ViewHolderType>()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val isFavourite = isCommentFavouriteInteractor.execute(comment, typeId)

        ChangeRecordCommentFieldViewData(
            // Only one at the time.
            id = 1L,
            // Do not update text if update coming from typing.
            text = if (fromCommentChange) null else comment,
            iconColor = if (isFavourite) {
                resourceRepo.getThemedAttr(R.attr.colorAccent, isDarkTheme)
            } else {
                colorMapper.toInactiveColor(isDarkTheme)
            },
        ).let(items::add)

        items += recordCommentSearchViewDataInteractor.getViewData(
            comment = comment,
            typeId = typeId,
            isSettingsAvailable = true,
        )

        return items
    }
}