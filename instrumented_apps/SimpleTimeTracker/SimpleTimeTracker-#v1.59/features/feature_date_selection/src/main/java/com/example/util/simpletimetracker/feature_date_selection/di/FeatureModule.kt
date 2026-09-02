package com.example.util.simpletimetracker.feature_date_selection.di

import com.example.util.simpletimetracker.feature_date_selection.api.DateSelectorMapper
import com.example.util.simpletimetracker.feature_date_selection.api.DateSelectorViewModelDelegate
import com.example.util.simpletimetracker.feature_date_selection.api.viewDelegate.DateSelectorViewDelegateProvider
import com.example.util.simpletimetracker.feature_date_selection.mapper.DateSelectorMapperImpl
import com.example.util.simpletimetracker.feature_date_selection.viewDelegate.DateSelectorViewDelegateProviderImpl
import com.example.util.simpletimetracker.feature_date_selection.viewModelDelegate.DateSelectorViewModelDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeatureModule {

    @Binds
    fun bindDateSelectorViewModelDelegate(impl: DateSelectorViewModelDelegateImpl): DateSelectorViewModelDelegate

    @Binds
    fun bindDateSelectorViewDelegateProvider(impl: DateSelectorViewDelegateProviderImpl): DateSelectorViewDelegateProvider

    @Binds
    fun bindDateSelectorMapper(impl: DateSelectorMapperImpl): DateSelectorMapper
}