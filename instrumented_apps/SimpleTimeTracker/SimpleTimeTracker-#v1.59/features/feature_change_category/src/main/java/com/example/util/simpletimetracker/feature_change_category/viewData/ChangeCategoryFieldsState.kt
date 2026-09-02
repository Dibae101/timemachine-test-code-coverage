package com.example.util.simpletimetracker.feature_change_category.viewData

import com.example.util.simpletimetracker.core.view.ViewChooserStateDelegate

data class ChangeCategoryFieldsState(
    val chooserState: ViewChooserStateDelegate.States,
    val additionalFieldsVisible: Boolean,
)
