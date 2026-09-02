package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.navigation.params.screen.RecordTagValueSelectionParams

interface OnTagValueSelectedListener {

    fun onTagValueSelected(
        params: RecordTagValueSelectionParams,
        data: Double,
    )
}