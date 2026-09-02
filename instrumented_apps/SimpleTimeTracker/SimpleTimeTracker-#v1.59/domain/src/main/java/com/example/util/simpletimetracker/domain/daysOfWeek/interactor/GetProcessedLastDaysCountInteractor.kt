package com.example.util.simpletimetracker.domain.daysOfWeek.interactor

import javax.inject.Inject

class GetProcessedLastDaysCountInteractor @Inject constructor() {

    fun execute(enteredCount: Long): Int {
        return enteredCount.toInt().coerceIn(2..365)
    }
}