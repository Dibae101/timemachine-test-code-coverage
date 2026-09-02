package com.example.util.simpletimetracker.feature_records_filter.interactor

import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTypeToTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.feature_records_filter.api.RecordsFilterExcludeInteractor
import com.example.util.simpletimetracker.feature_records_filter.api.RecordsFilterExcludeInteractor.ExcludeType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterSelectionType
import javax.inject.Inject

class RecordsFilterExcludeInteractorImpl @Inject constructor(
    private val recordsFilterUpdateInteractor: RecordsFilterUpdateInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordTypeToTagInteractor: RecordTypeToTagInteractor,
) : RecordsFilterExcludeInteractor {

    override suspend fun exclude(
        id: Long,
        type: ExcludeType,
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter> {
        // Shouldn't be possible
        if (id == UNTRACKED_ITEM_ID) return currentFilters

        return when (type) {
            is ExcludeType.Activity -> {
                recordsFilterUpdateInteractor.handleTypeClick(
                    type = RecordFilterSelectionType.Filter,
                    id = id,
                    allowSameIdInSelectedFiltered = true,
                    currentFilters = currentFilters,
                    recordTypes = recordTypeInteractor.getAll(),
                    recordTypeCategories = recordTypeCategoryInteractor.getAll(),
                    recordTags = recordTagInteractor.getAll(),
                    typesToTags = recordTypeToTagInteractor.getAll(),
                )
            }
            is ExcludeType.Category -> {
                recordsFilterUpdateInteractor.handleCategoryClick(
                    type = RecordFilterSelectionType.Filter,
                    id = id,
                    allowSameIdInSelectedFiltered = true,
                    currentFilters = currentFilters,
                    recordTypes = recordTypeInteractor.getAll(),
                    recordTypeCategories = recordTypeCategoryInteractor.getAll(),
                    recordTags = recordTagInteractor.getAll(),
                    typesToTags = recordTypeToTagInteractor.getAll(),
                )
            }
            is ExcludeType.Tag -> {
                recordsFilterUpdateInteractor.handleTagClick(
                    type = RecordFilterSelectionType.Filter,
                    currentFilters = currentFilters,
                    itemId = id,
                    allowSameIdInSelectedFiltered = true,
                )
            }
        }
    }

    override suspend fun excludeOther(
        id: Long,
        type: ExcludeType,
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter> {
        val date = currentFilters.filterIsInstance<RecordsFilter.Date>().firstOrNull()

        // Shouldn't be possible
        if (id == UNTRACKED_ITEM_ID) return listOfNotNull(RecordsFilter.Untracked, date)

        return when (type) {
            is ExcludeType.Activity -> {
                RecordsFilter.Activity(selected = listOf(id), filtered = emptyList())
            }
            is ExcludeType.Category -> {
                val item = if (id == UNCATEGORIZED_ITEM_ID) {
                    RecordsFilter.CategoryItem.Uncategorized
                } else {
                    RecordsFilter.CategoryItem.Categorized(id)
                }
                RecordsFilter.Category(selected = listOf(item), filtered = emptyList())
            }
            is ExcludeType.Tag -> {
                val item = if (id == UNCATEGORIZED_ITEM_ID) {
                    RecordsFilter.TagItem.Untagged
                } else {
                    RecordsFilter.TagItem.Tagged(id)
                }
                RecordsFilter.Tags(selected = listOf(item), filtered = emptyList())
            }
        }.let(::listOf) + listOfNotNull(date)
    }
}