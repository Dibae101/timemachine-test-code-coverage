package com.example.util.simpletimetracker.feature_categories.mapper

import com.example.util.simpletimetracker.core.mapper.OptionsListItemMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_categories.R
import com.example.util.simpletimetracker.feature_categories.interactor.CategoriesViewDataInteractor
import com.example.util.simpletimetracker.feature_categories.model.CategoriesOptionsListItem
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import javax.inject.Inject

class CategoriesOptionsListMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
    private val categoriesViewDataInteractor: CategoriesViewDataInteractor,
    private val optionsListItemMapper: OptionsListItemMapper,
) {

    suspend fun map(
        selectedIds: List<Long>,
    ): List<OptionsListParams.Item> {
        val result = mutableListOf<OptionsListParams.Item>()
        val shouldShowRelations = categoriesViewDataInteractor.hasData()

        result += OptionsListParams.Item(
            id = CategoriesOptionsListItem.Help,
            text = resourceRepo.getString(R.string.help),
            icon = R.drawable.unknown,
        )

        result += optionsListItemMapper.mapCommonItem(
            id = CategoriesOptionsListItem.EnabledSearch,
            isIconCheckVisible = prefsInteractor.getIsCategoriesSearchEnabled(),
        )

        result += optionsListItemMapper.mapCommonItem(
            id = CategoriesOptionsListItem.Filter,
            isIconCheckVisible = selectedIds.isNotEmpty(),
        )

        if (shouldShowRelations) {
            result += OptionsListParams.Item(
                id = CategoriesOptionsListItem.ShowRelations,
                text = resourceRepo.getString(R.string.categories_show_relations),
                icon = R.drawable.list,
                isIconCheckVisible = prefsInteractor.getIsCategoriesRelationsEnabled(),
            )
        }

        return result
    }
}