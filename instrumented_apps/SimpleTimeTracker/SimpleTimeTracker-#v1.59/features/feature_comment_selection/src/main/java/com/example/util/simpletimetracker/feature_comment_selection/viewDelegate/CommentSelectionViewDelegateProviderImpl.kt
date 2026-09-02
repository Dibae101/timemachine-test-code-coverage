package com.example.util.simpletimetracker.feature_comment_selection.viewDelegate

import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewDelegate
import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewDelegateProvider
import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewModelDelegate
import javax.inject.Inject

class CommentSelectionViewDelegateProviderImpl @Inject constructor() : CommentSelectionViewDelegateProvider {

    override fun provide(
        viewModel: CommentSelectionViewModelDelegate,
    ): CommentSelectionViewDelegate {
        return CommentSelectionViewDelegateImpl(viewModel)
    }
}