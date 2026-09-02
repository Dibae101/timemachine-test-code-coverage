/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagsSelection.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagChip
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagChipState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionButton
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionButtonState
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionHint
import com.example.util.simpletimetracker.features.tagsSelection.ui.TagSelectionHintState
import com.example.util.simpletimetracker.presentation.layout.ScaffoldedScrollingColumn
import com.example.util.simpletimetracker.presentation.ui.Divider
import com.example.util.simpletimetracker.presentation.ui.ErrorState
import com.example.util.simpletimetracker.presentation.ui.renderError
import com.example.util.simpletimetracker.utils.getString

sealed interface TagListState {

    data object Loading : TagListState

    data class Error(
        val error: ErrorState,
    ) : TagListState

    data class Empty(
        @StringRes val messageResId: Int,
    ) : TagListState

    data class Content(
        val items: List<Item>,
    ) : TagListState

    sealed interface Item {
        data class Tag(
            val tag: TagChipState,
        ) : Item

        data class Button(
            val data: TagSelectionButtonState,
        ) : Item

        data class Hint(
            val data: TagSelectionHintState,
        ) : Item

        data object Divider : Item

        sealed interface ButtonType {
            object Complete : ButtonType
            object Untagged : ButtonType
        }
    }
}

@Composable
fun TagList(
    state: TagListState,
    onButtonClick: (TagListState.Item.ButtonType) -> Unit = {},
    onToggleClick: (Long) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    ScaffoldedScrollingColumn {
        when (state) {
            is TagListState.Loading -> item {
                RenderLoadingState()
            }
            is TagListState.Error -> {
                renderError(
                    state = state.error,
                    onRefresh = onRefresh,
                )
            }
            is TagListState.Empty -> item {
                RenderEmptyState(state)
            }
            is TagListState.Content -> {
                renderContentState(
                    state = state,
                    onButtonClick = onButtonClick,
                    onToggleClick = onToggleClick,
                )
            }
        }
    }
}

@Composable
private fun RenderLoadingState() {
    CircularProgressIndicator(
        modifier = Modifier.width(64.dp),
    )
}

@Composable
private fun RenderEmptyState(
    state: TagListState.Empty,
) {
    Text(
        text = getString(state.messageResId),
        modifier = Modifier.padding(8.dp),
    )
}

private fun ScalingLazyListScope.renderContentState(
    state: TagListState.Content,
    onButtonClick: (TagListState.Item.ButtonType) -> Unit = {},
    onToggleClick: (Long) -> Unit = {},
) {
    item { Spacer(Modifier) }
    for (itemState in state.items) {
        when (itemState) {
            is TagListState.Item.Tag -> item {
                TagChip(
                    state = itemState.tag,
                    onClick = onToggleClick,
                )
            }
            is TagListState.Item.Button -> item {
                TagSelectionButton(
                    state = itemState.data,
                    onClick = onButtonClick,
                )
            }
            is TagListState.Item.Hint -> item {
                TagSelectionHint(
                    state = itemState.data,
                )
            }
            is TagListState.Item.Divider -> item {
                Divider()
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Loading() {
    TagList(
        state = TagListState.Loading,
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Error() {
    TagList(
        state = TagListState.Error(
            ErrorState(R.string.wear_loading_error),
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun NoTags() {
    TagList(
        state = TagListState.Empty(R.string.change_record_categories_empty),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun WithSomeTags() {
    TagList(
        state = TagListState.Content(
            items = listOf(
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 123,
                        name = "Sleep",
                        value = "",
                        color = 0xFF123456,
                        checked = false,
                        mode = TagChipState.TagSelectionMode.SINGLE,
                    ),
                ),
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 124,
                        name = "Personal",
                        value = "",
                        color = 0xFF123456,
                        checked = false,
                        mode = TagChipState.TagSelectionMode.SINGLE,
                    ),
                ),
            ),
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun MultiSelectMode() {
    TagList(
        state = TagListState.Content(
            items = listOf(
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 123,
                        name = "Sleep",
                        value = "123.1",
                        color = 0xFF123456,
                        checked = true,
                        mode = TagChipState.TagSelectionMode.MULTI,
                    ),
                ),
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 124,
                        name = "Personal",
                        value = "",
                        color = 0xFF123456,
                        checked = false,
                        mode = TagChipState.TagSelectionMode.MULTI,
                    ),
                ),
            ),
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preselected() {
    TagList(
        state = TagListState.Content(
            items = listOf(
                TagListState.Item.Hint(
                    data = TagSelectionHintState(
                        text = "Preselected",
                    ),
                ),
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 123,
                        name = "Sleep",
                        value = "123.1",
                        color = 0xFF123456,
                        checked = true,
                        mode = TagChipState.TagSelectionMode.SINGLE,
                    ),
                ),
                TagListState.Item.Divider,
                TagListState.Item.Tag(
                    tag = TagChipState(
                        id = 124,
                        name = "Personal",
                        value = "",
                        color = 0xFF123456,
                        checked = false,
                        mode = TagChipState.TagSelectionMode.SINGLE,
                    ),
                ),
            ),
        ),
    )
}