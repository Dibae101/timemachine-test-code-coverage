package com.example.util.simpletimetracker.data_local.activitySuggestion

import com.example.util.simpletimetracker.domain.activitySuggestion.model.ActivitySuggestion
import javax.inject.Inject

class ActivitySuggestionDataLocalMapper @Inject constructor() {

    fun map(dbo: ActivitySuggestionDBO): ActivitySuggestion {
        return ActivitySuggestion(
            id = dbo.id,
            forTypeId = dbo.forTypeId,
            suggestionIds = dbo.suggestionIds
                .split(',')
                .mapNotNull(String::toLongOrNull)
                .toSet(),
        )
    }

    fun map(domain: ActivitySuggestion): ActivitySuggestionDBO {
        return ActivitySuggestionDBO(
            id = domain.id,
            forTypeId = domain.forTypeId,
            suggestionIds = domain.suggestionIds
                .joinToString(separator = ","),
        )
    }
}