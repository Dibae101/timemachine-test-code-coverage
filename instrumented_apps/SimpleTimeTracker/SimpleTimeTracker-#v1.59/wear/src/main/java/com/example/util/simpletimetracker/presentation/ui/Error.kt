/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.presentation.layout.ScaffoldedScrollingColumn
import com.example.util.simpletimetracker.utils.getString

data class ErrorState(
    @StringRes val messageResId: Int,
)

fun ScalingLazyListScope.renderError(
    state: ErrorState,
    spacedBy: Dp = 0.dp,
    onRefresh: () -> Unit = {},
) {
    item {
        Icon(
            painter = painterResource(R.drawable.wear_connection_error),
            contentDescription = null,
            modifier = Modifier.padding(
                bottom = spacedBy,
            ),
        )
    }
    item {
        Text(
            text = getString(stringResId = state.messageResId),
            modifier = Modifier.padding(
                start = 8.dp,
                end = 8.dp,
                bottom = spacedBy,
            ),
            textAlign = TextAlign.Center,
        )
    }
    item {
        RefreshButton(onRefresh)
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    ScaffoldedScrollingColumn {
        renderError(
            state = ErrorState(R.string.wear_loading_error),
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, fontScale = 2f)
@Composable
private fun PreviewFontScale() {
    ScaffoldedScrollingColumn {
        renderError(
            state = ErrorState(R.string.wear_loading_error),
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun SpacedBy() {
    ScaffoldedScrollingColumn(
        spacedBy = 0.dp,
    ) {
        renderError(
            state = ErrorState(R.string.wear_loading_error),
            spacedBy = 10.dp,
        )
    }
}