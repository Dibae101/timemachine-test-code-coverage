package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.core.interactor.GetTotalStatisticsFilterInteractor
import com.example.util.simpletimetracker.domain.record.extension.getTypeIds
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import javax.inject.Inject

class StatisticsDetailTotalRecordsSelectedInteractor @Inject constructor(
    private val getTotalStatisticsFilterInteractor: GetTotalStatisticsFilterInteractor,
) {

    suspend fun execute(
        currentFilter: List<RecordsFilter>,
    ): Boolean {
        if (currentFilter.size != 1) return false
        val onlyFilter = currentFilter.firstOrNull() ?: return false
        // Don't show "total" if only one activity exists.
        if (onlyFilter is RecordsFilter.Activity && currentFilter.getTypeIds().size == 1) return false
        val chartFilter = when (onlyFilter) {
            is RecordsFilter.Activity -> ChartFilterType.ACTIVITY
            is RecordsFilter.Category -> ChartFilterType.CATEGORY
            is RecordsFilter.Tags -> ChartFilterType.RECORD_TAG
            else -> return false
        }
        val totalFilter = getTotalStatisticsFilterInteractor.execute(chartFilter)
        return totalFilter.prepareForComparison() == onlyFilter.prepareForComparison()
    }

    private fun RecordsFilter.prepareForComparison(): RecordsFilter {
        fun List<RecordsFilter.CategoryItem>.prepare(): List<RecordsFilter.CategoryItem> {
            return this.sortedBy {
                when (it) {
                    is RecordsFilter.CategoryItem.Categorized -> it.categoryId
                    is RecordsFilter.CategoryItem.Uncategorized -> -1
                }
            }
        }

        fun List<RecordsFilter.TagItem>.prepare(): List<RecordsFilter.TagItem> {
            return this.sortedBy {
                when (it) {
                    is RecordsFilter.TagItem.Tagged -> it.tagId
                    is RecordsFilter.TagItem.Untagged -> -1
                }
            }
        }

        return when (this) {
            is RecordsFilter.Activity -> {
                this.copy(
                    selected = this.selected.sorted(),
                    filtered = this.filtered.sorted(),
                )
            }
            is RecordsFilter.Category -> {
                this.copy(
                    selected = this.selected.prepare(),
                    filtered = this.filtered.prepare(),
                )
            }
            is RecordsFilter.Tags -> {
                this.copy(
                    selected = this.selected.prepare(),
                    filtered = this.filtered.prepare(),
                )
            }
            else -> this
        }
    }
}