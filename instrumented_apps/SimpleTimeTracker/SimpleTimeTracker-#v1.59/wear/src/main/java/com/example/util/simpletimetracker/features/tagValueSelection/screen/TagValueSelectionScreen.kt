/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagValueSelection.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.simpletimetracker.features.tagValueSelection.viewModel.TagValueSelectionViewModel
import com.example.util.simpletimetracker.features.tagValueSelection.viewModel.TagValueSelectionViewModel.Effect
import com.example.util.simpletimetracker.utils.collectEffects

@Composable
fun TagValueSelectionScreen(
    tagId: Long,
    onComplete: () -> Unit,
) {
    val viewModel = hiltViewModel<TagValueSelectionViewModel>()
    viewModel.init(tagId)
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effects.collectEffects(key = viewModel) {
        when (it) {
            is Effect.OnComplete -> onComplete()
        }
    }

    TagValueSelection(
        state = state,
        onButtonClick = viewModel::onButtonClick,
    )
}
