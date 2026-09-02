package com.example.util.simpletimetracker.feature_widget.single.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.lazySuspend
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.mapper.RecordTypeViewDataMapper
import com.example.util.simpletimetracker.domain.base.REPEAT_BUTTON_ITEM_ID
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.widget.interactor.WidgetInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.recordType.RecordTypeViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeSpecial.RunningRecordTypeSpecialViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSingleSettingsViewModel @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val widgetInteractor: WidgetInteractor,
    private val recordTypeViewDataMapper: RecordTypeViewDataMapper,
) : BaseViewModel() {

    lateinit var extra: WidgetSingleSettingsExtra

    val recordTypes: LiveData<List<ViewHolderType>> by lazySuspend { loadRecordTypesViewData() }
    val handled: LiveData<Int> = MutableLiveData()

    fun onSpecialRecordTypeClick(item: RunningRecordTypeSpecialViewData) {
        when (item.type) {
            is RunningRecordTypeSpecialViewData.Type.Repeat -> {
                onClick(REPEAT_BUTTON_ITEM_ID)
            }
            else -> return
        }
    }

    fun onRecordTypeClick(item: RecordTypeViewData) {
        onClick(item.id)
    }

    private fun onClick(typeId: Long) {
        viewModelScope.launch {
            prefsInteractor.setWidget(extra.widgetId, typeId)
            widgetInteractor.updateSingleWidget(extra.widgetId)
            handled.set(extra.widgetId)
        }
    }

    private suspend fun loadRecordTypesViewData(): List<ViewHolderType> {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val numberOfCards = prefsInteractor.getNumberOfCards()

        return recordTypeInteractor.getAll()
            .filter { !it.hidden }
            .map { recordTypeViewDataMapper.map(it, numberOfCards, isDarkTheme) }
            .takeUnless { it.isEmpty() }
            ?.plus(recordTypeViewDataMapper.mapToRepeatItem(numberOfCards, isDarkTheme))
            ?: recordTypeViewDataMapper.mapToEmpty()
    }
}
