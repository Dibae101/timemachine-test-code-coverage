package com.example.util.simpletimetracker.feature_comment_selection.di

import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewDelegateProvider
import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_comment_selection.viewDelegate.CommentSelectionViewDelegateProviderImpl
import com.example.util.simpletimetracker.feature_comment_selection.viewModelDelegate.CommentSelectionViewModelDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeatureModule {

    @Binds
    fun bindCommentSelectionViewModelDelegate(impl: CommentSelectionViewModelDelegateImpl): CommentSelectionViewModelDelegate

    @Binds
    fun bindCommentSelectionViewDelegateProvider(impl: CommentSelectionViewDelegateProviderImpl): CommentSelectionViewDelegateProvider
}