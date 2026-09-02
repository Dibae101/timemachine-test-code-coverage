package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.core.interactor.RecordFilterInteractor
import com.example.util.simpletimetracker.domain.record.extension.getCategoryItems
import com.example.util.simpletimetracker.domain.record.extension.getSelectedTags
import com.example.util.simpletimetracker.domain.record.extension.getTypeIds
import com.example.util.simpletimetracker.domain.record.extension.hasMultitaskFilter
import com.example.util.simpletimetracker.domain.record.extension.hasSelectedTagsFilter
import com.example.util.simpletimetracker.domain.record.extension.hasUntrackedFilter
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.extension.getFilteredCategoryItems
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTags
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTypeIds
import com.example.util.simpletimetracker.domain.record.extension.hasSelectedActivityFilter
import com.example.util.simpletimetracker.domain.record.extension.hasSelectedCategoryFilter
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_statistics_detail.mapper.StatisticsDetailViewDataMapper
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreviewCompareViewData
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreviewMoreViewData
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreviewViewData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatisticsDetailPreviewInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordFilterInteractor: RecordFilterInteractor,
    private val statisticsDetailViewDataMapper: StatisticsDetailViewDataMapper,
) {

    fun getPreviewType(
        filterParams: List<RecordsFilter>,
    ): PreviewType {
        return when {
            filterParams.hasUntrackedFilter() -> PreviewType.Untracked
            filterParams.hasMultitaskFilter() -> PreviewType.Multitask
            filterParams.hasSelectedActivityFilter() -> PreviewType.Activities
            filterParams.hasSelectedCategoryFilter() -> PreviewType.Categories
            filterParams.hasSelectedTagsFilter() -> PreviewType.SelectedTags
            else -> PreviewType.ActivitiesFromRecords
        }
    }

    suspend fun getPreviewData(
        filterParams: List<RecordsFilter>,
        total: Boolean,
        isExpanded: Boolean,
        isForComparison: Boolean,
    ): List<ViewHolderType> = withContext(Dispatchers.Default) {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val previewType = getPreviewType(filterParams)

        fun mapTotal(): List<ViewHolderType> {
            return statisticsDetailViewDataMapper.mapToTotalPreview(
                isDarkTheme = isDarkTheme,
                isForComparison = isForComparison,
            ).let(::listOf)
        }

        suspend fun mapActivities(
            selectedIds: List<Long>,
            filteredIds: List<Long>,
        ): List<ViewHolderType> {
            return recordTypeInteractor.getAll()
                .filter { it.id in selectedIds }
                .mapIndexed { index, type ->
                    statisticsDetailViewDataMapper.mapToPreview(
                        recordType = type,
                        isDarkTheme = isDarkTheme,
                        showName = !isForComparison && selectedIds.size == 1,
                        isForComparison = isForComparison,
                        isFiltered = type.id in filteredIds,
                    )
                }
        }

        val viewData = when (previewType) {
            is PreviewType.Untracked -> {
                statisticsDetailViewDataMapper.mapUntrackedPreview(
                    isDarkTheme = isDarkTheme,
                    isForComparison = isForComparison,
                ).let(::listOf)
            }
            is PreviewType.Multitask -> {
                statisticsDetailViewDataMapper.mapMultitaskPreview(
                    isDarkTheme = isDarkTheme,
                    isForComparison = isForComparison,
                ).let(::listOf)
            }
            is PreviewType.Activities -> {
                val selectedIds = filterParams.getTypeIds()
                val filteredIds = filterParams.getFilteredTypeIds()
                mapActivities(selectedIds = selectedIds, filteredIds = filteredIds)
            }
            is PreviewType.Categories -> {
                val selectedCategories = filterParams.getCategoryItems()
                val filteredCategories = filterParams.getFilteredCategoryItems()
                val categories = categoryInteractor.getAll().associateBy(Category::id)
                selectedCategories.mapNotNull {
                    when (it) {
                        is RecordsFilter.CategoryItem.Categorized -> {
                            val category = categories[it.categoryId] ?: return@mapNotNull null
                            statisticsDetailViewDataMapper.mapToCategorizedPreview(
                                category = category,
                                isDarkTheme = isDarkTheme,
                                isForComparison = isForComparison,
                                isFiltered = it in filteredCategories,
                            )
                        }
                        is RecordsFilter.CategoryItem.Uncategorized -> {
                            statisticsDetailViewDataMapper.mapToUncategorizedPreview(
                                isDarkTheme = isDarkTheme,
                                isForComparison = isForComparison,
                                isFiltered = it in filteredCategories,
                            )
                        }
                    }
                }
            }
            is PreviewType.SelectedTags -> {
                val selectedTags = filterParams.getSelectedTags()
                val filteredTags = filterParams.getFilteredTags()
                val types = recordTypeInteractor.getAll().associateBy(RecordType::id)
                val tags = recordTagInteractor.getAll().associateBy(RecordTag::id)
                selectedTags.mapNotNull {
                    when (it) {
                        is RecordsFilter.TagItem.Tagged -> {
                            val tag = tags[it.tagId] ?: return@mapNotNull null
                            statisticsDetailViewDataMapper.mapToTaggedPreview(
                                tag = tag,
                                types = types,
                                isDarkTheme = isDarkTheme,
                                isForComparison = isForComparison,
                                isFiltered = it in filteredTags,
                            )
                        }
                        is RecordsFilter.TagItem.Untagged -> {
                            statisticsDetailViewDataMapper.mapToUntaggedPreview(
                                isDarkTheme = isDarkTheme,
                                isForComparison = isForComparison,
                                isFiltered = it in filteredTags,
                            )
                        }
                    }
                }
            }
            is PreviewType.ActivitiesFromRecords -> {
                val records = recordFilterInteractor.getByFilter(filterParams)
                val selectedIds = records.map { it.typeIds }.flatten().distinct()
                val filteredIds = filterParams.getFilteredTypeIds()
                mapActivities(selectedIds = selectedIds, filteredIds = filteredIds)
            }
        }.let {
            if (total) mapTotal() + it else it
        }.let {
            if (it.size > MAX_PREVIEWS_COUNT && !isExpanded) {
                val type = if (isForComparison) {
                    StatisticsDetailPreviewViewData.Type.COMPARISON
                } else {
                    StatisticsDetailPreviewViewData.Type.FILTER
                }
                it.take(MAX_PREVIEWS_COUNT) + StatisticsDetailPreviewMoreViewData(type)
            } else {
                it
            }
        }

        return@withContext if (isForComparison) {
            buildComparisonViewData(viewData)
        } else {
            buildFilterViewData(viewData, isDarkTheme)
        }
    }

    private fun buildFilterViewData(
        viewData: List<ViewHolderType>,
        isDarkTheme: Boolean,
    ): List<ViewHolderType> {
        return when {
            viewData.isEmpty() -> {
                statisticsDetailViewDataMapper
                    .mapToPreviewEmpty(isDarkTheme)
                    .let(::listOf)
            }
            else -> {
                viewData
            }
        }
    }

    private fun buildComparisonViewData(
        viewData: List<ViewHolderType>,
    ): List<ViewHolderType> {
        return if (viewData.isEmpty()) {
            viewData
        } else {
            StatisticsDetailPreviewCompareViewData.let(::listOf) + viewData
        }
    }

    sealed interface PreviewType {
        data object Untracked : PreviewType
        data object Multitask : PreviewType
        data object Activities : PreviewType
        data object Categories : PreviewType
        data object SelectedTags : PreviewType
        data object ActivitiesFromRecords : PreviewType
    }

    companion object {
        private const val MAX_PREVIEWS_COUNT = 5
    }
}