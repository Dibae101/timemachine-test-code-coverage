package com.example.util.simpletimetracker.domain.record.model

data class RecordDataSelectionDialogResult(
    val fields: List<Field>,
    val preselectedTags: List<RecordBase.Tag>,
    val requiredValueSelectionTagIds: List<Long>,
) {
    sealed interface Field {
        object Tags : Field
        object Comment : Field
    }
}