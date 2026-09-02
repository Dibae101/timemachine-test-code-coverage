package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.mapper.ActivityFilterViewDataMapper
import com.example.util.simpletimetracker.domain.activityFilter.interactor.ActivityFilterInteractor
import com.example.util.simpletimetracker.domain.activityFilter.interactor.PredefinedFilterInteractor
import com.example.util.simpletimetracker.domain.activityFilter.model.ActivityFilter
import com.example.util.simpletimetracker.domain.activityFilter.model.PredefinedFilter
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.extension.search
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordShortcut.model.RecordShortcut
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import javax.inject.Inject

class ActivityFilterViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val activityFilterInteractor: ActivityFilterInteractor,
    private val predefinedFilterInteractor: PredefinedFilterInteractor,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val activityFilterViewDataMapper: ActivityFilterViewDataMapper,
) {

    suspend fun getFilter(): Filter {
        return if (prefsInteractor.getShowActivityFilters()) {
            Filter.ApplyFilter(
                userFilters = activityFilterInteractor.getAll(),
                predefinedFilters = predefinedFilterInteractor.getAll(),
            )
        } else {
            Filter.NoFilter
        }
    }

    fun getFilterViewData(
        filter: Filter,
        searchText: String,
        isDarkTheme: Boolean,
        isFiltersCollapsed: Boolean,
        appendAddButton: Boolean,
    ): List<ViewHolderType> {
        return when (filter) {
            is Filter.NoFilter -> {
                emptyList()
            }
            is Filter.ApplyFilter -> {
                val filtersSize = filter.userFilters.size + filter.predefinedFilters.size

                val result = mutableListOf<ViewHolderType>()
                if (!isFiltersCollapsed) {
                    result += filter.predefinedFilters.map {
                        activityFilterViewDataMapper.mapFiltered(
                            filter = it,
                            isDarkTheme = isDarkTheme,
                            selected = it.selected,
                        )
                    }.search(searchText, { name })
                    result += filter.userFilters.map {
                        activityFilterViewDataMapper.mapFiltered(
                            filter = it,
                            isDarkTheme = isDarkTheme,
                            selected = it.selected,
                        )
                    }.search(searchText, { name })
                }
                // Show collapse button if there are several filters,
                // or if they are collapsed, just in case (collapse and then remove all but one).
                if (filtersSize > 1 || isFiltersCollapsed) {
                    result += activityFilterViewDataMapper.mapToActivityFilterToggleItem(
                        isFiltersCollapsed = isFiltersCollapsed,
                        isDarkTheme = isDarkTheme,
                    )
                }
                if (appendAddButton && !isFiltersCollapsed) {
                    result += activityFilterViewDataMapper.mapToActivityFilterAddItem(
                        isDarkTheme = isDarkTheme,
                    )
                }
                result
            }
        }
    }

    suspend fun applyFilter(
        list: List<RecordType>,
        filter: Filter,
    ): List<RecordType> {
        return applyFilter(
            list = list,
            filter = filter,
            predicate = { data, typeIds -> data.id in typeIds },
        )
    }

    suspend fun applyFilterToShortcuts(
        list: List<RecordShortcut>,
        filter: Filter,
    ): List<RecordShortcut> {
        return applyFilter(
            list = list,
            filter = filter,
            predicate = { data, typeIds ->
                when (val target = data.target) {
                    is RecordShortcut.Target.Record -> target.typeId in typeIds
                    is RecordShortcut.Target.Setting -> true
                }
            },
        )
    }

    private suspend fun <T> applyFilter(
        list: List<T>,
        filter: Filter,
        predicate: (data: T, typeIds: List<Long>) -> Boolean,
    ): List<T> {
        if (filter !is Filter.ApplyFilter) return list

        val hasAnySelectedFilters = filter.userFilters.any { it.selected } ||
            filter.predefinedFilters.any { it.selected }

        return if (hasAnySelectedFilters) {
            val selectedTypes = getSelectedTypeIds(filter)
            list.filter { predicate(it, selectedTypes) }
        } else {
            list
        }
    }

    private suspend fun getSelectedTypeIds(
        filter: Filter.ApplyFilter,
    ): List<Long> {
        val selectedFilters = filter.userFilters.filter { it.selected }
        val predefinedSelectedFilters = filter.predefinedFilters.filter { it.selected }

        if (selectedFilters.isEmpty() &&
            predefinedSelectedFilters.isEmpty()
        ) {
            return emptyList()
        }

        val activityIds: List<Long> = selectedFilters
            .filter { it.type is ActivityFilter.Type.Activity }
            .map { it.selectedIds }
            .flatten()

        val fromCategoryIds: List<Long> = selectedFilters
            .filter { it.type is ActivityFilter.Type.Category }
            .map { it.selectedIds }
            .flatten()
            .takeUnless { it.isEmpty() }
            ?.let { getTypeIdsFromCategories(it) }
            .orEmpty()

        val fromPredefinedFilters: List<Long> = predefinedSelectedFilters
            .map { it.categoryId }
            .takeUnless { it.isEmpty() }
            ?.let { getTypeIdsFromCategories(it) }
            .orEmpty()

        return (activityIds + fromCategoryIds + fromPredefinedFilters).distinct()
    }

    private suspend fun getTypeIdsFromCategories(
        tagIds: List<Long>,
    ): List<Long> {
        val recordTypeCategories = recordTypeCategoryInteractor.getAll()
            .groupBy { it.categoryId }
            .mapValues { (_, value) -> value.map { it.recordTypeId } }
        return tagIds.mapNotNull { tagId -> recordTypeCategories[tagId] }.flatten()
    }

    sealed interface Filter {
        data object NoFilter : Filter
        data class ApplyFilter(
            val userFilters: List<ActivityFilter>,
            val predefinedFilters: List<PredefinedFilter>,
        ) : Filter
    }
}