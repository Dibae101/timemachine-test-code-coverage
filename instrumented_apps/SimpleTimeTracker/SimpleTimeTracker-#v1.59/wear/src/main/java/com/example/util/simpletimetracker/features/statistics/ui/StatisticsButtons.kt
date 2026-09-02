/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.statistics.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.presentation.theme.ColorInactive
import com.example.util.simpletimetracker.presentation.ui.ACTIVITY_VIEW_HEIGHT

@Composable
fun StatisticsButtons(
    modifier: Modifier = Modifier,
    spacedBy: Dp = 0.dp,
    onPrevClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    Row(modifier) {
        StatisticsButton(
            iconResId = R.drawable.wear_arrow_left,
            onClick = onPrevClick,
        )
        Spacer(
            modifier = Modifier.width(spacedBy),
        )
        StatisticsButton(
            iconResId = R.drawable.wear_arrow_right,
            onClick = onNextClick,
        )
    }
}

@Composable
private fun StatisticsButton(
    @DrawableRes iconResId: Int,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = Modifier.size(ACTIVITY_VIEW_HEIGHT.dp),
        onClick = onClick,
        content = {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
            )
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ColorInactive,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    StatisticsButtons(spacedBy = 16.dp)
}

@Preview(device = WearDevices.LARGE_ROUND, fontScale = 2f)
@Composable
private fun PreviewFontScale() {
    StatisticsButtons(spacedBy = 16.dp)
}
