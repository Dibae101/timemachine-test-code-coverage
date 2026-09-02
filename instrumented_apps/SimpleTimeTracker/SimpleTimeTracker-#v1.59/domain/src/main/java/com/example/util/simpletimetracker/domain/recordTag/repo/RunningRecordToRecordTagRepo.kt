package com.example.util.simpletimetracker.domain.recordTag.repo

import com.example.util.simpletimetracker.domain.record.model.RecordBase

interface RunningRecordToRecordTagRepo {

    suspend fun addRunningRecordTags(runningRecordId: Long, tags: List<RecordBase.Tag>)

    suspend fun removeAllByTagId(tagId: Long)

    suspend fun removeAllByRunningRecordId(runningRecordId: Long)

    suspend fun clear()
}