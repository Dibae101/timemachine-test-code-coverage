package com.example.util.simpletimetracker.feature_settings.api

import com.example.util.simpletimetracker.feature_views.spinner.CustomSpinner

data class CardOrderViewData(
    val items: List<CustomSpinner.CustomSpinnerItem>,
    val selectedPosition: Int,
    val isManualConfigButtonVisible: Boolean,
)