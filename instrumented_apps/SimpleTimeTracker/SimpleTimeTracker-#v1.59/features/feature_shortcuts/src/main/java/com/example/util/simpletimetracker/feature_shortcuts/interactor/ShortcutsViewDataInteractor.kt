package com.example.util.simpletimetracker.feature_shortcuts.interactor

import com.example.util.simpletimetracker.core.mapper.RecordShortcutViewDataMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordShortcut.interactor.RecordShortcutInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.EmptySpaceViewData
import com.example.util.simpletimetracker.feature_shortcuts.R
import com.example.util.simpletimetracker.feature_shortcuts.mapper.ShortcutsViewDataMapper
import javax.inject.Inject

class ShortcutsViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordShortcutInteractor: RecordShortcutInteractor,
    private val recordShortcutViewDataMapper: RecordShortcutViewDataMapper,
    private val shortcutsViewDataMapper: ShortcutsViewDataMapper,
) {

    suspend fun getViewData(
        navBarHeightDp: Int,
    ): List<ViewHolderType> {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val recordTypesMap = recordTypeInteractor.getAll().associateBy { it.id }
        val recordTags = recordTagInteractor.getAll()
        val shortcuts = recordShortcutInteractor.getAll()

        val addItem = listOf(shortcutsViewDataMapper.mapAddItem(isDarkTheme))

        val content = shortcuts.map { shortcut ->
            recordShortcutViewDataMapper.map(
                shortcut = shortcut,
                typesMap = recordTypesMap,
                tags = recordTags,
                isDarkTheme = isDarkTheme,
                isFiltered = false,
                isEnabled = false,
            )
        }.ifEmpty { mapEmptyData() }

        val bottomSpace = EmptySpaceViewData(
            id = "shortcuts_nav_bar_space".hashCode().toLong(),
            height = EmptySpaceViewData.ViewDimension.ExactSizeDp(navBarHeightDp),
            wrapBefore = true,
        )

        return addItem + content + bottomSpace
    }

    private fun mapEmptyData(): List<ViewHolderType> {
        return listOf(
            EmptyViewData(
                message = resourceRepo.getString(R.string.no_data),
                hint = "",
            ),
        )
    }
}
