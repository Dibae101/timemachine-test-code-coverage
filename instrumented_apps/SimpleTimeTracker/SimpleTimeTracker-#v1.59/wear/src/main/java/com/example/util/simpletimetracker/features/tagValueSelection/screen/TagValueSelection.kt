/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagValueSelection.screen

import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.TextViewCompat
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.domain.base.TAG_VALUE_DECIMAL_DELIMITER
import com.example.util.simpletimetracker.domain.model.WearActivityIcon
import com.example.util.simpletimetracker.features.tagValueSelection.ui.NumberButton
import com.example.util.simpletimetracker.features.tagValueSelection.ui.NumberEmpty
import com.example.util.simpletimetracker.presentation.theme.ColorInactive
import com.example.util.simpletimetracker.presentation.theme.ColorPositive
import com.example.util.simpletimetracker.presentation.ui.Divider

data class TagValueSelectionState(
    val value: String,
)

sealed interface TagValueSelectionButton {
    data class Number(val value: Int) : TagValueSelectionButton
    data object Dot : TagValueSelectionButton
    data object PlusMinus : TagValueSelectionButton
    data object Delete : TagValueSelectionButton
    data object Save : TagValueSelectionButton
}

@Composable
fun TagValueSelection(
    state: TagValueSelectionState,
    onButtonClick: (TagValueSelectionButton) -> Unit = {},
) {
    Scaffold(
        vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        },
    ) {
        Box(
            modifier = Modifier
                .padding(
                    top = 16.dp,
                    bottom = 16.dp,
                )
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val textColor = LocalContentColor.current.toArgb()
                AndroidView(
                    factory = { ctx ->
                        val view = AppCompatTextView(ctx)
                        view.gravity = Gravity.CENTER
                        view.setTextColor(textColor)
                        view.textSize = 24f
                        view.maxLines = 1
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            view,
                            1,
                            100,
                            1,
                            TypedValue.COMPLEX_UNIT_SP,
                        )
                        view
                    },
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .height(32.dp)
                        .fillMaxWidth(),
                    update = {
                        it.text = state.value
                    },
                )
                Divider()
                val buttonModifier = Modifier
                    .weight(1f)
                Row(
                    modifier = Modifier
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        TagValueSelectionButton.Number(7),
                        TagValueSelectionButton.Number(8),
                        TagValueSelectionButton.Number(9),
                        TagValueSelectionButton.Number(0),
                    ).forEach {
                        TagValueSelectionButton(
                            modifier = buttonModifier,
                            button = it,
                            onClick = onButtonClick,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        TagValueSelectionButton.Number(4),
                        TagValueSelectionButton.Number(5),
                        TagValueSelectionButton.Number(6),
                        TagValueSelectionButton.Dot,
                    ).forEach {
                        TagValueSelectionButton(
                            modifier = buttonModifier,
                            button = it,
                            onClick = onButtonClick,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        TagValueSelectionButton.Number(1),
                        TagValueSelectionButton.Number(2),
                        TagValueSelectionButton.Number(3),
                        TagValueSelectionButton.PlusMinus,
                    ).forEach {
                        TagValueSelectionButton(
                            modifier = buttonModifier,
                            button = it,
                            onClick = onButtonClick,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberEmpty(
                        modifier = buttonModifier,
                    )
                    TagValueSelectionButton(
                        modifier = buttonModifier,
                        button = TagValueSelectionButton.Save,
                        color = ColorPositive,
                        onClick = onButtonClick,
                    )
                    TagValueSelectionButton(
                        modifier = buttonModifier,
                        button = TagValueSelectionButton.Delete,
                        onClick = onButtonClick,
                    )
                    NumberEmpty(
                        modifier = buttonModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagValueSelectionButton(
    modifier: Modifier,
    color: Color = ColorInactive,
    button: TagValueSelectionButton,
    onClick: (TagValueSelectionButton) -> Unit,
) {
    val icon = when (button) {
        is TagValueSelectionButton.Number ->
            WearActivityIcon.Text(button.value.toString())
        is TagValueSelectionButton.Dot ->
            WearActivityIcon.Text(TAG_VALUE_DECIMAL_DELIMITER.toString())
        is TagValueSelectionButton.PlusMinus ->
            WearActivityIcon.Text("+/−")
        is TagValueSelectionButton.Delete ->
            WearActivityIcon.Image(R.drawable.wear_backspace)
        is TagValueSelectionButton.Save ->
            WearActivityIcon.Image(R.drawable.wear_check_mark)
    }
    val onItemClick = remember(icon) {
        { onClick(button) }
    }
    NumberButton(
        modifier = modifier,
        icon = icon,
        color = color,
        onClick = onItemClick,
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    val state = TagValueSelectionState(
        value = "Enter new value",
    )
    TagValueSelection(
        state = state,
    )
}