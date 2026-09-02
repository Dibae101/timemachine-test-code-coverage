package com.example.util.simpletimetracker.feature_comment_selection.api

interface CommentSelectionViewDelegateProvider {
    fun provide(viewModel: CommentSelectionViewModelDelegate): CommentSelectionViewDelegate
}