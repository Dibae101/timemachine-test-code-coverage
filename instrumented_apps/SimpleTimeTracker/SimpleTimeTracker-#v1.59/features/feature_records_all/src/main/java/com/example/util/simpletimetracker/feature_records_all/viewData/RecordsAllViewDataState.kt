package com.example.util.simpletimetracker.feature_records_all.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

sealed interface RecordsAllViewDataState {
    data class Loading(val viewData: List<ViewHolderType>) : RecordsAllViewDataState
    data class Content(val viewData: List<ViewHolderType>) : RecordsAllViewDataState
}