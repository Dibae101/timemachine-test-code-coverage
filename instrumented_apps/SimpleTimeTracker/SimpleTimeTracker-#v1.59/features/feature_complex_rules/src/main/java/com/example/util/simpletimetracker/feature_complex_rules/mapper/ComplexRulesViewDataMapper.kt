package com.example.util.simpletimetracker.feature_complex_rules.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.feature_base_adapter.button.ButtonViewData
import com.example.util.simpletimetracker.feature_complex_rules.viewData.ComplexRulesButtonViewData
import javax.inject.Inject

class ComplexRulesViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
) {

    fun mapAddItem(
        isDarkTheme: Boolean,
    ): ButtonViewData {
        return ButtonViewData(
            id = ComplexRulesButtonViewData(
                block = ComplexRulesButtonViewData.Block.ADD,
            ),
            text = resourceRepo.getString(R.string.running_records_add_type),
            icon = ButtonViewData.Icon.Hidden,
            backgroundColor = resourceRepo.getThemedAttr(R.attr.appInactiveColor, isDarkTheme),
            isEnabled = true,
            marginHorizontalDp = 4,
        )
    }
}