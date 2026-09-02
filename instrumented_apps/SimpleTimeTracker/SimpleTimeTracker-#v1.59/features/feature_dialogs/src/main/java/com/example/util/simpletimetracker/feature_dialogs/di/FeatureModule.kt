package com.example.util.simpletimetracker.feature_dialogs.di

import com.example.util.simpletimetracker.feature_dialogs.api.interactor.CardOrderChangedInteractor
import com.example.util.simpletimetracker.feature_dialogs.cardOrder.interactor.CardOrderChangedInteractorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeatureModule {

    @Binds
    fun bindCardOrderChangedInteractor(impl: CardOrderChangedInteractorImpl): CardOrderChangedInteractor
}