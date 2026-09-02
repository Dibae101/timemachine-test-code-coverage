package com.example.util.simpletimetracker.feature_shortcuts.viewData

import com.example.util.simpletimetracker.feature_base_adapter.button.ButtonViewData

data class ShortcutsButtonViewData(
    val block: Block,
) : ButtonViewData.Id {

    enum class Block {
        ADD,
    }
}
