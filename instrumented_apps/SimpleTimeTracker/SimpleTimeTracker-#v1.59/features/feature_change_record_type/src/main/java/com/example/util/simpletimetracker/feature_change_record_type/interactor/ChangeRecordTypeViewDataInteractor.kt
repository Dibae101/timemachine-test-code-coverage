package com.example.util.simpletimetracker.feature_change_record_type.interactor

import com.example.util.simpletimetracker.core.mapper.CategoryViewDataMapper
import com.example.util.simpletimetracker.core.mapper.CommonViewDataMapper
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.extension.addBetweenEach
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.divider.DividerViewData
import com.example.util.simpletimetracker.feature_change_record_type.viewData.ChangeRecordTypeCategoriesViewData
import javax.inject.Inject

class ChangeRecordTypeViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val categoryViewDataMapper: CategoryViewDataMapper,
    private val commonViewDataMapper: CommonViewDataMapper,
) {

    suspend fun getCategoriesViewData(
        selectedCategories: List<Long>,
    ): ChangeRecordTypeCategoriesViewData {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val categories = categoryInteractor.getAll()

        return if (categories.isNotEmpty()) {
            val selected = categories.filter { it.id in selectedCategories }
            val available = categories.filter { it.id !in selectedCategories }

            // Main hint
            val hintData = if (selected.isEmpty()) {
                listOf(categoryViewDataMapper.mapToCategoryHint())
            } else {
                emptyList()
            }

            // Selected
            val selectedData = mutableListOf<ViewHolderType>()
            if (selected.isNotEmpty()) {
                selectedData += commonViewDataMapper.mapSelected()
                selectedData += selected.map {
                    categoryViewDataMapper.mapCategory(
                        category = it,
                        isDarkTheme = isDarkTheme,
                    )
                }
            }

            // Available
            val availableData = mutableListOf<ViewHolderType>()
            if (available.isNotEmpty()) {
                availableData += commonViewDataMapper.mapAvailable()
                availableData += available.map {
                    categoryViewDataMapper.mapCategory(
                        category = it,
                        isDarkTheme = isDarkTheme,
                    )
                }
            }

            // Buttons
            val buttonsViewData = mutableListOf<ViewHolderType>()
            if (selected.isNotEmpty()) {
                buttonsViewData += categoryViewDataMapper.mapToUncategorizedItem(
                    isFiltered = false,
                    isDarkTheme = isDarkTheme,
                )
            }
            buttonsViewData += categoryViewDataMapper.mapToTypeTagAddItem(
                useShortName = true,
                isDarkTheme = isDarkTheme,
            )

            // All
            val viewData = listOf(
                hintData,
                selectedData,
                availableData,
                buttonsViewData,
            ).filter {
                it.isNotEmpty()
            }.addBetweenEach { index ->
                listOf(DividerViewData(index.toLong()))
            }.flatten()

            ChangeRecordTypeCategoriesViewData(
                selectedCount = selected.size,
                viewData = viewData,
            )
        } else {
            ChangeRecordTypeCategoriesViewData(
                selectedCount = 0,
                viewData = listOf(
                    categoryViewDataMapper.mapToCategoriesFirstHint(),
                    categoryViewDataMapper.mapToTypeTagAddItem(
                        useShortName = true,
                        isDarkTheme = isDarkTheme,
                    ),
                ),
            )
        }
    }
}