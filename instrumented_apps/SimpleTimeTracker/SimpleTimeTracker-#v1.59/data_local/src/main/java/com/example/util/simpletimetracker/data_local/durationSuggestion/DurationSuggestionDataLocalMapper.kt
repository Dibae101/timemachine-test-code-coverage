package com.example.util.simpletimetracker.data_local.durationSuggestion

import com.example.util.simpletimetracker.domain.durationSuggestion.model.DurationSuggestion
import javax.inject.Inject

class DurationSuggestionDataLocalMapper @Inject constructor() {

    fun map(dbo: DurationSuggestionDBO): DurationSuggestion {
        return DurationSuggestion(
            id = dbo.id,
            valueSeconds = dbo.valueSeconds,
        )
    }

    fun map(domain: DurationSuggestion): DurationSuggestionDBO {
        return DurationSuggestionDBO(
            id = domain.id,
            valueSeconds = domain.valueSeconds,
        )
    }
}