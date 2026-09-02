package com.example.util.simpletimetracker.feature_change_activity_filter.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class ChangeActivityFilterTypesViewData(
    val typeHint: String,
    val selectedCount: Int,
    val viewData: List<ViewHolderType>,
)