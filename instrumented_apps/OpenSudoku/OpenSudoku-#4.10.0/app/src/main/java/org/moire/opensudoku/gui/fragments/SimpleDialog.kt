/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2025 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import org.moire.opensudoku.R
import org.moire.opensudoku.utils.getSerializableUniversal
import java.io.Serial
import java.io.Serializable

class DialogParams : Serializable {
	var resultKey: String? = null
	@StringRes var messageId: Int = 0
	var message: String? = null
	@DrawableRes var iconId: Int = 0
	@StringRes var titleId: Int = R.string.app_name
	var title: String? = null
	@StringRes var positiveButtonString: Int = android.R.string.ok
	@StringRes var negativeButtonString: Int = android.R.string.cancel
	@StringRes var neutralButtonString: Int = android.R.string.cancel
	var isPositiveButtonCallback: Boolean = false
	var isNegativeButtonCallback: Boolean = false
	var isOnDismissCallback: Boolean = false

	companion object {
		@Serial private const val serialVersionUID: Long = 6849659877554238507L
	}
}

class SimpleDialog(params: DialogParams? = null) : DialogFragment(), DialogInterface.OnClickListener {
	private var params: DialogParams

	@Suppress("HardCodedStringLiteral") private val onPositiveButtonResultKey
		get() = "${params.resultKey}:POSITIVE"
	@Suppress("HardCodedStringLiteral") private val onNegativeButtonResultKey
		get() = "${params.resultKey}:NEGATIVE"
	@Suppress("HardCodedStringLiteral") private val onDismissResultKey
		get() = "${params.resultKey}:DISMISS"

	init {
		arguments = arguments ?: Bundle()
		this.params = params ?: DialogParams()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
        savedInstanceState?.getSerializableUniversal<DialogParams>("params")?.let { params = it }
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
		with(params) {
			builder.setIcon(iconId)

			if (title != null) {
				builder.setTitle(title)
			} else {
				builder.setTitle(titleId)
			}

			if (message != null) {
				builder.setMessage(message)
			} else {
				builder.setMessage(messageId)
			}

			if (params.isNegativeButtonCallback) {
				builder.setPositiveButton(positiveButtonString, this@SimpleDialog)
					.setNegativeButton(negativeButtonString, this@SimpleDialog)
					.setNeutralButton(neutralButtonString, null)
			} else if (params.isPositiveButtonCallback) {
				builder.setPositiveButton(positiveButtonString, this@SimpleDialog)
					.setNegativeButton(negativeButtonString, null)
			} else {
				builder.setPositiveButton(positiveButtonString, null)
			}
		}

		return builder.create()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		@Suppress("HardCodedStringLiteral")
		outState.putSerializable("params", params)
	}

	fun show(manager: FragmentManager) {
		super.show(manager, null)
	}

	fun show(manager: FragmentManager, @StringRes messageId: Int) {
		params.messageId = messageId
		params.message = null
		show(manager)
	}

	override fun show(manager: FragmentManager, message: String?) {
		params.message = message
		params.messageId = 0
		show(manager)
	}

	override fun onClick(dialog: DialogInterface?, whichButton: Int) {
		if (whichButton == BUTTON_POSITIVE && params.isPositiveButtonCallback) {
			setFragmentResult(onPositiveButtonResultKey, bundleOf())
		} else if (whichButton == BUTTON_NEGATIVE && params.isNegativeButtonCallback) {
			setFragmentResult(onNegativeButtonResultKey, bundleOf())
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		if (params.isOnDismissCallback) {
			setFragmentResult(onDismissResultKey, bundleOf())
		}
	}

	// should only be called from onCreate of an activity
	fun registerPositiveButtonCallback(activity: FragmentActivity, callback: () -> Unit) {
		require(params.resultKey != null)
		activity.supportFragmentManager.setFragmentResultListener(onPositiveButtonResultKey, activity) { _, _ -> callback() }
		params.isPositiveButtonCallback = true
	}

	// should only be called from onCreate of an activity
	fun registerNegativeButtonCallback(activity: FragmentActivity, callback: () -> Unit) {
		require(params.resultKey != null)
		activity.supportFragmentManager.setFragmentResultListener(onNegativeButtonResultKey, activity) { _, _ -> callback() }
		params.isNegativeButtonCallback = true
	}

	// should only be called from onCreate of an activity
	fun registerOnDismissCallback(activity: FragmentActivity, callback: (Bundle?) -> Unit) {
		require(params.resultKey != null)
		activity.supportFragmentManager.setFragmentResultListener(onDismissResultKey, activity) { _, _ -> callback(arguments) }
		params.isOnDismissCallback = true
	}
}
