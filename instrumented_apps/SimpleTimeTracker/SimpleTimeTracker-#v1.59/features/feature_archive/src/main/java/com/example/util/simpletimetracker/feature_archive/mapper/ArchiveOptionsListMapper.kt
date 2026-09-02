package com.example.util.simpletimetracker.feature_archive.mapper

import com.example.util.simpletimetracker.core.mapper.OptionsListItemMapper
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_archive.model.ArchiveOptionsListItem
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import javax.inject.Inject

class ArchiveOptionsListMapper @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val optionsListItemMapper: OptionsListItemMapper,
) {

    suspend fun map(): List<OptionsListParams.Item> {
        val result = mutableListOf<OptionsListParams.Item>()

        result += optionsListItemMapper.mapCommonItem(
            id = ArchiveOptionsListItem.EnabledSearch,
            isIconCheckVisible = prefsInteractor.getIsArchiveSearchEnabled(),
        )

        return result
    }
}