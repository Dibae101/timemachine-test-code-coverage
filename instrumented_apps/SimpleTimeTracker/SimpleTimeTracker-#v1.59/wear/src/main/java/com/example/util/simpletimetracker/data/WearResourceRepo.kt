/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.data

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.example.util.simpletimetracker.core.repo.BaseResourceRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WearResourceRepo @Inject constructor(
    @ApplicationContext private val context: Context,
) : BaseResourceRepo {

    override fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }

    override fun getString(@StringRes stringResId: Int, vararg args: Any): String {
        return context.getString(stringResId, *args)
    }

    override fun getQuantityString(@PluralsRes stringResId: Int, quantity: Int): String {
        return context.resources.getQuantityString(stringResId, quantity)
    }

    override fun getQuantityString(@PluralsRes stringResId: Int, quantity: Int, vararg args: Any): String {
        return context.resources.getQuantityString(stringResId, quantity, *args)
    }
}