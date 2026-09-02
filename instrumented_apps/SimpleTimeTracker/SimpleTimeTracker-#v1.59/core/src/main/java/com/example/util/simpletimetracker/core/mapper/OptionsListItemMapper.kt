package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.viewData.CommonOptionsListItem
import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import javax.inject.Inject

class OptionsListItemMapper @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val resourceRepo: ResourceRepo,
) {

    fun <T> mapCommonItem(
        id: T,
        isIconCheckVisible: Boolean = false,
    ): OptionsListParams.Item?
        where T : CommonOptionsListItem, T : OptionsListParams.Item.Id {
        val textResId: Int
        val iconResId: Int

        when (id) {
            is CommonOptionsListItem.EnabledSearch -> {
                textResId = R.string.enable_search_hint
                iconResId = R.drawable.search
            }
            is CommonOptionsListItem.Filter -> {
                textResId = R.string.chart_filter_hint
                iconResId = R.drawable.filter
            }
            is CommonOptionsListItem.Share -> {
                textResId = R.string.message_action_share
                iconResId = R.drawable.share
            }
            is CommonOptionsListItem.SelectDate -> {
                textResId = R.string.range_select_day
                iconResId = R.drawable.date
            }
            is CommonOptionsListItem.SelectRange -> {
                textResId = R.string.range_select_range
                iconResId = R.drawable.range
            }
            is CommonOptionsListItem.BackToToday -> {
                textResId = R.string.range_back_to_today
                iconResId = R.drawable.back
            }
            else -> return null
        }

        return OptionsListParams.Item(
            id = id,
            text = resourceRepo.getString(textResId),
            icon = iconResId,
            isIconCheckVisible = isIconCheckVisible,
        )
    }

    fun isIconCheckVisible(
        filteredIds: List<Long>,
        existingIds: Map<Long, Boolean>,
    ): Boolean {
        return filteredIds.any {
            val isSpecial = it in listOf(UNTRACKED_ITEM_ID, UNCATEGORIZED_ITEM_ID)
            val notRemoved = it in existingIds
            !isSpecial && notRemoved
        }
    }

    // Map is faster for contains.
    suspend fun getExistingIds(
        chartFilterType: ChartFilterType,
    ): Map<Long, Boolean> {
        return when (chartFilterType) {
            ChartFilterType.ACTIVITY -> recordTypeInteractor.getAll().map { it.id }
            ChartFilterType.CATEGORY -> categoryInteractor.getAll().map { it.id }
            ChartFilterType.RECORD_TAG -> recordTagInteractor.getAll().map { it.id }
        }.associateWith { true }
    }
}