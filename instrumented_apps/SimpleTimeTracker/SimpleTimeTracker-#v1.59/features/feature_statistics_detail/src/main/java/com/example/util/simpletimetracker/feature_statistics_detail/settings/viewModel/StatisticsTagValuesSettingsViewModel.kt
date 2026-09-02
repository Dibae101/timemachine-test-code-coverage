package com.example.util.simpletimetracker.feature_statistics_detail.settings.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.base.SingleLiveEvent
import com.example.util.simpletimetracker.core.extension.lazySuspend
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.domain.statistics.model.ChartValueMode
import com.example.util.simpletimetracker.domain.statistics.model.StatisticsDetailTagValueSettings
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_statistics_detail.settings.interactor.StatisticsTagValuesSettingsViewDataInteractor
import com.example.util.simpletimetracker.navigation.params.screen.StatisticsTagValuesSettingsParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsTagValuesSettingsViewModel @Inject constructor(
    private val viewDataInteractor: StatisticsTagValuesSettingsViewDataInteractor,
) : BaseViewModel() {

    val content: LiveData<List<ViewHolderType>> by lazySuspend { loadContent() }
    val settingsChanged: LiveData<StatisticsDetailTagValueSettings> = SingleLiveEvent()

    private var initialized: Boolean = false
    private var newSettings = StatisticsDetailTagValueSettings.getDefault()

    fun initialize(params: StatisticsTagValuesSettingsParams) {
        if (initialized) return
        newSettings = StatisticsDetailTagValueSettings(
            tagId = params.tagId,
            chartValueMode = params.chartValueMode,
            multiplyDuration = params.multiplyDuration,
            fillEmptyPeriods = params.fillEmptyPeriods,
            yAxisZoomed = params.yAxisZoomed,
        )
        initialized = true
    }

    fun onVisible() = viewModelScope.launch {
        updateContent()
    }

    fun onBlockClicked(block: SettingsBlock) {
        newSettings = when (block) {
            SettingsBlock.StatisticsTagValuesChartValueMode -> {
                newSettings.copy(
                    chartValueMode = when (newSettings.chartValueMode) {
                        ChartValueMode.TOTAL -> ChartValueMode.AVERAGE
                        ChartValueMode.AVERAGE -> ChartValueMode.TOTAL
                    },
                )
            }
            SettingsBlock.StatisticsTagValuesMultiplyDuration -> {
                newSettings.copy(multiplyDuration = !newSettings.multiplyDuration)
            }
            SettingsBlock.StatisticsTagValuesFillEmptyPeriods -> {
                newSettings.copy(fillEmptyPeriods = !newSettings.fillEmptyPeriods)
            }
            SettingsBlock.StatisticsTagValuesZoomYAxis -> {
                newSettings.copy(yAxisZoomed = !newSettings.yAxisZoomed)
            }
            else -> return // Do nothing.
        }
        onSettingsChanged()
    }

    private fun onSettingsChanged() {
        settingsChanged.set(newSettings)
        updateContent()
    }

    private fun updateContent() {
        content.set(loadContent())
    }

    private fun loadContent(): List<ViewHolderType> {
        return viewDataInteractor.execute(newSettings)
    }
}
