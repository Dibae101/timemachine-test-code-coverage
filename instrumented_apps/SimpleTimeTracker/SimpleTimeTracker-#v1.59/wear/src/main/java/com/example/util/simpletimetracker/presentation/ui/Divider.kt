/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.presentation.theme.ColorActive

@Composable
fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(ColorActive),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun DividerPreview() {
    Divider()
}
