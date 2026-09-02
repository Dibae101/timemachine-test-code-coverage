package com.example.util.simpletimetracker.domain.daysOfWeek.interactor

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek

interface GetCurrentDayInteractor {

    suspend fun execute(timestamp: Long): DayOfWeek
}