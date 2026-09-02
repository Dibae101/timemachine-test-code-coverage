package com.example.util.simpletimetracker.feature_records_filter.interactor

import com.example.util.simpletimetracker.core.interactor.RecordFilterInteractor
import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.domain.extension.orEmpty
import com.example.util.simpletimetracker.domain.extension.tryCast
import com.example.util.simpletimetracker.domain.record.extension.getAllTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getCategoryItems
import com.example.util.simpletimetracker.domain.record.extension.getCommentItems
import com.example.util.simpletimetracker.domain.record.extension.getDaysOfWeek
import com.example.util.simpletimetracker.domain.record.extension.getDuplicationItems
import com.example.util.simpletimetracker.domain.record.extension.getFilteredCategoryItems
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTags
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getManuallyFilteredItems
import com.example.util.simpletimetracker.domain.record.extension.getSelectedTags
import com.example.util.simpletimetracker.domain.record.extension.getTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getTypeIdsFromCategories
import com.example.util.simpletimetracker.domain.record.extension.hasMultitaskFilter
import com.example.util.simpletimetracker.domain.record.extension.hasUntrackedFilter
import com.example.util.simpletimetracker.domain.record.interactor.GetDuplicatedRecordsInteractor
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordTag.interactor.FilterSelectableTagsInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord.MultitaskRecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_records_filter.mapper.RecordsFilterViewDataMapper
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterCommentType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterDateType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterDuplicationsType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterSelectionType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordsFilterSelectedRecordsViewData
import com.example.util.simpletimetracker.feature_records_filter.model.RecordsFilterSelectedRecordsViewData.RecordsViewData
import com.example.util.simpletimetracker.feature_records_filter.viewData.RecordsFilterSelectionButtonType
import javax.inject.Inject

