package com.example.util.simpletimetracker.feature_change_record_tag.viewData

import com.example.util.simpletimetracker.domain.recordTag.model.RecordTagValueType
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData

data class ChangeRecordTagValueTypeViewData(
    val valueType: RecordTagValueType,
    override val name: String,
    override val isSelected: Boolean,
) : ButtonsRowViewData() {

    override val id: Long = valueType.ordinal.toLong()
}