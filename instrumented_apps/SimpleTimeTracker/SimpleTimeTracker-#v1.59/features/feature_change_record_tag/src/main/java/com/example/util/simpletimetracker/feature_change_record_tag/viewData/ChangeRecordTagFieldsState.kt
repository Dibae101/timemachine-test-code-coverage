package com.example.util.simpletimetracker.feature_change_record_tag.viewData

import com.example.util.simpletimetracker.core.view.ViewChooserStateDelegate

data class ChangeRecordTagFieldsState(
    val chooserState: ViewChooserStateDelegate.States,
    val additionalFieldsVisible: Boolean,
)
