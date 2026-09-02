package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import javax.inject.Inject

class RecordsFilterMapper @Inject constructor() {

    fun mapFilter(
        filterType: ChartFilterType,
        selectedId: Long,
    ): RecordsFilter {
        if (selectedId == UNTRACKED_ITEM_ID) {
            return RecordsFilter.Untracked
        }

        if (selectedId == UNCATEGORIZED_ITEM_ID) {
            when (filterType) {
                ChartFilterType.CATEGORY -> {
                    return RecordsFilter.CategoryItem.Uncategorized
                        .let(::listOf)
                        .let { RecordsFilter.Category(selected = it, filtered = emptyList()) }
                }
                ChartFilterType.RECORD_TAG -> {
                    return RecordsFilter.TagItem.Untagged
                        .let(::listOf)
                        .let { RecordsFilter.Tags(selected = it, filtered = emptyList()) }
                }
                ChartFilterType.ACTIVITY -> {
                    // Shouldn't happen normally.
                }
            }
        }

        return when (filterType) {
            ChartFilterType.ACTIVITY -> {
                listOf(selectedId)
                    .let { RecordsFilter.Activity(selected = it, filtered = emptyList()) }
            }
            ChartFilterType.CATEGORY -> {
                listOf(selectedId).map(RecordsFilter.CategoryItem::Categorized)
                    .let { RecordsFilter.Category(selected = it, filtered = emptyList()) }
            }
            ChartFilterType.RECORD_TAG -> {
                listOf(selectedId).map(RecordsFilter.TagItem::Tagged)
                    .let { RecordsFilter.Tags(selected = it, filtered = emptyList()) }
            }
        }
    }
}