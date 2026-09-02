package com.example.util.simpletimetracker.feature_main.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.model.NavigationTabProvider

class MainContentAdapter(
    fragment: Fragment,
    tabs: List<NavigationTab>,
    providers: Map<Class<out NavigationTab>, NavigationTabProvider>,
) : FragmentStateAdapter(fragment) {

    private val fragments = tabs.mapNotNull {
        providers[it::class.java]
    }.map {
        lazy { it.provide() }
    }

    override fun getItemCount(): Int =
        fragments.size

    override fun createFragment(position: Int): Fragment =
        fragments[position].value
}