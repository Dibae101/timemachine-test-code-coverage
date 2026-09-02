package com.example.util.simpletimetracker.core.viewData

import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import com.example.util.simpletimetracker.navigation.params.screen.RangeLengthParams
import kotlinx.parcelize.Parcelize

sealed interface RangeSelectionOptionsListItem : OptionsListParams.Item.Id {

    @Parcelize
    data class Simple(val rangeLengthParams: RangeLengthParams) : RangeSelectionOptionsListItem

    @Parcelize
    data object Custom : RangeSelectionOptionsListItem

    @Parcelize
    data object Last : RangeSelectionOptionsListItem

    @Parcelize
    data object SelectDate : RangeSelectionOptionsListItem
}