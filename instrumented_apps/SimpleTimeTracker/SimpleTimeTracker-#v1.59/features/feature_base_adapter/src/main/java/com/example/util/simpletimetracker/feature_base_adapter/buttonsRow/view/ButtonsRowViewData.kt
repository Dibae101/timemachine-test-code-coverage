package com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

abstract class ButtonsRowViewData : ViewHolderType {

    abstract val id: Long
    abstract val name: String
    abstract val isSelected: Boolean
    open val textSizeSp: Int? = null

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is ButtonsRowViewData

    companion object {
        const val SELECTED_BUTTON_TEST_TAG = "ButtonsRowViewDataSelected"
    }
}