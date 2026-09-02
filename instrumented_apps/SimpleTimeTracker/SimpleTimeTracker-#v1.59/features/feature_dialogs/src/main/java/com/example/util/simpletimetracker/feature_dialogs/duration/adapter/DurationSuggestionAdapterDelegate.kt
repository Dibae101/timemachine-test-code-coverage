package com.example.util.simpletimetracker.feature_dialogs.duration.adapter

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.setOnLongClickWith
import com.example.util.simpletimetracker.feature_dialogs.databinding.ItemDurationDialogSuggestionLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_dialogs.duration.adapter.DurationSuggestionViewData as ViewData

fun createDurationSuggestionAdapter(
    onClick: (ViewData) -> Unit,
    onLongClick: (ViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->
    with(binding) {
        item as ViewData

        tvItemItemDurationSuggestion.text = item.text
        containerItemDurationSuggestion.setOnClickWith(item, onClick)
        containerItemDurationSuggestion.setOnLongClickWith(item, onLongClick)
    }
}

data class DurationSuggestionViewData(
    val text: String,
    val type: Type,
) : ViewHolderType {

    override fun getUniqueId(): Long = text.hashCode().toLong()

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData

    sealed interface Type {
        data object Add : Type
        data class Value(val value: Long) : Type
    }
}
