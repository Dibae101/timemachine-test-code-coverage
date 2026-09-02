package com.example.util.simpletimetracker.domain.widget.interactor

import com.example.util.simpletimetracker.domain.widget.model.WidgetType

interface WidgetInteractor {

    fun initializeCachedViews()

    fun updateSingleWidget(widgetId: Int)

    fun updateSingleWidgets(typeIds: List<Long>)

    fun updateStatisticsWidget(widgetId: Int)

    fun updateQuickSettingsWidget(widgetId: Int)

    fun updateGridWidget(widgetId: Int)

    fun updateWidgets(type: WidgetType)
}