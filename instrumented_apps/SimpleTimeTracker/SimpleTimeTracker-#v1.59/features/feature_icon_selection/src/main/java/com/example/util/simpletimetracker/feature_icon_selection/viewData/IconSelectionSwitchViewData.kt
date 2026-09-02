package com.example.util.simpletimetracker.feature_icon_selection.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.domain.icon.IconType

data class IconSelectionSwitchViewData(
    val iconType: IconType,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = iconType.ordinal.toLong()
}