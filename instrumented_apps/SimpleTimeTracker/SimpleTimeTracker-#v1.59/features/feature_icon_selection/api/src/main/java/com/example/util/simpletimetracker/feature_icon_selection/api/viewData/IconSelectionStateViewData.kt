package com.example.util.simpletimetracker.feature_icon_selection.api.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

sealed interface IconSelectionStateViewData {

    data class Icons(
        val items: List<ViewHolderType>,
    ) : IconSelectionStateViewData

    object Text : IconSelectionStateViewData
}