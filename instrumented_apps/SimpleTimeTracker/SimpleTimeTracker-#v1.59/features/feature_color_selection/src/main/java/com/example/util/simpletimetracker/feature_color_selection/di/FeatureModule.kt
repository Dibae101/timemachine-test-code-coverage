package com.example.util.simpletimetracker.feature_color_selection.di

import com.example.util.simpletimetracker.feature_color_selection.ColorSelectionViewModelDelegateImpl
import com.example.util.simpletimetracker.feature_color_selection.api.ColorSelectionViewModelDelegate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeatureModule {

    @Binds
    fun bindColorSelectionViewModelDelegate(impl: ColorSelectionViewModelDelegateImpl): ColorSelectionViewModelDelegate
}