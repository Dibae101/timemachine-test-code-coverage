package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HelpDialogParams(
    val title: String,
    val text: CharSequence,
    val isFullscreen: Boolean = true,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = HelpDialogParams(
            title = "",
            text = "",
            isFullscreen = true,
        )
    }
}