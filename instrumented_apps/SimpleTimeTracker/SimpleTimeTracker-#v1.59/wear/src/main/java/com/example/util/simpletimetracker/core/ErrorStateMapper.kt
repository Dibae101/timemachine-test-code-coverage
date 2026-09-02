/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.core

import com.example.util.simpletimetracker.R
import com.example.util.simpletimetracker.presentation.ui.ErrorState
import javax.inject.Inject

class ErrorStateMapper @Inject constructor() {

    fun map(): ErrorState {
        return ErrorState(R.string.wear_loading_error)
    }
}