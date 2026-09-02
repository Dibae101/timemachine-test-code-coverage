package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class OptionsListParams(
    val items: List<Item>,
) : ScreenParams, Parcelable {

    @Parcelize
    data class Item(
        val id: Id,
        val text: String,
        @DrawableRes val icon: Int?,
        val isIconCheckVisible: Boolean = false,
        val isChecked: Boolean = false,
        val isSelected: Boolean = false,
    ) : Parcelable {

        interface Id : Parcelable
    }

    companion object {
        val Empty = OptionsListParams(
            items = emptyList(),
        )
    }
}