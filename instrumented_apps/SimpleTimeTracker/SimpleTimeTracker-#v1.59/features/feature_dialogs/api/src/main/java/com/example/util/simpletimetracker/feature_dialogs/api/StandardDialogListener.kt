package com.example.util.simpletimetracker.feature_dialogs.api

interface StandardDialogListener {

    fun onPositiveClick(tag: String? = null, data: Any? = null) {
        // Implement to handle clicks
    }

    fun onNegativeClick(tag: String? = null, data: Any? = null) {
        // Implement to handle clicks
    }
}