package com.example.util.simpletimetracker.feature_change_activity_filter.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.domain.activityFilter.model.ActivityFilter

data class ChangeActivityFilterTypeSwitchViewData(
    val type: ActivityFilter.Type,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = name.hashCode().toLong()
}