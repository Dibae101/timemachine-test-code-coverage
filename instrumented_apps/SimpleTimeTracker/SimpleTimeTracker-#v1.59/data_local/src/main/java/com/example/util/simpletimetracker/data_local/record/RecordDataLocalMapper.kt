package com.example.util.simpletimetracker.data_local.record

import com.example.util.simpletimetracker.data_local.recordTag.RecordToRecordTagDBO
import com.example.util.simpletimetracker.domain.extension.dropMillis
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import javax.inject.Inject

class RecordDataLocalMapper @Inject constructor() {

    fun map(dbo: RecordWithRecordTagsDBO): Record {
        return Record(
            id = dbo.record.id,
            typeId = dbo.record.typeId,
            timeStarted = dbo.record.timeStarted,
            timeEnded = dbo.record.timeEnded,
            comment = dbo.record.comment,
            tags = dbo.recordTags.map(::map),
        )
    }

    fun map(domain: Record): RecordDBO {
        return RecordDBO(
            id = domain.id,
            typeId = domain.typeId,
            timeStarted = domain.timeStarted.dropMillis(),
            timeEnded = domain.timeEnded.dropMillis(),
            comment = domain.comment,
            tagId = 0,
        )
    }

    fun map(dbo: RecordToRecordTagDBO): RecordBase.Tag {
        return RecordBase.Tag(
            tagId = dbo.recordTagId,
            numericValue = dbo.recordTagNumericValue,
        )
    }
}