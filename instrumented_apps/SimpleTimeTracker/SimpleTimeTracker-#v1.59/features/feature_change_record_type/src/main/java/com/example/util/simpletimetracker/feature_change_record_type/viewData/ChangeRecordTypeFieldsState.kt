package com.example.util.simpletimetracker.feature_change_record_type.viewData

import com.example.util.simpletimetracker.core.view.ViewChooserStateDelegate

data class ChangeRecordTypeFieldsState(
    val chooserState: ViewChooserStateDelegate.States,
    val additionalFieldsVisible: Boolean,
)
