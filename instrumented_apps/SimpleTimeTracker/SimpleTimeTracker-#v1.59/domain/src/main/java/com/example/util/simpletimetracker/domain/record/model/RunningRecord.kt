package com.example.util.simpletimetracker.domain.record.model

import com.example.util.simpletimetracker.domain.extension.dropMillis

data class RunningRecord(
    val id: Long,
    override val timeStarted: Long,
    override val comment: String,
    override val tags: List<RecordBase.Tag>,
) : RecordBase {

    override val typeIds: List<Long> = listOf(id)
    override val timeEnded: Long get() = System.currentTimeMillis().dropMillis()
}