package com.example.util.simpletimetracker.feature_suggestions.interactor

import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.CalculateAdjacentActivitiesInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor.GetParam
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import javax.inject.Inject

class ActivitySuggestionsCalculateInteractor @Inject constructor(
    private val timeMapper: TimeMapper,
    private val prefsInteractor: PrefsInteractor,
    private val recordInteractor: RecordInteractor,
    private val calculateAdjacentActivitiesInteractor: CalculateAdjacentActivitiesInteractor,
) {

    suspend fun execute(
        typeIds: List<Long>,
    ): List<Result> {
        // TODO selectable range and number of suggestions?
        val range = timeMapper.getRangeStartAndEnd(
            rangeLength = RangeLength.Last(365),
            shift = 0,
            firstDayOfWeek = prefsInteractor.getFirstDayOfWeek(),
            startOfDayShift = prefsInteractor.getStartOfDayShift(),
        )
        val records = recordInteractor.getWithParams(GetParam.FromRange(range))

        val data = calculateAdjacentActivitiesInteractor.calculateNextActivities(
            typeIds = typeIds,
            records = records,
            maxCount = MAX_COUNT,
        )

        return typeIds.mapNotNull { typeId ->
            val thisTypeSuggestions = data[typeId]
                ?: return@mapNotNull null
            Result(
                typeId = typeId,
                suggestions = thisTypeSuggestions.map { it.typeId }.toSet(),
            )
        }
    }

    data class Result(
        val typeId: Long,
        val suggestions: Set<Long>,
    )

    companion object {
        private const val MAX_COUNT = 5
    }
}