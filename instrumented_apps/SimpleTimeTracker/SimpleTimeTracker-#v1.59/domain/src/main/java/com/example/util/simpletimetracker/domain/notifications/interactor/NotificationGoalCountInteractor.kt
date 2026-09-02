package com.example.util.simpletimetracker.domain.notifications.interactor

interface NotificationGoalCountInteractor {

    suspend fun checkAndShow(typeId: Long)
}