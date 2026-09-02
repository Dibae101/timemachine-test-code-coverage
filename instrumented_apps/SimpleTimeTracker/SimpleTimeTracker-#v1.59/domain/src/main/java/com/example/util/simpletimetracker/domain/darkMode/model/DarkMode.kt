package com.example.util.simpletimetracker.domain.darkMode.model

sealed interface DarkMode {
    object System : DarkMode
    object Enabled : DarkMode
    object Disabled : DarkMode
}