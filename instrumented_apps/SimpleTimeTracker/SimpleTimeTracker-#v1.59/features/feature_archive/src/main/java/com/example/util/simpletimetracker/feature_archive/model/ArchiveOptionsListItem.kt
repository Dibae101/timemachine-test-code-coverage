package com.example.util.simpletimetracker.feature_archive.model

import com.example.util.simpletimetracker.core.viewData.CommonOptionsListItem
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import kotlinx.parcelize.Parcelize

sealed interface ArchiveOptionsListItem : OptionsListParams.Item.Id {

    @Parcelize
    data object EnabledSearch : ArchiveOptionsListItem, CommonOptionsListItem.EnabledSearch
}