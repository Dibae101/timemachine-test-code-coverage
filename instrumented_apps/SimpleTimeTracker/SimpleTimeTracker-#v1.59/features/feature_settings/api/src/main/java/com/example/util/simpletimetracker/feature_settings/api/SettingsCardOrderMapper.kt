package com.example.util.simpletimetracker.feature_settings.api

import com.example.util.simpletimetracker.domain.recordType.model.CardOrder

interface SettingsCardOrderMapper {
    fun toCardOrderViewData(currentOrder: CardOrder): CardOrderViewData
    fun toCardOrder(position: Int): CardOrder
}