package com.example.util.simpletimetracker.domain.record.model

import com.example.util.simpletimetracker.domain.extension.orZero

data class MultitaskRecord(
    val records: List<Record>,
) : RecordBase {

    override val typeIds: List<Long> = records.map(Record::typeIds).flatten()
    override val timeStarted: Long = records.firstOrNull()?.timeStarted.orZero()
    override val timeEnded: Long = records.firstOrNull()?.timeEnded.orZero()
    override val comment: String = records.map(Record::comment).distinct().joinToString(separator = " ")
    override val tags: List<RecordBase.Tag> = records.map(Record::tags).flatten()
}