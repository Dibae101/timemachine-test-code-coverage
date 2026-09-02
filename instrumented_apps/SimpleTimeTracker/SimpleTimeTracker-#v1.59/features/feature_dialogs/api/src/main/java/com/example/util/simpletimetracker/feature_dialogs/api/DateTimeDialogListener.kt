package com.example.util.simpletimetracker.feature_dialogs.api

interface DateTimeDialogListener {

    fun onDateTimeSet(timestamp: Long, tag: String? = null)
}