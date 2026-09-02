package com.example.util.simpletimetracker.feature_goals.di

import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.model.NavigationTabKey
import com.example.util.simpletimetracker.core.model.NavigationTabProvider
import com.example.util.simpletimetracker.feature_goals.view.GoalsFragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
class GoalsModule {

    @Provides
    @IntoMap
    @NavigationTabKey(NavigationTab.Goals::class)
    fun bindNavigationTab(): NavigationTabProvider {
        return NavigationTabProvider { GoalsFragment.newInstance() }
    }
}