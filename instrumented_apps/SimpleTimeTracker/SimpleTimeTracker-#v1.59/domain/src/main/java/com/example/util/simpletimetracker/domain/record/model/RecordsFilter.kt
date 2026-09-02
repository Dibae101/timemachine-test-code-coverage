package com.example.util.simpletimetracker.domain.record.model

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength

sealed interface RecordsFilter {

    // Incompatible with Activity, Category, Comment, SelectedTags, FilteredTags, ManuallyFiltered.
    data object Untracked : RecordsFilter

    data object Multitask : RecordsFilter

    data class Activity(val selected: List<Long>, val filtered: List<Long>) : RecordsFilter

    data class Category(val selected: List<CategoryItem>, val filtered: List<CategoryItem>) : RecordsFilter

    data class Comment(val items: List<CommentItem>) : RecordsFilter

    data class Date(val range: RangeLength, val position: Int) : RecordsFilter

    data class Tags(val selected: List<TagItem>, val filtered: List<TagItem>) : RecordsFilter

    data class ManuallyFiltered(val items: List<ManuallyFilteredItem>) : RecordsFilter

    data class DaysOfWeek(val items: List<DayOfWeek>) : RecordsFilter

    data class TimeOfDay(val range: Range) : RecordsFilter // duration-from, duration-to in range.

    data class Duration(val range: Range) : RecordsFilter // duration-from, duration-to in range.

    data class Duplications(val items: List<DuplicationsItem>) : RecordsFilter

    sealed interface CommentItem {
        data object NoComment : CommentItem
        data object AnyComment : CommentItem
        data class Comment(val text: String) : CommentItem
    }

    sealed interface CategoryItem {
        data class Categorized(val categoryId: Long) : CategoryItem
        data object Uncategorized : CategoryItem
    }

    sealed interface TagItem {
        data class Tagged(val tagId: Long) : TagItem
        data object Untagged : TagItem
    }

    sealed interface DuplicationsItem {
        data object SameActivity : DuplicationsItem
        data object SameTimes : DuplicationsItem
    }

    sealed interface ManuallyFilteredItem {
        data class Tracked(val id: Long) : ManuallyFilteredItem
        data class Running(val id: Long) : ManuallyFilteredItem
        data class Multitask(val ids: List<Long>) : ManuallyFilteredItem
        data class Untracked(val range: Range) : ManuallyFilteredItem {
            private val id: Long = range.timeStarted
            override fun equals(other: Any?): Boolean = (other as? Untracked)?.id == id
            override fun hashCode(): Int = id.hashCode()
        }
    }
}