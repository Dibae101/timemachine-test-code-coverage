package com.example.util.simpletimetracker.feature_settings.api

interface OnSettingChangedInteractor {
    suspend fun onAllowMultitaskingChange()
    suspend fun onRetroactiveTrackingModeChange()
}