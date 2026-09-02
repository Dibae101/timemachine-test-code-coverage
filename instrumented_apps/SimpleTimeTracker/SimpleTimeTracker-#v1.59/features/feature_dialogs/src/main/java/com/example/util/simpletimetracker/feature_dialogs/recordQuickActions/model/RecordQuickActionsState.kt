package com.example.util.simpletimetracker.feature_dialogs.recordQuickActions.model

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

data class RecordQuickActionsState(
    val buttons: List<ViewHolderType>,
    val helpData: CharSequence,
    val hintData: Hint?,
) {

    sealed interface Hint {
        data class MultiSelect(val hint: String) : Hint
        data class Record(
            val name: String,
            val iconId: RecordTypeIcon,
            val color: Int,
            val timeStarted: String,
            val timeEnded: String?,
            val duration: String,
        ) : Hint
    }
}