package com.example.util.simpletimetracker.domain.pomodoro.interactor

interface PomodoroCycleNotificationInteractor {

    suspend fun checkAndReschedule()

    fun cancel()
}