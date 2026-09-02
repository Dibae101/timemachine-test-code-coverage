package com.example.util.simpletimetracker.domain.widget.model

sealed interface QuickSettingsWidgetType {
    object AllowMultitasking : QuickSettingsWidgetType
    object ShowRecordTagSelection : QuickSettingsWidgetType
}