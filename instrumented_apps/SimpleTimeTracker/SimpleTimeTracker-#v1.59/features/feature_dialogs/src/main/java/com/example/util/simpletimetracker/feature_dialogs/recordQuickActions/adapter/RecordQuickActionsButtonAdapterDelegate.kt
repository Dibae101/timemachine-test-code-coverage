package com.example.util.simpletimetracker.feature_dialogs.recordQuickActions.adapter

import androidx.annotation.DrawableRes
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_dialogs.recordQuickActions.adapter.RecordQuickActionsWidthHolder.Width
import com.example.util.simpletimetracker.feature_dialogs.recordQuickActions.model.RecordQuickActionsButton
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_dialogs.databinding.RecordQuickActionsButtonItemBinding as Binding
import com.example.util.simpletimetracker.feature_dialogs.recordQuickActions.adapter.RecordQuickActionsButtonViewData as ViewData

fun createRecordQuickActionsButtonAdapterDelegate(
    onClick: (RecordQuickActionsButton) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.buttonIconText = item.text
        root.buttonIconRes = item.icon
        root.setOnClickWith(item.block, onClick)
    }
}

data class RecordQuickActionsButtonViewData(
    override val block: RecordQuickActionsButton,
    override val width: Width = Width.Small,
    val text: String,
    @DrawableRes val icon: Int,
) : ViewHolderType,
    RecordQuickActionsBlockHolder,
    RecordQuickActionsWidthHolder {

    override fun getUniqueId(): Long = block.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData
}

interface RecordQuickActionsBlockHolder {
    val block: RecordQuickActionsButton
}

interface RecordQuickActionsWidthHolder {
    val width: Width

    sealed interface Width {
        data object Full : Width
        data object Small : Width
    }
}