package com.example.util.simpletimetracker.feature_records_filter.di

import com.example.util.simpletimetracker.feature_records_filter.api.RecordsFilterExcludeInteractor
import com.example.util.simpletimetracker.feature_records_filter.interactor.RecordsFilterExcludeInteractorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RecordsFilterModule {

    @Binds
    fun bindRecordsFilterExcludeInteractor(impl: RecordsFilterExcludeInteractorImpl): RecordsFilterExcludeInteractor
}