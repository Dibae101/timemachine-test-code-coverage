package com.example.util.simpletimetracker.feature_data_edit.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.interactor.RecordTagViewDataInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.interactor.NeedTagValueSelectionInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.DataEditTagSelectionDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagValueSelectionParams
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DataEditTagSelectionViewModel @Inject constructor(
    private val router: Router,
    private val recordTagViewDataInteractor: RecordTagViewDataInteractor,
    private val needTagValueSelectionInteractor: NeedTagValueSelectionInteractor,
) : ViewModel() {

    lateinit var extra: DataEditTagSelectionDialogParams

    val viewData: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initial.value = listOf(LoaderViewData())
                initial.value = loadViewData()
            }
            initial
        }
    }
    val tagSelected: LiveData<List<RecordBase.Tag>> = MutableLiveData()

    private var selectedIds: List<RecordBase.Tag> = emptyList()

    fun onTagClick(item: CategoryViewData) {
        viewModelScope.launch {
            when (item) {
                is CategoryViewData.Record.Tagged -> {
                    val needValueSelection = if (extra.showValueSelection) {
                        needTagValueSelectionInteractor.execute(
                            selectedTagIds = selectedIds.map { it.tagId },
                            clickedTagId = item.id,
                        )
                    } else {
                        false
                    }
                    if (needValueSelection) {
                        RecordTagValueSelectionParams(
                            tag = DATA_EDIT_TAG_VALUE_SELECTION,
                            tagId = item.id,
                        ).let(router::navigate)
                    } else {
                        selectedIds = selectedIds.addOrRemove(item.id)
                    }
                }
                is CategoryViewData.Record.Untagged -> {
                    selectedIds = emptyList()
                }
                else -> return@launch
            }
            updateViewData()
        }
    }

    fun onTagValueSelected(
        params: RecordTagValueSelectionParams,
        value: Double,
    ) {
        if (params.tag != DATA_EDIT_TAG_VALUE_SELECTION) return
        selectedIds = selectedIds + RecordBase.Tag(
            tagId = params.tagId,
            numericValue = value,
        )
        updateViewData()
    }

    fun onSaveClick() {
        tagSelected.set(selectedIds)
    }

    private fun updateViewData() = viewModelScope.launch {
        val data = loadViewData()
        viewData.set(data)
    }

    private suspend fun loadViewData(): List<ViewHolderType> {
        val result: MutableList<ViewHolderType> = mutableListOf()

        recordTagViewDataInteractor.getViewData(
            selectedTags = selectedIds,
            typeIds = extra.typeIds,
            multipleChoiceAvailable = true,
            showBigEmptyHint = false,
            showHint = false,
            showArchived = true,
            searchText = "",
            fromSearchChange = false,
            buttons = emptyList(),
        ).data.let(result::addAll)

        return result
    }

    companion object {
        private const val DATA_EDIT_TAG_VALUE_SELECTION = "DATA_EDIT_TAG_VALUE_SELECTION"
    }
}