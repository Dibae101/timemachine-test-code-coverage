package com.example.util.simpletimetracker.domain.record.model

sealed interface MultiSelectedRecordId {
    val id: Long

    data class Tracked(override val id: Long) : MultiSelectedRecordId

    data class Untracked(
        val timeStartedTimestamp: Long,
        val timeEndedTimestamp: Long,
    ) : MultiSelectedRecordId {
        override val id: Long = timeStartedTimestamp
        override fun equals(other: Any?): Boolean = (other as? Untracked)?.id == id
        override fun hashCode(): Int = id.hashCode()
    }

    data class Running(override val id: Long) : MultiSelectedRecordId
}