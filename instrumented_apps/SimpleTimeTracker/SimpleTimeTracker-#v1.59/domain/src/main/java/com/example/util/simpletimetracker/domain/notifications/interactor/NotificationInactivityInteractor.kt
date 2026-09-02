package com.example.util.simpletimetracker.domain.notifications.interactor

interface NotificationInactivityInteractor {

    suspend fun checkAndSchedule()

    fun cancel()

    suspend fun show()
}