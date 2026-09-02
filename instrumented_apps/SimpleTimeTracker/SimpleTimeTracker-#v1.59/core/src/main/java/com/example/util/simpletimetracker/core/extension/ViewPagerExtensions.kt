package com.example.util.simpletimetracker.core.extension

import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.findRecycler(): RecyclerView? {
    return children.filterIsInstance<RecyclerView>().firstOrNull()
}