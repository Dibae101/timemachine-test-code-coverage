package com.example.util.simpletimetracker.domain.base

import javax.inject.Inject

class CurrentTimestampProvider @Inject constructor() {

    fun get(): Long {
        return System.currentTimeMillis()
    }
}