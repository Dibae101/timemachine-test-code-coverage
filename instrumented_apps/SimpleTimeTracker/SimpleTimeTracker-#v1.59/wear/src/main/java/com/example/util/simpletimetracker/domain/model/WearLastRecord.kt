package com.example.util.simpletimetracker.domain.model

data class WearLastRecord(
    val activityId: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val tags: List<WearCurrentActivity.Tag>,
)