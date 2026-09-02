package com.example.util.simpletimetracker.feature_dialogs.optionsDialog

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.optionsList.OptionsListViewData
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import javax.inject.Inject

class OptionsListViewDataInteractor @Inject constructor() {

    fun getViewData(
        extra: OptionsListParams,
    ): List<ViewHolderType> {
        return extra.items.map(::mapData)
    }

    private fun mapData(
        item: OptionsListParams.Item,
    ): OptionsListViewData {
        return OptionsListViewData(
            id = OptionsListItemId(item.id),
            text = item.text,
            icon = item.icon,
            isIconCheckVisible = item.isIconCheckVisible,
            isChecked = item.isChecked,
            isSelected = item.isSelected,
        )
    }
}