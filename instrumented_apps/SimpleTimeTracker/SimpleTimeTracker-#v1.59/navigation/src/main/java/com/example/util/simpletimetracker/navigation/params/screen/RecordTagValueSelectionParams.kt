package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordTagValueSelectionParams(
    val tag: String? = null,
    val title: String? = null,
    val tagId: Long,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = RecordTagValueSelectionParams(
            tag = null,
            tagId = 0,
        )
    }
}
