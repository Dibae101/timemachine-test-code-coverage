package com.example.util.simpletimetracker.feature_complex_rules.viewData

import com.example.util.simpletimetracker.feature_base_adapter.button.ButtonViewData

data class ComplexRulesButtonViewData(
    val block: Block,
) : ButtonViewData.Id {

    enum class Block {
        ADD,
    }
}