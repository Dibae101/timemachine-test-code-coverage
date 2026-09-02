package com.example.util.simpletimetracker.feature_records_all.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.extension.toModel
import com.example.util.simpletimetracker.core.interactor.GetChangeRecordNavigationParamsInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_records_all.interactor.RecordsAllViewDataInteractor
import com.example.util.simpletimetracker.feature_records_all.mapper.RecordsAllViewDataMapper
import com.example.util.simpletimetracker.feature_records_all.model.RecordsAllSortOrder
import com.example.util.simpletimetracker.feature_records_all.viewData.RecordsAllSortOrderViewData
import com.example.util.simpletimetracker.feature_records_all.viewData.RecordsAllViewDataState
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordFromRecordsAllParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRunningRecordFromRecordsAllParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRunningRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordsAllParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterParam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordsAllViewModel @Inject constructor(
    private val router: Router,
    private val recordsAllViewDataInteractor: RecordsAllViewDataInteractor,
    private val recordsAllViewDataMapper: RecordsAllViewDataMapper,
    private val prefsInteractor: PrefsInteractor,
    private val getChangeRecordNavigationParamsInteractor: GetChangeRecordNavigationParamsInteractor,
) : ViewModel() {

    lateinit var extra: RecordsAllParams

    val records: LiveData<RecordsAllViewDataState> by lazy {
        return@lazy MutableLiveData<RecordsAllViewDataState>().let { initial ->
            viewModelScope.launch {
                initial.value = RecordsAllViewDataState.Loading(listOf(LoaderViewData()))
                initial.value = RecordsAllViewDataState.Content(loadRecordsViewData())
            }
            initial
        }
    }

    val sortOrderViewData: LiveData<RecordsAllSortOrderViewData> by lazy {
        MutableLiveData<RecordsAllSortOrderViewData>().let { initial ->
            viewModelScope.launch { initial.value = loadSortOrderViewData() }
            initial
        }
    }

    private var sortOrder: RecordsAllSortOrder = RecordsAllSortOrder.TIME_STARTED

    fun onRunningRecordClick(
        item: RunningRecordViewData,
        sharedElements: Pair<Any, String>,
    ) = viewModelScope.launch {
        val useMilitaryTimeFormat = prefsInteractor.getUseMilitaryTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val durationFormat = prefsInteractor.getDurationFormat()
        val params = getChangeRecordNavigationParamsInteractor.execute(
            item = item,
            from = ChangeRunningRecordParams.From.Records,
            useMilitaryTimeFormat = useMilitaryTimeFormat,
            showSeconds = showSeconds,
            durationFormat = durationFormat,
            sharedElements = sharedElements,
        )
        router.navigate(
            data = ChangeRunningRecordFromRecordsAllParams(params),
            sharedElements = mapOf(sharedElements),
        )
    }

    fun onRecordClick(
        item: RecordViewData,
        sharedElements: Pair<Any, String>,
    ) = viewModelScope.launch {
        val useMilitaryTimeFormat = prefsInteractor.getUseMilitaryTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val durationFormat = prefsInteractor.getDurationFormat()
        val params = getChangeRecordNavigationParamsInteractor.execute(
            item = item,
            from = ChangeRecordParams.From.RecordsAll,
            shift = 0,
            useMilitaryTimeFormat = useMilitaryTimeFormat,
            showSeconds = showSeconds,
            durationFormat = durationFormat,
            sharedElements = sharedElements,
        )
        router.navigate(
            data = ChangeRecordFromRecordsAllParams(params),
            sharedElements = mapOf(sharedElements),
        )
    }

    fun onVisible() {
        updateRecords()
    }

    fun onNeedUpdate() {
        updateRecords()
    }

    fun onRecordTypeOrderSelected(position: Int) {
        sortOrder = recordsAllViewDataMapper.toSortOrder(position)
        records.set(RecordsAllViewDataState.Loading(listOf(LoaderViewData())))
        updateRecords()
        updateSortOrderViewData()
    }

    private fun updateRecords() = viewModelScope.launch {
        val data = loadRecordsViewData()
        records.set(RecordsAllViewDataState.Content(data))
    }

    private suspend fun loadRecordsViewData(): List<ViewHolderType> {
        return recordsAllViewDataInteractor.getViewData(
            filter = extra.filter.map(RecordsFilterParam::toModel),
            sortOrder = sortOrder,
        )
    }

    private fun updateSortOrderViewData() {
        val data = loadSortOrderViewData()
        sortOrderViewData.set(data)
    }

    private fun loadSortOrderViewData(): RecordsAllSortOrderViewData {
        return recordsAllViewDataMapper.toSortOrderViewData(sortOrder)
    }
}
