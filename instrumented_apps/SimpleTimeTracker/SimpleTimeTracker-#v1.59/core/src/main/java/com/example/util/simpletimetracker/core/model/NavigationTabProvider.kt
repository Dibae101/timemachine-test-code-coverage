package com.example.util.simpletimetracker.core.model

import androidx.fragment.app.Fragment

fun interface NavigationTabProvider {
    fun provide(): Fragment
}