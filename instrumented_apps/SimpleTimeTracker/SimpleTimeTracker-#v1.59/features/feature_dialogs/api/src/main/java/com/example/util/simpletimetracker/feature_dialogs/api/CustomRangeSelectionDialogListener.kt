package com.example.util.simpletimetracker.feature_dialogs.api

import com.example.util.simpletimetracker.domain.record.model.Range

interface CustomRangeSelectionDialogListener {

    fun onCustomRangeSelected(range: Range)
}