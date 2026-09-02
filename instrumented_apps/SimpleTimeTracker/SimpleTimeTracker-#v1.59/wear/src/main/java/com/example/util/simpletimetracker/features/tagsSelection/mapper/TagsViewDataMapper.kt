/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagsSelection.mapper

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.core.ErrorStateMapper
import com.example.util.simpletimetracker.data.WearResourceRepo
import com.example.util.simpletimetracker.domain.model.WearTag
import com.example.util.simpletimetracker.domain.model.WearRecordTag
import com.example.util.simpletimetracker.domain.recordTag.RecordTagValueFormatMapper
import com.example.util.simpletimetracker.features.tagsSelection.screen.TagListState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagChipState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionButtonState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionHintState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagsLoadingState
import com.example.util.simpletimetracker.presentation.theme.ColorActive
import com.example.util.simpletimetracker.presentation.theme.ColorInactive
import javax.inject.Inject

class TagsViewDataMapper @Inject constructor(
    private val resourceRepo: WearResourceRepo,
    private val errorStateMapper: ErrorStateMapper,
    private val recordTagValueFormatMapper: RecordTagValueFormatMapper,
) {

    fun mapErrorState(): TagListState.Error {
        return TagListState.Error(errorStateMapper.map())
    }

    fun mapState(
        tags: List<WearTag>,
        selectedTags: List<WearRecordTag>,
        loadingState: TagsLoadingState,
        multipleChoiceAvailable: Boolean,
    ): TagListState {
        val listState = if (tags.isEmpty()) {
            mapEmptyState()
        } else {
            mapContentState(
                tags = tags,
                selectedTags = selectedTags,
                loadingState = loadingState,
                multipleChoiceAvailable = multipleChoiceAvailable,
            )
        }

        return listState
    }

    private fun mapEmptyState(): TagListState.Empty {
        return TagListState.Empty(R.string.change_record_categories_empty)
    }

    private fun mapContentState(
        tags: List<WearTag>,
        selectedTags: List<WearRecordTag>,
        loadingState: TagsLoadingState,
        multipleChoiceAvailable: Boolean,
    ): TagListState.Content {
        val selectedTagsMap = selectedTags.associateBy { it.tagId }
        val selectedTagIds = selectedTagsMap.keys
        val mode = if (multipleChoiceAvailable) {
            TagChipState.TagSelectionMode.MULTI
        } else {
            TagChipState.TagSelectionMode.SINGLE
        }

        val tagItems = tags.map {
            it.id to mapTag(
                tag = it,
                loadingState = loadingState,
                selectedTagsMap = selectedTagsMap,
                selectedTagIds = selectedTagIds,
                mode = mode,
            )
        }
        val items = mutableListOf<TagListState.Item>()
        if (multipleChoiceAvailable) {
            items += tagItems.map { it.second }
        } else {
            val selected = tagItems.filter { it.first in selectedTagIds }.map { it.second }
            val available = tagItems.filter { it.first !in selectedTagIds }.map { it.second }
            if (selectedTags.isNotEmpty()) {
                val hintData = TagSelectionHintState(resourceRepo.getString(R.string.something_preselected))
                items += TagListState.Item.Hint(hintData)
            }
            items += selected
            if (selected.isNotEmpty() && available.isNotEmpty()) {
                items += TagListState.Item.Divider
            }
            items += available
        }

        val buttons = mutableListOf<TagListState.Item>()
        buttons += mapButton(
            textResId = R.string.change_record_untagged,
            color = ColorInactive,
            buttonType = TagListState.Item.ButtonType.Untagged,
            loadingState = loadingState,
        )
        if (multipleChoiceAvailable || selectedTags.isNotEmpty()) {
            buttons += mapButton(
                textResId = R.string.duration_dialog_save,
                color = ColorActive,
                buttonType = TagListState.Item.ButtonType.Complete,
                loadingState = loadingState,
            )
        }

        return TagListState.Content(
            items = items + buttons,
        )
    }

    private fun mapTag(
        tag: WearTag,
        loadingState: TagsLoadingState,
        selectedTagsMap: Map<Long, WearRecordTag>,
        selectedTagIds: Set<Long>,
        mode: TagChipState.TagSelectionMode,
    ): TagListState.Item.Tag {
        val isLoading = (loadingState as? TagsLoadingState.LoadingTag)
            ?.tagId == tag.id

        return TagListState.Item.Tag(
            tag = TagChipState(
                id = tag.id,
                name = tag.name,
                value = selectedTagsMap[tag.id]?.numericValue
                    ?.let(recordTagValueFormatMapper::map).orEmpty(),
                color = tag.color,
                checked = tag.id in selectedTagIds,
                mode = mode,
                isLoading = isLoading,
            ),
        )
    }

    private fun mapButton(
        @StringRes textResId: Int,
        color: Color,
        buttonType: TagListState.Item.ButtonType,
        loadingState: TagsLoadingState,
    ): TagListState.Item.Button {
        val isLoading = (loadingState as? TagsLoadingState.LoadingButton)
            ?.buttonType == buttonType

        return TagListState.Item.Button(
            TagSelectionButtonState(
                text = resourceRepo.getString(textResId),
                color = color,
                buttonType = buttonType,
                isLoading = isLoading,
            ),
        )
    }
}