package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordTagSelectionParams(
    val typeId: Long,
    val fields: List<FieldParam>,
    val preselectedTags: List<RecordTagParam>,
    val requiredValueSelectionTagIds: List<Long>,
) : Parcelable, ScreenParams {

    sealed interface FieldParam : Parcelable {
        @Parcelize
        object Tags : FieldParam

        @Parcelize
        object Comment : FieldParam
    }

    companion object {
        val Empty = RecordTagSelectionParams(
            typeId = 0L,
            fields = emptyList(),
            preselectedTags = emptyList(),
            requiredValueSelectionTagIds = emptyList(),
        )
    }
}
