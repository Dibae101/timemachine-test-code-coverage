package com.example.util.simpletimetracker.feature_records_filter.viewData

import com.example.util.simpletimetracker.feature_base_adapter.buttonDouble.DoubleButtonsViewData

data class RecordsFilterSelectionButtonType(
    val type: Type,
    val subtype: Subtype,
) : DoubleButtonsViewData.Type {

    sealed interface Type {
        data object Activities : Type
        data object Categories : Type
        data object Tags : Type
    }

    sealed interface Subtype {
        data object SelectAll : Subtype
        data object SelectNone : Subtype
    }
}
