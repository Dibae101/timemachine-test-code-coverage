package com.example.util.simpletimetracker.feature_data_edit.dialog

import com.example.util.simpletimetracker.domain.record.model.RecordBase

interface DataEditTagSelectionDialogListener {

    fun onTagsSelected(tag: String, tagIds: List<RecordBase.Tag>)
    fun onTagsDismissed()
}