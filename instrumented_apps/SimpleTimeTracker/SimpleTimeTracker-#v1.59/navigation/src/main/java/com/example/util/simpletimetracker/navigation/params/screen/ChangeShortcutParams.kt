package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

sealed interface ChangeShortcutParams : ScreenParams, Parcelable {

    @Parcelize
    data class Change(
        val id: Long,
        val transitionName: String = "",
        val preview: Preview? = null,
    ) : ChangeShortcutParams

    @Parcelize
    data class Preview(
        val name: String,
        @ColorInt val color: Int,
        val icon: RecordTypeIconParams?,
        @ColorInt val iconColor: Int,
        val iconAlpha: Float,
    ) : Parcelable

    @Parcelize
    object New : ChangeShortcutParams
}
