package com.example.util.simpletimetracker.core.mapper

import androidx.annotation.DrawableRes
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.ADJUST
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.CHANGE_ACTIVITY
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.CHANGE_TAG
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.CONTINUE
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.DUPLICATE
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.MERGE
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.MOVE
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.MULTISELECT
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.REPEAT
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.SHORTCUT
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.SPLIT
import com.example.util.simpletimetracker.domain.recordAction.model.RecordQuickAction.STOP
import javax.inject.Inject

class RecordQuickActionMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
) {

    fun mapText(data: RecordQuickAction): String {
        return when (data) {
            CONTINUE -> R.string.change_record_continue
            REPEAT -> R.string.change_record_repeat
            DUPLICATE -> R.string.change_record_duplicate
            MOVE -> R.string.change_record_move
            MERGE -> R.string.change_record_merge
            SPLIT -> R.string.change_record_split
            ADJUST -> R.string.change_record_adjust
            SHORTCUT -> R.string.change_record_shortcut
            STOP -> R.string.notification_record_type_stop
            MULTISELECT -> R.string.change_record_multiselect
            CHANGE_ACTIVITY -> R.string.data_edit_change_activity
            CHANGE_TAG -> R.string.data_edit_change_tag
        }.let(resourceRepo::getString)
    }

    fun mapHint(data: RecordQuickAction): String? {
        return when (data) {
            CONTINUE -> R.string.change_record_continue_hint
            REPEAT -> R.string.change_record_repeat_hint
            DUPLICATE -> R.string.change_record_duplicate_hint
            MOVE -> R.string.change_record_move_hint
            MERGE -> R.string.change_record_merge_hint
            SPLIT -> R.string.change_record_split_hint
            ADJUST -> R.string.change_record_change_adjacent_records
            SHORTCUT -> R.string.change_record_shortcut_hint
            STOP -> null
            MULTISELECT -> R.string.change_record_multiselect_hint
            CHANGE_ACTIVITY -> null
            CHANGE_TAG -> null
        }?.let(resourceRepo::getString)
    }

    @DrawableRes
    fun mapIcon(data: RecordQuickAction): Int {
        return when (data) {
            CONTINUE -> R.drawable.action_continue
            REPEAT -> R.drawable.repeat
            DUPLICATE -> R.drawable.action_copy
            MOVE -> R.drawable.action_move
            MERGE -> R.drawable.action_merge
            SPLIT -> R.drawable.action_divide
            ADJUST -> R.drawable.action_change
            SHORTCUT -> R.drawable.favorite_border
            STOP -> R.drawable.action_stop
            MULTISELECT -> R.drawable.action_multiselect
            CHANGE_ACTIVITY, CHANGE_TAG -> R.drawable.action_change_item
        }
    }
}