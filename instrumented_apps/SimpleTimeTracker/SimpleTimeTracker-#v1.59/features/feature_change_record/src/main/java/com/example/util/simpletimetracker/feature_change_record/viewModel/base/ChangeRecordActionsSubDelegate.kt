package com.example.util.simpletimetracker.feature_change_record.viewModel.base

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

interface ChangeRecordActionsSubDelegate {

    fun attach(bridge: ChangeRecordDelegateBridge)

    fun getViewData(): List<ViewHolderType>

    suspend fun updateViewData()
}