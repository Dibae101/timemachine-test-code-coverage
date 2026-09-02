package com.example.util.simpletimetracker.domain.extension

inline fun <T> List<T>.removeIf(crossinline filter: (T) -> Boolean): List<T> {
    return this.toMutableList().apply { removeAll { filter(it) } }
}

inline fun <T> Set<T>.removeIf(crossinline filter: (T) -> Boolean): Set<T> {
    return this.toMutableSet().apply { removeAll { filter(it) } }
}