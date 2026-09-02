package com.example.util.simpletimetracker.feature_change_record.viewModel.delegates

import com.example.util.simpletimetracker.core.mapper.RecordQuickActionMapper
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_change_record.mapper.ChangeRecordViewDataMapper
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordDelegateBridge
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordActionsSubDelegate
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogType
import javax.inject.Inject

class ChangeRecordActionsMoveDelegate @Inject constructor(
    private val router: Router,
    private val prefsInteractor: PrefsInteractor,
    private val changeRecordViewDataMapper: ChangeRecordViewDataMapper,
    private val recordQuickActionMapper: RecordQuickActionMapper,
) : ChangeRecordActionsSubDelegate {

    private var bridge: ChangeRecordDelegateBridge? = null
    private var viewData: List<ViewHolderType> = emptyList()

    override fun attach(bridge: ChangeRecordDelegateBridge) {
        this.bridge = bridge
    }

    override fun getViewData(): List<ViewHolderType> {
        return viewData
    }

    override suspend fun updateViewData() {
        viewData = loadViewData()
        bridge?.send(ChangeRecordDelegateBridge.Action.UpdateViewData)
    }

    suspend fun onMoveClickDelegate() {
        val params = bridge?.getParams() ?: return
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val showSeconds = prefsInteractor.getShowSeconds()

        router.navigate(
            DateTimeDialogParams(
                tag = MOVE_TIME_STARTED_TAG,
                timestamp = params.baseParams.newTimeStarted,
                type = DateTimeDialogType.DATETIME(),
                useMilitaryTime = useMilitaryTime,
                firstDayOfWeek = firstDayOfWeek,
                showSeconds = showSeconds,
            ),
        )
    }

    private suspend fun loadViewData(): List<ViewHolderType> {
        val params = bridge?.getParams()
            ?: return emptyList()
        if (!params.moveParams.isAvailable) return emptyList()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val action = RecordQuickAction.MOVE

        val result = mutableListOf<ViewHolderType>()
        result += HintViewData(
            text = recordQuickActionMapper.mapHint(action).orEmpty(),
        )
        result += changeRecordViewDataMapper.mapRecordActionButton(
            action = action,
            isEnabled = params.baseParams.isButtonEnabled,
            isDarkTheme = isDarkTheme,
        )
        return result
    }

    companion object {
        const val MOVE_TIME_STARTED_TAG = "move_time_started_tag"
    }
}