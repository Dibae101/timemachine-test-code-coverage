package com.example.util.simpletimetracker.feature_date_selection.api.viewData

data class DateSelectorButtonsViewData(
    val addButton: Button,
    val optionsButton: Button,
) {

    sealed interface Button {
        data object Hidden : Button
        data class Visible(val iconResId: Int) : Button
    }
}