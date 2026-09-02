package com.example.util.simpletimetracker.feature_change_goals.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.domain.recordType.model.RecordTypeGoal

data class ChangeRecordTypeGoalSubtypeViewData(
    val subtype: RecordTypeGoal.Subtype,
    override val name: String,
    override val isSelected: Boolean,
    override val textSizeSp: Int?,
) : ButtonsRowViewData() {

    override val id: Long = subtype::class.java.simpleName.hashCode().toLong()
}