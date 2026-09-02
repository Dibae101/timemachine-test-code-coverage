package com.example.util.simpletimetracker.feature_icon_selection.api.viewData

sealed class IconSelectionScrollViewData {

    data class ScrollTo(val position: Int) : IconSelectionScrollViewData()
    object NoScroll : IconSelectionScrollViewData()
}