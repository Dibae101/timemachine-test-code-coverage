package com.example.util.simpletimetracker.feature_dialogs.chartFilter.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.interactor.ChartFilterViewDataInteractor
import com.example.util.simpletimetracker.core.mapper.ChartFilterViewDataMapper
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.core.viewData.ChartFilterTypeViewData
import com.example.util.simpletimetracker.domain.base.ARCHIVED_BUTTON_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordType.RecordTypeViewData
import com.example.util.simpletimetracker.feature_dialogs.chartFilter.model.ChartFilterDataSelectionResult
import com.example.util.simpletimetracker.navigation.params.screen.ChartFilterDialogParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartFilterViewModel @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val chartFilterViewDataMapper: ChartFilterViewDataMapper,
    private val chartFilterViewDataInteractor: ChartFilterViewDataInteractor,
) : ViewModel() {

    lateinit var extra: ChartFilterDialogParams

    val filterTypeViewData: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initializeChartFilterType()
                initial.value = loadFilterTypeViewData()
            }
            initial
        }
    }
    val types: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initializeChartFilterType()
                initial.value = listOf(LoaderViewData())
                initial.value = loadTypesViewData()
            }
            initial
        }
    }
    val onDataSelected: LiveData<ChartFilterDataSelectionResult> = MutableLiveData()

    private var filterType: ChartFilterType = ChartFilterType.ACTIVITY
    private var isArchivedShown: Boolean = false

    // Cache
    private var recordTypesCache: List<RecordType>? = null
    private var categoriesCache: List<Category>? = null
    private var recordTagsCache: List<RecordTag>? = null

    // Filtered ids
    private var typeIdsFiltered: List<Long> = mutableListOf()
    private var categoryIdsFiltered: List<Long> = mutableListOf()
    private var recordTagIdsFiltered: List<Long> = mutableListOf()

    fun onFilterTypeClick(viewData: ButtonsRowViewData) {
        viewModelScope.launch {
            if (viewData !is ChartFilterTypeViewData) return@launch
            filterType = viewData.filterType
            updateFilterTypeViewData()
            updateTypesViewData()
            sendResult()
        }
    }

    fun onRecordTypeClick(item: RecordTypeViewData) = viewModelScope.launch {
        if (item.id == ARCHIVED_BUTTON_ITEM_ID) {
            isArchivedShown = !isArchivedShown
        } else {
            typeIdsFiltered = typeIdsFiltered.toMutableList().apply { addOrRemove(item.id) }
        }
        sendResult()
        updateRecordTypesViewData()
    }

    fun onCategoryClick(item: CategoryViewData) = viewModelScope.launch {
        when (item) {
            is CategoryViewData.Category -> {
                categoryIdsFiltered = categoryIdsFiltered.toMutableList()
                    .apply { addOrRemove(item.id) }
            }
            is CategoryViewData.Record -> {
                if (item.id == ARCHIVED_BUTTON_ITEM_ID) {
                    isArchivedShown = !isArchivedShown
                } else {
                    recordTagIdsFiltered = recordTagIdsFiltered.toMutableList()
                        .apply { addOrRemove(item.id) }
                }
            }
        }
        sendResult()
        updateTypesViewData()
    }

    fun onShowAllClick() = viewModelScope.launch {
        when (filterType) {
            ChartFilterType.ACTIVITY -> typeIdsFiltered = emptyList()
            ChartFilterType.CATEGORY -> categoryIdsFiltered = emptyList()
            ChartFilterType.RECORD_TAG -> recordTagIdsFiltered = emptyList()
        }
        sendResult()
        updateTypesViewData()
    }

    fun onHideAllClick() = viewModelScope.launch {
        when (filterType) {
            ChartFilterType.ACTIVITY -> {
                typeIdsFiltered = getTypesCache().map(RecordType::id) +
                    UNTRACKED_ITEM_ID
            }
            ChartFilterType.CATEGORY -> {
                categoryIdsFiltered = getCategoriesCache().map(Category::id) +
                    UNTRACKED_ITEM_ID +
                    UNCATEGORIZED_ITEM_ID
            }
            ChartFilterType.RECORD_TAG -> {
                recordTagIdsFiltered = getTagsCache().map(RecordTag::id) +
                    UNTRACKED_ITEM_ID +
                    UNCATEGORIZED_ITEM_ID
            }
        }
        sendResult()
        updateTypesViewData()
    }

    private fun sendResult() {
        val result = ChartFilterDataSelectionResult(
            chartFilterType = filterType,
            dataIds = when (filterType) {
                ChartFilterType.ACTIVITY -> typeIdsFiltered
                ChartFilterType.CATEGORY -> categoryIdsFiltered
                ChartFilterType.RECORD_TAG -> recordTagIdsFiltered
            },
        )
        onDataSelected.set(result)
    }

    private fun initializeChartFilterType() {
        filterType = extra.chartFilterType
        typeIdsFiltered = extra.filteredTypeIds
        categoryIdsFiltered = extra.filteredCategoryIds
        recordTagIdsFiltered = extra.filteredTagIds
    }

    private suspend fun getTypesCache(): List<RecordType> {
        return recordTypesCache ?: run {
            recordTypeInteractor.getAll().also { recordTypesCache = it }
        }
    }

    private suspend fun getCategoriesCache(): List<Category> {
        return categoriesCache ?: run {
            categoryInteractor.getAll().also { categoriesCache = it }
        }
    }

    private suspend fun getTagsCache(): List<RecordTag> {
        return recordTagsCache ?: run {
            recordTagInteractor.getAll().also { recordTagsCache = it }
        }
    }

    private fun updateFilterTypeViewData() {
        val data = loadFilterTypeViewData()
        filterTypeViewData.set(data)
    }

    private fun loadFilterTypeViewData(): List<ViewHolderType> {
        return chartFilterViewDataMapper.mapToFilterTypeViewData(filterType)
    }

    private fun updateTypesViewData() {
        when (filterType) {
            ChartFilterType.ACTIVITY -> updateRecordTypesViewData()
            ChartFilterType.CATEGORY -> updateCategoriesViewData()
            ChartFilterType.RECORD_TAG -> updateTagsViewData()
        }
    }

    private suspend fun loadTypesViewData(): List<ViewHolderType> {
        return when (filterType) {
            ChartFilterType.ACTIVITY -> loadRecordTypesViewData()
            ChartFilterType.CATEGORY -> loadCategoriesViewData()
            ChartFilterType.RECORD_TAG -> loadTagsViewData()
        }
    }

    private fun updateRecordTypesViewData() = viewModelScope.launch {
        val data = loadRecordTypesViewData()
        types.set(data)
    }

    private suspend fun loadRecordTypesViewData(): List<ViewHolderType> {
        return chartFilterViewDataInteractor.loadRecordTypesViewData(
            types = getTypesCache(),
            typeIdsFiltered = typeIdsFiltered,
            isArchivedShown = isArchivedShown,
        )
    }

    private fun updateCategoriesViewData() = viewModelScope.launch {
        val data = loadCategoriesViewData()
        types.set(data)
    }

    private suspend fun loadCategoriesViewData(): List<ViewHolderType> {
        return chartFilterViewDataInteractor.loadCategoriesViewData(
            categories = getCategoriesCache(),
            categoryIdsFiltered = categoryIdsFiltered,
        )
    }

    private fun updateTagsViewData() = viewModelScope.launch {
        val data = loadTagsViewData()
        types.set(data)
    }

    private suspend fun loadTagsViewData(): List<ViewHolderType> {
        return chartFilterViewDataInteractor.loadTagsViewData(
            tags = getTagsCache(),
            types = getTypesCache(),
            recordTagsFiltered = recordTagIdsFiltered,
            isArchivedShown = isArchivedShown,
        )
    }
}