class RecordsFilterUpdateInteractor @Inject constructor(
    private val filterSelectableTagsInteractor: FilterSelectableTagsInteractor,
    private val recordsFilterViewDataMapper: RecordsFilterViewDataMapper,
    private val getDuplicatedRecordsInteractor: GetDuplicatedRecordsInteractor,
    private val recordFilterInteractor: RecordFilterInteractor,
) {

    fun handleTypeClick(
        type: RecordFilterSelectionType,
        id: Long,
        allowSameIdInSelectedFiltered: Boolean,
        currentFilters: List<RecordsFilter>,
        recordTypes: List<RecordType>,
        recordTypeCategories: List<RecordTypeCategory>,
        recordTags: List<RecordTag>,
        typesToTags: List<RecordTypeToTag>,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val currentIds = when (type) {
            is RecordFilterSelectionType.Select -> filters.getTypeIds()
            is RecordFilterSelectionType.Filter -> filters.getFilteredTypeIds()
        }.toMutableList()
        val currentIdsFromCategories = filters.getTypeIdsFromCategories(
            recordTypes = recordTypes,
            recordTypeCategories = recordTypeCategories,
        )

        // Switch from categories to types in these categories.
        if (type is RecordFilterSelectionType.Select &&
            currentIdsFromCategories.isNotEmpty()
        ) {
            currentIds.addAll(currentIdsFromCategories)
        }

        val newIds = currentIds.toMutableList().apply { addOrRemove(id) }

        return handleSelectTypes(
            type = type,
            currentFilters = filters,
            newIds = newIds,
            allowSameIdInSelectedFiltered = allowSameIdInSelectedFiltered,
        ).let {
            checkTagFilterConsistency(
                currentFilters = it,
                recordTypes = recordTypes,
                recordTypeCategories = recordTypeCategories,
                recordTags = recordTags,
                typesToTags = typesToTags,
            )
        }
    }

    fun handleCategoryClick(
        type: RecordFilterSelectionType,
        id: Long,
        allowSameIdInSelectedFiltered: Boolean,
        currentFilters: List<RecordsFilter>,
        recordTypes: List<RecordType>,
        recordTypeCategories: List<RecordTypeCategory>,
        recordTags: List<RecordTag>,
        typesToTags: List<RecordTypeToTag>,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val currentItems = when (type) {
            RecordFilterSelectionType.Select -> filters.getCategoryItems()
            RecordFilterSelectionType.Filter -> filters.getFilteredCategoryItems()
        }

        val newItems = when (id) {
            UNCATEGORIZED_ITEM_ID -> RecordsFilter.CategoryItem.Uncategorized
            else -> RecordsFilter.CategoryItem.Categorized(id)
        }.let { currentItems.toMutableList().apply { addOrRemove(it) } }

        return handleSelectCategories(
            type = type,
            currentFilters = filters,
            newItems = newItems,
            allowSameIdInSelectedFiltered = allowSameIdInSelectedFiltered,
        ).let {
            checkTagFilterConsistency(
                currentFilters = it,
                recordTypes = recordTypes,
                recordTypeCategories = recordTypeCategories,
                recordTags = recordTags,
                typesToTags = typesToTags,
            )
        }
    }

    fun handleTagClick(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        itemId: Long,
        allowSameIdInSelectedFiltered: Boolean,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val currentTags = when (type) {
            RecordFilterSelectionType.Select -> filters.getSelectedTags()
            RecordFilterSelectionType.Filter -> filters.getFilteredTags()
        }

        val newTags = when (itemId) {
            UNCATEGORIZED_ITEM_ID -> RecordsFilter.TagItem.Untagged
            else -> RecordsFilter.TagItem.Tagged(itemId)
        }.let { currentTags.toMutableList().apply { addOrRemove(it) } }

        return handleSelectTags(
            type = type,
            currentFilters = filters,
            newItems = newTags,
            allowSameIdInSelectedFiltered = allowSameIdInSelectedFiltered,
        )
    }

    fun handleUntrackedClick(
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val hasUntrackedFilter = filters.hasUntrackedFilter()

        if (!hasUntrackedFilter) {
            val filtersAvailableWithUntrackedFilter = listOf(
                RecordsFilter.Date::class.java,
                RecordsFilter.DaysOfWeek::class.java,
                RecordsFilter.TimeOfDay::class.java,
                RecordsFilter.Duration::class.java,
            )
            filters.removeAll {
                it::class.java !in filtersAvailableWithUntrackedFilter
            }

            filters.add(RecordsFilter.Untracked)
        } else {
            filters.removeAll { it is RecordsFilter.Untracked }
        }

        return filters
    }

    fun handleMultitaskClick(
        currentFilters: List<RecordsFilter>,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()

        filters.removeAll { it is RecordsFilter.ManuallyFiltered }
        if (!filters.hasMultitaskFilter()) {
            filters.removeAll { it is RecordsFilter.Untracked }
            filters.add(RecordsFilter.Multitask)
        } else {
            filters.removeAll { it is RecordsFilter.Multitask }
        }

        return filters
    }

    fun handleDuplicationsFilterClick(
        currentFilters: List<RecordsFilter>,
        itemType: FilterViewData.Type,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val currentItems = filters.getDuplicationItems()

        val clickedItem = when (itemType) {
            is RecordFilterDuplicationsType.SameActivity -> {
                RecordsFilter.DuplicationsItem.SameActivity
            }
            is RecordFilterDuplicationsType.SameTimes -> {
                RecordsFilter.DuplicationsItem.SameTimes
            }
            else -> return currentFilters
        }
        val hasDefaultItem = currentItems.any { it is RecordsFilter.DuplicationsItem.SameTimes }
        val defaultItemIsClicked = clickedItem is RecordsFilter.DuplicationsItem.SameTimes
        val newItems = currentItems.toMutableList().apply {
            when {
                hasDefaultItem && defaultItemIsClicked -> {
                    // Remove all filters if default will be removed.
                    clear()
                }
                !hasDefaultItem && !defaultItemIsClicked -> {
                    // Add default filter if it is not added.
                    add(RecordsFilter.DuplicationsItem.SameTimes)
                    addOrRemove(clickedItem)
                }
                else -> {
                    addOrRemove(clickedItem)
                }
            }
        }

        filters.removeAll { it is RecordsFilter.Duplications }
        if (newItems.isNotEmpty()) filters.add(RecordsFilter.Duplications(newItems))

        return filters
    }

    fun handleCommentFilterClick(
        currentFilters: List<RecordsFilter>,
        itemType: FilterViewData.Type,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val currentItems = filters.getCommentItems()

        val clickedItem = when (itemType) {
            is RecordFilterCommentType.NoComment -> {
                RecordsFilter.CommentItem.NoComment
            }
            is RecordFilterCommentType.AnyComment -> {
                RecordsFilter.CommentItem.AnyComment
            }
            else -> return currentFilters
        }
        val newItems = currentItems.toMutableList().apply {
            if (clickedItem !in this) clear()
            addOrRemove(clickedItem)
        }

        filters.removeAll { it is RecordsFilter.Comment }
        if (newItems.isNotEmpty()) filters.add(RecordsFilter.Comment(newItems))

        return filters
    }

    fun handleCommentChange(
        currentFilters: List<RecordsFilter>,
        text: String,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        filters.removeAll { it is RecordsFilter.Comment }
        if (text.isNotEmpty()) {
            val newItems = RecordsFilter.CommentItem.Comment(text).let(::listOf)
            filters.add(RecordsFilter.Comment(newItems))
        }
        return filters
    }

    fun handleRecordClick(
        currentFilters: List<RecordsFilter>,
        viewData: ViewHolderType,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val newItem = viewData.toManuallyFilteredItem() ?: return currentFilters
        val newItems = filters.getManuallyFilteredItems()
            .toMutableMap()
            .apply { addOrRemove(newItem, true) }
        filters.removeAll { it is RecordsFilter.ManuallyFiltered }
        if (newItems.isNotEmpty()) {
            filters.add(RecordsFilter.ManuallyFiltered(newItems.keys.toList()))
        }
        return filters
    }

    fun handleInvertSelection(
        currentFilters: List<RecordsFilter>,
        recordsViewData: RecordsFilterSelectedRecordsViewData?,
    ): List<RecordsFilter> {
        if (recordsViewData == null || recordsViewData.isLoading) return currentFilters

        val filters = currentFilters.toMutableList()
        val filteredItems = filters.getManuallyFilteredItems()
        val selectedItems = recordsViewData.recordsViewData.tryCast<RecordsViewData.Content>()
            ?.viewData.orEmpty()
            .mapNotNull {
                val item = it.toManuallyFilteredItem() ?: return@mapNotNull null
                if (item !in filteredItems) item else null
            }

        filters.removeAll { it is RecordsFilter.ManuallyFiltered }
        if (selectedItems.isNotEmpty()) filters.add(RecordsFilter.ManuallyFiltered(selectedItems))
        return filters
    }

    suspend fun handleFilterDuplicates(
        currentFilters: List<RecordsFilter>,
        recordsViewData: RecordsFilterSelectedRecordsViewData?,
    ): List<RecordsFilter> {
        if (recordsViewData == null || recordsViewData.isLoading) return currentFilters

        val filters = currentFilters.toMutableList()
        filters.removeAll { it is RecordsFilter.ManuallyFiltered }
        val records = recordFilterInteractor.getByFilter(filters)
        val result = getDuplicatedRecordsInteractor.execute(
            filters = filters.getDuplicationItems(),
            records = records,
        )
        val selectedIds = recordsViewData.recordsViewData.tryCast<RecordsViewData.Content>()
            ?.viewData.orEmpty()
            .mapNotNull {
                if (it !is RecordViewData.Tracked) return@mapNotNull null
                if (it.id in result.duplications) it.toManuallyFilteredItem() else null
            }

        if (selectedIds.isNotEmpty()) filters.add(RecordsFilter.ManuallyFiltered(selectedIds))
        return filters
    }

    fun onDurationSet(
        currentFilters: List<RecordsFilter>,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        filters.removeAll { it is RecordsFilter.Duration }
        filters.add(RecordsFilter.Duration(Range(rangeStart, rangeEnd)))
        return filters
    }

    fun handleDateSet(
        currentFilters: List<RecordsFilter>,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val range = Range(rangeStart, rangeEnd)
        filters.removeAll { it is RecordsFilter.Date }
        filters.add(RecordsFilter.Date(RangeLength.Custom(range), 0))
        return filters
    }

    fun handleRangeSet(
        currentFilters: List<RecordsFilter>,
        itemType: FilterViewData.Type,
        currentRange: Range,
    ): List<RecordsFilter> {
        val rangeLength = (itemType as? RecordFilterDateType)?.rangeLength
            ?: return currentFilters
        val newRange = if (rangeLength is RangeLength.Custom) {
            val newCustomRange = Range(currentRange.timeStarted, currentRange.timeEnded)
            RangeLength.Custom(newCustomRange)
        } else {
            rangeLength
        }
        val filters = currentFilters.toMutableList()
        filters.removeAll { it is RecordsFilter.Date }
        filters.add(RecordsFilter.Date(newRange, 0))
        return filters
    }

    fun handleTimeOfDaySet(
        currentFilters: List<RecordsFilter>,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        filters.removeAll { it is RecordsFilter.TimeOfDay }
        filters.add(RecordsFilter.TimeOfDay(Range(rangeStart, rangeEnd)))
        return filters
    }

    fun removeFilter(
        currentFilters: List<RecordsFilter>,
        type: RecordFilterType,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val filterClass = recordsFilterViewDataMapper.mapToClass(type)
        filterClass?.let { filters.removeAll { filterClass.isInstance(it) } }
        when (type) {
            is RecordFilterType.Activity -> filters.removeAll { it is RecordsFilter.Category }
            is RecordFilterType.Category -> filters.removeAll { it is RecordsFilter.Activity }
            is RecordFilterType.Multitask,
            is RecordFilterType.Untracked,
            -> filters.removeAll { it is RecordsFilter.ManuallyFiltered }
            else -> Unit
        }
        return filters
    }

    fun handleDayOfWeekClick(
        currentFilters: List<RecordsFilter>,
        dayOfWeek: DayOfWeek,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        val newDays = filters.getDaysOfWeek()
            .toMutableList()
            .apply { addOrRemove(dayOfWeek) }

        filters.removeAll { it is RecordsFilter.DaysOfWeek }
        if (newDays.isNotEmpty()) filters.add(RecordsFilter.DaysOfWeek(newDays))
        return filters
    }

    fun onTypesSelectionButtonClick(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        subtype: RecordsFilterSelectionButtonType.Subtype,
        recordTypes: List<RecordType>,
        recordTypeCategories: List<RecordTypeCategory>,
        recordTags: List<RecordTag>,
        typesToTags: List<RecordTypeToTag>,
    ): List<RecordsFilter> {
        val newIds = when (subtype) {
            is RecordsFilterSelectionButtonType.Subtype.SelectAll -> recordTypes.map { it.id }
            is RecordsFilterSelectionButtonType.Subtype.SelectNone -> emptyList()
        }
        return handleSelectTypes(
            type = type,
            currentFilters = currentFilters,
            newIds = newIds,
            allowSameIdInSelectedFiltered = false,
        ).let {
            checkTagFilterConsistency(
                currentFilters = it,
                recordTypes = recordTypes,
                recordTypeCategories = recordTypeCategories,
                recordTags = recordTags,
                typesToTags = typesToTags,
            )
        }
    }

    fun onCategoriesSelectionButtonClick(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        subtype: RecordsFilterSelectionButtonType.Subtype,
        categories: List<Category>,
        recordTypes: List<RecordType>,
        recordTypeCategories: List<RecordTypeCategory>,
        recordTags: List<RecordTag>,
        typesToTags: List<RecordTypeToTag>,
    ): List<RecordsFilter> {
        val newItems = when (subtype) {
            is RecordsFilterSelectionButtonType.Subtype.SelectAll -> {
                categories
                    .map { RecordsFilter.CategoryItem.Categorized(it.id) }
                    .plus(RecordsFilter.CategoryItem.Uncategorized)
            }
            is RecordsFilterSelectionButtonType.Subtype.SelectNone -> {
                emptyList()
            }
        }
        return handleSelectCategories(
            type = type,
            currentFilters = currentFilters,
            newItems = newItems,
            allowSameIdInSelectedFiltered = false,
        ).let {
            checkTagFilterConsistency(
                currentFilters = it,
                recordTypes = recordTypes,
                recordTypeCategories = recordTypeCategories,
                recordTags = recordTags,
                typesToTags = typesToTags,
            )
        }
    }

    fun onTagsSelectionButtonClick(
        currentFilters: List<RecordsFilter>,
        subtype: RecordsFilterSelectionButtonType.Subtype,
        type: RecordFilterSelectionType,
        tags: List<RecordTag>,
    ): List<RecordsFilter> {
        val newItems = when (subtype) {
            is RecordsFilterSelectionButtonType.Subtype.SelectAll -> {
                tags
                    .map { RecordsFilter.TagItem.Tagged(it.id) }
                    .plus(RecordsFilter.TagItem.Untagged)
            }
            is RecordsFilterSelectionButtonType.Subtype.SelectNone -> {
                emptyList()
            }
        }
        return handleSelectTags(
            type = type,
            currentFilters = currentFilters,
            newItems = newItems,
            allowSameIdInSelectedFiltered = false,
        )
    }

    private fun checkTagFilterConsistency(
        currentFilters: List<RecordsFilter>,
        recordTypes: List<RecordType>,
        recordTypeCategories: List<RecordTypeCategory>,
        recordTags: List<RecordTag>,
        typesToTags: List<RecordTypeToTag>,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()
        // Update tags according to selected activities.
        // If no activities selected - show all tags.
        val newTypeIds: List<Long> = filters
            .getAllTypeIds(recordTypes, recordTypeCategories)
            .takeUnless { it.isEmpty() }
            ?: recordTypes.map(RecordType::id)
        val tagIds = recordTags.map(RecordTag::id)
        val selectableTagIds = filterSelectableTagsInteractor.execute(
            tagIds = tagIds,
            typesToTags = typesToTags,
            typeIds = newTypeIds,
        )

        fun update(tags: List<RecordsFilter.TagItem>): List<RecordsFilter.TagItem> {
            return tags.filter {
                when (it) {
                    is RecordsFilter.TagItem.Tagged -> {
                        it.tagId in selectableTagIds
                    }
                    is RecordsFilter.TagItem.Untagged -> {
                        true
                    }
                }
            }
        }

        val newSelectedTags = update(filters.getSelectedTags())
        val newFilteredTags = update(filters.getFilteredTags())
        filters.removeAll { filter -> filter is RecordsFilter.Tags }
        if (newSelectedTags.isNotEmpty() || newFilteredTags.isNotEmpty()) {
            filters.add(RecordsFilter.Tags(selected = newSelectedTags, filtered = newFilteredTags))
        }

        return filters
    }

    private fun handleSelectTypes(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        newIds: List<Long>,
        allowSameIdInSelectedFiltered: Boolean,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()

        val newFilter = when (type) {
            is RecordFilterSelectionType.Select -> RecordsFilter.Activity(
                selected = newIds,
                filtered = filters.getFilteredTypeIds().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newIds else true
                },
            )
            is RecordFilterSelectionType.Filter -> RecordsFilter.Activity(
                selected = filters.getTypeIds().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newIds else true
                },
                filtered = newIds,
            )
        }
        filters.removeAll { it is RecordsFilter.Untracked }
        filters.removeAll { it is RecordsFilter.Activity }
        if (type is RecordFilterSelectionType.Select) {
            val currentFilteredItems = filters.getFilteredCategoryItems()
            val newCategoryFilter = RecordsFilter.Category(
                selected = emptyList(),
                filtered = currentFilteredItems,
            )
            filters.removeAll { it is RecordsFilter.Category }
            if (currentFilteredItems.isNotEmpty()) filters.add(newCategoryFilter)
        }
        if (newFilter.selected.isNotEmpty() || newFilter.filtered.isNotEmpty()) {
            filters.add(newFilter)
        }

        return filters
    }

    private fun handleSelectCategories(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        newItems: List<RecordsFilter.CategoryItem>,
        allowSameIdInSelectedFiltered: Boolean,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()

        val newFilter = when (type) {
            is RecordFilterSelectionType.Select -> RecordsFilter.Category(
                selected = newItems,
                filtered = filters.getFilteredCategoryItems().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newItems else true
                },
            )
            is RecordFilterSelectionType.Filter -> RecordsFilter.Category(
                selected = filters.getCategoryItems().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newItems else true
                },
                filtered = newItems,
            )
        }
        filters.removeAll { it is RecordsFilter.Untracked }
        filters.removeAll { it is RecordsFilter.Category }
        if (type is RecordFilterSelectionType.Select) {
            val currentFilteredIds = filters.getFilteredTypeIds()
            val newActivityFilter = RecordsFilter.Activity(
                selected = emptyList(),
                filtered = currentFilteredIds,
            )
            filters.removeAll { it is RecordsFilter.Activity }
            if (currentFilteredIds.isNotEmpty()) filters.add(newActivityFilter)
        }
        if (newFilter.selected.isNotEmpty() || newFilter.filtered.isNotEmpty()) {
            filters.add(newFilter)
        }

        return filters
    }

    private fun handleSelectTags(
        type: RecordFilterSelectionType,
        currentFilters: List<RecordsFilter>,
        newItems: List<RecordsFilter.TagItem>,
        allowSameIdInSelectedFiltered: Boolean,
    ): List<RecordsFilter> {
        val filters = currentFilters.toMutableList()

        val newFilter = when (type) {
            RecordFilterSelectionType.Select -> RecordsFilter.Tags(
                selected = newItems,
                filtered = filters.getFilteredTags().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newItems else true
                },
            )
            RecordFilterSelectionType.Filter -> RecordsFilter.Tags(
                selected = filters.getSelectedTags().filter {
                    if (!allowSameIdInSelectedFiltered) it !in newItems else true
                },
                filtered = newItems,
            )
        }
        filters.removeAll { it is RecordsFilter.Untracked }
        filters.removeAll { it is RecordsFilter.Tags }
        if (newFilter.selected.isNotEmpty() || newFilter.filtered.isNotEmpty()) {
            filters.add(newFilter)
        }

        return filters
    }

    private fun ViewHolderType.toManuallyFilteredItem(): RecordsFilter.ManuallyFilteredItem? {
        return when (this) {
            is RecordViewData.Tracked ->
                RecordsFilter.ManuallyFilteredItem.Tracked(this.id)
            is RecordViewData.Untracked ->
                Range(this.timeStartedTimestamp, this.timeEndedTimestamp)
                    .let { RecordsFilter.ManuallyFilteredItem.Untracked(it) }
            is RunningRecordViewData ->
                RecordsFilter.ManuallyFilteredItem.Running(this.id)
            is MultitaskRecordViewData ->
                RecordsFilter.ManuallyFilteredItem.Multitask(this.ids)
            else -> null
        }
    }
}