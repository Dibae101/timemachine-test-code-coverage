package com.example.util.simpletimetracker.feature_categories.model

import com.example.util.simpletimetracker.core.viewData.CommonOptionsListItem
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import kotlinx.parcelize.Parcelize

sealed interface CategoriesOptionsListItem : OptionsListParams.Item.Id {

    @Parcelize
    data object Filter : CategoriesOptionsListItem, CommonOptionsListItem.Filter

    @Parcelize
    data object EnabledSearch : CategoriesOptionsListItem, CommonOptionsListItem.EnabledSearch

    @Parcelize
    data object ShowRelations : CategoriesOptionsListItem

    @Parcelize
    data object Help : CategoriesOptionsListItem
}