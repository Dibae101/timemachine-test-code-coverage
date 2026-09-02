/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagsSelection.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices

@Immutable
data class TagSelectionHintState(
    val text: String,
)

@Composable
fun TagSelectionHint(
    state: TagSelectionHintState,
) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.text,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            lineHeight = 11.sp,
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    TagSelectionHint(
        state = TagSelectionHintState(
            text = "Hint text",
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewLong() {
    TagSelectionHint(
        state = TagSelectionHintState(
            text = "Hint text hint text hint text hint text",
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND, fontScale = 2f)
@Composable
private fun FontScaled() {
    TagSelectionHint(
        state = TagSelectionHintState(
            text = "Hint text",
        ),
    )
}
