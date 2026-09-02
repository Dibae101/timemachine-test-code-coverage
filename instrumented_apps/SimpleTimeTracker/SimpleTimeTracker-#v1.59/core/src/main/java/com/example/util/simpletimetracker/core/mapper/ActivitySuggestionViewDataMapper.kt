package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.activitySuggestion.model.ActivitySuggestion
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.activitySuggestion.ActivitySuggestionViewData
import com.example.util.simpletimetracker.feature_base_adapter.listElement.ListElementViewData
import javax.inject.Inject

class ActivitySuggestionViewDataMapper @Inject constructor(
    private val iconMapper: IconMapper,
    private val colorMapper: ColorMapper,
) {

    fun mapSuggestionFiltered(
        suggestion: ActivitySuggestion,
        isDarkTheme: Boolean,
        typesMap: Map<Long, RecordType>,
        typesOrder: List<Long>,
        isFiltered: Boolean,
    ): ActivitySuggestionViewData {
        val activityItems = mapTypes(
            typeIds = listOf(suggestion.forTypeId).toSet(),
            isDarkTheme = isDarkTheme,
            typesMap = typesMap,
            typesOrder = typesOrder,
        )
        val suggestionItems = mapTypes(
            typeIds = suggestion.suggestionIds.toSet(),
            isDarkTheme = isDarkTheme,
            typesMap = typesMap,
            typesOrder = typesOrder,
        )

        return ActivitySuggestionViewData(
            id = suggestion.id,
            activity = activityItems,
            suggestions = suggestionItems,
            color = if (isFiltered) {
                colorMapper.toInactiveColor(isDarkTheme)
            } else {
                colorMapper.toActiveColor(isDarkTheme)
            },
        )
    }

    private fun mapTypes(
        typeIds: Set<Long>,
        isDarkTheme: Boolean,
        typesMap: Map<Long, RecordType>,
        typesOrder: List<Long>,
    ): List<ViewHolderType> {
        val data = typeIds
            .sortedBy { typesOrder.indexOf(it) }
            .mapNotNull { typesMap[it] }
        if (data.isEmpty()) return emptyList()

        val result = mutableListOf<ViewHolderType>()
        result += data.map {
            ListElementViewData(
                text = it.name,
                icon = iconMapper.mapIcon(it.icon),
                color = colorMapper.mapToColorInt(
                    color = it.color,
                    isDarkTheme = isDarkTheme,
                ),
            )
        }
        return result
    }
}