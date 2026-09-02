package com.example.util.simpletimetracker.feature_views.extension

inline fun <T> T?.ifNull(newValue: () -> T): T {
    return this ?: newValue()
}