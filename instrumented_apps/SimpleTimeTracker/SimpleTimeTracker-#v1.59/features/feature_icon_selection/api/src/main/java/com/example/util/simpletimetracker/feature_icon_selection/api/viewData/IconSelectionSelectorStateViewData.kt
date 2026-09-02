package com.example.util.simpletimetracker.feature_icon_selection.api.viewData

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.domain.icon.IconImageState

sealed interface IconSelectionSelectorStateViewData {

    data class Available(
        val state: IconImageState,
        @ColorInt val searchButtonColor: Int,
        @ColorInt val favouriteButtonColor: Int,
    ) : IconSelectionSelectorStateViewData

    object None : IconSelectionSelectorStateViewData
}