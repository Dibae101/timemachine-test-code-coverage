package com.example.util.simpletimetracker.feature_change_record.viewModel.delegates

import com.example.util.simpletimetracker.core.mapper.RecordQuickActionMapper
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordAction.interactor.RecordActionShortcutMediator
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_change_record.mapper.ChangeRecordViewDataMapper
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordDelegateBridge
import com.example.util.simpletimetracker.feature_change_record.viewModel.base.ChangeRecordActionsSubDelegate
import com.example.util.simpletimetracker.navigation.Router
import javax.inject.Inject

class ChangeRecordActionsShortcutDelegate @Inject constructor(
    private val router: Router,
    private val prefsInteractor: PrefsInteractor,
    private val recordActionShortcutMediator: RecordActionShortcutMediator,
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
        viewData = loadShortcutViewData()
        bridge?.send(ChangeRecordDelegateBridge.Action.UpdateViewData)
    }

    suspend fun onShortcutClickDelegate() {
        val params = bridge?.getParams() ?: return
        recordActionShortcutMediator.execute(
            typeId = params.baseParams.newTypeId,
            comment = params.baseParams.newComment,
            tagIds = params.baseParams.newTags,
        )
        // TODO show "Action complete" message to all actions.
        // Exit.
        router.back()
    }

    private suspend fun loadShortcutViewData(): List<ViewHolderType> {
        val params = bridge?.getParams() ?: return emptyList()
        if (!params.shortcutParams.isAvailable) return emptyList()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val action = RecordQuickAction.SHORTCUT

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