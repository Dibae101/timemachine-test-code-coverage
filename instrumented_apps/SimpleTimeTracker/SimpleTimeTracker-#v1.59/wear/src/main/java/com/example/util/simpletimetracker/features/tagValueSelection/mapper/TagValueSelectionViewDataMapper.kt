/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.features.tagValueSelection.mapper

import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.data.WearResourceRepo
import com.example.util.simpletimetracker.features.tagValueSelection.screen.TagValueSelectionState
import javax.inject.Inject

class TagValueSelectionViewDataMapper @Inject constructor(
    private val resourceRepo: WearResourceRepo,
) {

    fun mapState(
        currentValue: String,
    ): TagValueSelectionState {
        val value = if (currentValue.isEmpty()) {
            resourceRepo.getString(R.string.change_record_type_value_selection_hint)
        } else {
            currentValue
        }

        return TagValueSelectionState(
            value = value,
        )
    }
}