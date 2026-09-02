package com.example.util.simpletimetracker.feature_icon_selection.di

import com.example.util.simpletimetracker.feature_icon_selection.api.IconSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.mapper.IconSelectionMapper
import com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate.IconSelectionViewDelegateProvider
import com.example.util.simpletimetracker.feature_icon_selection.viewDelegate.IconSelectionViewDelegateProviderImpl
import com.example.util.simpletimetracker.feature_icon_selection.viewModelDelegate.IconSelectionViewModelDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.util.simpletimetracker.feature_icon_selection.mapper.IconSelectionMapper as IconSelectionMapperImpl

@Module
@InstallIn(SingletonComponent::class)
interface FeatureModule {

    @Binds
    fun bindIconSelectionViewModelDelegate(impl: IconSelectionViewModelDelegateImpl): IconSelectionViewModelDelegate

    @Binds
    fun bindIconSelectionViewDelegateProvider(impl: IconSelectionViewDelegateProviderImpl): IconSelectionViewDelegateProvider

    @Binds
    fun bindIconSelectionMapper(impl: IconSelectionMapperImpl): IconSelectionMapper
}