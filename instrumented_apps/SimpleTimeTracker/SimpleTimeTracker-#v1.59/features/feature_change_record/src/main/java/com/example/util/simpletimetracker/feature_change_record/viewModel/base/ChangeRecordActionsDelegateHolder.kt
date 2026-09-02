package com.example.util.simpletimetracker.feature_change_record.viewModel.base

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.divider.DividerViewData
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsAdjustDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsContinueDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsDuplicateDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsMergeDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsMoveDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsRepeatDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsShortcutDelegate
import com.example.util.simpletimetracker.feature_change_record.viewModel.delegates.ChangeRecordActionsSplitDelegate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChangeRecordActionsDelegateHolder @Inject constructor(
    val mergeDelegate: ChangeRecordActionsMergeDelegate,
    val splitDelegate: ChangeRecordActionsSplitDelegate,
    val adjustDelegate: ChangeRecordActionsAdjustDelegate,
    val continueDelegate: ChangeRecordActionsContinueDelegate,
    val repeatDelegate: ChangeRecordActionsRepeatDelegate,
    val duplicateDelegate: ChangeRecordActionsDuplicateDelegate,
    val moveDelegate: ChangeRecordActionsMoveDelegate,
    val shortcutDelegate: ChangeRecordActionsShortcutDelegate,
) {

    private val delegatesList = listOf(
        splitDelegate,
        adjustDelegate,
        continueDelegate,
        repeatDelegate,
        duplicateDelegate,
        moveDelegate,
        mergeDelegate,
        shortcutDelegate,
    )

    fun attach(
        bridge: ChangeRecordDelegateBridge,
    ) {
        delegatesList.forEach { it.attach(bridge) }
    }

    fun clear() {
        delegatesList.forEach { (it as? ViewModelDelegate)?.clear() }
    }

    fun loadViewData(): List<ViewHolderType> {
        val result = mutableListOf<ViewHolderType>()

        delegatesList.map {
            it.getViewData()
        }.forEachIndexed { index, items ->
            if (items.isEmpty()) return@forEachIndexed
            if (index != 0) result += DividerViewData(index.toLong())
            result += items
        }

        return result
    }

    suspend fun updateViewData() {
        coroutineScope {
            delegatesList.forEach { delegate ->
                launch { delegate.updateViewData() }
            }
        }
    }
}