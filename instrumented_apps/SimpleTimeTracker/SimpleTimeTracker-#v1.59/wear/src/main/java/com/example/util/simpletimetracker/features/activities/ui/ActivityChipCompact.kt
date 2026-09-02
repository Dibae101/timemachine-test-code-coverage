/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.activities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.domain.model.WearActivityIcon
import com.example.util.simpletimetracker.presentation.remember.rememberDurationSince
import com.example.util.simpletimetracker.utils.scaledSp
import com.example.util.simpletimetracker.utils.durationToLabelShort
import java.time.Instant

@Immutable
data class ActivityChipCompatState(
    val id: Long,
    val icon: WearActivityIcon,
    val color: Long,
    val type: ActivityChipType = ActivityChipType.Base,
    val timeHint: ActivityChipState.TimeHint = ActivityChipState.TimeHint.None,
    val timeHint2: ActivityChipState.TimeHint = ActivityChipState.TimeHint.None,
    val isLoading: Boolean = false,
)

@Composable
fun ActivityChipCompact(
    modifier: Modifier = Modifier,
    state: ActivityChipCompatState,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier,
    ) {
        Button(
            modifier = Modifier.fillMaxSize(),
            content = {
                if (!state.isLoading) {
                    ActivityIcon(
                        activityIcon = state.icon,
                        modifier = Modifier.fillMaxSize(0.5f),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(0.5f),
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(state.color),
            ),
            onClick = onClick,
        )
        TimeHint(state.timeHint, Alignment.TopCenter)
        TimeHint(state.timeHint2, Alignment.BottomCenter)
    }
}

@Composable
private fun BoxScope.TimeHint(
    timeHint: ActivityChipState.TimeHint,
    alignment: Alignment,
) {
    when (timeHint) {
        is ActivityChipState.TimeHint.Timer -> {
            val startedDiff = rememberDurationSince(timeHint.startedAt)
            durationToLabelShort(startedDiff)
        }
        is ActivityChipState.TimeHint.Duration -> {
            val timestamp = java.time.Duration.ofMillis(timeHint.millis)
            durationToLabelShort(timestamp)
        }
        is ActivityChipState.TimeHint.None -> {
            null
        }
    }?.let {
        Box(
            modifier = Modifier
                .align(alignment)
                .clip(CircleShape)
                .background(color = Color.Black.copy(alpha = .7F)),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 2.dp),
                text = it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.scaledSp(),
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("🎉"),
            color = 0xFF123456,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewText() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("Zzzz"),
            color = 0xFFABCDEF,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewIcon() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Image(R.drawable.ic_hotel_24px),
            color = 0xFFABCDEF,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewLoading() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Image(R.drawable.ic_hotel_24px),
            color = 0xFFABCDEF,
            isLoading = true,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewRunning() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("🎉"),
            color = 0xFF123456,
            timeHint = ActivityChipState.TimeHint.Timer(
                Instant.now().toEpochMilli() - 36500000,
            ),
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND, fontScale = 2f)
@Composable
private fun PreviewRunningFontScale() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("🎉"),
            color = 0xFF123456,
            timeHint = ActivityChipState.TimeHint.Timer(
                Instant.now().toEpochMilli() - 36500000,
            ),
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewRunningLoading() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("🎉"),
            color = 0xFF123456,
            timeHint = ActivityChipState.TimeHint.Timer(
                Instant.now().toEpochMilli() - 36500000,
            ),
            isLoading = true,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewDuration() {
    ActivityChipCompact(
        modifier = Modifier.size(48.dp),
        state = ActivityChipCompatState(
            id = 0,
            icon = WearActivityIcon.Text("🎉"),
            color = 0xFF123456,
            timeHint = ActivityChipState.TimeHint.Timer(
                Instant.now().toEpochMilli() - 3650000,
            ),
            timeHint2 = ActivityChipState.TimeHint.Duration(
                36500000,
            ),
        ),
    )
}
