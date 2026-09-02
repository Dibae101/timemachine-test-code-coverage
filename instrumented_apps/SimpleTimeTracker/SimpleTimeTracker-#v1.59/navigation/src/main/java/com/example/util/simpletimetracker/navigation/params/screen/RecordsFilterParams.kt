package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordsFilterParams(
    val tag: String,
    val title: String,
    val flags: Flags,
    val filters: List<RecordsFilterParam>,
    val defaultLastDaysNumber: Int,
) : ScreenParams, Parcelable {

    @Parcelize
    data class Flags(
        val dateSelectionAvailable: Boolean,
        val untrackedSelectionAvailable: Boolean,
        val multitaskSelectionAvailable: Boolean,
        val duplicationsSelectionAvailable: Boolean,
        val favouriteSelectionAvailable: Boolean,
        val addRunningRecords: Boolean,
    ) : Parcelable

    companion object {
        val Empty = RecordsFilterParams(
            tag = "",
            title = "",
            flags = Flags(
                dateSelectionAvailable = true,
                untrackedSelectionAvailable = true,
                multitaskSelectionAvailable = true,
                duplicationsSelectionAvailable = true,
                favouriteSelectionAvailable = true,
                addRunningRecords = true,
            ),
            filters = emptyList(),
            defaultLastDaysNumber = 0,
        )
    }
}