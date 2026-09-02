package com.example.util.simpletimetracker.domain.activityFilter.interactor

import com.example.util.simpletimetracker.domain.activityFilter.model.PredefinedFilter
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import javax.inject.Inject

class PredefinedFilterInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val categoryInteractor: CategoryInteractor,
) {

    suspend fun getAll(): List<PredefinedFilter> {
        if (!prefsInteractor.getShowCategoriesAsPredefinedFilters()) {
            return emptyList()
        }

        val categories = categoryInteractor.getAll()
        val selectedIds = prefsInteractor.getSelectedPredefinedFilters()
        return categories.map { category ->
            PredefinedFilter(
                categoryId = category.id,
                name = category.name,
                color = category.color,
                selected = category.id in selectedIds,
            )
        }
    }

    suspend fun changeSelected(id: Long, selected: Boolean) {
        val currentIds = prefsInteractor.getSelectedPredefinedFilters()
        val newIds = if (selected) {
            currentIds + id
        } else {
            currentIds.filter { it != id }
        }
        prefsInteractor.setSelectedPredefinedFilters(newIds)
    }

    suspend fun disableAll() {
        prefsInteractor.setSelectedPredefinedFilters(emptyList())
    }
}