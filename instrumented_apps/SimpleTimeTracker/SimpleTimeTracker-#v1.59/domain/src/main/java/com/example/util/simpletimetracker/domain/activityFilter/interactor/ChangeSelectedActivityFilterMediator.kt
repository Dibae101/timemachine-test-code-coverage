package com.example.util.simpletimetracker.domain.activityFilter.interactor

import com.example.util.simpletimetracker.domain.activityFilter.model.ActivityFilterType
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class ChangeSelectedActivityFilterMediator @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val activityFilterInteractor: ActivityFilterInteractor,
    private val predefinedFilterInteractor: PredefinedFilterInteractor,
) {

    suspend fun onFilterClicked(
        id: Long,
        type: ActivityFilterType,
        selected: Boolean,
    ) {
        val newValue = !selected
        if (newValue && !prefsInteractor.getAllowMultipleActivityFilters()) {
            activityFilterInteractor.disableAll()
            predefinedFilterInteractor.disableAll()
        }
        when (type) {
            is ActivityFilterType.Default -> {
                activityFilterInteractor.changeSelected(id, newValue)
            }
            is ActivityFilterType.Predefined -> {
                predefinedFilterInteractor.changeSelected(id, newValue)
            }
        }
    }
}