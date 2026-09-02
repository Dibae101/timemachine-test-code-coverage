package com.example.util.simpletimetracker.domain.base

data class OneShotValue<T>(
    private var value: T?,
) {

    fun getValue(): T? {
        return value.also { value = null }
    }
}