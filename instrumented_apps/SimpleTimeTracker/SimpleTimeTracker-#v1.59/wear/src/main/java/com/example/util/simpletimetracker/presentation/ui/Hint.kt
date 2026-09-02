/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices

@Immutable
data class HintState(
    val hint: String,
)

@Composable
fun Hint(
    state: HintState,
) {
    Text(
        text = state.hint,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Light,
        fontSize = 11.sp,
        lineHeight = 11.sp,
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun HintPreview() {
    Hint(
        state = HintState(
            hint = "Hint",
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun HintLongPreview() {
    Hint(
        state = HintState(
            hint = "Hint Hint Hint Hint Hint Hint Hint Hint Hint Hint Hint",
        ),
    )
}