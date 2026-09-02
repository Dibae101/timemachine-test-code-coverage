package com.example.util.simpletimetracker.feature_change_record.viewModel.delegates

import com.example.util.simpletimetracker.core.mapper.RecordQuickActionMapper
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordAction.interactor.RecordActionRepeatMediator
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_change_record.mapper.ChangeRecordViewDataMapper
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordDelegateBridge
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordActionsSubDelegate
import javax.inject.Inject

class ChangeRecordActionsRepeatDelegate @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordActionRepeatMediator: RecordActionRepeatMediator,
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
        viewData = loadRepeatViewData()
        bridge?.send(ChangeRecordDelegateBridge.Action.UpdateViewData)
    }

    suspend fun onRepeatClickDelegate() {
        val params = bridge?.getParams() ?: return
        // Exit.
        ChangeRecordDelegateBridge.Action.OnSaveClickDelegate(
            doAfter = {
                recordActionRepeatMediator.execute(
                    typeId = params.baseParams.newTypeId,
                    comment = params.baseParams.newComment,
                    tags = params.baseParams.newTags,
                )
            },
        ).let { bridge?.send(it) }
    }

    private suspend fun loadRepeatViewData(): List<ViewHolderType> {
        val params = bridge?.getParams()
            ?: return emptyList()
        if (!params.repeatParams.isAvailable) return emptyList()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val action = RecordQuickAction.REPEAT

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