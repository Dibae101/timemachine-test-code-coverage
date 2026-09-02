package com.example.util.simpletimetracker.feature_dialogs.typesSelection.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.recordTag.interactor.NeedTagValueSelectionInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTagValueType
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordType.RecordTypeViewData
import com.example.util.simpletimetracker.feature_dialogs.typesSelection.interactor.TypesSelectionViewDataInteractor
import com.example.util.simpletimetracker.feature_dialogs.typesSelection.model.TypesSelectionCacheHolder
import com.example.util.simpletimetracker.feature_dialogs.typesSelection.model.TypesSelectionResult
import com.example.util.simpletimetracker.resources.R
import com.example.util.simpletimetracker.feature_dialogs.typesSelection.model.TagValueModeSelectionData
import com.example.util.simpletimetracker.feature_dialogs.typesSelection.viewData.TypesSelectionDialogViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagValueSelectionParams
import com.example.util.simpletimetracker.navigation.params.screen.TypesSelectionDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.StandardDialogParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TypesSelectionViewModel @Inject constructor(
    private val router: Router,
    private val resourceRepo: ResourceRepo,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val typesSelectionViewDataInteractor: TypesSelectionViewDataInteractor,
    private val needTagValueSelectionInteractor: NeedTagValueSelectionInteractor,
) : ViewModel() {

    lateinit var extra: TypesSelectionDialogParams

    val viewData: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initial.value = listOf(LoaderViewData())
                initial.value = loadViewData()
            }
            initial
        }
    }
    val viewState: LiveData<TypesSelectionDialogViewData> by lazy {
        MutableLiveData(loadViewState())
    }
    val onDataSelected: LiveData<TypesSelectionResult> = MutableLiveData()
    val saveButtonEnabled: LiveData<Boolean> = MutableLiveData(true)

    private var initialized: Boolean = false
    private var viewDataCache: List<TypesSelectionCacheHolder> = emptyList()

    private var dataIdsSelected: List<Long> = emptyList()
    private var tagValuesSelected: List<RecordBase.Tag> = emptyList()
    private var tagValueOnStartIds: List<Long> = emptyList()

    fun onRecordTypeClick(item: RecordTypeViewData) {
        if (extra.isMultiSelectAvailable) {
            dataIdsSelected = dataIdsSelected.addOrRemove(item.id)
            updateViewData()
        } else {
            val result = TypesSelectionResult(
                dataIds = listOf(item.id),
                tagValues = emptyList(),
                selectValueOnStartTagIds = tagValueOnStartIds.toList(),
            )
            onDataSelected.set(result)
        }
    }

    fun onCategoryClick(item: CategoryViewData) {
        val clickedTag = viewDataCache
            .firstOrNull { it.id == item.id }
            as? TypesSelectionCacheHolder.Tag
        val clickedTagData = clickedTag?.data
        val isSelected = item.id in dataIdsSelected

        if (!isSelected && shouldShowTagValueModeChooser(clickedTagData)) {
            openTagValueModeChooser(item.id)
            return
        }

        val needValueSelection = extra.allowTagValueSelection &&
            needTagValueSelectionInteractor.execute(
                selectedTagIds = dataIdsSelected,
                clickedTag = clickedTagData,
            )

        if (needValueSelection) {
            requestTagValueSelection(item.id)
        } else if (extra.isMultiSelectAvailable) {
            dataIdsSelected = dataIdsSelected.addOrRemove(item.id)
            tagValuesSelected = tagValuesSelected.filter { it.tagId in dataIdsSelected }
            tagValueOnStartIds = tagValueOnStartIds.filter { it in dataIdsSelected }
            updateViewData()
        } else {
            val result = TypesSelectionResult(
                dataIds = listOf(item.id),
                tagValues = emptyList(),
                selectValueOnStartTagIds = tagValueOnStartIds,
            )
            onDataSelected.set(result)
        }
    }

    fun onCategoryValueSelected(
        params: RecordTagValueSelectionParams,
        value: Double,
    ) {
        if (params.tag != CHANGE_RECORD_TAG_VALUE_SELECTION) return
        val id = params.tagId
        val tag = RecordBase.Tag(tagId = id, numericValue = value)
        viewModelScope.launch {
            if (extra.isMultiSelectAvailable) {
                dataIdsSelected = dataIdsSelected
                    .toMutableList().apply { add(id) }
                tagValuesSelected = tagValuesSelected
                    .filter { it.tagId != id }
                    .toMutableList().apply { add(tag) }
                updateViewData()
            } else {
                val result = TypesSelectionResult(
                    dataIds = listOf(id),
                    tagValues = listOf(tag),
                    selectValueOnStartTagIds = tagValueOnStartIds,
                )
                onDataSelected.set(result)
            }
        }
    }

    fun onPositiveClick(tag: String?, data: Any?) {
        if (tag != TAG_VALUE_MODE_DIALOG) return
        onTagValueModeSelected(data, setNow = true)
    }

    fun onNegativeClick(tag: String?, data: Any?) {
        if (tag != TAG_VALUE_MODE_DIALOG) return
        onTagValueModeSelected(data, setNow = false)
    }

    fun onShowAllClick() {
        dataIdsSelected = viewDataCache.map(TypesSelectionCacheHolder::id)
        updateViewData()
    }

    fun onHideAllClick() {
        dataIdsSelected = emptyList()
        tagValuesSelected = emptyList()
        tagValueOnStartIds = emptyList()
        updateViewData()
    }

    private fun onTagValueModeSelected(data: Any?, setNow: Boolean) = viewModelScope.launch {
        val tagId = (data as? TagValueModeSelectionData)?.tagId ?: return@launch

        if (setNow) {
            // Wait for dialog to close.
            delay(300)
            requestTagValueSelection(tagId)
        } else if (extra.isMultiSelectAvailable) {
            dataIdsSelected = dataIdsSelected.toMutableList().apply { add(tagId) }
            tagValueOnStartIds = tagValueOnStartIds + tagId
            updateViewData()
        } else {
            val result = TypesSelectionResult(
                dataIds = listOf(tagId),
                tagValues = emptyList(),
                selectValueOnStartTagIds = listOf(tagId),
            )
            onDataSelected.set(result)
        }
    }

    private fun requestTagValueSelection(tagId: Long) {
        RecordTagValueSelectionParams(
            tag = CHANGE_RECORD_TAG_VALUE_SELECTION,
            tagId = tagId,
        ).let(router::navigate)
    }

    private fun openTagValueModeChooser(tagId: Long) {
        StandardDialogParams(
            tag = TAG_VALUE_MODE_DIALOG,
            data = TagValueModeSelectionData(tagId = tagId),
            message = resourceRepo.getString(R.string.change_complex_tag_value_dialog_message),
            btnPositive = resourceRepo.getString(R.string.time_now),
            btnNegative = resourceRepo.getString(R.string.change_complex_tag_value_set_later),
        ).let(router::navigate)
    }

    private fun shouldShowTagValueModeChooser(clickedTag: RecordTag?): Boolean {
        return extra.allowSelectTagValueOnStart &&
            clickedTag?.valueType == RecordTagValueType.NUMERIC
    }

    fun onSaveClick() {
        saveButtonEnabled.set(false)
        viewModelScope.launch {
            val result = TypesSelectionResult(
                dataIds = dataIdsSelected,
                tagValues = tagValuesSelected,
                selectValueOnStartTagIds = tagValueOnStartIds.toList(),
            )
            onDataSelected.set(result)
        }
    }

    private fun loadViewState(): TypesSelectionDialogViewData {
        return TypesSelectionDialogViewData(
            title = extra.title,
            subtitle = extra.subtitle,
            isButtonsVisible = extra.isMultiSelectAvailable,
        )
    }

    private fun updateViewData() = viewModelScope.launch {
        val data = loadViewData()
        viewData.set(data)
    }

    private suspend fun loadViewData(): List<ViewHolderType> {
        val types = recordTypeInteractor.getAll()

        if (!initialized) {
            viewDataCache = typesSelectionViewDataInteractor.loadCache(
                extra = extra,
                types = types,
            )
            val viewDataIds = viewDataCache.map { it.id }
            dataIdsSelected = extra
                .selectedTypeIds
                // Remove non existent ids.
                .filter { it in viewDataIds }
            tagValuesSelected = extra
                .selectedTagValues
                .filter { it.tagId in dataIdsSelected }
                .map {
                    RecordBase.Tag(
                        tagId = it.tagId,
                        numericValue = it.numericValue,
                    )
                }
            tagValueOnStartIds = extra
                .selectedTagValueOnStart
                .filter { it in dataIdsSelected }
            initialized = true
        }

        return typesSelectionViewDataInteractor.getViewData(
            extra = extra,
            types = types,
            dataIdsSelected = dataIdsSelected,
            tagValuesSelected = tagValuesSelected,
            tagValueOnStartIds = tagValueOnStartIds,
            viewDataCache = viewDataCache,
        )
    }

    companion object {
        private const val CHANGE_RECORD_TAG_VALUE_SELECTION = "TYPES_SELECTION_TAG_VALUE_SELECTION"
        private const val TAG_VALUE_MODE_DIALOG = "TYPES_SELECTION_TAG_VALUE_MODE_DIALOG"
    }
}
