package com.example.util.simpletimetracker.feature_change_record.viewModel.delegates

import com.example.util.simpletimetracker.core.mapper.RecordQuickActionMapper
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordAction.interactor.RecordActionDuplicateMediator
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_change_record.mapper.ChangeRecordViewDataMapper
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordDelegateBridge
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordActionsSubDelegate
import javax.inject.Inject

class ChangeRecordActionsDuplicateDelegate @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordActionDuplicateMediator: RecordActionDuplicateMediator,
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
        viewData = loadDuplicateViewData()
        bridge?.send(ChangeRecordDelegateBridge.Action.UpdateViewData)
    }

    suspend fun onDuplicateClickDelegate() {
        val params = bridge?.getParams() ?: return
        recordActionDuplicateMediator.execute(
            typeId = params.baseParams.newTypeId,
            timeStarted = params.baseParams.newTimeStarted,
            timeEnded = params.duplicateParams.newTimeEnded,
            comment = params.baseParams.newComment,
            tagIds = params.baseParams.newTags,
        )
        bridge?.send(ChangeRecordDelegateBridge.Action.OnSaveClickDelegate())
    }

    private suspend fun loadDuplicateViewData(): List<ViewHolderType> {
        val params = bridge?.getParams() ?: return emptyList()
        if (!params.duplicateParams.isAvailable) return emptyList()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val action = RecordQuickAction.DUPLICATE

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
}