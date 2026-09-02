package com.example.util.simpletimetracker.core.viewData

import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData

sealed interface CommentFilterTypeViewData : FilterViewData.Type {
    data object Similar : CommentFilterTypeViewData
    data object Favourite : CommentFilterTypeViewData
    data object Last : CommentFilterTypeViewData
}