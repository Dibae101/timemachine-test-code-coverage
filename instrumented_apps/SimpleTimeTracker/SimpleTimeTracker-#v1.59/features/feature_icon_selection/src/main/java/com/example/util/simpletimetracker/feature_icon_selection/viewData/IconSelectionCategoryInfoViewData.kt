package com.example.util.simpletimetracker.feature_icon_selection.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionTypeViewData

data class IconSelectionCategoryInfoViewData(
    val type: IconSelectionTypeViewData,
    val text: String,
    val isLast: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = type.id

    override fun isValidType(other: ViewHolderType): Boolean =
        other is IconSelectionCategoryInfoViewData
}