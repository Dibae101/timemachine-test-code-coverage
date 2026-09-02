/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.presentation.datePicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.util.simpletimetracker.presentation.datePicker.WearDatePickerViewModel.Effect
import com.example.util.simpletimetracker.utils.collectEffects
import com.google.android.horologist.composables.DatePicker
import java.time.LocalDate

@Composable
fun WearDatePicker(
    date: LocalDate,
    onComplete: () -> Unit = {},
) {
    val viewModel = hiltViewModel<WearDatePickerViewModel>()

    viewModel.effects.collectEffects(key = viewModel) {
        when (it) {
            is Effect.OnComplete -> onComplete()
        }
    }

    DatePicker(
        date = date,
        onDateConfirm = viewModel::onDateConfirm,
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun Preview() {
    WearDatePicker(
        date = LocalDate.now(),
    )
}

@Preview(device = WearDevices.LARGE_ROUND, fontScale = 2f)
@Composable
private fun PreviewFontScale() {
    WearDatePicker(
        date = LocalDate.now(),
    )
}