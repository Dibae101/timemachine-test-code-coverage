package com.example.util.simpletimetracker.feature_settings.partialRestore.interactor

import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.backup.model.PartialBackupRestoreData
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData
import com.example.util.simpletimetracker.feature_settings.R
import com.example.util.simpletimetracker.feature_settings.partialRestore.mapper.PartialRestoreViewDataMapper
import com.example.util.simpletimetracker.feature_settings.partialRestore.model.PartialRestoreFilterType
import com.example.util.simpletimetracker.feature_settings.partialRestore.utils.getIds
import javax.inject.Inject

class PartialRestoreViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val mapper: PartialRestoreViewDataMapper,
    private val colorMapper: ColorMapper,
    private val resourceRepo: ResourceRepo,
) {

    fun getInitialFilters(
        data: PartialBackupRestoreData,
    ): Map<PartialRestoreFilterType, Set<Long>> {
        return availableFilters.filter {
            data.getIds(it, existing = false).isNotEmpty()
        }.associateWith {
            emptySet()
        }
    }

    suspend fun getFiltersViewData(
        data: PartialBackupRestoreData,
        filters: Map<PartialRestoreFilterType, Set<Long>>,
    ): List<ViewHolderType> {
        val isDarkTheme = prefsInteractor.getDarkMode()

        return filters.toList().mapIndexed { index, (type, ids) ->
            val allIds = data.getIds(type, existing = false)
            val selectedIds = allIds.filter { it !in ids }
            val selected = selectedIds.isNotEmpty()
            val name = mapper.mapFilterName(
                filter = type,
                selectedIds = selectedIds,
            )
            val color = if (selected) {
                colorMapper.toActiveColor(isDarkTheme)
            } else {
                colorMapper.toInactiveColor(isDarkTheme)
            }
            FilterViewData(
                id = index.toLong(),
                type = type,
                name = name,
                color = color,
                isBtnVisible = selected,
                selected = selected,
            )
        }.ifEmpty {
            HintViewData(
                resourceRepo.getString(R.string.no_data),
            ).let(::listOf)
        }
    }

    companion object {
        private val availableFilters = listOfNotNull(
            PartialRestoreFilterType.Activities,
            PartialRestoreFilterType.Categories,
            PartialRestoreFilterType.Tags,
            PartialRestoreFilterType.Records,
            PartialRestoreFilterType.RecordShortcuts,
            PartialRestoreFilterType.ActivityFilters,
            PartialRestoreFilterType.FavouriteComments,
            PartialRestoreFilterType.FavouriteColors,
            PartialRestoreFilterType.FavouriteIcons,
            PartialRestoreFilterType.ComplexRules,
            PartialRestoreFilterType.ActivitySuggestions,
        )
    }
}