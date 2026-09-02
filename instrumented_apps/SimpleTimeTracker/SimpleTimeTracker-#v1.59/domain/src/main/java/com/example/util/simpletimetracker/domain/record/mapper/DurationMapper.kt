package com.example.util.simpletimetracker.domain.record.mapper

import com.example.util.simpletimetracker.domain.extension.dropSeconds
import javax.inject.Inject

class DurationMapper @Inject constructor() {

    fun map(
        timeStarted: Long,
        timeEnded: Long,
        showSeconds: Boolean,
    ): Long {
        return if (showSeconds) {
            timeEnded - timeStarted
        } else {
            timeEnded.dropSeconds() - timeStarted.dropSeconds()
        }
    }
}