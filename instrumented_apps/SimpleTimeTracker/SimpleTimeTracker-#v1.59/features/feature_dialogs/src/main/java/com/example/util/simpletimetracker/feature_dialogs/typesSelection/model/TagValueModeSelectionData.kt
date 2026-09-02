package com.example.util.simpletimetracker.feature_dialogs.typesSelection.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagValueModeSelectionData(
    val tagId: Long,
) : Parcelable
