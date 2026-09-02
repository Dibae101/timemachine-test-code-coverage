package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.domain.record.extension.getAllTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getCommentItems
import com.example.util.simpletimetracker.domain.record.extension.getComments
import com.example.util.simpletimetracker.domain.record.extension.getDate
import com.example.util.simpletimetracker.domain.record.extension.getDaysOfWeek
import com.example.util.simpletimetracker.domain.record.extension.getDuration
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTags
import com.example.util.simpletimetracker.domain.record.extension.getSelectedTags
import com.example.util.simpletimetracker.domain.record.extension.getTaggedIds
import com.example.util.simpletimetracker.domain.record.extension.getTimeOfDay
import com.example.util.simpletimetracker.domain.record.extension.getTypeIds
import com.example.util.simpletimetracker.domain.record.extension.hasAnyComment
import com.example.util.simpletimetracker.domain.record.extension.hasMultitaskFilter
import com.example.util.simpletimetracker.domain.record.extension.hasNoComment
import com.example.util.simpletimetracker.domain.record.extension.hasUntaggedItem
import com.example.util.simpletimetracker.domain.record.extension.hasUntrackedFilter
import com.example.util.simpletimetracker.domain.extension.toRange
import com.example.util.simpletimetracker.domain.record.interactor.GetMultitaskRecordsInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.mapper.RangeMapper
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.extension.plus
import com.example.util.simpletimetracker.domain.record.extension.getAllFilteredTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getDuplicationItems
import com.example.util.simpletimetracker.domain.record.extension.getFilteredTypeIds
import com.example.util.simpletimetracker.domain.record.extension.getManuallyFilteredItems
import com.example.util.simpletimetracker.domain.record.extension.hasDuplicationsFilter
import com.example.util.simpletimetracker.domain.record.extension.hasFilteredCategoryFilter
import com.example.util.simpletimetracker.domain.record.extension.hasSelectedCategoryFilter
import com.example.util.simpletimetracker.domain.record.extension.toManuallyFilteredItem
import com.example.util.simpletimetracker.domain.record.interactor.GetDuplicatedRecordsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.GetUntrackedRecordsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor.GetParam
import com.example.util.simpletimetracker.domain.record.model.MultitaskRecord
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.record.model.RunningRecord
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordFilterInteractor @Inject constructor(
    private val interactor: RecordInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val getUntrackedRecordsInteractor: GetUntrackedRecordsInteractor,
    private val getMultitaskRecordsInteractor: GetMultitaskRecordsInteractor,
    private val getDuplicatedRecordsInteractor: GetDuplicatedRecordsInteractor,
    private val timeMapper: TimeMapper,
    private val rangeMapper: RangeMapper,
    private val prefsInteractor: PrefsInteractor,
) {

    // RangeLength.All return empty range, need to check separately.
    suspend fun getRange(
        filter: RecordsFilter.Date,
    ): Range {
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()

        return timeMapper.getRangeStartAndEnd(
            rangeLength = filter.range,
            shift = filter.position,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift,
        )
    }

    suspend fun getByFilter(
        filters: List<RecordsFilter>,
    ): List<RecordBase> = withContext(Dispatchers.Default) {
        if (filters.isEmpty()) return@withContext emptyList()

        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val calendar: Calendar = Calendar.getInstance()
        var types: List<RecordType>? = null
        var typeCategories: List<RecordTypeCategory>? = null
        val typeIds: Set<Long> = when {
            filters.hasSelectedCategoryFilter() -> {
                types = recordTypeInteractor.getAll()
                typeCategories = recordTypeCategoryInteractor.getAll()
                filters.getAllTypeIds(types, typeCategories)
            }
            else -> filters.getTypeIds()
        }.toSet()
        val filteredTypeIds: Set<Long> = when {
            filters.hasFilteredCategoryFilter() -> {
                types = types ?: recordTypeInteractor.getAll()
                typeCategories = typeCategories ?: recordTypeCategoryInteractor.getAll()
                filters.getAllFilteredTypeIds(types, typeCategories)
            }
            else -> filters.getFilteredTypeIds()
        }.toSet()
        val hasUntracked: Boolean = filters.hasUntrackedFilter()
        val hasMultitask: Boolean = filters.hasMultitaskFilter()
        val runningRecords = runningRecordInteractor.getAll()
        val selectedCommentItems: List<RecordsFilter.CommentItem> = filters.getCommentItems()
        val comments: List<String> = selectedCommentItems.getComments().map(String::lowercase)
        val selectedNoComment: Boolean = selectedCommentItems.hasNoComment()
        val selectedAnyComment: Boolean = selectedCommentItems.hasAnyComment()
        val ranges: List<Range> = filters.getDate()?.let { getRange(it) }?.let(::listOf).orEmpty()
        val definedRanges = ranges.filter { it.timeStarted != 0L && it.timeEnded != 0L }
        val selectedTagItems: List<RecordsFilter.TagItem> = filters.getSelectedTags()
        val selectedTaggedIds: Set<Long> = selectedTagItems.getTaggedIds().toSet()
        val selectedUntagged: Boolean = selectedTagItems.hasUntaggedItem()
        val filteredTagItems: List<RecordsFilter.TagItem> = filters.getFilteredTags()
        val filteredTaggedIds: Set<Long> = filteredTagItems.getTaggedIds().toSet()
        val filteredUntagged: Boolean = filteredTagItems.hasUntaggedItem()
        val manuallyFilteredItems: Map<RecordsFilter.ManuallyFilteredItem, Boolean> = filters.getManuallyFilteredItems()
        val daysOfWeek: Set<DayOfWeek> = filters.getDaysOfWeek()
        val timeOfDay: Range? = filters.getTimeOfDay()
        val timeOfDayRanges: List<Range> = timeOfDay?.let(::getTimeOfDayRanges).orEmpty()
        val durations: List<Range> = filters.getDuration()?.let(::listOf).orEmpty()
        val duplicationItems: List<RecordsFilter.DuplicationsItem> = filters.getDuplicationItems()

        val records: List<RecordBase> = when {
            hasUntracked -> {
                val range = definedRanges.firstOrNull() ?: Range(0, 0)
                val records = getAllRecords(range, runningRecords)
                    .map(RecordBase::toRange)
                getUntrackedRecordsInteractor.get(range, records)
            }
            hasMultitask -> {
                val range = definedRanges.firstOrNull() ?: Range(0, 0)
                val records = getAllRecords(range, runningRecords)
                getMultitaskRecordsInteractor.get(records)
            }
            typeIds.isNotEmpty() && definedRanges.isNotEmpty() -> {
                val result = mutableMapOf<Long, Record>()
                definedRanges
                    .map { interactor.getWithParams(GetParam.FromRangeByType(typeIds, it)) }
                    .flatten()
                    .forEach { result[it.id] = it }
                result.values.toList()
            }
            typeIds.isNotEmpty() && comments.isNotEmpty() -> {
                val result = mutableMapOf<Long, Record>()
                comments
                    .map { interactor.getWithParams(GetParam.TypeWithComment(typeIds, it)) }
                    .flatten()
                    .forEach { result[it.id] = it }
                result.values.toList()
            }
            typeIds.isNotEmpty() -> {
                interactor.getWithParams(GetParam.Type(typeIds))
            }
            definedRanges.isNotEmpty() -> {
                val result = mutableMapOf<Long, Record>()
                definedRanges
                    .map { interactor.getWithParams(GetParam.FromRange(it)) }
                    .flatten()
                    .forEach { result[it.id] = it }
                result.values.toList()
            }
            comments.isNotEmpty() -> {
                interactor.getWithParams(GetParam.Comment(comments.firstOrNull().orEmpty()))
            }
            selectedAnyComment -> {
                interactor.getWithParams(GetParam.AnyComment)
            }
            selectedTagItems.isNotEmpty() -> {
                val tagged = selectedTaggedIds.takeIf { it.isNotEmpty() }
                    ?.let { interactor.getWithParams(GetParam.Tagged(it)) }.orEmpty()
                val untagged = selectedUntagged.takeIf { it }
                    ?.let { interactor.getWithParams(GetParam.Untagged) }.orEmpty()
                tagged + untagged
            }
            else -> interactor.getAll()
        }.let {
            // For these filter running records are added separately.
            if (filters.hasUntrackedFilter() || filters.hasMultitaskFilter()) {
                it
            } else {
                it + runningRecords
            }
        }

        val duplicationIds: Map<Long, Boolean> = if (filters.hasDuplicationsFilter()) {
            getDuplicatedRecordsInteractor.execute(
                filters = duplicationItems,
                records = records,
            ).let {
                it.original + it.duplications
            }.associateWith { true }
        } else {
            emptyMap()
        }

        fun RecordBase.selectedByActivity(): Boolean {
            if (typeIds.isEmpty()) return true
            return this.typeIds.any { it in typeIds }
        }

        fun RecordBase.filteredByActivity(): Boolean {
            if (filteredTypeIds.isEmpty()) return false
            return this.typeIds.any { it in filteredTypeIds }
        }

        fun RecordBase.selectedByMultitask(): Boolean {
            return !hasMultitask || this is MultitaskRecord
        }

        fun RecordBase.selectedByComment(): Boolean {
            if (selectedCommentItems.isEmpty()) return true
            val comment = this.comment.lowercase()
            return (selectedNoComment && comment.isEmpty()) ||
                (selectedAnyComment && comment.isNotEmpty()) ||
                (comment.isNotEmpty() && comments.any { comment.contains(it) })
        }

        fun RecordBase.selectedByDate(): Boolean {
            if (ranges.isEmpty()) return true
            // Overall range.
            if (ranges.any { it.isUndefined }) return true
            return ranges.any { range -> timeStarted < range.timeEnded && timeEnded > range.timeStarted }
        }

        fun RecordBase.selectedByTag(): Boolean {
            if (selectedTagItems.isEmpty()) return true
            return if (tags.isNotEmpty()) {
                tags.any { tag -> tag.tagId in selectedTaggedIds }
            } else {
                selectedUntagged
            }
        }

        fun RecordBase.filteredByTag(): Boolean {
            if (filteredTagItems.isEmpty()) return false
            return if (tags.isNotEmpty()) {
                tags.any { tag -> tag.tagId in filteredTaggedIds }
            } else {
                filteredUntagged
            }
        }

        fun RecordBase.isManuallyFiltered(): Boolean {
            if (manuallyFilteredItems.isEmpty()) return false
            return this.toManuallyFilteredItem() in manuallyFilteredItems
        }

        fun RecordBase.selectedByDuplications(): Boolean {
            if (duplicationItems.isEmpty()) return true
            if (this !is Record) return true
            return id in duplicationIds
        }

        fun RecordBase.selectedByDayOfWeek(): Boolean {
            if (daysOfWeek.isEmpty()) return true

            val daysOfRecord: MutableSet<DayOfWeek> = mutableSetOf()
            val startDay = timeMapper.getDayOfWeek(
                timestamp = timeStarted,
                calendar = calendar,
                startOfDayShift = startOfDayShift,
            )
            val endDay = timeMapper.getDayOfWeek(
                timestamp = timeEnded,
                calendar = calendar,
                startOfDayShift = startOfDayShift,
            )
            daysOfRecord.add(startDay)
            daysOfRecord.add(endDay)

            // Check long records.
            if (duration > dayInMillis) {
                var check = true
                calendar.timeInMillis = timeStarted
                // Continuously add one day to time start until reach time ended.
                while (check && daysOfRecord.size != 7) {
                    calendar.apply {
                        add(Calendar.DATE, 1)
                        if (timeInMillis < timeEnded) {
                            timeInMillis -= startOfDayShift
                            get(Calendar.DAY_OF_WEEK)
                                .let(timeMapper::toDayOfWeek)
                                .let(daysOfRecord::add)
                            timeInMillis += startOfDayShift
                        } else {
                            check = false
                        }
                    }
                }
            }

            return daysOfRecord.any { it in daysOfWeek }
        }

        fun RecordBase.selectedByTimeOfDay(): Boolean {
            if (timeOfDay == null) return true

            // Check empty range.
            if (timeOfDay.duration == 0L) return false
            // Check long records.
            if (duration > dayInMillis) return true

            val recordStart = timeStarted - timeMapper.getStartOfDayTimeStamp(timeStarted, calendar)
            val recordEnd = timeEnded - timeMapper.getStartOfDayTimeStamp(timeEnded, calendar)
            val recordRanges = if (recordStart <= recordEnd) {
                listOf(Range(recordStart, recordEnd))
            } else {
                listOf(Range(0, recordEnd), Range(recordStart, dayInMillis))
            }

            return recordRanges.any { recordRange ->
                timeOfDayRanges.any { it.isOverlappingWith(recordRange) }
            }
        }

        fun RecordBase.selectedByDuration(): Boolean {
            if (durations.isEmpty()) return true
            return durations.any { duration >= it.timeStarted && duration <= it.timeEnded }
        }

        return@withContext records.filter { record ->
            record.selectedByActivity() &&
                !record.filteredByActivity() &&
                record.selectedByMultitask() &&
                record.selectedByComment() &&
                record.selectedByDate() &&
                record.selectedByTag() &&
                !record.filteredByTag() &&
                !record.isManuallyFiltered() &&
                record.selectedByDayOfWeek() &&
                record.selectedByTimeOfDay() &&
                record.selectedByDuration() &&
                record.selectedByDuplications()
        }
    }

    private suspend fun getAllRecords(
        range: Range,
        runningRecords: List<RunningRecord>,
    ): List<RecordBase> {
        val records = if (range.isUndefined) {
            interactor.getAll() + runningRecords
        } else {
            interactor.getWithParams(GetParam.FromRange(range)) +
                rangeMapper.getRunningRecordsFromRange(runningRecords, range)
        }
        return records
    }

    private fun getTimeOfDayRanges(timeOfDay: Range): List<Range> {
        return if (timeOfDay.timeStarted <= timeOfDay.timeEnded) {
            listOf(
                Range(timeOfDay.timeStarted, timeOfDay.timeEnded),
            )
        } else {
            listOf(
                Range(0, timeOfDay.timeEnded),
                Range(timeOfDay.timeStarted, dayInMillis),
            )
        }
    }

    companion object {
        private val dayInMillis = TimeUnit.DAYS.toMillis(1)
    }
}
