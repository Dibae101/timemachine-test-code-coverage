package com.example.util.simpletimetracker.feature_records_filter.mapper

import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.core.text.buildSpannedString
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.RecordTagViewDataMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_records_filter.R
import com.example.util.simpletimetracker.feature_views.TextViewRoundedSpans
import com.example.util.simpletimetracker.feature_views.extension.setSpan
import com.example.util.simpletimetracker.feature_views.extension.toSpannableString
import javax.inject.Inject

class RecordsFilterFavouriteViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
    private val colorMapper: ColorMapper,
    private val recordsFilterViewDataMapper: RecordsFilterViewDataMapper,
    private val recordTagViewDataMapper: RecordTagViewDataMapper,
) {

    fun mapFilterName(
        filter: RecordsFilter,
        useMilitaryTime: Boolean,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
        isDarkTheme: Boolean,
        recordTypes: Map<Long, RecordType>,
        recordTypesOrder: List<Long>,
        categories: Map<Long, Category>,
        categoriesOrder: List<Long>,
        recordTags: Map<Long, RecordTag>,
        recordTagsOrder: List<Long>,
    ): CharSequence {
        val exclude = resourceRepo.getString(R.string.records_filter_exclude)
        val unknownText = "?"
        val unknownColor = Color.BLACK
        val colorAccent = resourceRepo.getThemedAttr(R.attr.colorAccent, isDarkTheme)
        val untaggedColor = resourceRepo.getThemedAttr(R.attr.appUntrackedColor, isDarkTheme)

        val filterName = filter
            .let(recordsFilterViewDataMapper::mapToViewData)
            .let(recordsFilterViewDataMapper::mapInactiveFilterName)
            .toSpannableString()
            .setSpan(span = StyleSpan(BOLD))

        fun AppColor.mapColor(): Int {
            return colorMapper.mapToColorInt(this, isDarkTheme)
        }

        fun mapItems(
            selected: List<Pair<Int, String>>,
            filtered: List<Pair<Int, String>>,
        ): CharSequence {
            fun SpannableStringBuilder.appendSpace() {
                repeat(times = 2, action = { append(" ") })
            }

            fun toColoredSpan(colorToText: Pair<Int, String>): CharSequence {
                val span = TextViewRoundedSpans.MarkerSpan(colorToText.first)
                return buildSpannedString {
                    appendSpace()
                    append(colorToText.second.toSpannableString().setSpan(span = span))
                    appendSpace()
                }
            }
            return buildSpannedString {
                selected.map(::toColoredSpan).forEach(::append)
                if (filtered.isNotEmpty()) {
                    append(" $exclude ")
                    filtered.map(::toColoredSpan).forEach(::append)
                }
            }
        }

        fun mapActivityItem(item: Long): Pair<Int, String> {
            val color = recordTypes[item]?.color?.mapColor() ?: Color.BLACK
            val text = recordTypes[item]?.name ?: unknownText
            return color to text
        }

        fun mapCategoryItem(item: RecordsFilter.CategoryItem): Pair<Int, String> {
            val color: Int
            val text: String
            when (item) {
                is RecordsFilter.CategoryItem.Categorized -> {
                    color = categories[item.categoryId]?.color?.mapColor() ?: unknownColor
                    text = categories[item.categoryId]?.name ?: unknownText
                }
                is RecordsFilter.CategoryItem.Uncategorized -> {
                    color = untaggedColor
                    text = resourceRepo.getString(R.string.uncategorized_time_name)
                }
            }
            return color to text
        }

        fun mapTagItem(item: RecordsFilter.TagItem): Pair<Int, String> {
            val color: Int
            val text: String
            when (item) {
                is RecordsFilter.TagItem.Tagged -> {
                    color = recordTags[item.tagId]
                        ?.let { recordTagViewDataMapper.mapColor(it, recordTypes) }
                        ?.mapColor() ?: unknownColor
                    text = recordTags[item.tagId]?.name ?: unknownText
                }
                is RecordsFilter.TagItem.Untagged -> {
                    color = untaggedColor
                    text = resourceRepo.getString(R.string.change_record_untagged)
                }
            }
            return color to text
        }

        fun mapDayOfWeek(item: DayOfWeek): Pair<Int, String> {
            val color = colorAccent
            val text = timeMapper.toShortDayOfWeekName(item)
            return color to text
        }

        val filterValue = when (filter) {
            is RecordsFilter.Untracked,
            is RecordsFilter.Multitask,
            is RecordsFilter.Duplications,
            is RecordsFilter.Comment,
            is RecordsFilter.Date,
            is RecordsFilter.ManuallyFiltered,
            is RecordsFilter.TimeOfDay,
            is RecordsFilter.Duration,
            -> recordsFilterViewDataMapper.mapFilterValue(
                filter = filter,
                useMilitaryTime = useMilitaryTime,
                startOfDayShift = startOfDayShift,
                firstDayOfWeek = firstDayOfWeek,
            )
            is RecordsFilter.Activity -> {
                mapItems(
                    selected = filter.selected
                        .sortActivities(recordTypesOrder)
                        .map(::mapActivityItem),
                    filtered = filter.filtered
                        .sortActivities(recordTypesOrder)
                        .map(::mapActivityItem),
                )
            }
            is RecordsFilter.Category -> {
                mapItems(
                    selected = filter.selected
                        .sortCategories(categoriesOrder)
                        .map(::mapCategoryItem),
                    filtered = filter.filtered
                        .sortCategories(categoriesOrder)
                        .map(::mapCategoryItem),
                )
            }
            is RecordsFilter.Tags -> {
                mapItems(
                    selected = filter.selected
                        .sortTags(recordTagsOrder)
                        .map(::mapTagItem),
                    filtered = filter.filtered
                        .sortTags(recordTagsOrder)
                        .map(::mapTagItem),
                )
            }
            is RecordsFilter.DaysOfWeek -> {
                mapItems(
                    selected = filter.items.map(::mapDayOfWeek),
                    filtered = emptyList(),
                )
            }
        }

        return buildSpannedString {
            append(filterName)
            if (filterValue.isNotEmpty()) {
                append(" ").append(filterValue)
            }
        }
    }

    private fun List<Long>.sortActivities(
        order: List<Long>,
    ): List<Long> {
        return this.sortedBy { order.indexOf(it) }
    }

    private fun List<RecordsFilter.CategoryItem>.sortCategories(
        order: List<Long>,
    ): List<RecordsFilter.CategoryItem> {
        return this.sortedBy {
            when (it) {
                is RecordsFilter.CategoryItem.Categorized -> order.indexOf(it.categoryId)
                is RecordsFilter.CategoryItem.Uncategorized -> Int.MAX_VALUE
            }
        }
    }

    private fun List<RecordsFilter.TagItem>.sortTags(
        order: List<Long>,
    ): List<RecordsFilter.TagItem> {
        return this.sortedBy {
            when (it) {
                is RecordsFilter.TagItem.Tagged -> order.indexOf(it.tagId)
                is RecordsFilter.TagItem.Untagged -> Int.MAX_VALUE
            }
        }
    }
}