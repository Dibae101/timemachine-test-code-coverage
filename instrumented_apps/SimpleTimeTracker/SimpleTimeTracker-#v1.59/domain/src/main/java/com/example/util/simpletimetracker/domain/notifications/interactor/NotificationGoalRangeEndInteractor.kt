package com.example.util.simpletimetracker.domain.notifications.interactor

interface NotificationGoalRangeEndInteractor {

    suspend fun checkAndReschedule()

    fun cancel()
}