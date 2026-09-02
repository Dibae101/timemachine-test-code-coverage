package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import javax.inject.Inject
import kotlin.collections.plus

class GetTotalStatisticsFilterInteractor @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
) {

    suspend fun execute(
        filterType: ChartFilterType,
    ): RecordsFilter {
        return when (filterType) {
            ChartFilterType.ACTIVITY -> {
                val typeIds = recordTypeInteractor.getAll()
                    .map(RecordType::id)
                RecordsFilter.Activity(selected = typeIds, filtered = emptyList())
            }
            ChartFilterType.CATEGORY -> {
                val categoryIds = categoryInteractor.getAll()
                    .map(Category::id)
                val items = categoryIds
                    .map(RecordsFilter.CategoryItem::Categorized) +
                    RecordsFilter.CategoryItem.Uncategorized
                RecordsFilter.Category(selected = items, filtered = emptyList())
            }
            ChartFilterType.RECORD_TAG -> {
                val tagIds = recordTagInteractor.getAll()
                    .map(RecordTag::id)
                val items = tagIds
                    .map(RecordsFilter.TagItem::Tagged) +
                    RecordsFilter.TagItem.Untagged
                RecordsFilter.Tags(selected = items, filtered = emptyList())
            }
        }
    }
}