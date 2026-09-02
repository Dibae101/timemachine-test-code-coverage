package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.mapper.CategoryViewDataMapper
import com.example.util.simpletimetracker.core.mapper.CommonViewDataMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.extension.addBetweenEach
import com.example.util.simpletimetracker.domain.extension.plusAssign
import com.example.util.simpletimetracker.domain.extension.search
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.interactor.GetSelectableTagsInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_base_adapter.divider.DividerViewData
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.EmptySpaceViewData
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import javax.inject.Inject

class RecordTagViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val getSelectableTagsInteractor: GetSelectableTagsInteractor,
    private val categoryViewDataMapper: CategoryViewDataMapper,
    private val commonViewDataMapper: CommonViewDataMapper,
) {

    // TODO also change selection in other places (add Selected / Available hints etc).
    // TODO remove InfoViewData and mapSelectedHint in CommonViewDataMapper.
    // TODO add shortcut to start timer from tag selection when activity selection.
    // typeId is empty - show all tags.
    suspend fun getViewData(
        selectedTags: List<RecordBase.Tag>,
        typeIds: List<Long>,
        multipleChoiceAvailable: Boolean,
        showBigEmptyHint: Boolean,
        showHint: Boolean,
        showArchived: Boolean,
        searchText: String,
        fromSearchChange: Boolean,
        buttons: List<Button>,
    ): Result {
        fun List<RecordTag>.filterArchived(): List<RecordTag> {
            return if (showArchived) this else this.filterNot { it.archived }
        }

        val isDarkTheme = prefsInteractor.getDarkMode()
        val isSearchEnabled = prefsInteractor.getIsTagSearchEnabled() && Button.SEARCH in buttons
        val showAllTags = prefsInteractor.getIsShowAllTagsEnabled() && Button.ALL_TAGS in buttons
        val allTags = recordTagInteractor.getAll().filterArchived()
        val showAddButton = Button.ADD in buttons

        if (allTags.isEmpty()) {
            return mapEmpty(
                showBigEmptyHint = showBigEmptyHint,
                showAddButton = showAddButton,
                isDarkTheme = isDarkTheme,
            )
        }

        val recordTags = getSelectableTagsInteractor.execute(*typeIds.toLongArray()).filterArchived()
        val recordTagIds = recordTags.map { it.id }
        val types = recordTypeInteractor.getAll().associateBy { it.id }
        val actualSearchText = if (isSearchEnabled) searchText else ""
        val selectedTagsMap = selectedTags.associateBy { it.tagId }
        val selectedTagIds = selectedTagsMap.keys
        val selectedBeforeSearch = allTags
            .filter { it.id in selectedTagIds }
        val selected = selectedBeforeSearch
            .search(text = actualSearchText) { name }
        val available = recordTags
            .filter { it.id !in selectedTagIds }
            .search(text = actualSearchText) { name }
        val tagsFromOtherActivities = allTags
            .filter { it.id !in recordTagIds }
            .search(text = actualSearchText) { name }
        val availableFromOtherActivities = if (showAllTags) {
            tagsFromOtherActivities.filter { it.id !in selectedTagIds }
        } else {
            emptyList()
        }

        // Dummy
        val emptyViewData = listOf(
            EmptySpaceViewData(
                id = 0,
                width = EmptySpaceViewData.ViewDimension.MatchParent,
            ),
        )

        val searchViewData = if (isSearchEnabled) {
            CommentFieldViewData(
                id = "tag_selection_search".hashCode().toLong(),
                text = if (fromSearchChange) null else actualSearchText,
                marginTopDp = -3,
                marginHorizontal = 8,
                hint = resourceRepo.getString(R.string.search_hint),
                valueType = CommentFieldViewData.ValueType.TextSingleLine,
                type = CommentFieldType,
            ).let(::listOf)
        } else {
            emptyList()
        }

        // Hint
        val hintViewData = if (showHint && selected.isEmpty() && !isSearchEnabled) {
            listOf(categoryViewDataMapper.mapToRecordTagHint())
        } else {
            emptyList()
        }

        // Selected
        val selectedViewData = mutableListOf<ViewHolderType>()
        if (selected.isNotEmpty()) {
            selectedViewData += if (multipleChoiceAvailable) {
                commonViewDataMapper.mapSelected()
            } else {
                commonViewDataMapper.mapPreselected()
            }
            selectedViewData += selected.map {
                categoryViewDataMapper.mapRecordTagWithValue(
                    tag = it,
                    tagData = selectedTagsMap[it.id],
                    types = types,
                    isDarkTheme = isDarkTheme,
                )
            }
        }

        // Available
        val availableViewData = mutableListOf<ViewHolderType>()
        if (available.isNotEmpty()) {
            availableViewData += commonViewDataMapper.mapAvailable()
        }
        categoryViewDataMapper.groupToTagGroups(available).forEach { (groupName, tags) ->
            if (groupName.isNotEmpty()) {
                availableViewData += HintViewData(
                    text = groupName,
                    paddingTop = 0,
                    paddingBottom = 0,
                )
            }

            availableViewData += tags.map {
                categoryViewDataMapper.mapRecordTag(
                    tag = it,
                    types = types,
                    isDarkTheme = isDarkTheme,
                )
            }
        }

        // From other
        val availableFromOtherViewData = mutableListOf<ViewHolderType>()
        if (availableFromOtherActivities.isNotEmpty()) {
            availableFromOtherViewData += HintViewData(
                text = resourceRepo.getString(R.string.change_record_tag_from_other_activity),
                paddingTop = 0,
                paddingBottom = 0,
            )

            availableFromOtherViewData += availableFromOtherActivities.map {
                categoryViewDataMapper.mapRecordTag(
                    tag = it,
                    types = types,
                    isDarkTheme = isDarkTheme,
                )
            }
        }

        // Buttons
        val buttonsViewData = mutableListOf<ViewHolderType>()
        if (Button.UNTAGGED in buttons) {
            buttonsViewData += categoryViewDataMapper.mapToUntaggedItem(
                isDarkTheme = isDarkTheme,
                isFiltered = false,
            )
        }
        if (Button.ALL_TAGS in buttons && tagsFromOtherActivities.isNotEmpty()) {
            buttonsViewData += categoryViewDataMapper.mapToRecordTagShowAllItem(
                isEnabled = showAllTags,
                isDarkTheme = isDarkTheme,
            )
        }
        if (Button.SEARCH in buttons) {
            buttonsViewData += categoryViewDataMapper.mapToTagSearchItem(
                isEnabled = isSearchEnabled,
                isDarkTheme = isDarkTheme,
            )
        }
        if (showAddButton) {
            buttonsViewData += categoryViewDataMapper.mapToRecordTagAddItem(
                useShortName = true,
                isDarkTheme = isDarkTheme,
            )
        }

        // All
        val viewData = listOf(
            searchViewData to true,
            hintViewData to true,
            selectedViewData to true,
            availableViewData to true,
            availableFromOtherViewData to true,
            buttonsViewData to (multipleChoiceAvailable || availableFromOtherViewData.isNotEmpty()),
        ).filter {
            it.first.isNotEmpty()
        }.addBetweenEach(
            map = { it.first },
            spacingProducer = { index, _, second ->
                if (second?.second == true) {
                    listOf(DividerViewData("divider_$index".hashCode().toLong()))
                } else {
                    null
                }
            },
        ).flatten().takeIf {
            it.isNotEmpty()
        }?.let {
            // Add empty invisible item, otherwise when HintViewData as a first item disappears,
            // whole list collapses to zero height.
            emptyViewData + it
        } ?: listOf(commonViewDataMapper.mapSelectedHint(isEmpty = true))

        return Result(
            selectedCount = selectedBeforeSearch.size,
            data = viewData,
        )
    }

    private suspend fun mapEmpty(
        showBigEmptyHint: Boolean,
        showAddButton: Boolean,
        isDarkTheme: Boolean,
    ): Result {
        val viewData = mutableListOf<ViewHolderType>()
        viewData += if (showBigEmptyHint && recordTagInteractor.isEmpty()) {
            categoryViewDataMapper.mapToTagsFirstHint()
        } else {
            categoryViewDataMapper.mapToRecordTagsEmpty()
        }
        if (showAddButton) {
            viewData += categoryViewDataMapper.mapToRecordTagAddItem(
                useShortName = true,
                isDarkTheme = isDarkTheme,
            )
        }
        return Result(
            selectedCount = 0,
            data = viewData,
        )
    }

    data class Result(
        val selectedCount: Int,
        val data: List<ViewHolderType>,
    )

    enum class Button {
        ADD,
        UNTAGGED,
        ALL_TAGS,
        SEARCH,
    }

    data object CommentFieldType : CommentFieldViewData.Type
}
