package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordTagParam(
    val tagId: Long,
    val numericValue: Double?,
) : Parcelable