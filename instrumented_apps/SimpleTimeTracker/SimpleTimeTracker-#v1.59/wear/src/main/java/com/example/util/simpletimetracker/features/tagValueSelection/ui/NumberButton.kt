/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagValueSelection.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.domain.model.WearActivityIcon
import com.example.util.simpletimetracker.features.activities.ui.ActivityIcon
import com.example.util.simpletimetracker.presentation.theme.ColorInactive

@Composable
fun NumberButton(
    modifier: Modifier = Modifier,
    icon: WearActivityIcon,
    color: Color,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Button(
            modifier = Modifier
                .fillMaxSize(),
            content = {
                ActivityIcon(
                    activityIcon = icon,
                    modifier = Modifier.fillMaxSize(0.5f),
                )
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = color,
            ),
            onClick = onClick,
        )
    }
}

@Composable
fun NumberEmpty(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewText() {
    NumberButton(
        modifier = Modifier.size(48.dp),
        color = ColorInactive,
        icon = WearActivityIcon.Text("1"),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewIcon() {
    NumberButton(
        modifier = Modifier.size(48.dp),
        color = ColorInactive,
        icon = WearActivityIcon.Image(R.drawable.wear_check_mark),
    )
}