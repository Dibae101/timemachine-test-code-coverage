package com.example.util.simpletimetracker.feature_suggestions.viewData

import com.example.util.simpletimetracker.feature_base_adapter.button.ButtonViewData

data class ActivitySuggestionsButtonViewData(
    val block: Block,
) : ButtonViewData.Id {

    enum class Block {
        ADD,
        CALCULATE,
    }
}