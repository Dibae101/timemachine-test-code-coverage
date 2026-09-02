package com.example.util.simpletimetracker.feature_change_shortcut.viewData

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.domain.recordShortcut.model.RecordShortcut
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordShortcut.RecordShortcutViewData
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

data class ChangeShortcutViewData(
    val targetModes: List<TargetModeButtonViewData>,
    val shortcutPreview: RecordShortcutViewData,
    val recordTypePreview: RecordTypePreviewViewData,
    val recordTagsPreview: RecordTagsPreviewViewData,
    val actionPreview: String,
    val showRecordTarget: Boolean,
    val showSettingTarget: Boolean,
) {

    data class TargetModeButtonViewData(
        val mode: RecordShortcut.TargetMode,
        override val name: String,
        override val isSelected: Boolean,
    ) : ButtonsRowViewData() {

        override val id: Long = mode.ordinal.toLong()
    }

    data class RecordTypePreviewViewData(
        val icon: RecordTypeIcon?,
        @ColorInt val color: Int,
    )

    data class RecordTagsPreviewViewData(
        val count: Int,
    )
}
