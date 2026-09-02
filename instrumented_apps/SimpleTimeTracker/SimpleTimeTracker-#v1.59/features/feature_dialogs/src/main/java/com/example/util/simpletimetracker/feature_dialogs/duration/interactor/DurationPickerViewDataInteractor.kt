package com.example.util.simpletimetracker.feature_dialogs.duration.interactor

import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.durationSuggestion.interactor.DurationSuggestionInteractor
import com.example.util.simpletimetracker.domain.durationSuggestion.model.DurationSuggestion
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_dialogs.R
import com.example.util.simpletimetracker.feature_dialogs.duration.adapter.DurationSuggestionViewData
import com.example.util.simpletimetracker.navigation.params.screen.DurationDialogParams
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DurationPickerViewDataInteractor @Inject constructor(
    private val timeMapper: TimeMapper,
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
    private val durationSuggestionInteractor: DurationSuggestionInteractor,
) {

    suspend fun getSuggestionsViewData(
        extra: DurationDialogParams,
    ): List<ViewHolderType> {
        if (extra.value !is DurationDialogParams.Value.DurationSeconds) return emptyList()

        val data = durationSuggestionInteractor.getAll()
            .ifEmpty { prepopulateSuggestions() }

        return data.map {
            DurationSuggestionViewData(
                text = timeMapper.formatDuration(it.valueSeconds),
                type = DurationSuggestionViewData.Type.Value(it.valueSeconds),
            )
        } + DurationSuggestionViewData(
            text = resourceRepo.getString(R.string.running_records_add_type),
            type = DurationSuggestionViewData.Type.Add,
        )
    }

    private suspend fun prepopulateSuggestions(): List<DurationSuggestion> {
        return if (prefsInteractor.getDurationSuggestionsWasPrepopulated()) {
            emptyList()
        } else {
            prefsInteractor.setDurationSuggestionsWasPrepopulated(true)
            getDefaultSuggestions().map {
                DurationSuggestion(valueSeconds = it)
            }.onEach {
                durationSuggestionInteractor.add(it)
            }
        }
    }

    private fun getDefaultSuggestions(): List<Long> {
        return listOf(
            TimeUnit.MINUTES.toSeconds(1),
            TimeUnit.MINUTES.toSeconds(15),
            TimeUnit.MINUTES.toSeconds(60),
        )
    }
}