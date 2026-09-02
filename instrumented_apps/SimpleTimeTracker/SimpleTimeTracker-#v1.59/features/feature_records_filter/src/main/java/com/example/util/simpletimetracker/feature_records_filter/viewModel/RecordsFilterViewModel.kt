package com.example.util.simpletimetracker.feature_records_filter.viewModel

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.extension.toModel
import com.example.util.simpletimetracker.core.interactor.RecordFilterInteractor
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.base.ARCHIVED_BUTTON_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.extension.getDate
import com.example.util.simpletimetracker.domain.record.extension.getDuration
import com.example.util.simpletimetracker.domain.record.extension.getTimeOfDay
import com.example.util.simpletimetracker.domain.record.extension.hasManuallyFiltered
import com.example.util.simpletimetracker.domain.record.model.FavouriteRecordsFilter
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTypeToTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeGoalInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.model.RecordTypeGoal
import com.example.util.simpletimetracker.domain.recordsFilter.interactor.FavouriteRecordsFilterInteractor
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.feature_base_adapter.R
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.buttonDouble.DoubleButtonsViewData
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.dayOfWeek.DayOfWeekViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord.MultitaskRecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordType.RecordTypeViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordsFilter.FavouriteRecordsFilterViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_records_filter.adapter.RecordsFilterButtonViewData
import com.example.util.simpletimetracker.feature_records_filter.adapter.RecordsFilterRangeViewData
import com.example.util.simpletimetracker.feature_records_filter.interactor.RecordsFilterUpdateInteractor
import com.example.util.simpletimetracker.feature_records_filter.interactor.RecordsFilterViewDataInteractor
import com.example.util.simpletimetracker.feature_records_filter.mapper.RecordsFilterViewDataMapper
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterActivitiesType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterCommentType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterDateType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterDuplicationsType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterSelectionType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordFilterType
import com.example.util.simpletimetracker.feature_records_filter.model.RecordsFilterSelectedRecordsViewData
import com.example.util.simpletimetracker.feature_records_filter.model.RecordsFilterSelectionState
import com.example.util.simpletimetracker.feature_records_filter.model.type
import com.example.util.simpletimetracker.feature_records_filter.viewData.CategoryFilteredType
import com.example.util.simpletimetracker.feature_records_filter.viewData.RecordsFilterSelectionButtonType
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogType
import com.example.util.simpletimetracker.navigation.params.screen.DurationDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterParam
import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterResultParams
import com.example.util.simpletimetracker.navigation.params.screen.StandardDialogParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class RecordsFilterViewModel @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val viewDataInteractor: RecordsFilterViewDataInteractor,
    private val recordsFilterViewDataMapper: RecordsFilterViewDataMapper,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordTypeToTagInteractor: RecordTypeToTagInteractor,
    private val recordTypeGoalInteractor: RecordTypeGoalInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
    private val router: Router,
    private val recordsFilterUpdateInteractor: RecordsFilterUpdateInteractor,
    private val recordFilterInteractor: RecordFilterInteractor,
    private val favouriteRecordsFilterInteractor: FavouriteRecordsFilterInteractor,
) : ViewModel() {

    private lateinit var extra: RecordsFilterParams

    val filtersViewData: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initial.value = listOf(LoaderViewData())
                initial.value = loadFiltersViewData()
            }
            initial
        }
    }
    val filterSelectionContent: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initial.value = listOf(LoaderViewData())
                initial.value = loadFilterSelectionViewData()
            }
            initial
        }
    }
    val recordsViewData: LiveData<RecordsFilterSelectedRecordsViewData> by lazy {
        return@lazy MutableLiveData<RecordsFilterSelectedRecordsViewData>().let { initial ->
            viewModelScope.launch {
                initial.value = getRecordsLoadState(showLoader = true)
                initial.value = loadRecordsViewData()
            }
            initial
        }
    }
    val filterSelectionVisibility: LiveData<Boolean> by lazy {
        MutableLiveData(loadFilterSelectionVisibility())
    }
    val changedFilters: LiveData<RecordsFilterResultParams> = MutableLiveData()
    val keyboardVisibility: LiveData<Boolean> = MutableLiveData(false)

    private var filters: List<RecordsFilter> = emptyList()
    private var filterSelectionState: RecordsFilterSelectionState = RecordsFilterSelectionState.Hidden
    private var tagSelectionState: RecordFilterSelectionType = RecordFilterSelectionType.Select
    private var activitiesSelectionState: RecordFilterActivitiesType = RecordFilterActivitiesType.Activities
    private var isArchivedTypesShown: Boolean = false
    private var isArchivedTagsShown: Boolean = false
    private var isDeleteFavouriteEnabled: Boolean = false
    private val defaultRange: Range by lazy { viewDataInteractor.getDefaultDateRange() }
    private val defaultDurationRange: Range by lazy { viewDataInteractor.getDefaultDurationRange() }
    private val defaultTimeOfDayRange: Range by lazy { viewDataInteractor.getDefaultTimeOfDayRange() }
    private var filtersLoadJob: Job? = null
    private var filtersSelectionLoadJob: Job? = null
    private var recordsLoadJob: Job? = null
    private var filterDuplicationsJob: Job? = null

    // Cache
    private var types: List<RecordType> = emptyList()
    private var recordTypeCategories: List<RecordTypeCategory>? = null
    private var categories: List<Category>? = null
    private var recordTags: List<RecordTag>? = null
    private var recordTypeToTag: List<RecordTypeToTag>? = null
    private var goals: List<RecordTypeGoal>? = null

    fun init(extra: RecordsFilterParams) {
        this.extra = extra
        filters = extra.filters.map(RecordsFilterParam::toModel).toMutableList()

        viewDataInteractor.filterAvailableFilters(extra, filters)
            .let { recordsFilterViewDataMapper.mapInitialFilter(it) }
            ?.let(RecordsFilterSelectionState::Visible)
            ?.let(this::filterSelectionState::set)
    }

    fun onFilterClick(item: FilterViewData) {
        val itemType = item.type as? RecordFilterType ?: return

        if (itemType is RecordFilterType.Multitask) {
            handleMultitaskClick()
            updateViewDataOnFiltersChanged()
            return
        }

        filterSelectionState = when (val currentFilterState = filterSelectionState) {
            is RecordsFilterSelectionState.Hidden -> {
                RecordsFilterSelectionState.Visible(itemType)
            }
            is RecordsFilterSelectionState.Visible -> {
                if (currentFilterState.type == itemType) {
                    RecordsFilterSelectionState.Hidden
                } else {
                    RecordsFilterSelectionState.Visible(itemType)
                }
            }
        }

        keyboardVisibility.set(false)
        updateFilters()
        updateFilterSelectionViewData()
        updateFilterSelectionVisibility()
    }

    fun onFilterRemoveClick(item: FilterViewData) {
        val itemType = item.type as? RecordFilterType ?: return
        removeFilter(itemType)
    }

    fun onRecordTypeClick(item: RecordTypeViewData) = viewModelScope.launch {
        if (item.id == ARCHIVED_BUTTON_ITEM_ID) {
            handleArchivedTypeClick()
        } else {
            handleTypeClick(RecordFilterSelectionType.Select, item.id)
        }
        updateViewDataOnFiltersChanged()
    }

    fun onFilteredRecordTypeClick(item: RecordTypeViewData) = viewModelScope.launch {
        handleTypeClick(RecordFilterSelectionType.Filter, item.id)
        updateViewDataOnFiltersChanged()
    }

    fun onCategoryClick(item: CategoryViewData) = viewModelScope.launch {
        when (item) {
            is CategoryViewData.Category -> {
                handleCategoryClick(item)
            }
            is CategoryViewData.Record -> when (item.id) {
                UNTRACKED_ITEM_ID -> handleUntrackedClick()
                ARCHIVED_BUTTON_ITEM_ID -> handleArchivedTagClick()
                else -> handleTagClick(item)
            }
        }
        updateViewDataOnFiltersChanged()
    }

    fun onSelectionButtonClick(item: DoubleButtonsViewData.Type) = viewModelScope.launch {
        val data = item
            as? RecordsFilterSelectionButtonType
            ?: return@launch
        val type = data.type
        val subtype = data.subtype
        when (type) {
            is RecordsFilterSelectionButtonType.Type.Activities -> {
                filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Activity)
                filters = recordsFilterUpdateInteractor.onTypesSelectionButtonClick(
                    type = RecordFilterSelectionType.Select,
                    currentFilters = filters,
                    subtype = subtype,
                    recordTypes = getTypesCache(),
                    recordTypeCategories = getRecordTypeCategoriesCache(),
                    recordTags = getTagsCache(),
                    typesToTags = getRecordTypeToTagCache(),
                )
            }
            is RecordsFilterSelectionButtonType.Type.Categories -> {
                filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Category)
                filters = recordsFilterUpdateInteractor.onCategoriesSelectionButtonClick(
                    type = RecordFilterSelectionType.Select,
                    currentFilters = filters,
                    subtype = subtype,
                    categories = getCategoriesCache(),
                    recordTypes = getTypesCache(),
                    recordTypeCategories = getRecordTypeCategoriesCache(),
                    recordTags = getTagsCache(),
                    typesToTags = getRecordTypeToTagCache(),
                )
            }
            is RecordsFilterSelectionButtonType.Type.Tags -> {
                filters = recordsFilterUpdateInteractor.onTagsSelectionButtonClick(
                    currentFilters = filters,
                    subtype = subtype,
                    type = tagSelectionState,
                    tags = getTagsCache(),
                )
            }
        }
        updateViewDataOnFiltersChanged()
    }

    fun onInnerFilterClick(item: FilterViewData) = viewModelScope.launch {
        val type = item.type
        when (type) {
            is RecordFilterCommentType -> handleCommentFilterClick(item)
            is RecordFilterDateType -> onDateRangeClick(item)
            is RecordFilterDuplicationsType -> handleDuplicationsFilterClick(item)
            is RecordFilterSelectionType -> handleTagSelectionTypeClick(type)
            is RecordFilterActivitiesType -> handleActivitiesSelectionClick(type)
            else -> {
                // Do nothing.
            }
        }
    }

    fun onCommentChange(text: String) {
        handleCommentChange(text)
        updateViewDataOnFiltersChanged()
    }

    fun onPositiveDialogClick(tag: String?, data: Any?) {
        when (tag) {
            DELETE_FAVOURITE_DIALOG_TAG -> onFavouriteFilterDeleteClick(data)
        }
    }

    fun onDateTimeSet(timestamp: Long, tag: String?) = viewModelScope.launch {
        when (tag) {
            TIME_STARTED_TAG, TIME_ENDED_TAG -> handleDateSet(timestamp, tag)
            TIME_OF_DAY_FROM_TAG, TIME_OF_DAY_TO_TAG -> handleTimeOfDaySet(timestamp, tag)
        }
    }

    fun onRangeTimeClick(fieldType: RecordsFilterRangeViewData.FieldType) {
        viewModelScope.launch {
            when (filterSelectionState.type) {
                RecordFilterType.Date -> handleDateFieldClick(fieldType)
                RecordFilterType.Duration -> handleDurationFieldClick(fieldType)
                RecordFilterType.TimeOfDay -> handleTimeOfDayFieldClick(fieldType)
                else -> return@launch
            }
        }
    }

    fun onDurationSet(duration: Long, tag: String?) {
        val requestedTags = listOf(
            DURATION_FROM_TAG,
            DURATION_TO_TAG,
        )
        if (tag !in requestedTags) return

        val durationInMillis = duration * 1000
        var (rangeStart, rangeEnd) = filters.getDuration() ?: defaultDurationRange

        when (tag) {
            DURATION_FROM_TAG -> {
                if (durationInMillis != rangeStart) {
                    rangeStart = durationInMillis
                    if (durationInMillis > rangeEnd) rangeEnd = durationInMillis
                }
            }
            DURATION_TO_TAG -> {
                if (durationInMillis != rangeEnd) {
                    rangeEnd = durationInMillis
                    if (durationInMillis < rangeStart) rangeStart = durationInMillis
                }
            }
        }

        filters = recordsFilterUpdateInteractor.onDurationSet(
            currentFilters = filters,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )

        updateViewDataOnFiltersChanged()
    }

    fun onRecordClick(
        item: RecordViewData,
        @Suppress("UNUSED_PARAMETER") sharedElements: Pair<Any, String>,
    ) {
        handleRecordClick(item)
    }

    fun onRunningRecordClick(
        item: RunningRecordViewData,
        @Suppress("UNUSED_PARAMETER") sharedElements: Pair<Any, String>? = null,
    ) {
        handleRecordClick(item)
    }

    fun onMultitaskRecordClick(
        item: MultitaskRecordViewData,
    ) {
        handleRecordClick(item)
    }

    fun onInnerFilterButtonClick(viewData: RecordsFilterButtonViewData) {
        when (viewData.type) {
            RecordsFilterButtonViewData.Type.INVERT_SELECTION -> {
                handleInvertSelection()
            }
            RecordsFilterButtonViewData.Type.FILTER_DUPLICATES -> {
                handleFilterDuplicates()
            }
            RecordsFilterButtonViewData.Type.SAVE_FAVOURITE -> {
                handleSaveFavourite()
            }
            RecordsFilterButtonViewData.Type.DELETE_FAVOURITE -> {
                isDeleteFavouriteEnabled = !isDeleteFavouriteEnabled
                updateFilterSelectionViewData()
            }
        }
    }

    fun onDayOfWeekClick(viewData: DayOfWeekViewData) {
        handleDayOfWeekClick(viewData.dayOfWeek)
        updateViewDataOnFiltersChanged()
    }

    fun onFavouriteFilterClick(viewData: FavouriteRecordsFilterViewData) {
        if (!viewData.isEnabled) return
        if (isDeleteFavouriteEnabled) {
            router.navigate(
                StandardDialogParams(
                    tag = DELETE_FAVOURITE_DIALOG_TAG,
                    data = DeleteFavouriteFilterData(viewData.id),
                    title = resourceRepo.getString(R.string.change_record_type_delete_alert),
                    message = resourceRepo.getString(R.string.archive_deletion_alert),
                    btnPositive = resourceRepo.getString(R.string.ok),
                    btnNegative = resourceRepo.getString(R.string.cancel),
                ),
            )
            return
        }
        viewModelScope.launch {
            val newFilter = favouriteRecordsFilterInteractor.get(viewData.id)?.filter
                ?: return@launch

            val dateFilter = filters.getDate()
            filters = if (
                (newFilter.getDate() == null || !extra.flags.dateSelectionAvailable) &&
                dateFilter != null
            ) {
                // Keep the current date unless the favourite provides its own.
                newFilter.plus(dateFilter)
            } else {
                newFilter
            }
            updateViewDataOnFiltersChanged()
        }
    }

    private fun onFavouriteFilterDeleteClick(data: Any?) {
        val data = data as? DeleteFavouriteFilterData ?: return
        viewModelScope.launch {
            favouriteRecordsFilterInteractor.remove(data.id)
            updateFilterSelectionViewData()
        }
    }

    private suspend fun onDateRangeClick(viewData: FilterViewData) {
        filters = recordsFilterUpdateInteractor.handleRangeSet(
            currentFilters = filters,
            itemType = viewData.type,
            currentRange = getCurrentRange(),
        )
        updateViewDataOnFiltersChanged()
    }

    fun onShowRecordsListClick() {
        filterSelectionState = RecordsFilterSelectionState.Hidden
        updateFilters()
        updateFilterSelectionVisibility()
    }

    private suspend fun handleTypeClick(
        type: RecordFilterSelectionType,
        id: Long,
    ) {
        filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Activity)
        filters = recordsFilterUpdateInteractor.handleTypeClick(
            type = type,
            id = id,
            allowSameIdInSelectedFiltered = false,
            currentFilters = filters,
            recordTypes = getTypesCache(),
            recordTypeCategories = getRecordTypeCategoriesCache(),
            recordTags = getTagsCache(),
            typesToTags = getRecordTypeToTagCache(),
        )
    }

    private suspend fun handleCategoryClick(item: CategoryViewData) {
        filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Category)
        filters = recordsFilterUpdateInteractor.handleCategoryClick(
            type = if (item.type is CategoryFilteredType) {
                RecordFilterSelectionType.Filter
            } else {
                RecordFilterSelectionType.Select
            },
            id = item.id,
            allowSameIdInSelectedFiltered = false,
            currentFilters = filters,
            recordTypes = getTypesCache(),
            recordTypeCategories = getRecordTypeCategoriesCache(),
            recordTags = getTagsCache(),
            typesToTags = getRecordTypeToTagCache(),
        )
    }

    private fun handleUntrackedClick() {
        filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Untracked)
        filters = recordsFilterUpdateInteractor.handleUntrackedClick(
            currentFilters = filters,
        )
    }

    private fun handleMultitaskClick() {
        filters = recordsFilterUpdateInteractor.handleMultitaskClick(
            currentFilters = filters,
        )
    }

    private fun handleArchivedTypeClick() {
        isArchivedTypesShown = !isArchivedTypesShown
    }

    private fun handleArchivedTagClick() {
        isArchivedTagsShown = !isArchivedTagsShown
    }

    private fun handleCommentFilterClick(item: FilterViewData) {
        filters = recordsFilterUpdateInteractor.handleCommentFilterClick(
            currentFilters = filters,
            itemType = item.type,
        )
        updateViewDataOnFiltersChanged()
    }

    private fun handleDuplicationsFilterClick(item: FilterViewData) {
        filters = recordsFilterUpdateInteractor.handleDuplicationsFilterClick(
            currentFilters = filters,
            itemType = item.type,
        )
        updateViewDataOnFiltersChanged()
    }

    private fun handleTagSelectionTypeClick(type: RecordFilterSelectionType) {
        tagSelectionState = type
        updateFilterSelectionViewData()
    }

    private fun handleActivitiesSelectionClick(type: RecordFilterActivitiesType) {
        activitiesSelectionState = type
        updateFilterSelectionViewData()
    }

    private fun handleCommentChange(text: String) {
        filters = recordsFilterUpdateInteractor.handleCommentChange(
            currentFilters = filters,
            text = text,
        )
    }

    private fun handleTagClick(item: CategoryViewData.Record) {
        filters = recordsFilterUpdateInteractor.handleTagClick(
            type = tagSelectionState,
            currentFilters = filters,
            itemId = item.id,
            allowSameIdInSelectedFiltered = false,
        )
    }

    private fun handleRecordClick(viewData: ViewHolderType) {
        filters = recordsFilterUpdateInteractor.handleRecordClick(
            currentFilters = filters,
            viewData = viewData,
        )
        checkManualFilterVisibility()
        updateViewDataOnFiltersChanged(showLoader = false)
    }

    private fun removeFilter(type: RecordFilterType) {
        filters = recordsFilterUpdateInteractor.removeFilter(
            currentFilters = filters,
            type = type,
        )

        // Switch back to activity if category removed.
        val currentSelectionType = (filterSelectionState as? RecordsFilterSelectionState.Visible)?.type
        if (currentSelectionType == RecordFilterType.Category) {
            filterSelectionState = RecordsFilterSelectionState.Visible(RecordFilterType.Activity)
        }
        checkManualFilterVisibility()
        updateViewDataOnFiltersChanged()
    }

    private fun handleInvertSelection() {
        filters = recordsFilterUpdateInteractor.handleInvertSelection(
            currentFilters = filters,
            recordsViewData = recordsViewData.value,
        )
        checkManualFilterVisibility()
        updateViewDataOnFiltersChanged()
    }

    private fun handleFilterDuplicates() {
        filterDuplicationsJob?.cancel()
        filterDuplicationsJob = viewModelScope.launch {
            filters = recordsFilterUpdateInteractor.handleFilterDuplicates(
                currentFilters = filters,
                recordsViewData = recordsViewData.value,
            )
            checkManualFilterVisibility()
            updateViewDataOnFiltersChanged()
        }
    }

    private fun handleSaveFavourite() = viewModelScope.launch {
        // Remove unavailable but present filters (mainly Date from statistics detail).
        val availableFilters = viewDataInteractor.filterAvailableFilters(extra, filters)
        if (availableFilters.isEmpty()) return@launch
        val data = FavouriteRecordsFilter(
            id = 0L, // Creates new record,
            filter = availableFilters,
        )
        favouriteRecordsFilterInteractor.add(data)
        updateFilterSelectionViewData()
    }

    private fun handleDayOfWeekClick(dayOfWeek: DayOfWeek) {
        filters = recordsFilterUpdateInteractor.handleDayOfWeekClick(
            currentFilters = filters,
            dayOfWeek = dayOfWeek,
        )
    }

    private fun checkManualFilterVisibility() {
        if (
            !filters.hasManuallyFiltered() &&
            filterSelectionState.type == RecordFilterType.ManuallyFiltered
        ) {
            filterSelectionState = RecordsFilterSelectionState.Hidden
            updateFilterSelectionVisibility()
        }
    }

    private suspend fun handleDateFieldClick(fieldType: RecordsFilterRangeViewData.FieldType) {
        val range = getCurrentRange()

        when (fieldType) {
            RecordsFilterRangeViewData.FieldType.TIME_STARTED -> {
                viewDataInteractor.getDateTimeDialogParams(
                    tag = TIME_STARTED_TAG,
                    timestamp = range.timeStarted,
                )
            }
            RecordsFilterRangeViewData.FieldType.TIME_ENDED -> {
                viewDataInteractor.getDateTimeDialogParams(
                    tag = TIME_ENDED_TAG,
                    timestamp = range.timeEnded,
                )
            }
        }.let(router::navigate)
    }

    private fun handleDurationFieldClick(fieldType: RecordsFilterRangeViewData.FieldType) {
        val range = filters.getDuration() ?: defaultDurationRange

        when (fieldType) {
            RecordsFilterRangeViewData.FieldType.TIME_STARTED -> DurationDialogParams(
                tag = DURATION_FROM_TAG,
                value = DurationDialogParams.Value.DurationSeconds(
                    duration = range.timeStarted / 1000,
                ),
                hideDisableButton = true,
            )
            RecordsFilterRangeViewData.FieldType.TIME_ENDED -> DurationDialogParams(
                tag = DURATION_TO_TAG,
                value = DurationDialogParams.Value.DurationSeconds(
                    duration = range.timeEnded / 1000,
                ),
                hideDisableButton = true,
            )
        }.let(router::navigate)
    }

    private suspend fun handleTimeOfDayFieldClick(fieldType: RecordsFilterRangeViewData.FieldType) {
        val range = filters.getTimeOfDay() ?: defaultTimeOfDayRange
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val startOfDay = timeMapper.getStartOfDayTimeStamp()

        when (fieldType) {
            RecordsFilterRangeViewData.FieldType.TIME_STARTED -> DateTimeDialogParams(
                tag = TIME_OF_DAY_FROM_TAG,
                type = DateTimeDialogType.TIME,
                timestamp = startOfDay + range.timeStarted,
                useMilitaryTime = useMilitaryTime,
            )
            RecordsFilterRangeViewData.FieldType.TIME_ENDED -> DateTimeDialogParams(
                tag = TIME_OF_DAY_TO_TAG,
                type = DateTimeDialogType.TIME,
                timestamp = startOfDay + range.timeEnded,
                useMilitaryTime = useMilitaryTime,
            )
        }.let(router::navigate)
    }

    private suspend fun handleDateSet(timestamp: Long, tag: String?) {
        var (rangeStart, rangeEnd) = getCurrentRange()

        when (tag) {
            TIME_STARTED_TAG -> {
                if (timestamp != rangeStart) {
                    rangeStart = timestamp
                    if (timestamp > rangeEnd) rangeEnd = timestamp
                }
            }
            TIME_ENDED_TAG -> {
                if (timestamp != rangeEnd) {
                    rangeEnd = timestamp
                    if (timestamp < rangeStart) rangeStart = timestamp
                }
            }
        }

        filters = recordsFilterUpdateInteractor.handleDateSet(
            currentFilters = filters,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )

        updateViewDataOnFiltersChanged()
    }

    private fun handleTimeOfDaySet(timestamp: Long, tag: String?) {
        var (rangeStart, rangeEnd) = filters.getTimeOfDay() ?: defaultTimeOfDayRange
        val startOfDay = timeMapper.getStartOfDayTimeStamp()
        val normalizedTimeStamp = (timestamp - startOfDay)
            .coerceIn(0..TimeUnit.DAYS.toMillis(1))

        when (tag) {
            TIME_OF_DAY_FROM_TAG -> rangeStart = normalizedTimeStamp
            TIME_OF_DAY_TO_TAG -> rangeEnd = normalizedTimeStamp
        }

        filters = recordsFilterUpdateInteractor.handleTimeOfDaySet(
            currentFilters = filters,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )

        updateViewDataOnFiltersChanged()
    }

    private fun updateViewDataOnFiltersChanged(
        showLoader: Boolean = true,
    ) {
        updateFilters()
        updateFilterSelectionViewData()
        updateRecords(showLoader)
    }

    private suspend fun getCurrentRange(): Range {
        val filter = filters.getDate() ?: return defaultRange

        return if (filter.range is RangeLength.All) {
            Range(0, System.currentTimeMillis())
        } else {
            recordFilterInteractor.getRange(filter)
        }
    }

    private fun getRecordsLoadState(
        showLoader: Boolean,
    ): RecordsFilterSelectedRecordsViewData {
        val loadingViewData = RecordsFilterSelectedRecordsViewData.RecordsViewData.Loading(
            viewData = listOf(LoaderViewData()),
        )

        return RecordsFilterSelectedRecordsViewData(
            isLoading = true,
            selectedRecordsCount = "",
            showListButtonIsVisible = false,
            recordsViewData = if (showLoader) {
                loadingViewData
            } else {
                recordsViewData.value?.recordsViewData ?: loadingViewData
            },
        )
    }

    private suspend fun getTypesCache(): List<RecordType> {
        return types.takeUnless { it.isEmpty() }
            ?: run { recordTypeInteractor.getAll().also { types = it } }
    }

    private suspend fun getCategoriesCache(): List<Category> {
        return categories ?: run {
            categoryInteractor.getAll().also { categories = it }
        }
    }

    private suspend fun getRecordTypeCategoriesCache(): List<RecordTypeCategory> {
        return recordTypeCategories ?: run {
            recordTypeCategoryInteractor.getAll().also { recordTypeCategories = it }
        }
    }

    private suspend fun getTagsCache(): List<RecordTag> {
        return recordTags ?: run {
            recordTagInteractor.getAll().also { recordTags = it }
        }
    }

    private suspend fun getRecordTypeToTagCache(): List<RecordTypeToTag> {
        return recordTypeToTag ?: run {
            recordTypeToTagInteractor.getAll().also { recordTypeToTag = it }
        }
    }

    private suspend fun getGoalsCache(): List<RecordTypeGoal> {
        return goals ?: run {
            recordTypeGoalInteractor.getAllTypeGoals().also { goals = it }
        }
    }

    private fun updateFilterSelectionVisibility() {
        val data = loadFilterSelectionVisibility()
        filterSelectionVisibility.set(data)
    }

    private fun loadFilterSelectionVisibility(): Boolean {
        return filterSelectionState is RecordsFilterSelectionState.Visible
    }

    private fun updateFilters() {
        filtersLoadJob?.cancel()
        filtersLoadJob = viewModelScope.launch {
            val data = loadFiltersViewData()
            filtersViewData.set(data)
        }
    }

    private suspend fun loadFiltersViewData(): List<ViewHolderType> {
        changedFilters.set(
            RecordsFilterResultParams(
                tag = extra.tag,
                filters = filters,
            ),
        )
        return viewDataInteractor.getFiltersViewData(
            extra = extra,
            selectionState = filterSelectionState,
            filters = filters,
        )
    }

    private fun updateRecords(
        showLoader: Boolean,
    ) {
        recordsLoadJob?.cancel()
        recordsLoadJob = viewModelScope.launch {
            recordsViewData.set(getRecordsLoadState(showLoader))
            val data = loadRecordsViewData()
            recordsViewData.set(data)
        }
    }

    private suspend fun loadRecordsViewData(): RecordsFilterSelectedRecordsViewData {
        return viewDataInteractor.getRecordsViewData(
            extra = extra,
            filters = filters,
            recordTypes = getTypesCache().associateBy(RecordType::id),
            recordTags = getTagsCache(),
            goals = getGoalsCache().groupBy { it.idData.value },
        )
    }

    private fun updateFilterSelectionViewData() {
        if (filterSelectionState is RecordsFilterSelectionState.Hidden) return
        filtersSelectionLoadJob?.cancel()
        filtersSelectionLoadJob = viewModelScope.launch {
            val data = loadFilterSelectionViewData()
            filterSelectionContent.set(data)
        }
    }

    private suspend fun loadFilterSelectionViewData(): List<ViewHolderType> {
        val type = filterSelectionState.type ?: return emptyList()

        return when (type) {
            RecordFilterType.Untracked,
            RecordFilterType.Multitask,
            RecordFilterType.Activity,
            RecordFilterType.Category,
            -> {
                viewDataInteractor.getActivityFilterSelectionViewData(
                    type = activitiesSelectionState,
                    extra = extra,
                    filters = filters,
                    isArchivedShown = isArchivedTypesShown,
                    types = getTypesCache(),
                    recordTypeCategories = getRecordTypeCategoriesCache(),
                    categories = getCategoriesCache(),
                )
            }
            RecordFilterType.Comment -> {
                viewDataInteractor.getCommentFilterSelectionViewData(
                    filters = filters,
                )
            }
            RecordFilterType.Tags -> {
                viewDataInteractor.getTagsFilterSelectionViewData(
                    type = tagSelectionState,
                    filters = filters,
                    isArchivedShown = isArchivedTagsShown,
                    types = getTypesCache(),
                    recordTypeCategories = getRecordTypeCategoriesCache(),
                    recordTags = getTagsCache(),
                    recordTypesToTags = getRecordTypeToTagCache(),
                )
            }
            RecordFilterType.Date -> {
                viewDataInteractor.getDateFilterSelectionViewData(
                    filters = filters,
                    currentRange = getCurrentRange(),
                    extra = extra,
                )
            }
            RecordFilterType.ManuallyFiltered -> {
                viewDataInteractor.getManualFilterSelectionViewData(
                    filters = filters,
                    recordTypes = getTypesCache().associateBy(RecordType::id),
                    recordTags = getTagsCache(),
                    goals = getGoalsCache().groupBy { it.idData.value },
                )
            }
            RecordFilterType.DaysOfWeek -> {
                viewDataInteractor.getDaysOfWeekFilterSelectionViewData(
                    filters = filters,
                )
            }
            RecordFilterType.TimeOfDay -> {
                viewDataInteractor.getTimeOfDayFilterSelectionViewData(
                    filters = filters,
                    defaultRange = defaultTimeOfDayRange,
                )
            }
            RecordFilterType.Duration -> {
                viewDataInteractor.getDurationFilterSelectionViewData(
                    filters = filters,
                    defaultRange = defaultDurationRange,
                )
            }
            RecordFilterType.Duplications -> {
                viewDataInteractor.getDuplicationsFilterSelectionViewData(
                    filters = filters,
                )
            }
            RecordFilterType.Favourite -> {
                viewDataInteractor.getFavouriteFiltersSelectionViewData(
                    filters = filters,
                    extra = extra,
                    isDeleteEnabled = isDeleteFavouriteEnabled,
                    recordTypes = getTypesCache(),
                    categories = getCategoriesCache(),
                    recordTags = getTagsCache(),
                )
            }
        }
    }

    @Parcelize
    private data class DeleteFavouriteFilterData(
        val id: Long,
    ) : Parcelable

    companion object {
        private const val TIME_STARTED_TAG = "records_filter_range_selection_time_started_tag"
        private const val TIME_ENDED_TAG = "records_filter_range_selection_time_ended_tag"
        private const val DURATION_FROM_TAG = "records_filter_duration_selection_from_tag"
        private const val DURATION_TO_TAG = "records_filter_duration_selection_to_tag"
        private const val TIME_OF_DAY_FROM_TAG = "records_filter_time_of_day_selection_from_tag"
        private const val TIME_OF_DAY_TO_TAG = "records_filter_time_of_day_selection_to_tag"
        private const val DELETE_FAVOURITE_DIALOG_TAG = "records_filter_delete_alert_dialog_tag"
    }
}
