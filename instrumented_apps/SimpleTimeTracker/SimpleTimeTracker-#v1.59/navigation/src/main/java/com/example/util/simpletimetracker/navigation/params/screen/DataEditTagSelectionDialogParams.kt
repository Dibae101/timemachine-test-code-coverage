package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataEditTagSelectionDialogParams(
    val tag: String,
    val typeIds: List<Long>,
    val showValueSelection: Boolean,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = DataEditTagSelectionDialogParams(
            tag = "",
            typeIds = emptyList(),
            showValueSelection = true,
        )
    }
}
