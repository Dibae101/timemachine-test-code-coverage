package com.example.util.simpletimetracker.feature_change_shortcut.mapper

import com.example.util.simpletimetracker.core.mapper.RecordShortcutViewDataMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.recordShortcut.model.RecordShortcut
import com.example.util.simpletimetracker.feature_change_shortcut.R
import com.example.util.simpletimetracker.feature_change_shortcut.adapter.ChangeShortcutSettingActionViewData
import com.example.util.simpletimetracker.feature_change_shortcut.viewData.ChangeShortcutViewData
import javax.inject.Inject

class ChangeShortcutViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val recordShortcutViewDataMapper: RecordShortcutViewDataMapper,
) {

    fun mapTargetModes(
        selected: RecordShortcut.TargetMode,
    ): List<ChangeShortcutViewData.TargetModeButtonViewData> {
        return listOf(
            RecordShortcut.TargetMode.Record,
            RecordShortcut.TargetMode.Setting,
        ).map { mapMode(mode = it, selected = selected) }
    }

    fun mapSettingActionsTitle(
        selected: RecordShortcut.SettingAction?,
    ): String {
        return selected
            ?.let { recordShortcutViewDataMapper.mapActionTitle(it) }
            ?: resourceRepo.getString(R.string.change_complex_rule_choose_action)
    }

    fun mapSettingActions(
        actionsOrder: List<RecordShortcut.SettingAction>,
    ): List<ChangeShortcutSettingActionViewData> {
        return actionsOrder.map { action ->
            ChangeShortcutSettingActionViewData(
                action = action,
                text = recordShortcutViewDataMapper.mapActionTitle(action),
            )
        }
    }

    fun mapMode(
        mode: RecordShortcut.TargetMode,
        selected: RecordShortcut.TargetMode,
    ): ChangeShortcutViewData.TargetModeButtonViewData {
        val name = when (mode) {
            RecordShortcut.TargetMode.Record -> {
                resourceRepo.getQuantityString(
                    stringResId = R.plurals.statistics_detail_times_tracked,
                    quantity = 1,
                )
            }
            RecordShortcut.TargetMode.Setting -> {
                resourceRepo.getString(R.string.shortcut_navigation_settings)
            }
        }
        return ChangeShortcutViewData.TargetModeButtonViewData(
            mode = mode,
            name = name,
            isSelected = selected == mode,
        )
    }
}
