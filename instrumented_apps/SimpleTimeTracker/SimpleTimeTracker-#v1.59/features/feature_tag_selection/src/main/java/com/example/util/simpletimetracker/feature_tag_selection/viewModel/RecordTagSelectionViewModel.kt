package com.example.util.simpletimetracker.feature_tag_selection.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.extension.toModel
import com.example.util.simpletimetracker.core.interactor.IsMultipleTagChoiceAvailableInteractor
import com.example.util.simpletimetracker.core.interactor.RecordCommentSearchViewDataInteractor
import com.example.util.simpletimetracker.core.interactor.RecordTagViewDataInteractor
import com.example.util.simpletimetracker.core.viewData.CommentFilterTypeViewData
import com.example.util.simpletimetracker.domain.extension.addOrRemove
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.interactor.AddTagToTypeIfNotExistMediator
import com.example.util.simpletimetracker.domain.recordTag.interactor.NeedTagValueSelectionInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryAddViewData
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordComment.RecordCommentViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.FilterViewData
import com.example.util.simpletimetracker.feature_tag_selection.interactor.RecordTagSelectionViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagParam
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagSelectionParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagValueSelectionParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// No tag selection dialog:
// - start right away with preselected tags
// Tag selection dialog:
// - show preselected
// - can deselect preselected
// - multi choice
// - value selection is added to preselected tags
// - value selection is queued
// Close after one (no preselected):
// - close after one selected
// - no multi choice
// - value selection queue is not possible because they are added to preselected
// Close after one (have preselected):
// - close after one not working (?)
// - show preselected (?)
// - can deselect preselected (?)
// - multi choice is forced
// - value selection is added to preselected tags
// - value selection is queued (?)
@HiltViewModel
class RecordTagSelectionViewModel @Inject constructor(
    private val router: Router,
    private val viewDataInteractor: RecordTagSelectionViewDataInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val prefsInteractor: PrefsInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val addTagToTypeIfNotExistMediator: AddTagToTypeIfNotExistMediator,
    private val needTagValueSelectionInteractor: NeedTagValueSelectionInteractor,
    private val isMultipleTagChoiceAvailableInteractor: IsMultipleTagChoiceAvailableInteractor,
    private val recordCommentSearchViewDataInteractor: RecordCommentSearchViewDataInteractor,
) : BaseViewModel() {

    lateinit var extra: RecordTagSelectionParams
    var softInputMode: Int? = null

    val viewData: LiveData<List<ViewHolderType>> by lazy {
        return@lazy MutableLiveData<List<ViewHolderType>>().let { initial ->
            viewModelScope.launch {
                initial.value = listOf(LoaderViewData())
                initial.value = loadViewData(
                    fromCommentChange = false,
                    fromSearchChange = false,
                )
            }
            initial
        }
    }
    val saveButtonVisibility: LiveData<Boolean> = MutableLiveData()
    val saveClicked: LiveData<Unit> = MutableLiveData()

    private var newComment: String = ""
    private var newTags: List<RecordBase.Tag> = emptyList()
    private var initialDataLoaded: Boolean = false
    private var searchLoadJob: Job? = null
    private var isMultipleChoiceAvailable: Boolean = true
    private var tagSearchText: String = ""

    fun onCategoryClick(item: CategoryViewData) {
        viewModelScope.launch {
            when (item) {
                is CategoryViewData.Record.Tagged -> {
                    val wasSelectedBefore = newTags.any { it.tagId == item.id }
                    val needValueSelection = needTagValueSelectionInteractor.execute(
                        selectedTagIds = newTags.map { it.tagId },
                        clickedTagId = item.id,
                    )
                    if (needValueSelection) {
                        requestTagValueSelection(item.id, item.name)
                    } else if (wasSelectedBefore &&
                        !isMultipleChoiceAvailable &&
                        extra.preselectedTags.any { it.tagId == item.id }
                    ) {
                        // Disallow deselection for preselected tags.
                    } else {
                        newTags = newTags.addOrRemove(item.id)
                        onTagSelected()
                    }
                }
                is CategoryViewData.Record.Untagged -> {
                    newTags = emptyList()
                    onTagSelected()
                }
                else -> return@launch
            }
        }
    }

    fun onCategoryValueSelected(
        params: RecordTagValueSelectionParams,
        value: Double,
    ) {
        if (params.tag != RECORD_TAG_SELECTION_TAG_VALUE_SELECTION) return
        viewModelScope.launch {
            newTags = newTags.filter { it.tagId != params.tagId } +
                RecordBase.Tag(tagId = params.tagId, numericValue = value)
            startRequiredTagValueSelectionIfNeeded()
            if (params.tagId in extra.requiredValueSelectionTagIds) {
                // Ignore "close after one" if tag requires value.
                updateViewData()
            } else {
                onTagSelected()
            }
        }
    }

    fun onCategorySpecialClick(viewData: CategoryAddViewData) {
        when (viewData.type) {
            is CategoryAddViewData.Type.ShowAll -> viewModelScope.launch {
                val current = prefsInteractor.getIsShowAllTagsEnabled()
                prefsInteractor.setIsShowAllTagsEnabled(!current)
                updateViewData()
            }
            is CategoryAddViewData.Type.EnableSearch -> viewModelScope.launch {
                val current = prefsInteractor.getIsTagSearchEnabled()
                prefsInteractor.setIsTagSearchEnabled(!current)
                updateViewData()
            }
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            saveClicked()
        }
    }

    fun onCommentClick(item: RecordCommentViewData) {
        if (item.text != newComment) {
            newComment = item.text
            updateViewData()
        }
    }

    fun onCommentFilterClick(item: FilterViewData) = viewModelScope.launch {
        val data = item.type as? CommentFilterTypeViewData ?: return@launch
        val type = recordCommentSearchViewDataInteractor.map(data)
        val newFilters = prefsInteractor.getHiddenCommentFilters().toMutableSet()
        newFilters.addOrRemove(type)
        prefsInteractor.setHiddenCommentFilters(newFilters.toSet())
        updateViewData()
    }

    fun onCommentChange(viewData: CommentFieldViewData, text: String) {
        when (viewData.type) {
            is CommentFieldViewData.Type.Default -> {
                if (text != newComment) {
                    newComment = text
                    updateViewData(fromCommentChange = true)
                }
            }
            is RecordTagViewDataInteractor.CommentFieldType -> {
                if (text != tagSearchText) {
                    tagSearchText = text
                    // Do not delay on clear.
                    updateViewData(fromSearchChange = true, delayUpdate = text.isNotEmpty())
                }
            }
        }
    }

    private suspend fun onTagSelected() {
        if (isMultipleChoiceAvailable) updateViewData() else saveClicked()
    }

    private suspend fun saveClicked() {
        addRunningRecordMediator.startTimer(
            typeId = extra.typeId,
            tags = newTags,
            comment = newComment,
            useSelectedTags = true,
        )
        addTagToTypeIfNotExistMediator.execute(
            typeId = extra.typeId,
            tagIds = newTags.map(RecordBase.Tag::tagId),
        )
        saveClicked.set(Unit)
    }

    private suspend fun initializeData() {
        if (initialDataLoaded) return
        newTags = extra.preselectedTags.map(RecordTagParam::toModel)
        isMultipleChoiceAvailable = isMultipleTagChoiceAvailableInteractor.execute(
            typeId = extra.typeId,
            closeAfterOne = prefsInteractor.getRecordTagSelectionCloseAfterOne(),
            excludedActivities = prefsInteractor.getCloseAfterOneTagExcludeActivities().toSet(),
        )
        updateButtonVisibility()
        initialDataLoaded = true
        startRequiredTagValueSelectionIfNeeded()
    }

    private fun updateButtonVisibility() {
        saveButtonVisibility.set(loadButtonVisibility())
    }

    private fun loadButtonVisibility(): Boolean {
        val showTags = RecordTagSelectionParams.FieldParam.Tags in extra.fields
        val showCommentInput = RecordTagSelectionParams.FieldParam.Comment in extra.fields

        return when {
            showTags -> isMultipleChoiceAvailable || extra.preselectedTags.isNotEmpty()
            showCommentInput -> true
            else -> false
        }
    }

    private fun updateViewData(
        fromCommentChange: Boolean = false,
        fromSearchChange: Boolean = false,
        delayUpdate: Boolean = false,
    ) {
        searchLoadJob?.cancel()
        searchLoadJob = viewModelScope.launch {
            if (delayUpdate) delay(500)
            val data = loadViewData(
                fromCommentChange = fromCommentChange,
                fromSearchChange = fromSearchChange,
            )
            viewData.set(data)
        }
    }

    private suspend fun loadViewData(
        fromCommentChange: Boolean,
        fromSearchChange: Boolean,
    ): List<ViewHolderType> {
        initializeData()

        return viewDataInteractor.getViewData(
            extra = extra,
            selectedTags = newTags,
            multipleChoiceAvailable = isMultipleChoiceAvailable,
            comment = newComment,
            tagSearch = tagSearchText,
            fromCommentChange = fromCommentChange,
            fromSearchChange = fromSearchChange,
        )
    }

    private fun isRequiredTagValueSelectionMissingValue(tagId: Long): Boolean {
        return newTags.any { it.tagId == tagId && it.numericValue == null }
    }

    // TODO VALUE TAG don't show tag selection dialog, show value dialog directly
    // TODO VALUE TAG add check retroactive mode to loaded preselected tags?
    private suspend fun startRequiredTagValueSelectionIfNeeded() {
        val nextRequiredTagId = extra.requiredValueSelectionTagIds
            .firstOrNull { isRequiredTagValueSelectionMissingValue(it) }
            ?: return
        delay(300)
        requestTagValueSelection(
            tagId = nextRequiredTagId,
            tagName = recordTagInteractor.get(nextRequiredTagId)?.name,
        )
    }

    private fun requestTagValueSelection(
        tagId: Long,
        tagName: String?,
    ) {
        RecordTagValueSelectionParams(
            tag = RECORD_TAG_SELECTION_TAG_VALUE_SELECTION,
            title = tagName,
            tagId = tagId,
        ).let(router::navigate)
    }

    companion object {
        private const val RECORD_TAG_SELECTION_TAG_VALUE_SELECTION = "RECORD_TAG_SELECTION_TAG_VALUE_SELECTION"
    }
}
