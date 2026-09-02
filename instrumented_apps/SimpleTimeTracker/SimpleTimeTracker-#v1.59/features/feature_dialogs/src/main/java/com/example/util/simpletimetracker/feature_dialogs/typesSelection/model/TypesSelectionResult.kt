package com.example.util.simpletimetracker.feature_dialogs.typesSelection.model

import com.example.util.simpletimetracker.domain.record.model.RecordBase

data class TypesSelectionResult(
    val dataIds: List<Long>,
    val tagValues: List<RecordBase.Tag>,
    val selectValueOnStartTagIds: List<Long>,
)